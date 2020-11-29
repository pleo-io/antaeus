/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceLog
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Message

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchByStatus(status: InvoiceStatus): List<Invoice> {
        return dal.fetchInvoicesByStatus(status)
    }

    fun updateStatus(id: Int, status: InvoiceStatus): Int {
        return dal.updateInvoiceStatus(id, status)
    }

    fun addInvoiceLog (id: Int, message: Message): Int? {
        return dal.createInvoiceLog(id, message)
    }

    fun fetchInvoiceLogs(from: Long, to: Long): List<InvoiceLog> {
        return dal.fetchInvoiceLogs(from, to)
    }
}
