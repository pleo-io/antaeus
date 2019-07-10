package io.pleo.antaeus.core.services

import fixtures.Fixtures.Companion.createPaidInvoice
import fixtures.Fixtures.Companion.createPendingInvoice
import io.mockk.*
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {

    private val dal = mockk<AntaeusDal>()
    private val invoiceService = InvoiceService(dal = dal)


    @Test
    fun `will throw if customer is not found`() {
        every { dal.fetchInvoice(404) } returns null
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
        verify { dal.fetchInvoice(404) }

    }

    @Test
    fun `returns list of pending invoices`() {
        //given
        val expected = Invoice(id = 1, customerId = 1,
                amount = Money(BigDecimal.valueOf(1L), Currency.EUR),
                status = InvoiceStatus.PENDING)
        every { dal.fetchPendingInvoices() } returns listOf(expected)

        //when
        val actual = invoiceService.fetchPendingInvoices()

        //then
        assertEquals(actual, listOf(expected))
        verify { dal.fetchPendingInvoices() }
    }

    @Test
    fun `update invoice status`() {
        //given
        val expected = createPaidInvoice()
        every { dal.updateInvoiceStatus(any(), any()) } just Runs

        //when
        invoiceService.updateInvoiceStatus(expected, InvoiceStatus.PAID)

        //then
        verify { dal.updateInvoiceStatus(eq(expected), eq(InvoiceStatus.PAID), null) }
    }

    @Test
    fun `update invoice status and error column if error is present`() {
        //given
        val dal = mockk<AntaeusDal>()
        val expected = createPaidInvoice()
        val errorMessage = "error message"
        every { dal.updateInvoiceStatus(any(), any(), any()) } just Runs
        val invoiceService = InvoiceService(dal = dal)

        //when
        invoiceService.updateInvoiceStatus(expected, InvoiceStatus.PAID, errorMessage)

        //then
        verify { dal.updateInvoiceStatus(eq(expected), eq(InvoiceStatus.PAID), eq(errorMessage)) }
    }
}
