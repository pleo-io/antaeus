package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val PaymentScope = CoroutineScope(Dispatchers.IO)

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun billInvoices(status: String): List<Invoice> {
        val pendingInvoices = invoiceService.fetchByStatus(status)
        pendingInvoices.forEach {
            PaymentScope.launch{
                val paymentflow = processPayment(it)
                paymentflow.collect {
                    val result = it
                    logger.info { result }
                }
            }
        }

        return invoiceService.fetchByStatus(InvoiceStatus.PAID.toString())
    }

    fun billInvoice(id: Int): Invoice {
        val invoice = invoiceService.fetch(id)
        PaymentScope.launch{
            if (invoice.status != InvoiceStatus.PAID) {
                val paymentflow = processPayment(invoice)

                paymentflow.collect {
                    val result = it
                    logger.info { result }
                }
            }
        }

        return invoice
    }

    private fun processPayment(invoice: Invoice): Flow<Result<Boolean>> {
        return flow {
            val charge = paymentProvider.charge(invoice)
            when(charge){
                true -> { invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID.toString()) }
                false -> { invoiceService.updateStatus(invoice.id, InvoiceStatus.UNPAID.toString()) }
            }
            emit(Result.success(charge))
        }.retryWhen { cause, attempt ->
            if (cause is NetworkException && attempt < 2) {
                return@retryWhen true
            } else if(cause is CurrencyMismatchException) {
                logger.error(cause) { CurrencyMismatchException(invoice.id, invoice.customerId) }
                invoiceService.updateStatus(invoice.id, InvoiceStatus.CURRENCY_MISMATCH.toString())
                return@retryWhen false
            } else if(cause is CustomerNotFoundException) {
                logger.error(cause) { CustomerNotFoundException(invoice.customerId) }
                invoiceService.updateStatus(invoice.id, InvoiceStatus.INVALID_CUSTOMER.toString())
                return@retryWhen false
            } else {
                logger.error(cause) { NetworkException() }
                invoiceService.updateStatus(invoice.id, InvoiceStatus.FAILED.toString())
                return@retryWhen false
            }
        }
    }
}
