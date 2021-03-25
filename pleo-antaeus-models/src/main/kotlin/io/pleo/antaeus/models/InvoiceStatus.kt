package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    FAILED,
    PAID,
    UNPAID,
    CURRENCY_MISMATCH,
    INVALID_CUSTOMER
}
