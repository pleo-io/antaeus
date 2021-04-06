package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val log = KotlinLogging.logger {}

    /*
     * Charges all the pending invoices and return the list of paid invoices
     */
    fun chargePendingInvoices() {
        invoiceService.fetchPendingInvoices().forEach{
            try {
                if (paymentProvider.charge(it)) {
                    invoiceService.setInvoicePaid(it.id)
                    log.info("Invoice ${it.id} successfully paid!")
                } else {
                    log.warn { "Invoice ${it.id} cannot be paid: customer ${it.customerId} account balance did not allow the charge" }
                    // TODO notify the business: message queue, db record, ...
                }
            } catch (e: CustomerNotFoundException) {
                log.error("Invoice ${it.id} cannot be paid: customer ${it.customerId} not found")
                // TODO notify admin of this data inconsistency: message queue, log alert, ...
            } catch (e: CurrencyMismatchException) {
                log.error("Invoice ${it.id} cannot be paid: currency mismatch with customer ${it.customerId}")
                // TODO notify business that actions must be taken to fix the problem: message queue, db record, ...
            } catch (e: NetworkException) {
                log.error("Invoice ${it.id} cannot be paid: network issue")
                // TODO add this invoice in a queue so the billing can be retried later
            }
        }
    }

}
