package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.schedulers.BillingScheduler
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    private val log = KotlinLogging.logger {}

    /**
     * Charges all the pending invoices and return the list of paid invoices
     */
    fun chargePendingInvoices(): List<Invoice> {

        val paidInvoices = mutableListOf<Invoice>()

        val pendingInvoices = invoiceService.fetchPendingInvoices()
        pendingInvoices.forEach {
            try {
                if (paymentProvider.charge(it)) {
                    paidInvoices.add(invoiceService.setInvoicePaid(it.id))
                    log.info("Invoice ${it.id} successfully paid!")
                } else {
                    log.warn { "Invoice ${it.id} cannot be paid: customer ${it.customerId} account balance did not allow the charge" }
                    // TODO notify the business: message queue, db record, ...
                }
            } catch (e: CustomerNotFoundException) {
                log.error("Invoice ${it.id} cannot be paid: customer ${it.customerId} not found")
                // TODO notify admin of this data inconsistency: message queue, log alert, ...
            } catch (e: CurrencyMismatchException) {
                log.error("Invoice ${it.id} cannot be paid: currency mismatch with customer ${it.customerId}")
                // TODO notify business that actions must be taken to fix the problem: message queue, db record, ...
            } catch (e: NetworkException) {
                log.error("Invoice ${it.id} cannot be paid: network issue")
                // TODO add this invoice in a queue so the billing can be retried later
            }
        }

        log.info("${paidInvoices.size} have been paid on ${pendingInvoices.size}")
        return paidInvoices.toList()
    }

    /**
     * Schedule billing on 1st day of each month
     *
     * @throws BillingSchedulerException scheduler fail to start
     */
    @Throws(BillingSchedulerException::class)
    fun scheduleMonthly(): Boolean {
        val firstOfTheMonthCronExp = "0 0 0 1 * ?"
        return schedule(firstOfTheMonthCronExp)
    }

    /**
     * Schedule billing with a custom cron expression or 1st of each month if null
     *
     * @throws BillingSchedulerException scheduler fail to start
     * @throws InvalidCronException cronExp is invalid
     */
    @Throws(BillingSchedulerException::class, InvalidCronException::class)
    fun schedule(cronExp: String?): Boolean {
        if (cronExp == null || cronExp.isEmpty()) {
            throw InvalidCronException()
        }

        val billingScheduler = BillingScheduler(this)
        log.info("Scheduling pending invoices billing...")
        return billingScheduler.start(cronExp)
    }
}
