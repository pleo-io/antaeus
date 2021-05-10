/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceChargedException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransaction
import java.util.*

class InvoiceService(private val dal: AntaeusDal) {
    private val logger = KotlinLogging.logger {}

    suspend fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    suspend fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    suspend fun fetchByStatusAndTargetDate(status: InvoiceStatus, targetDate: Date): Iterable<Invoice> {
        return dal.fetchInvoicesByStatusAndTargetDate(status.toString(), targetDate)
    }

    suspend fun chargeInvoice(invoiceId: Int, chargeAction: suspend (invoice: Invoice) -> Boolean): Invoice {
        dal.withTransaction {
            addLogger(StdOutSqlLogger)
            val invoice = fetch(invoiceId)
            if (invoice.status == InvoiceStatus.PAID) {
                throw InvoiceChargedException(invoiceId)
            }
            val invoicePaymentId = dal.createInvoicePayment(invoice.amount, invoice, false, Date())
            logger.info { "inv[$invoiceId] - created payment: $invoicePaymentId" }
            val success = chargeAction(invoice)
            logger.info { "inv[$invoiceId] - charge action status: $success" }
            if (success) {
                suspendedTransaction {
                    logger.info { "inv[$invoiceId] - before invoice status update" }
                    dal.updateInvoicePaymentStatus(invoicePaymentId, success = true, paymentDate = Date())
                    dal.updateInvoiceStatus(invoice.id, status = InvoiceStatus.PAID)
                    logger.info { "inv[$invoiceId] - after invoice status update" }
                }
            }
        }
        return fetch(invoiceId)
    }
}
