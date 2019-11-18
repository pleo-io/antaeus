package io.pleo.antaeus.core.workers.billing

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.infrastructure.dto.InvoiceBillingWorkerDTO
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.workers.AbstractWorker
import io.pleo.antaeus.core.workers.interceptor.PreExecutionValidatorInterceptor
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class InvoiceBillingWorker(
        private val invoiceService: InvoiceService,
        private val preExecutionValidatorChain: List<PreExecutionValidatorInterceptor<Invoice>>,
        private val paymentProvider: PaymentProvider
): AbstractWorker<InvoiceBillingWorkerDTO>() {

    private val logger = KotlinLogging.logger {}

    override fun handle(workerDTO: InvoiceBillingWorkerDTO) {
        try {
            val invoice = this.invoiceService.fetch(workerDTO.invoiceId)
            logger.info("Billing invoice: '${invoice.id}' for customer: '${invoice.customerId}")

            // pre-execution checks
            if (!this.validate(invoice)) return

            val response = this.paymentProvider.charge(invoice)
            // handle response
        } catch (ex: InvoiceNotFoundException) {
            // Invoice was not found
        } catch (ex: CustomerNotFoundException) {
            // Customer was not found
        }
    }

    private fun validate(invoice: Invoice): Boolean {
        val result = true
        this.preExecutionValidatorChain.forEach { validator -> result && validator.handle(invoice) }
        return result
    }
}
