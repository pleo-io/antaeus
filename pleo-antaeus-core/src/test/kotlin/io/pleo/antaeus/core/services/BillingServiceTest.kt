package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency.USD
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.random.Random.Default.nextInt

internal class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider>()
    private val invoiceService = mockk<InvoiceService>()
    private val billingService: BillingService =
        BillingService(
            paymentProvider = paymentProvider,
            invoiceService = invoiceService
        )

    @Test
    fun `should bill invoices when all charged`() {
        //given
        every { paymentProvider.charge(any()) }.returns(true)
        every { invoiceService.fetchPending() }.returns(listOf(
            Invoice(1, 1, Money(BigDecimal(10.0), USD), PENDING),
            Invoice(2, 2, Money(BigDecimal(10.0), USD), PENDING),
            Invoice(3, 3, Money(BigDecimal(10.0), USD), PENDING)
        ))
        every { invoiceService.markAsPaid(any()) } just Runs

        //when
        billingService.billInvoices()

        //then
        verify { invoiceService.markAsPaid(1) }
        verify { invoiceService.markAsPaid(2) }
        verify { invoiceService.markAsPaid(3) }
    }

    @Test
    fun `should not bill not charged invoices`() {
        //given
        val chargeableInvoice = Invoice(1, 1, Money(BigDecimal(10.0), USD), PENDING)
        val unChargeableInvoice = Invoice(2, 2, Money(BigDecimal(10.0), USD), PENDING)
        every { paymentProvider.charge(chargeableInvoice) }.returns(true)
        every { paymentProvider.charge(unChargeableInvoice) }.returns(false)
        every { invoiceService.fetchPending() }.returns(listOf(
            chargeableInvoice, unChargeableInvoice
        ))
        every { invoiceService.markAsPaid(any()) } just Runs

        //when
        billingService.billInvoices()

        //then
        verify {
            invoiceService.fetchPending()
            invoiceService.markAsPaid(1)
        }
        confirmVerified(invoiceService)
    }

    @Test
    fun `should not bill invoice when payment provider throws CustomerNotFoundException`(){
        //given
        every { paymentProvider.charge(any()) }.throws(CustomerNotFoundException(nextInt()))
        every { invoiceService.fetchPending() }.returns(listOf(
            Invoice(nextInt(), nextInt(), Money(BigDecimal(10.0), USD), PENDING)))

        //when
        billingService.billInvoices()

        //then
        verify {
            invoiceService.fetchPending()
        }
        confirmVerified(invoiceService)
    }

    @Test
    fun `should not bill invoice when payment provider throws NetworkException`(){
        //given
        every { paymentProvider.charge(any()) }.throws(NetworkException())
        every { invoiceService.fetchPending() }.returns(listOf(
            Invoice(nextInt(), nextInt(), Money(BigDecimal(10.0), USD), PENDING)))

        //when
        billingService.billInvoices()

        //then
        verify {
            invoiceService.fetchPending()
        }
        confirmVerified(invoiceService)
    }

    @Test
    fun `should not bill invoice when payment provider throws CurrencyMismatchException`(){
        //given
        every { paymentProvider.charge(any()) }.throws(CurrencyMismatchException(nextInt(), nextInt()))
        every { invoiceService.fetchPending() }.returns(listOf(
            Invoice(nextInt(), nextInt(), Money(BigDecimal(10.0), USD), PENDING)))

        //when
        billingService.billInvoices()

        //then
        verify {
            invoiceService.fetchPending()
        }
        confirmVerified(invoiceService)
    }
}