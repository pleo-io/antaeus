/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

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

    // TODO: filter out paid invoices, paginate over invoices
    suspend fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    suspend fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    suspend fun chargeInvoice(invoiceId: Int, chargeAction: (invoice: Invoice) -> Boolean): Invoice {
        dal.withTransaction {
            val txId = this.hashCode()
            addLogger(StdOutSqlLogger)
            val invoice = fetch(invoiceId)
            val invoicePaymentId = dal.createInvoicePayment(invoice.amount, invoice, false, Date())
            logger.info { "$txId - created payment $invoicePaymentId" }
            val success = chargeAction(invoice)
            logger.info { "$txId - after charge action: $success" }
            if (success) {
                suspendedTransaction {
                    logger.info { "${this.hashCode()} - update invoice status" }
                    dal.updateInvoicePaymentStatus(invoicePaymentId, success = true, paymentDate = Date())
                    dal.updateInvoiceStatus(invoice.id, status = InvoiceStatus.PAID)
                }
            }
        }
        return fetch(invoiceId)
    }
}
