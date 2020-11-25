package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will return all invoices`() {
        // ARRANGE
        val invoices = mutableListOf<Invoice>()
        val invoice1 = Invoice(1,1,Money(1.toBigDecimal(), Currency.DKK),InvoiceStatus.PAID)
        val invoice2 = Invoice(2,2,Money(2.toBigDecimal(), Currency.USD),InvoiceStatus.PENDING)
        val invoice3 = Invoice(3,3,Money(3.toBigDecimal(), Currency.GBP),InvoiceStatus.PENDING)
        invoices.add(invoice1)
        invoices.add(invoice2)
        invoices.add(invoice3)

        val test = mockk<AntaeusDal> {
            every { invoiceService.fetchAll() } returns invoices
        }

        // ACT
        val result = invoiceService.fetchAll()

        // ASSERT
        verify { invoiceService.fetchAll() }
        assertEquals(invoices, result)
    }


}
