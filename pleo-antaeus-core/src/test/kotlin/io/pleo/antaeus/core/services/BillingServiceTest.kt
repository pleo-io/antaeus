package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import reactor.test.StepVerifier
import java.math.BigDecimal
import kotlin.random.Random

internal class BillingServiceTest {

    private val invoiceService = mockk<InvoiceService>()
    private val paymentProvider = mockk<PaymentProvider>()

    @Test
    fun `charge all pending invoices`() {
        //given
        val expected = createInvoice()
        every { invoiceService.fetchPendingInvoices() } returns listOf(expected)
        every { paymentProvider.charge(any()) } returns true
        val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

        //when
        StepVerifier.create(billingService.chargePendingInvoices())
                .expectNext(expected)
                .expectComplete()
                .verify()

        //then
        verify { invoiceService.fetchPendingInvoices() }
        verify { paymentProvider.charge(expected) }

    }

    private fun createInvoice() :Invoice {
        return Invoice(id = 1, customerId = 1,
                amount = Money(BigDecimal.valueOf(1L), Currency.EUR),
                status = InvoiceStatus.PENDING)
    }
}
