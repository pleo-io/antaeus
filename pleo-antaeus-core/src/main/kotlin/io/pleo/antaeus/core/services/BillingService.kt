package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun billInvoices() {
        val pendingInvoice = invoiceService.fetchPending()

        pendingInvoice.forEach {
            if(paymentProvider.charge(it)) {
                invoiceService.markAsPaid(it.id)
            }
        }
    }
}
