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
        verifyInteractions(pendingInvoice,
                InvoiceStatus.ERROR,
                InsufficientFundsException(pendingInvoice.id, pendingInvoice.customerId).message)
    }

    @Test
    fun `should mark invoice status as error if PaymentProvider throws a CustomerNotFoundException`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        val customerNotFoundException = CustomerNotFoundException(pendingInvoice.customerId)
        every { paymentProvider.charge(any()) } throws customerNotFoundException

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.ERROR, customerNotFoundException.message)

    }

    @Test
    fun `should mark invoice status as error if PaymentProvider throws a NetworkException`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        val networkException = NetworkException()
        every { paymentProvider.charge(any()) } throws networkException

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.ERROR, networkException.message)

    }

    @Test
    fun `should retry 3 times if PaymentProvider throws a NetworkException on initial attempt`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        every { paymentProvider.charge(any()) } throws NetworkException()

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verify(exactly = 4) { paymentProvider.charge(pendingInvoice) }

    }

    @Test
    fun `should mark invoice status as error if PaymentProvider throws a CurrencyMismatchException`() {
        //given
        val pendingInvoice = setupPendingInvoices()
        val currencyMismatchException = CurrencyMismatchException(pendingInvoice.id, pendingInvoice.customerId)
        every { paymentProvider.charge(any()) } throws currencyMismatchException

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectComplete()
                .verify()

        //then
        verifyInteractions(pendingInvoice, InvoiceStatus.ERROR, currencyMismatchException.message)

    }

    @Test
    fun `should charge all invoices if errors occur in between processing invoices`() {
        //given
        val pendingFirst = createPendingInvoice()
        val pendingSecond = createPendingInvoice()
        val pendingThird = createPendingInvoice()
        val pendingFourth = createPendingInvoice()
        val pendingFifth = createPendingInvoice()

        every { invoiceService.fetchPendingInvoices() } returns listOf(pendingFirst, pendingSecond, pendingThird, pendingFourth, pendingFifth)
        every { invoiceService.updateInvoiceStatus(any(), any(), any()) } just Runs

        every { paymentProvider.charge(pendingFirst) } throws CurrencyMismatchException(pendingFirst.id, pendingFirst.customerId)
        every { paymentProvider.charge(pendingSecond) } returns true
        every { paymentProvider.charge(pendingThird) } throws CustomerNotFoundException(pendingThird.customerId)
        every { paymentProvider.charge(pendingFourth) } returns false
        every { paymentProvider.charge(pendingFifth) } returns true

        //when-then
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectNextMatches { it.status == InvoiceStatus.PAID }
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectNextMatches { it.status == InvoiceStatus.ERROR }
                .expectNextMatches { it.status == InvoiceStatus.PAID }
                .expectComplete()
                .verify()
    }

    private fun setupPendingInvoices(): Invoice {
        val expected = createPendingInvoice()
        every { invoiceService.fetchPendingInvoices() } returns listOf(expected)
        every { invoiceService.updateInvoiceStatus(expected, any(), any()) } just Runs
        return expected
    }

    private fun verifyInteractions(expected: Invoice, expectedStatus: InvoiceStatus, errorMsg : String? = null) {
        verify { invoiceService.fetchPendingInvoices() }
        verify { invoiceService.updateInvoiceStatus(expected, expectedStatus, errorMsg) }
        verify { paymentProvider.charge(expected) }
    }

}
