package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun billInvoices() {
        val pendingInvoice = invoiceService.fetchPending()

        pendingInvoice.forEach {
            val isCharged = chargeCustomer(it)
            if (isCharged) {
                invoiceService.markAsPaid(it.id)
            }
        }
    }

    /**
     * Add error handling
     */
    private fun chargeCustomer(it: Invoice): Boolean {
        try {
            return paymentProvider.charge(it)
        } catch (e: CustomerNotFoundException) {
            logger.error { e }
        } catch (e: CurrencyMismatchException) {
            logger.error { e }
        } catch (e: NetworkException) {
            logger.error { e }
        }
        return false
    }
}
