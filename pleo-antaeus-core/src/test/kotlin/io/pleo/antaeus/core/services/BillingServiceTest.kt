package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.internalSubstitute
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.MoneyValueOutOfRangeException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTest {
    private val invoice1 = Invoice(1, 1, Money(BigDecimal.valueOf(100), Currency.DKK), InvoiceStatus.PENDING)
    private val invoice2 = Invoice(2, 1, Money(BigDecimal.valueOf(0), Currency.DKK), InvoiceStatus.PENDING)
    private val invoice3 = Invoice(3, 1, Money(BigDecimal.valueOf(501), Currency.DKK), InvoiceStatus.PENDING)
    private val invoice4 = Invoice(4, 1, Money(BigDecimal.valueOf(100), Currency.EUR), InvoiceStatus.PENDING)
    private val invoice5 = Invoice(5, 1, Money(BigDecimal.valueOf(100), Currency.DKK), InvoiceStatus.PAID)
    private val invoice6 = Invoice(6, 3, Money(BigDecimal.valueOf(100), Currency.DKK), InvoiceStatus.PENDING)

    private val customer1 = Customer(1, Currency.DKK)
    private val customer2 = Customer(2, Currency.EUR)
    private val customers = mockk<AntaeusDal> {
        every {fetchCustomers() } returns listOf(customer1, customer2)
        every {fetchCustomer(customer1.id)} returns customer1
        every {fetchCustomer(3) } returns null
    }


    private fun paymentProvider() = mockk<PaymentProvider> {
        every { charge(invoice1) } returnsMany listOf(true)
    }

    @Test
    fun `will throw if invoice customer id is not found`() {
        assertThrows<CustomerNotFoundException> {
            val invoices1 = mockk<AntaeusDal> {
                every { fetchInvoices() } returns listOf (invoice6)
                every { fetchInvoice(invoice6.id) } returns invoice6
            }
            val invoiceService = InvoiceService(dal = invoices1)
            val customerService = CustomerService(dal = customers)
            val billingService = BillingService(customerService = customerService, paymentProvider = paymentProvider(), invoiceService = invoiceService)
            billingService.chargeInvoices()
        }
    }

    @Test
    fun ` MoneyValueOutOfRangeException thrown for 0 or less money value`() {
        assertThrows<MoneyValueOutOfRangeException> {
            val invoices1 = mockk<AntaeusDal> {
                every { fetchInvoices() } returns listOf (invoice2)
                every { fetchInvoice(invoice2.id) } returns invoice2
            }
            val invoiceService = InvoiceService(dal = invoices1)
            val customerService = CustomerService(dal = customers)
            val billingService = BillingService(customerService = customerService, paymentProvider = paymentProvider(), invoiceService = invoiceService)
            billingService.chargeInvoices()
        }
    }

    @Test
    fun `currencyMismatchException thrown`() {
        assertThrows<CurrencyMismatchException> {
            val invoices1 = mockk<AntaeusDal> {
                every { fetchInvoices() } returns listOf (invoice4)
                every { fetchInvoice(invoice4.id) } returns invoice4
            }
            val invoiceService = InvoiceService(dal = invoices1)
            val customerService = CustomerService(dal = customers)
            val billingService = BillingService(customerService = customerService, paymentProvider = paymentProvider(), invoiceService = invoiceService)
            billingService.chargeInvoices()
        }
    }

    @Test
    fun `MoneyValueOutOfRangeException thrown for excessive invoice money value`() {
        assertThrows<MoneyValueOutOfRangeException> {
            val invoices1 = mockk<AntaeusDal> {
                every { fetchInvoices() } returns listOf (invoice3)
                every { fetchInvoice(invoice3.id) } returns invoice3
            }
            val invoiceService = InvoiceService(dal = invoices1)
            val customerService = CustomerService(dal = customers)
            val billingService = BillingService(customerService = customerService, paymentProvider = paymentProvider(), invoiceService = invoiceService)
            billingService.chargeInvoices()
        }
    }
}
