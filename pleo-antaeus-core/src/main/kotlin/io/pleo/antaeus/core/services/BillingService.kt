package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService) {

    private val logger = KotlinLogging.logger {}

    fun chargePendingInvoices(): Flux<Invoice> {
        return fetchPendingInvoices()
                .flatMap { invoice ->
                    chargeInvoice(invoice)
                            .flatMapMany { updateInvoiceStatus(invoice, it) }
                }
    }

    private fun fetchPendingInvoices(): Flux<Invoice> {
        return Flux.fromIterable(invoiceService.fetchPendingInvoices())
    }

    private fun chargeInvoice(invoice: Invoice): Mono<Boolean> {
        return Mono.fromCallable { paymentProvider.charge(invoice) }
    }

    private fun updateInvoiceStatus(invoice: Invoice, charged: Boolean): Mono<Invoice> {
        //TODO: update db here based on the success of the charge
        return Mono.just(invoice)
    }

}
