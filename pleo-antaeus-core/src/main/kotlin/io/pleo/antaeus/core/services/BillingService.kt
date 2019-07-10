package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InsufficientFundsException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceStatus.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.retry.Retry
import java.time.Duration.ofMillis


class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService) {

    private val logger = KotlinLogging.logger {}
    private val networkExceptionRetry: Retry<Any> = Retry.anyOf<Any>(NetworkException::class.java)
            .retryMax(3)
            .exponentialBackoff(ofMillis(1000), null)

    fun chargePendingInvoices(): Flux<Invoice> {
        return fetchPendingInvoices()
                .flatMap { invoice ->
                    chargeInvoice(invoice)
                            .flatMap { processInvoicePayment(invoice, it) }
                            .onErrorResume { processPaymentError(invoice, it) }
                }
    }

    private fun fetchPendingInvoices(): Flux<Invoice> {
        return Flux.fromIterable(invoiceService.fetchPendingInvoices())
    }

    private fun chargeInvoice(invoice: Invoice): Mono<Boolean> {
        return Mono.fromCallable {
            paymentProvider.charge(invoice)
        }.retryWhen(networkExceptionRetry)
    }

    private fun processPaymentError(invoice: Invoice, error: Throwable): Mono<Invoice> {
        return updateStatus(invoice, ERROR, error.cause?.message ?: error.message)
    }

    private fun processInvoicePayment(invoice: Invoice, isCharged: Boolean): Mono<Invoice> {
        return when (isCharged) {
            true -> updateStatus(invoice, PAID)
            else -> throw InsufficientFundsException(invoice.id, invoice.customerId)
        }
    }

    private fun updateStatus(invoice: Invoice, newStatus: InvoiceStatus, errorMsg : String? = null): Mono<Invoice> {
        invoiceService.updateInvoiceStatus(invoice, newStatus, errorMsg)
        return Mono.just(invoice.copy(status = newStatus))
    }

}
