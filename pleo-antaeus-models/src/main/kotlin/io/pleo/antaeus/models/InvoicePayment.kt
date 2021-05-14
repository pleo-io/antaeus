package io.pleo.antaeus.models

import java.util.*

data class InvoicePayment(
        val id: Int,
        val invoiceId: Int,
        val amount: Money,
        val paymentDate: Date,
        val success: Boolean
)