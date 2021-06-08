package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency.USD
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider>()
    private val invoiceService = mockk<InvoiceService>()
    private val billingService: BillingService = BillingService(paymentProvider, invoiceService)

    @Test
    fun `should bill invoices when all charged`() {
        //given
        every { paymentProvider.charge(any()) }.returns(true)
        every { invoiceService.fetchPending() }.returns(listOf(
            Invoice(1, 1, Money(BigDecimal(10.0), USD), PENDING),
            Invoice(2, 2, Money(BigDecimal(10.0), USD), PENDING),
            Invoice(3, 3, Money(BigDecimal(10.0), USD), PENDING)
        ))
        every { invoiceService.markAsPaid(any()) } just Runs

        //when
        billingService.billInvoices()

        //then
        verify { invoiceService.markAsPaid(1) }
        verify { invoiceService.markAsPaid(2) }
        verify { invoiceService.markAsPaid(3) }
    }

    @Test
    fun `should not bill not charged invoices`() {
        //given
        val chargeableInvoice = Invoice(1, 1, Money(BigDecimal(10.0), USD), PENDING)
        val unChargeableInvoice = Invoice(2, 2, Money(BigDecimal(10.0), USD), PENDING)
        every { paymentProvider.charge(chargeableInvoice) }.returns(true)
        every { paymentProvider.charge(unChargeableInvoice) }.returns(false)
        every { invoiceService.fetchPending() }.returns(listOf(
            chargeableInvoice, unChargeableInvoice
        ))
        every { invoiceService.markAsPaid(any()) } just Runs

        //when
        billingService.billInvoices()

        //then
        verify {
            invoiceService.fetchPending()
            invoiceService.markAsPaid(1)
        }
        confirmVerified(invoiceService)
    }
}