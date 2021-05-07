package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    suspend fun chargeInvoice(invoice: Invoice): Invoice {
        return invoiceService.chargeInvoice(invoice.id) { existingInvoice ->
            paymentProvider.charge(existingInvoice)
        }
        TODO("check current invoice status and target date")
    }
}
