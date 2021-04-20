/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.dals.InvoiceDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val invoiceDal: InvoiceDal) {
    fun fetchAll(): List<Invoice> {
        return invoiceDal.fetchInvoices()
    }

    @Throws(InvoiceNotFoundException::class)
    fun fetch(id: Int): Invoice {
        return invoiceDal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchPendingInvoices(): List<Invoice> {
        return invoiceDal.fetchPendingInvoices()
    }

    fun setInvoicePaid(id: Int): Invoice {
        fetch(id) // exception raised if invoice id doesn't exists
        return invoiceDal.updateInvoiceStatus(id, InvoiceStatus.PAID)!! // Cannot be null here
    }
}
