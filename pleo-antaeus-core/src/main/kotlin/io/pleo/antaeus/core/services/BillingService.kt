package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InsufficientFundsException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoiceStatus.*
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService) {

    private val logger = KotlinLogging.logger {}

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
        }
    }

    private fun processPaymentError(invoice: Invoice, error: Throwable): Mono<Invoice> {
        //TODO: save error reason in DB
        return updateStatus(invoice, ERROR)
    }

    private fun processInvoicePayment(invoice: Invoice, isCharged: Boolean): Mono<Invoice> {
        return when (isCharged) {
            true -> updateStatus(invoice, PAID)
            else -> throw InsufficientFundsException(invoice.id, invoice.customerId)
        }
    }

    private fun updateStatus(invoice: Invoice, newStatus: InvoiceStatus): Mono<Invoice> {
        invoiceService.updateInvoiceStatus(invoice, newStatus)
        return Mono.just(invoice.copy(status = newStatus))
    }

}
