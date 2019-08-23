package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Assertions.assertEquals

class BillingServiceTest {
    private val customerService = mockk<CustomerService>()
    private val invoice = mockk<Invoice>()
    private val customer = mockk<Customer>()

    @Test
    fun `will update the invoice status as PAID when charging is successful`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } returns true
        }
        val invoiceService = mockk<InvoiceService> {
            every { updateStatus(invoice.id, InvoiceStatus.PAID) } returns invoice
        }
        val billingService = BillingService(paymentProvider, invoiceService, customerService)
        val updatedInvoice = billingService.charge(invoice)

        assertEquals(updatedInvoice.status, InvoiceStatus.PAID)
    }

    @Test
    fun `will update the invoice status as INSUFFICIENTFUNDS when charging fails`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } returns false
        }
        val invoiceService = mockk<InvoiceService> {
            every { updateStatus(invoice.id, InvoiceStatus.INSUFFICIENTFUNDS) } returns invoice
        }
        val billingService = BillingService(paymentProvider, invoiceService, customerService)
        val updatedInvoice = billingService.charge(invoice)

        assertEquals(updatedInvoice.status, InvoiceStatus.INSUFFICIENTFUNDS)
    }

    @Test
    fun `will update the currency in invoice table if PaymentProvider throws a CurrencyMismatchException`() {
        val invoice = mockk<Invoice>()
        val currencyMismatchException = mockk<CurrencyMismatchException>()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } throws currencyMismatchException
        }
        val customerService = mockk<CustomerService> {
            every { fetch(customer.id) } returns customer
        }
        val invoiceService = mockk<InvoiceService> {
            every { updateStatus(invoice.id, InvoiceStatus.CURRENCYMISMATCH) } returns invoice
            every { updateCurrency(invoice.id, customer.currency) } returns invoice
        }
        val billingService = BillingService(paymentProvider, invoiceService, customerService)
        val updatedInvoice = billingService.charge(invoice)

        assertEquals(updatedInvoice.amount.currency, customer.currency)
    }

    @Test
    fun `will update the invoice status as CURRENCYMISMATCH if PaymentProvider throws a CurrencyMismatchException`() {
        val invoice = mockk<Invoice>()
        val currencyMismatchException = mockk<CurrencyMismatchException>()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } throws currencyMismatchException
        }
        val customerService = mockk<CustomerService> {
            every { fetch(customer.id) } returns customer
        }
        val invoiceService = mockk<InvoiceService> {
            every { updateStatus(invoice.id, InvoiceStatus.CURRENCYMISMATCH) } returns invoice
            every { updateCurrency(invoice.id, customer.currency) } returns invoice
        }
        val billingService = BillingService(paymentProvider, invoiceService, customerService)
        val updatedInvoice = billingService.charge(invoice)

        assertEquals(updatedInvoice.status, InvoiceStatus.CURRENCYMISMATCH)
    }

    @Test
    fun `will update the invoice status as CUSTOMERNOTFOUND if PaymentProvider throws a CustomerNotFoundException`() {
        val invoice = mockk<Invoice>()
        val customerNotFoundException = mockk<CustomerNotFoundException>()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } throws customerNotFoundException
        }
        val invoiceService = mockk<InvoiceService> {
            every { updateStatus(invoice.id, InvoiceStatus.CUSTOMERNOTFOUND) } returns invoice
        }
        val billingService = BillingService(paymentProvider, invoiceService, customerService)
        val updatedInvoice = billingService.charge(invoice)

        assertEquals(updatedInvoice.status, InvoiceStatus.CUSTOMERNOTFOUND)
    }

    @Test
    fun `will update the invoice status as NETWORKFAIL if PaymentProvider throws a NetworkException`() {
        val invoice = mockk<Invoice>()
        val networkException = mockk<NetworkException>()
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(invoice) } throws networkException
        }
        val invoiceService = mockk<InvoiceService> {
            every { updateStatus(invoice.id, InvoiceStatus.NETWORKFAIL) } returns invoice
        }
        val billingService = BillingService(paymentProvider, invoiceService, customerService)
        val updatedInvoice = billingService.charge(invoice)

        assertEquals(updatedInvoice.status, InvoiceStatus.NETWORKFAIL)
    }
}