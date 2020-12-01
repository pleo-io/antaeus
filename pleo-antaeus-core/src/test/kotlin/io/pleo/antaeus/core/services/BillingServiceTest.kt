package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProvider>()
    private val invoiceService = mockk<InvoiceService>()
    private val scheduleService = mockk<ScheduleService>()

    private fun createInvoice(status: InvoiceStatus) =
        Invoice(1, amount = Money(BigDecimal.valueOf(42),Currency.DKK), customerId = 1, status = status)

    @BeforeEach
    fun setup() {
        every { scheduleService.timeUntilNextBilling(Schedule.MONTHLY) } returns 0
    }

    @Test
    fun `will update invoice status to PAID after successful charging`() {
        val invoice = createInvoice(InvoiceStatus.PENDING)

        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING)} returns listOf(invoice)
        every { invoiceService.updateStatus(any(), any())} returns 1
        every { paymentProvider.charge(any()) } returns true
        every { invoiceService.fetchInvoiceLogs(any(), any())} returns emptyList()

        BillingService(paymentProvider, invoiceService, scheduleService).start()

        verify{ paymentProvider.charge(invoice) }
        verify{ invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID) }
        verify(exactly = 0) { invoiceService.addInvoiceLog(any(), any())}
    }

    @Test
    fun `will create invoice log due to handled exception`() {
        val invoice = createInvoice(InvoiceStatus.PENDING)

        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING)} returns listOf(invoice)
        every { invoiceService.updateStatus(any(), any())} returns 1
        every { invoiceService.addInvoiceLog(any(), any())} returns 1
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(invoice.id, invoice.customerId)
        every { invoiceService.fetchInvoiceLogs(any(), any())} returns emptyList()

        BillingService(paymentProvider, invoiceService, scheduleService).start()

        verify(exactly = 0){ invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID) }
        verify { invoiceService.addInvoiceLog(invoice.id, Message.CURRENCY_MISMATCH)}
    }

    @Test
    fun `will not attempt to charge paid invoice`() {
        val invoice = createInvoice(InvoiceStatus.PAID)

        every { invoiceService.fetchByStatus(InvoiceStatus.PENDING)} returns emptyList()
        every { invoiceService.fetchInvoiceLogs(any(), any())} returns emptyList()

        BillingService(paymentProvider, invoiceService, scheduleService).start()

        verify(exactly = 0){ paymentProvider.charge(invoice) }
    }
}