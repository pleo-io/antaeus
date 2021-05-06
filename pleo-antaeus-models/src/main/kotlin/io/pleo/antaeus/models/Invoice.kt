package io.pleo.antaeus.models

import java.util.*

data class Invoice(
        val id: Int,
        val customerId: Int,
        val amount: Money,
        val status: InvoiceStatus,
        var targetDate: Date,
        var createdAt: Date
)
