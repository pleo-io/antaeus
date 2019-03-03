package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.FAILED
import io.pleo.antaeus.models.InvoiceStatus.IN_PROGRESS
import io.pleo.antaeus.models.InvoiceStatus.PAID
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import io.pleo.antaeus.models.InvoiceStatus.UNKNOWN
import mu.KLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.onErrorResume
import reactor.core.scheduler.Schedulers
import java.time.Duration.ofSeconds

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {

    private companion object : KLogging()

    fun chargeAll() =
        pendingInvoices()
            .limitRate(200, 0)
            .flatMap({ invoice ->
                charge(invoice)
                    .doOnNext { newStatus -> dal.updateStatus(newStatus = newStatus, invoiceId = invoice.id) }
                    .doOnError { logger.error("Unable to charge invoice $invoice.", it) }
                    .map { invoice.copy(status = it) }
                    .onErrorResume { Mono.empty() }
            }, 50)
            .doOnComplete { logger.info { "All done." } }

    private fun pendingInvoices() =
        Flux.create<Invoice> { sink ->
            sink.onRequest { n ->
                val invoices = dal.fetchInvoicesAndChangeStatus(
                    status = PENDING,
                    newStatus = IN_PROGRESS,
                    limit = n.toInt()
                )
                when (invoices.size) {
                    0 -> sink.complete()
                    else -> {
                        invoices.forEach { sink.next(it) }
                        if (invoices.size < n) sink.complete()
                    }

                }
            }
        }
            .subscribeOn(Schedulers.elastic())

    private fun charge(invoice: Invoice) =
        Mono
            .fromCallable {
                paymentProvider.charge(invoice)
            }
            .publishOn(Schedulers.elastic())
            .map { status ->
                when (status) {
                    true -> PAID
                    else -> FAILED
                }
            }
            .timeout(ofSeconds(5), Mono.error(NetworkException()))
            .doOnError { logger.error("Failed to charge invoice. $invoice", it) }
            .doOnNext { logger.info { "Charged invoice $invoice. Status $it" } }
            .onErrorResume(CustomerNotFoundException::class) { Mono.just(FAILED) }
            .onErrorResume(CurrencyMismatchException::class) { Mono.just(FAILED) }
            .retryBackoff(3, ofSeconds(3))
            .onErrorResume { Mono.just(UNKNOWN) }

}