package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Message
import mu.KotlinLogging
import java.time.Instant

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
// TODO - turn into asynchronous behaviour
    private val log = KotlinLogging.logger("BillingService")

    fun start() {
        //TODO schedule the execution for monthlyBilling
        val pendingInvoices = invoiceService.fetchByStatus(InvoiceStatus.PENDING)
        executeBilling(pendingInvoices)
    }

    private fun executeBilling (invoices: List<Invoice>) {

        for (invoice in invoices) {
            charge(invoice)
        }

        handleProblematicInvoices()
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
            //TODO try again in, say, 1 min; retry 3 times before logging it
        }

    }

    private fun handleProblematicInvoices() {
        val now = Instant.now().toEpochMilli()
        //logs for the last 24h - property ideally kept in a config file
        if(invoiceService.fetchInvoiceLogs(now - 24*60*60*60*1000L, now).isNotEmpty())
            log.debug("There are invoices that need further handling")
        // TODO send a message with the amount of logs
        else
            log.debug { "All invoices have been charged successfully" }

        // TODO automate the handling process
    }
}
