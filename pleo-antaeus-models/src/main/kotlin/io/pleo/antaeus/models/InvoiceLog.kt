package io.pleo.antaeus.models

data class InvoiceLog (
    val id: Int,
    val invoiceId: Int,
    val timestamp: Long,
    val message: Message
)