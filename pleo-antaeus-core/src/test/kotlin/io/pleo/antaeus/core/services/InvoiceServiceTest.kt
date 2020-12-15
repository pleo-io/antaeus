package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.random.Random

class InvoiceServiceTest {

    private val invoices = List(size = 7) {
        Invoice(
                id = Random.nextInt(),
                customerId = Random.nextInt(),
                amount = Money(100.toBigDecimal(), Currency.USD),
                status = InvoiceStatus.PENDING
        )
    }

    val invoice = Invoice(
            id = Random.nextInt(),
            customerId = Random.nextInt(),
            amount = Money(100.toBigDecimal(), Currency.USD),
            status = InvoiceStatus.PENDING
    )

    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(any()) } returns invoice
        every { fetchInvoice(404) } returns null
        every { fetchInvoices() } returns invoices
        every { fetchInvoicesByStatus("PENDING") } returns invoices

    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will fetch an invoice`() {
        val result = invoiceService.fetch(1)
        assertNotNull(result)
    }

    @Test
    fun `will fetch invoices`() {
        val result = invoiceService.fetchAll()
        assertTrue(result.size == 7)
    }

    @Test
    fun `will fetch invoices by status`() {
        val result = invoiceService.fetchAll()
        assertTrue(result.size == 7)
    }
}
