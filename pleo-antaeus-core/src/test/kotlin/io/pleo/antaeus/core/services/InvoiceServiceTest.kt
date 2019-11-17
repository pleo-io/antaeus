package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {

    private val pendingInvoice = Invoice(
            id = 1,
            customerId = 1,
            amount = Money(value = BigDecimal.valueOf(1000), currency = Currency.DKK ),
            status = InvoiceStatus.PENDING)

    private val scheduledInvoice = Invoice(
            id = 1,
            customerId = 1,
            amount = Money(value = BigDecimal.valueOf(1000), currency = Currency.DKK ),
            status = InvoiceStatus.SCHEDULED)

    private val pendingInvoices  = listOf(pendingInvoice)

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchPendingInvoices() } returns pendingInvoices
        every { updateInvoiceStatus(any(), any()) } returns scheduledInvoice
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will fetch invoices with status PENDING`() {
        assert(
                invoiceService.fetchAllPending() == pendingInvoices
        )
    }

    @Test
    fun `will update invoice status`() {
        assert(
                invoiceService.updateStatus(id = 1, status = InvoiceStatus.SCHEDULED) == scheduledInvoice
        )
    }

    @Test
    fun `updating non-existent invoice throws InvoiceNotFoundException`() {
        every { dal.updateInvoiceStatus(any(), any()) } returns null
        assertThrows<InvoiceNotFoundException> {
            invoiceService.updateStatus(id = 1, status = InvoiceStatus.SCHEDULED)
        }
    }
}
