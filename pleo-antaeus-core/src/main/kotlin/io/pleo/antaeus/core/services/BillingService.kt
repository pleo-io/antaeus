package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    suspend fun chargeInvoice(invoice: Invoice): Invoice {
        logger.info { "inv[${invoice.id}] begin chargeInvoice" }
        return invoiceService.chargeInvoice(invoice.id) rt@{ existingInvoice ->
            retry(2, false) {
                paymentProvider.charge(existingInvoice)
            }
        }
    }
}

suspend fun <T> retry(
    times: Int,
    failureValue: T,
    delayMs: Long = 1000,
    block: suspend () -> T
) = coroutineScope rt@{

    (1..times).fold(failureValue) { _, retryNum ->
        try {
            return@rt block()
        } catch (e: NetworkException) {
            logger.error(e) { "Trying again... $retryNum" }
            delay(delayMs)
            return@fold if (retryNum == times) throw e else failureValue
        }
    }
}