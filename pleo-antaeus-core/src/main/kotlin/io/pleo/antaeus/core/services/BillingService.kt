package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.time.*

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {
    private var insufficientFundsRetryQueue: MutableList<Invoice> = mutableListOf()
    private var currencyMismatchRetryQueue: MutableList<Invoice> = mutableListOf()
    private var networkErrorRetryQueue: MutableList<Invoice> = mutableListOf()

    // Get all pending invoices and start charging payments
    suspend fun start() {
        val pendingInvoices = invoiceService.fetchAllWithStatus(InvoiceStatus.PENDING)
        chargeAll(pendingInvoices)
        val allInvoices = invoiceService.fetchAll()
        val unpaidInvoices = allInvoices.filterNot { invoice -> invoice.status != InvoiceStatus.PAID }
        if (unpaidInvoices.isEmpty()) notifyAdminOnSlack("All billings are successfully completed for this month.")
    }

    // Charge the payments for a list of invoices and recurse through the queues of failed invoice lists
    private suspend fun chargeAll(invoices: List<Invoice>) {
        if (invoices.isEmpty()) return
        invoices.forEach { invoice -> charge(invoice) }
        // Wait for 2 minutes to retry billings failed due to network
        delay(120000)
        chargeAll(networkErrorRetryQueue)
        networkErrorRetryQueue = networkErrorRetryQueue.filter { invoice -> invoice.status != InvoiceStatus.NETWORKFAIL } as MutableList<Invoice>
        chargeAll(currencyMismatchRetryQueue)
        currencyMismatchRetryQueue = currencyMismatchRetryQueue.filter { invoice -> invoice.status != InvoiceStatus.CURRENCYMISMATCH } as MutableList<Invoice>
        // Wait for 24 hours to retry charging the invoices that had insufficient funds
        delay((24 * 60 *60 * 1000).toLong())
        chargeAll(insufficientFundsRetryQueue)
        insufficientFundsRetryQueue = insufficientFundsRetryQueue.filter { invoice -> invoice.status != InvoiceStatus.INSUFFICIENTFUNDS } as MutableList<Invoice>
    }

    // Charge the payment of an invoice
    fun charge(invoice: Invoice): Invoice {
        try {
            val paymentCharged = paymentProvider.charge(invoice)
            if (paymentCharged) {
                invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID)
                return invoice
            }
            if (invoice.status == InvoiceStatus.INSUFFICIENTFUNDS) {
                val message = "Transaction for customer ${invoice.customerId} failed for " +
                        "${LocalDate.now().dayOfMonth} times dues to insufficient funds."
                notifyAdminOnSlack(message)
                // Notify the customer only in every 5 days until the payment is charged
                if((LocalDate.now().dayOfMonth).rem(5) == 0) notifyCustomerByEmail("Insufficient funds")
                return invoice
            }
            val updatedInvoice = invoiceService.updateStatus(invoice.id, InvoiceStatus.INSUFFICIENTFUNDS)
            insufficientFundsRetryQueue.add(updatedInvoice)
        }
        catch (e: CurrencyMismatchException) {
            var updatedInvoice = invoiceService.updateStatus(invoice.id, InvoiceStatus.CURRENCYMISMATCH)
            logger.debug { "The currency for customer with id ${invoice.customerId} and the currency for " +
                    "customer's invoice did not match. Original error: $e" }
            // Overwrite the currency in invoice table with the one from customers table
            val customer = customerService.fetch(updatedInvoice.customerId)
            updatedInvoice = invoiceService.updateCurrency(updatedInvoice.id, currency = customer.currency)
            currencyMismatchRetryQueue.add(updatedInvoice)
        }
        catch (e: CustomerNotFoundException) {
            invoiceService.updateStatus(invoice.id, InvoiceStatus.CUSTOMERNOTFOUND)
            logger.debug { "Customer with id ${invoice.customerId} was not found." }
        }
        catch (e: NetworkException) {
            networkErrorRetryQueue.add(invoice)
            logger.debug { "There was a problem with the network, will retry the request in 60 seconds. Original error: $e" }
        }
        return invoice
    }

    private fun notifyAdminOnSlack(message: String) {
        // Send a message to a Slack channel regarding billings
        logger.info { message }
    }

    private fun notifyCustomerByEmail(subject: String) {
        // Send an email to the customer regarding the subject
    }
}