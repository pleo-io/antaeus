/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.util.*

class InvoiceService(private val dal: AntaeusDal) {
    // TODO: filter out paid invoices, paginate over invoices
    suspend fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    suspend fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    private val logger = KotlinLogging.logger {}

    suspend fun chargeInvoice(invoiceId: Int, chargeAction: (invoice: Invoice) -> Boolean): Invoice {
        val invoice = fetch(invoiceId)
        val invoicePaymentId = dal.createInvoicePayment(invoice.amount, invoice, false, Date())
        return dal.withTransaction {
            val success = chargeAction(invoice);
            if (success) {
                dal.updateInvoicePaymentStatus(invoicePaymentId, true, Date())
                dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            }
            fetch(invoice.id)
        }
    }
}
