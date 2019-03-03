package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceStatus.FAILED
import io.pleo.antaeus.models.InvoiceStatus.IN_PROGRESS
import io.pleo.antaeus.models.InvoiceStatus.PAID
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import io.pleo.antaeus.models.InvoiceStatus.UNKNOWN
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import reactor.test.test
import java.math.BigDecimal
import java.time.Duration

class BillingServiceTest {

    private val dal = mockk<AntaeusDal>()
    private val paymentProvider = mockk<PaymentProvider>()

    @BeforeEach
    fun setUp() {
        every { dal.updateStatus(any(), any()) } returns 1
    }

    @Test
    fun `pays pending invoice`() {
        // given pending invoice
        val invoice = invoice()
        every { dal.fetchInvoicesAndChangeStatus(PENDING, IN_PROGRESS, any()) } returns listOf(invoice())

        // and payment provider returns true
        every { paymentProvider.charge(any()) } returns true

        // then
        BillingService(paymentProvider, dal)
            .chargeAll()
            .test()
            .expectNextMatches { it.status == PAID }
            .verifyComplete()

        // and
        verify { paymentProvider.charge(invoice) }
        verify { dal.updateStatus(invoiceId = invoice.id, newStatus = PAID) }
    }

    @Test
    fun `paid invoice should not be paid again`() {
        // given no pending invoices
        every { dal.fetchInvoicesAndChangeStatus(PENDING, IN_PROGRESS, any()) } returns emptyList()

        // and payment provider returns true
        val paymentProvider = mockk<PaymentProvider>()
        every { paymentProvider.charge(any()) } returns true

        // then
        BillingService(paymentProvider, dal).chargeAll()
            .test()
            .verifyComplete()

        verify(exactly = 0) { paymentProvider.charge(any()) }
        verify(exactly = 0) { dal.updateStatus(invoiceId = any(), newStatus = PAID) }

    }

    @Test
    fun `invoice should have FAILED status when payment fails`() {
        // given pending invoice
        val invoice = invoice()
        every { dal.fetchInvoicesAndChangeStatus(PENDING, IN_PROGRESS, any()) } returns listOf(invoice())

        // and payment provider returns false
        val paymentProvider = mockk<PaymentProvider>()
        every { paymentProvider.charge(any()) } returns false

        // then
        BillingService(paymentProvider, dal)
            .chargeAll()
            .test()
            .expectNextMatches { it.status == FAILED }
            .verifyComplete()

        // and
        verify { paymentProvider.charge(invoice) }
        verify { dal.updateStatus(invoiceId = invoice.id, newStatus = FAILED) }
    }

    @Test
    fun `retries when payment fails because of network problem`() {
        // given pending invoice
        val invoice = invoice()
        every { dal.fetchInvoicesAndChangeStatus(PENDING, IN_PROGRESS, any()) } returns listOf(invoice())

        // and payment provider returns network exception
        val paymentProvider = mockk<PaymentProvider>()
        every { paymentProvider.charge(any()) } throws NetworkException()

        // then
        StepVerifier.withVirtualTime { BillingService(paymentProvider, dal).chargeAll() }
            .thenAwait(Duration.ofSeconds(30))
            .expectNextMatches { it.status == UNKNOWN }
            .verifyComplete()

        verify(exactly = 4) { paymentProvider.charge(invoice) }
        verify { dal.updateStatus(invoiceId = invoice.id, newStatus = UNKNOWN) }
    }

    private fun invoice(status: InvoiceStatus = PENDING) =
        Invoice(
            id = 1,
            amount = Money(
                value = BigDecimal(10.0),
                currency = Currency.EUR
            ),
            customerId = 1,
            status = status
        )
}