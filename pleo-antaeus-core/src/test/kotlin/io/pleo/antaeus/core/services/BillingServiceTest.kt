package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val invoicePending =
        Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PENDING)
    private val invoicePaid =
        Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PAID)

    private val paymentProvider = mockk<PaymentProvider> { every { charge(invoicePending) } returns true }
    private val invoiceService = mockk<InvoiceService> {
        every { fetchPendingInvoices() } returns listOf(invoicePending)
        every { setInvoicePaid(1) } returns invoicePaid
    }
    private val billingService = BillingService(paymentProvider, invoiceService)

    @Test
    fun `setInvoicePaid update the status to PAID`() {
        val invoice = billingService.chargePendingInvoices().first()
        Assertions.assertEquals(invoicePaid, invoice)
    }
}
