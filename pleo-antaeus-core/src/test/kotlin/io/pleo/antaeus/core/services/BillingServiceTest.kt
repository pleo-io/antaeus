package io.pleo.antaeus.core.services

import fixtures.Fixtures.Companion.createPendingInvoice
import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InsufficientFundsException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import reactor.test.StepVerifier

internal class BillingServiceTest {

    private val invoiceService = mockk<InvoiceService>()
    private val paymentProvider = mockk<PaymentProvider>()
    private val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

    @Test
    fun `charge all pending invoices`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        every { paymentProvider.charge(any()) } returns true

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.PAID }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.PAID)
    }

    @Test
    fun `charge mark invoice status as error if PaymentProvider returns false`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        every { paymentProvider.charge(any()) } returns false

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.ERROR)
    }

    @Test
    fun `should mark invoice status as error if PaymentProvider throws a CustomerNotFoundException`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        every { paymentProvider.charge(any()) } throws CustomerNotFoundException(pendingInvoice.customerId)

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.ERROR)

    }

    @Test
    fun `should mark invoice status as error if PaymentProvider throws a NetworkException`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        every { paymentProvider.charge(any()) } throws NetworkException()

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.ERROR)

    }

    @Test
    fun `should mark invoice status as error if PaymentProvider throws a CurrencyMismatchException`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(pendingInvoice.id, pendingInvoice.customerId)

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.ERROR)

    }

    private fun setupPendingInvoices(): Invoice {
        val expected = createPendingInvoice()
        every { invoiceService.fetchPendingInvoices() } returns listOf(expected)
        every { invoiceService.updateInvoiceStatus(expected, any()) } just Runs
        return expected
    }

    private fun verifyInteractions(expected: Invoice, expectedStatus: InvoiceStatus) {
        verify { invoiceService.fetchPendingInvoices() }
        verify { invoiceService.updateInvoiceStatus(expected, expectedStatus) }
        verify { paymentProvider.charge(expected) }
    }

}
