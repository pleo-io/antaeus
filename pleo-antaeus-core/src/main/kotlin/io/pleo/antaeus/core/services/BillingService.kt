package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Message
import mu.KotlinLogging
import java.time.Instant
import kotlinx.coroutines.*
import io.pleo.antaeus.core.services.config.ServiceConfiguration

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val scheduleService: ScheduleService
) {

    private val log = KotlinLogging.logger("BillingService")

    fun start() {

        GlobalScope.launch {
            delay(scheduleService.timeUntilNextBilling(ServiceConfiguration.billingScheme))

            val pendingInvoices = invoiceService.fetchByStatus(InvoiceStatus.PENDING)
            executeBilling(pendingInvoices)
        }
    }

    private suspend fun executeBilling (invoices: List<Invoice>) {

        for (invoice in invoices) {
            coroutineScope {
                charge(invoice)
            }
        }

        handleProblematicInvoices()

        delay(scheduleService.timeUntilNextBilling(ServiceConfiguration.billingScheme))
        executeBilling(invoiceService.fetchByStatus(InvoiceStatus.PENDING))
    }

    private fun charge(invoice: Invoice) {

        try {
            if (paymentProvider.charge(invoice)) {
                if (invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID) == 0) {
                    log.debug("Invoice was paid but status was not updated")
                    invoiceService.addInvoiceLog(invoice.id, Message.INVOICE_PAID)
                }
            }
            else {
                log.debug("Insufficient funds")
                invoiceService.addInvoiceLog(invoice.id, Message.INSUFFICIENT_FUNDS)
            }
        } catch (e: CustomerNotFoundException) {
            invoiceService.addInvoiceLog(invoice.id, Message.CUSTOMER_NOT_FOUND)
        } catch (e: CurrencyMismatchException) {
            invoiceService.addInvoiceLog(invoice.id, Message.CURRENCY_MISMATCH)
        } catch (e: NetworkException) {
            log.debug("Network error")
            invoiceService.addInvoiceLog(invoice.id, Message.NETWORK_ERROR)
            //TODO retry mechanism before logging
        }

    }

    fun handleProblematicInvoices() {
        //TODO separate trouble shooting service that periodically checks for new logs
        val now = Instant.now().toEpochMilli()
        if(invoiceService.fetchInvoiceLogs(now - ServiceConfiguration.invoiceTroubleShootingHeartbeatMs, now).isNotEmpty())
            log.debug("There are invoices that need further handling")
        // TODO send a message with the amount of logs
        else
            log.debug { "All invoices have been charged successfully" }
    }
}
