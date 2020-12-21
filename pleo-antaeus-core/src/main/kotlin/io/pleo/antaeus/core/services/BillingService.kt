package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService
) {

    fun chargeInvoices(invoiceStatus: InvoiceStatus): Boolean {
        when(invoiceStatus) {
            InvoiceStatus.PENDING -> handlePendingInvoices()
            //Todo: In case we have more invoice status(eg. Error, Partially Paid, Dispute), other functions can be added
        }
        logger.info { "The payment of the selected invoices has been processed" }
        return true
    }

    private fun handlePendingInvoices() {
        val pendingInvoices: List<Invoice> = invoiceService.fetchInvoicesWithStatus(InvoiceStatus.PENDING)
        for (pendingInvoice in pendingInvoices) {
            chargePendingInvoice(pendingInvoice)
        }
    }

    fun chargePendingInvoice(invoice: Invoice) {
        try {

            if (paymentProvider.charge(invoice)) {
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            } else {
                //Todo: new InvoiceStatus could be set accordingly to the error here
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.ERROR)
                logger.info { "Failed to charge the invoice ${invoice.id} from customer ${invoice.customerId}." }
            }

        } catch (e: CurrencyMismatchException) {
            //Todo: new InvoiceStatus could be set accordingly to the Exception here
            invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.ERROR)
            logger.error(e) { "The currency is not matching the invoice ${invoice.id}." }
            throw e
        } catch (e: CustomerNotFoundException) {
            //Todo: new InvoiceStatus could be set accordingly to the Exception here
            invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.ERROR)
            logger.error(e) { "The customerId ${invoice.customerId} specified in the invoice was not found ${invoice.id}." }
            throw e
        } catch (e: InvoiceNotFoundException) {
            //Todo: new InvoiceStatus could be set accordingly to the Exception here
            invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.ERROR)
            logger.error(e) { "The Invoice ${invoice.id} could not be found." }
            throw e
        } catch (e: NetworkException) {
            //Todo: new InvoiceStatus could be set accordingly to the Exception here
            invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.ERROR)
            logger.error(e) { "A network issue was found." }
            throw e
        }
    }
}
