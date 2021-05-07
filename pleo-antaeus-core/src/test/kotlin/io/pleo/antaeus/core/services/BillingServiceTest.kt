package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoicePaymentTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
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
import setupInitialData

val invoiceForNotRegisteredCustomer = createInvoice();
val invoiceWithCurrencyMismatch = createInvoice();
val invoiceWithNetworkException = createInvoice();
val invoiceCharged = createInvoice();
val invoiceNotCharged = createInvoice();

class BillingServiceTest {
    private val tables = arrayOf(InvoiceTable, InvoicePaymentTable, CustomerTable)

    private val db by lazy { Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;", "org.h2.Driver", "root", "") }
    private val dal = AntaeusDal(db = db)

    private val paymentProvider = mockk<PaymentProvider> {
        coEvery { charge(invoiceForNotRegisteredCustomer) } throws CustomerNotFoundException(invoiceForNotRegisteredCustomer.customerId)
        coEvery { charge(invoiceWithCurrencyMismatch) } throws CurrencyMismatchException(invoiceWithCurrencyMismatch.id, invoiceWithCurrencyMismatch.customerId)
        coEvery { charge(invoiceWithNetworkException) } throws NetworkException()
        coEvery { charge(invoiceCharged) } returns true
        coEvery { charge(invoiceNotCharged) } returns false
    }

    private val invoiceService = spyk(InvoiceService(dal)) {
        coEvery { fetch(invoiceForNotRegisteredCustomer.id) } returns invoiceForNotRegisteredCustomer
        coEvery { fetch(invoiceWithCurrencyMismatch.id) } returns invoiceWithCurrencyMismatch
        coEvery { fetch(invoiceWithNetworkException.id) } returns invoiceWithNetworkException
        coEvery { fetch(invoiceCharged.id) } returns invoiceCharged
        coEvery { fetch(invoiceNotCharged.id) } returns invoiceNotCharged
    }

    val billingService = BillingService(paymentProvider, invoiceService)

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
        assertThrows<CustomerNotFoundException> {
            billingService.chargeInvoice(invoiceForNotRegisteredCustomer)
        }
        assertInvoiceCharged(invoiceForNotRegisteredCustomer, paymentsNumber = 1, paymentsSuccess = false);
    }

    @Test
    internal fun `fail to charge invoice for billing with currency mismatch`() = runBlocking {
        assertThrows<CurrencyMismatchException> {
            billingService.chargeInvoice(invoiceWithCurrencyMismatch)
        }
        assertInvoiceCharged(invoiceWithCurrencyMismatch, paymentsNumber = 1, paymentsSuccess = false);
    }

    @Test
    internal fun `fail to charge invoice when network exception occurs`() = runBlocking {
        assertThrows<NetworkException> {
            billingService.chargeInvoice(invoiceWithNetworkException)
        }
        assertInvoiceCharged(invoiceWithNetworkException, paymentsNumber = 1, paymentsSuccess = false);
    }

    @Test
    internal fun `fail to charge invoice when insufficient customer funds`() = runBlocking {
        billingService.chargeInvoice(invoiceNotCharged)
        assertInvoiceCharged(invoiceNotCharged, paymentsNumber = 1, paymentsSuccess = false);
    }

    @Test
    // FIXME unstable
    internal fun `charged invoice`() = runBlocking {
        val invoice = billingService.chargeInvoice(invoiceCharged)
        assertInvoiceCharged(invoiceCharged, paymentsNumber = 1, paymentsSuccess = true);
        Assertions.assertEquals(InvoiceStatus.PAID, invoice.status)
    }

    private suspend fun assertInvoiceCharged(invoice: Invoice, paymentsNumber: Int, paymentsSuccess: Boolean) {
        val invoicePayments = dal.fetchInvoicePayments(invoice.id)
        Assertions.assertEquals(paymentsNumber, invoicePayments.size);
        Assertions.assertEquals(paymentsSuccess, invoicePayments[0].success);
    }
}
