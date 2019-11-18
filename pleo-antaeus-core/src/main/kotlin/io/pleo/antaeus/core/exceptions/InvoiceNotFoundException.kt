package io.pleo.antaeus.core.exceptions

class InvoiceNotFoundException(id: Int) : EntityNotFoundException("Invoice", id) {
    val invoiceId = id
}
