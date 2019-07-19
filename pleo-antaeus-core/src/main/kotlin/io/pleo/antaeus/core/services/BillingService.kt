package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider
) {

    private val logger = KotlinLogging.logger {}

    /** Single invoice payment function
     *
     * @param Invoice to pay
     * @return Invoice object whose InvoiceStatus depends on charge function's return
     */

    fun singleInvoicePayment(invoice: Invoice): Invoice {
       var isValid = false
       try {
           isValid = paymentProvider.charge(invoice)
       }
       catch (e: Exception) {
           // Handling exceptions
       }
       return if (isValid) invoice.copy(status = InvoiceStatus.PAID) else invoice.copy(status =
       InvoiceStatus.PENDING)
   }

    /** Bulk invoice payment
     *
     * @param List of Invoices to pay
     * @return List of paid invoices
     */
    fun bulkInvoicesPayment(invoicesList: List<Invoice>): List<Invoice> {
        return invoicesList.map { singleInvoicePayment(it) }
    }
}