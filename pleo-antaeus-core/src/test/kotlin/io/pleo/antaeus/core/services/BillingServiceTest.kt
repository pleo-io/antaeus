package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceChargedException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.factories.createInvoice
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

val invoiceForNotRegisteredCustomer = createInvoice(id = 1, customerId = 1, status = InvoiceStatus.PENDING)
val invoiceWithCurrencyMismatch = createInvoice(
    id = 11,
    customerId = 2,
    status = InvoiceStatus.PENDING,
    amount = Money((10).toBigDecimal(), Currency.GBP)
)
val invoiceWithNetworkException = createInvoice(id = 21, customerId = 3, status = InvoiceStatus.PENDING)
val invoiceWillBeCharged = createInvoice(id = 31, customerId = 4, status = InvoiceStatus.PENDING)
val invoiceAlreadyCharged = createInvoice(id = 32, customerId = 4, status = InvoiceStatus.PAID)
val invoiceNotCharged = createInvoice(id = 41, customerId = 5, status = InvoiceStatus.PENDING)

class BillingServiceTest {
    private val tables = arrayOf(InvoiceTable, InvoicePaymentTable, CustomerTable)

    private val db by lazy { Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;", "org.h2.Driver", "root", "") }
    private val dal = AntaeusDal(db = db)

    private fun createBillingService(
        invoice: Invoice,
        chargeInvoiceStub: PaymentProvider.(invoice: Invoice) -> Unit = {
            coEvery { charge(it) } returns false
        },
        fetchInvoiceStub: InvoiceService.(invoice: Invoice) -> Unit = {
            coEvery { fetch(it.id) } returns it
        },
        fetchCustomerStub: CustomerService.(invoice: Invoice) -> Unit = {
            coEvery { fetch(it.customerId) } returns Customer(it.customerId, it.amount.currency)
        },
    ): BillingService {
        val paymentProvider = mockk<PaymentProvider> { chargeInvoiceStub(invoice) }
        val invoiceService = spyk(InvoiceService(dal)) { fetchInvoiceStub(invoice) }
        val customerService = mockk<CustomerService> { fetchCustomerStub(invoice) }

        return BillingService(paymentProvider, invoiceService, customerService)
    }

    @BeforeEach
    fun before() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // Drop all existing tables to ensure a clean slate on each run
            SchemaUtils.drop(*tables)
            // Create all tables
            SchemaUtils.create(*tables)
            runBlocking {
                setupInitialData(dal = dal)
            }
        }
    }

    @Test
    internal fun `fail to charge invoice for not existing customer`() = runBlocking {
        val billingService = createBillingService(
            invoice = invoiceForNotRegisteredCustomer,
            chargeInvoiceStub = { invoice ->
                coEvery { charge(invoice) } throws CustomerNotFoundException(invoice.customerId)
            }
        )
        assertThrows<CustomerNotFoundException> {
            billingService.chargeInvoice(invoiceForNotRegisteredCustomer)
        }
        assertInvoiceCharged(invoiceForNotRegisteredCustomer, paymentsNumber = 1, paymentsSuccess = false)
    }

    @Test
    internal fun `fail to charge invoice for billing with currency mismatch`() = runBlocking {
        val billingService = createBillingService(
            invoice = invoiceWithCurrencyMismatch,
            fetchCustomerStub = {
                coEvery { fetch(it.customerId) } returns Customer(it.customerId, Currency.EUR)
            }
        )
        assertThrows<CurrencyMismatchException> {
            billingService.chargeInvoice(invoiceWithCurrencyMismatch)
        }
        assertInvoiceCharged(invoiceWithCurrencyMismatch, paymentsNumber = 0, paymentsSuccess = false)
    }

    @Test
    internal fun `fail to charge invoice for billing with currency mismatch on payment provider side`() = runBlocking {
        val billingService = createBillingService(
            invoice = invoiceWithCurrencyMismatch,
            chargeInvoiceStub = {
                coEvery { charge(it) } throws CurrencyMismatchException(invoiceId = it.id, customerId = it.customerId)
            }
        )
        assertThrows<CurrencyMismatchException> {
            billingService.chargeInvoice(invoiceWithCurrencyMismatch)
        }
        assertInvoiceCharged(invoiceWithCurrencyMismatch, paymentsNumber = 1, paymentsSuccess = false)
    }

    @Test
    internal fun `fail to charge invoice when network exception occurs`() = runBlocking {
        val billingService = createBillingService(
            invoice = invoiceWithNetworkException,
            chargeInvoiceStub = {
                coEvery { charge(it) } throws NetworkException()
            }
        )
        assertThrows<NetworkException> {
            billingService.chargeInvoice(invoiceWithNetworkException)
        }
        assertInvoiceCharged(invoiceWithNetworkException, paymentsNumber = 1, paymentsSuccess = false)
    }

    @Test
    internal fun `fail to charge invoice when insufficient customer funds`() = runBlocking {
        val billingService = createBillingService(invoice = invoiceNotCharged)
        billingService.chargeInvoice(invoiceNotCharged)
        assertInvoiceCharged(invoiceNotCharged, paymentsNumber = 1, paymentsSuccess = false)
    }

    @Test
    internal fun `charged invoice`() = runBlocking {
        val billingService = createBillingService(
            invoice = invoiceWillBeCharged,
            chargeInvoiceStub = {
                coEvery { charge(it) } returns true
            }
        )

        val invoice = billingService.chargeInvoice(invoiceWillBeCharged)
        assertInvoiceCharged(invoiceWillBeCharged, paymentsNumber = 1, paymentsSuccess = true)
        val chargedInvoice = dal.fetchInvoice(invoice.id)
        Assertions.assertEquals(InvoiceStatus.PAID, chargedInvoice?.status)
    }

    @Test
    internal fun `fail to charge already charged invoice`() = runBlocking<Unit> {
        val billingService = createBillingService(
            invoice = invoiceAlreadyCharged,
            chargeInvoiceStub = {
                coEvery { charge(it) } returns true
            }
        )

        assertThrows<InvoiceChargedException> {
            billingService.chargeInvoice(invoiceAlreadyCharged)
        }
    }

    private suspend fun assertInvoiceCharged(invoice: Invoice, paymentsNumber: Int, paymentsSuccess: Boolean) {
        val invoicePayments = dal.fetchInvoicePayments(invoice.id)
        Assertions.assertEquals(paymentsNumber, invoicePayments.size)
        invoicePayments.firstOrNull()?.let {
            Assertions.assertEquals(paymentsSuccess, it.success)
        }
    }
}
