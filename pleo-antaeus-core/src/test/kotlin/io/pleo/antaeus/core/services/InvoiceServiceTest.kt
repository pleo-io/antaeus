package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.dals.InvoiceDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {

    // TODO use an in memory test DB to run the unit tests in order test the Dal queries as well

    private val invoice1 =
        Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PENDING)


    private val invoiceDal = mockk<InvoiceDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoice(1) } returns invoice1
        every { fetchPendingInvoices() } returns listOf(
            Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PENDING)
        )
        every { updateInvoiceStatus(1,InvoiceStatus.PAID) } returns
                Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PAID)
    }

    private val invoiceService = InvoiceService(invoiceDal = invoiceDal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `retrieve pending invoices`() {
        val i = invoiceService.fetchPendingInvoices().first()
        assertEquals(invoice1, i)
    }

    @Test
    fun `setInvoicePaid update status to PAID`() {
        val i = invoiceService.setInvoicePaid(1)
        assertEquals(InvoiceStatus.PAID, i.status)
    }

    @Test
    fun `setInvoicePaid throw if invoice not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.setInvoicePaid(404)
        }
    }
}
