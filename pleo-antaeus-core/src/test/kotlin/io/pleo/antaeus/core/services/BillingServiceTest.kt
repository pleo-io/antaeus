package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BillingServiceTest {
    private fun init() : BillingService{
        val invoices = mutableListOf<Invoice>()
        val invoicePending = Invoice(1,1, Money(1.toBigDecimal(), Currency.USD), InvoiceStatus.PENDING)
        invoices.add(invoicePending)

        val invoicePaid = Invoice(1,1, Money(1.toBigDecimal(), Currency.USD), InvoiceStatus.PAID)

        val dal = mockk<AntaeusDal> {
            every { fetchInvoicesPending() } returns invoices
            every { payInvoice(1) } returns invoicePaid
        }
        val paymentProvider = mockk<PaymentProvider>()

        return BillingService(dal = dal, paymentProvider = paymentProvider)
    }

    @Test
    fun `will pay pending invoices(change status to PAID)`() {
        // ARRANGE
        val billingService = init()
        val expected = mutableListOf<Invoice>()
        val invoicePaid = Invoice(1,1, Money(1.toBigDecimal(), Currency.USD), InvoiceStatus.PAID)
        expected.add(invoicePaid)

        // ACT
        val result = billingService.payPendingInvoices()

        // ASSERT
        Assertions.assertEquals(expected, result)
    }
}