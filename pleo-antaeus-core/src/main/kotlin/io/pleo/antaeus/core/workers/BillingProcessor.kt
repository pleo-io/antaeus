package io.pleo.antaeus.core.workers

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.*

const val PROCESSORS_NUMBER = 8

class BillingProcessor(
    private val billingService: BillingService,
    private val invoiceService: InvoiceService,
    private val processorsNum: Int = PROCESSORS_NUMBER
) {

    private val logger = KotlinLogging.logger {}

    suspend fun process(targetDate: Date) = coroutineScope {
        val producer = invoiceGenerator(targetDate)
        repeat(processorsNum) {
            // TODO: gather info about processed jobs, use actor to handle this
            billingProcessor(it, producer)
        }
    }

    fun CoroutineScope.invoiceGenerator(targetDate: Date) = produce {
        val invoices = invoiceService.fetchBy(status = InvoiceStatus.PENDING, targetDate = targetDate)
        logger.info { "${invoices.count()} invoices to charge ${invoices.map { it.id }}" }
        invoices.forEach { send(it) }
    }

    suspend fun billingProcessor(id: Int, channel: ReceiveChannel<Invoice>) = coroutineScope {
        launch {
            for (invoice in channel) {
                try {
                    logger.info { "Processor #$id received $invoice" }
                    billingService.chargeInvoice(invoice)
                } catch (e: Exception) {
                    logger.error(e) { "Unexpected Error: Invoice Charge" }
                }
            }
        }
    }
}