package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val pendingInvoice = Invoice(1, 1, Money(BigDecimal.valueOf(6), Currency.EUR), InvoiceStatus.PENDING)

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoicesWithStatus(InvoiceStatus.PENDING) } returns listOf(pendingInvoice)
        every { updateInvoiceStatus(1, InvoiceStatus.PAID) } just Runs
        every { updateInvoiceStatus(1, InvoiceStatus.ERROR) } just Runs
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `should return true when payment provider charges the bill successfully`() {

        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(pendingInvoice) } returns true

        val billingService = BillingService(
                invoiceService = invoiceService,
                paymentProvider = paymentProvider
        )

        assertTrue(billingService.chargeInvoices(InvoiceStatus.PENDING))
        verify(atLeast = 1, atMost = 1) { paymentProvider.charge(pendingInvoice) }
        verify(atLeast = 1, atMost = 1) { invoiceService.updateInvoiceStatus(pendingInvoice.id, InvoiceStatus.PAID) }
    }

    @Test
    fun `should return logging error when payment provider charging fails`() {

        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(pendingInvoice) } returns false

        val billingService = BillingService(
                invoiceService = invoiceService,
                paymentProvider = paymentProvider
        )

        assertTrue(billingService.chargeInvoices(InvoiceStatus.PENDING))
        verify(atLeast = 1, atMost = 1) { paymentProvider.charge(pendingInvoice) }
        verify(atLeast = 1, atMost = 1) { invoiceService.updateInvoiceStatus(pendingInvoice.id, InvoiceStatus.ERROR) }
    }

    @Test
    fun `should throw CurrencyMismatchException when the currency is does not match`() {

        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(pendingInvoice) } throws CurrencyMismatchException(invoiceId = pendingInvoice.id, customerId = pendingInvoice.customerId)

        val billingService = BillingService(
                invoiceService = invoiceService,
                paymentProvider = paymentProvider
        )

        assertThrows(CurrencyMismatchException::class.java) {
            billingService.chargePendingInvoice(pendingInvoice)
        }
        verify(atLeast = 1, atMost = 1) { paymentProvider.charge(pendingInvoice) }
        verify(atLeast = 1, atMost = 1) { invoiceService.updateInvoiceStatus(pendingInvoice.id, InvoiceStatus.ERROR) }
    }

    @Test
    fun `should throw CustomerNotFoundException when the customer is not found`() {

        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(pendingInvoice) } throws CustomerNotFoundException(id = pendingInvoice.customerId)

        val billingService = BillingService(
                invoiceService = invoiceService,
                paymentProvider = paymentProvider
        )

        assertThrows(CustomerNotFoundException::class.java) {
            billingService.chargePendingInvoice(pendingInvoice)
        }
        verify(atLeast = 1, atMost = 1) { paymentProvider.charge(pendingInvoice) }
        verify(atLeast = 1, atMost = 1) { invoiceService.updateInvoiceStatus(pendingInvoice.id, InvoiceStatus.ERROR) }
    }

    @Test
    fun `should throw InvoiceNotFoundException when the invoice is not found`() {

        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(pendingInvoice) } throws InvoiceNotFoundException(id = pendingInvoice.id)

        val billingService = BillingService(
                invoiceService = invoiceService,
                paymentProvider = paymentProvider
        )

        assertThrows(InvoiceNotFoundException::class.java) {
            billingService.chargePendingInvoice(pendingInvoice)
        }
        verify(atLeast = 1, atMost = 1) { paymentProvider.charge(pendingInvoice) }
        verify(atLeast = 1, atMost = 1) { invoiceService.updateInvoiceStatus(pendingInvoice.id, InvoiceStatus.ERROR) }
    }

    @Test
    fun `should throw NetworkExeption when there is an network issue`() {

        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(pendingInvoice) } throws NetworkException()

        val billingService = BillingService(
                invoiceService = invoiceService,
                paymentProvider = paymentProvider
        )

        assertThrows(NetworkException::class.java) {
            billingService.chargePendingInvoice(pendingInvoice)
        }
        verify(atLeast = 1, atMost = 1) { paymentProvider.charge(pendingInvoice) }
        verify(atLeast = 1, atMost = 1) { invoiceService.updateInvoiceStatus(pendingInvoice.id, InvoiceStatus.ERROR) }
    }
}

