package io.pleo.antaeus.core.workers.billing

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.CurrencyConversionProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.infrastructure.dto.InvoiceBillingWorkerTaskDTO
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.workers.AbstractWorker
import io.pleo.antaeus.core.workers.interceptor.PreExecutionValidationInterceptor
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

/**
 * An invoicing billing worker that extends an [AbstractWorker]
 */
class InvoiceBillingWorker(
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService,
        private val preExecutionValidatorChain: List<PreExecutionValidationInterceptor<Invoice>>,
        private val paymentProvider: PaymentProvider,
        private val currencyConversionProvider: CurrencyConversionProvider
): AbstractWorker<InvoiceBillingWorkerTaskDTO>(InvoiceBillingWorkerTaskDTO::class.java) {

    private val logger = KotlinLogging.logger {}

    override val queueName = System.getenv("INVOICE_BILLING_QUEUE")!!

    override fun handle(workerDTO: InvoiceBillingWorkerTaskDTO) {
        val invoiceId = workerDTO.invoiceId
        val invoice = this.fetchInvoice(invoiceId) ?: return
        try {
            if (!this.preExecutionChecks(invoice)) return
            logger.info {"Attempting to charge invoice='${invoiceId}' for customer='${invoice.customerId}"}
            val response = this.paymentProvider.charge(invoice)
            this.handleResponse(invoice, response)
        } catch (ex: InvoiceNotFoundException) {
            this.handleInvoiceNotFoundException(ex)
        } catch (ex: CustomerNotFoundException) {
            this.handleCustomerNotFoundException(ex)
        } catch (ex: CurrencyMismatchException) {
            this.handleCurrencyMismatchException(invoice, ex)
        } catch (ex: NetworkException) {
            this.handleNetworkException(invoice, ex)
        }
    }

    private fun fetchInvoice(invoiceId: Int): Invoice? {
        return try {
            this.invoiceService.fetch(invoiceId)
        } catch (ex: InvoiceNotFoundException) {
            this.handleInvoiceNotFoundException(ex)
            null
        }
    }

    private fun preExecutionChecks(invoice: Invoice): Boolean {
        val result = true
        this.preExecutionValidatorChain.forEach { validator -> result && validator.validate(invoice) }
        logger.info {"Pre-execution validation checks result='${result}'"}
        return result
    }

    private fun handleResponse(invoice: Invoice, response: Boolean) {
        val status = if (response) InvoiceStatus.PAID else InvoiceStatus.FAILED
        logger.info {
            "Updating invoice with id='${invoice.id}' with status='${status.name}' having received " +
                    "response='$response' from payment provider"
        }
        this.invoiceService.updateStatus(invoice.id, status)
    }

    /**
     * A handle for an InvoiceNotFoundException
     */
    private fun handleInvoiceNotFoundException(ex: InvoiceNotFoundException) {
        logger.error(ex) {"An invoice with id='${ex.invoiceId}' was not found"}
    }

    /**
     * A handle for a CustomerNotFoundException
     */
    private fun handleCustomerNotFoundException(ex: CustomerNotFoundException) {
        logger.error(ex) {"A customer with id='${ex.customerId}' was not found"}
    }

    /**
     * A handle for a CurrencyMismatchException
     */
    private fun handleCurrencyMismatchException(invoice: Invoice, ex: CurrencyMismatchException) {
        logger.error(ex) { ex.message }
        // try currency conversion
        try {
            val customer = customerService.fetch(invoice.customerId)
            currencyConversionProvider.convert(invoice.amount, customer.currency)
        } catch (ex: CurrencyNotSupportedException) {
            logger.error(ex) { ex.message }
        }
        this.retry()
    }

    /**
     * A handle for a NetworkException
     */
    private fun handleNetworkException(invoice: Invoice, ex: NetworkException) {
        logger.error(ex) { ex.message }
        this.retry()
    }

    /**
     * Retry logic
     */
    private fun retry() {
       logger.info { "Retrying" }
        // TODO: add logic here
    }
}
