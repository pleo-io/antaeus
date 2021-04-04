package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.dals.InvoiceDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {

    /*
     * TODO use an in memory test DB including test samples to run the unit tests
     *  (like the one created when the application starts)
     */

    private val dal = mockk<InvoiceDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(invoiceDal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `retrieve only pending invoices`() {
        // TODO
    }

    @Test
    fun `setInvoicePaid update the status to PAID`() {
        // TODO
    }

    @Test
    fun `setInvoicePaid throw if invoice not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.setInvoicePaid(404)
        }
    }
}
