package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val pendingInvoices = mockk<List<Invoice>>{
        every { isEmpty() } returns false
    }

    private val paidInvoices = mockk<List<Invoice>>{
        every { isEmpty() } returns false
    }

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { updateInvoiceStatus(404, InvoiceStatus.PENDING) } returns null
        every { fetchInvoicesByStatus(InvoiceStatus.PENDING) } returns pendingInvoices
        every { fetchInvoicesByStatus(InvoiceStatus.PAID) } returns paidInvoices
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will throw if invoice to be updated is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.updateInvoiceStatusById(404, InvoiceStatus.PENDING)
        }
    }

    @Test
    fun `pending invoices query is not empty`() {
        assert(invoiceService.fetchInvoicesByStatus(InvoiceStatus.PENDING).isNotEmpty())
    }

    @Test
    fun `paid invoices query is not empty`() {
        assert(invoiceService.fetchInvoicesByStatus(InvoiceStatus.PAID).isNotEmpty())
    }
}