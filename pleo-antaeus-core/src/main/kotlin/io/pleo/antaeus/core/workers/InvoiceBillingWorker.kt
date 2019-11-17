package io.pleo.antaeus.core.workers

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.infrastructure.dto.InvoiceBillingWorkerDTO
import io.pleo.antaeus.core.services.InvoiceService

class InvoiceBillingWorker(
        private val invoiceService: InvoiceService,
        private val paymentProvider: PaymentProvider
): AbstractWorker<InvoiceBillingWorkerDTO>() {

    override fun handle(invoiceBillingWorkerDTO: InvoiceBillingWorkerDTO) {
        try {
            val invoice = this.invoiceService.fetch(invoiceBillingWorkerDTO.invoiceId)

            // pre-execution checks

            val response = this.paymentProvider.charge(invoice)
            // handle response

        } catch (ex: InvoiceNotFoundException) {
            // Invoice was not found
        }
    }
}
