package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    // FIXME use a proper logger framework instead of println
    /*
     * Charges all the pending invoices and return the list of paid invoices
     */
    fun chargePendingInvoices(): List<Invoice> {
        val invoices = invoiceService.fetchPendingInvoices()
        val paidInvoices: List<Invoice> = mutableListOf()
        mutableListOf<Invoice>()
        for (i in invoices) {
            try {
                if (paymentProvider.charge(i)) {
                    invoiceService.setInvoicePaid(i.id)
                    println("INFO - Invoice " + i.id + " successfully paid!")
                }
            } catch (e: CustomerNotFoundException) {
                println("WARNING - Invoice " + i.id + " cannot be paid because customer " + i.customerId + " could not be found")
                // TODO notify admin of this data inconsistency (using a message queue for example)
            } catch (e: CurrencyMismatchException) {
                println("WARNING - Invoice " + i.id + " cannot be paid because currency doesn't match the one of the customer " + i.customerId)
                // TODO notify business that actions must be taken to fix the problem (message queue)
            } catch (e: NetworkException) {
                println("WARNING - Invoice " + i.id + " cannot be paid because of a network issue")
                // TODO add this invoice in a queue so the billing can be retried later
            }
        }
        return paidInvoices
    }

}
