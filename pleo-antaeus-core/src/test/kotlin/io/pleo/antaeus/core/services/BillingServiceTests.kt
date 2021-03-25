package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random


class BillingServiceTest {
    val invoices = List(size = 7) {
        Invoice(
                id = Random.nextInt(),
                customerId = Random.nextInt(),
                amount = Money(100.toBigDecimal(), Currency.USD),
                status = InvoiceStatus.PENDING
        )
    }

    val paidInvoices = List(size = 7) {
        Invoice(
                id = Random.nextInt(),
                customerId = Random.nextInt(),
                amount = Money(100.toBigDecimal(), Currency.USD),
                status = InvoiceStatus.PAID
        )
    }

    val paidInvoice = Invoice(
            id = Random.nextInt(),
            customerId = Random.nextInt(),
            amount = Money(100.toBigDecimal(), Currency.USD),
            status = InvoiceStatus.PAID
    )

    @Test
    fun `it bills an unpaid invoice`() {
        val invoice = invoices[1]
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns paidInvoice
            every { fetchInvoice(any()) } returns invoice
        }
        val invoiceService = InvoiceService(dal = dal)

        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } returns true }
        val billingService = BillingService(paymentProvider, invoiceService)
        val results = billingService.billInvoice(invoice.id)

        verify(exactly = 1) {
            dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID.toString())
        }
    }

    @Test
    fun `it doesnt bill a paid invoice`() {
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns paidInvoice
            every { fetchInvoicesByStatus(any()) } returns paidInvoices
            every { fetchInvoice(any()) } returns paidInvoice
        }
        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } returns true }
        val billingService = BillingService(paymentProvider, invoiceService)
        val results = billingService.billInvoice(1)

        verify(exactly = 0) {
            dal.updateInvoiceStatus(1, InvoiceStatus.PAID.toString())
        }
    }

    @Test
    fun `it updates invoice status to PAID when an invoice is successfully charged`() {
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns paidInvoice
            every { fetchInvoicesByStatus(any()) } returns invoices
        }
        val invoiceService = InvoiceService(dal = dal)

        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } returns true }
        val billingService = BillingService(paymentProvider, invoiceService)
        val results = billingService.billInvoices("PENDING")

        verify(exactly = 7) {
            dal.updateInvoiceStatus(any(), InvoiceStatus.PAID.toString())
        }
    }

    @Test
    fun `it updates invoice status to UNPAID when an invoice is unsuccessfully charged`() {
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns paidInvoice
            every { fetchInvoicesByStatus(any()) } returns invoices
        }
        val invoiceService = InvoiceService(dal = dal)
        val aymentProvider = mockk<PaymentProvider> { every { charge(any()) } returns false }
        val billingService = BillingService(aymentProvider, invoiceService)
        val results = billingService.billInvoices("PENDING")

        verify(exactly = 7) {
            dal.updateInvoiceStatus(any(), InvoiceStatus.UNPAID.toString())
        }
    }

    @Test
    fun `doesnt update invoice status if an NetworkException is encountered`() {
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns paidInvoice
            every { fetchInvoicesByStatus(any()) } returns invoices
        }
        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } throws NetworkException() }
        val billingService = BillingService(paymentProvider, invoiceService)
        val result = billingService.billInvoices("PENDING")

        assertThrows<NetworkException> {
            paymentProvider.charge(invoices[1])
        }

        verify(exactly = 7) {
            dal.updateInvoiceStatus(any(), InvoiceStatus.FAILED.toString())
        }
    }

    @Test
    fun `updates invoice status to unpaid if CustomerNotFoundException is thrown`() {
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns paidInvoice
            every { fetchInvoicesByStatus(any()) } returns invoices
        }
        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } throws CustomerNotFoundException(1) }
        val billingService = BillingService(paymentProvider, invoiceService)
        val result = billingService.billInvoices("PENDING")

        assertThrows<CustomerNotFoundException> {
            paymentProvider.charge(invoices[1])
        }

        verify(exactly = 7) {
            dal.updateInvoiceStatus(any(), InvoiceStatus.INVALID_CUSTOMER.toString())
        }
    }

    @Test
    fun `updates invoice status to unpaid if CurrencyMismatchException is thrown`() {
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns paidInvoice
            every { fetchInvoicesByStatus(any()) } returns invoices
        }
        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockk<PaymentProvider> { every { charge(any()) } throws CurrencyMismatchException(1,1) }
        val billingService = BillingService(paymentProvider, invoiceService)
        val result = billingService.billInvoices("PENDING")

        assertThrows<CurrencyMismatchException> {
            paymentProvider.charge(invoices[1])
        }

        verify(exactly = 7) {
            dal.updateInvoiceStatus(any(), InvoiceStatus.CURRENCY_MISMATCH.toString())
        }
    }
}
