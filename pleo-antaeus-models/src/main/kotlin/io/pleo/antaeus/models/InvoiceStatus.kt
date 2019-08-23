package io.pleo.antaeus.models

enum class InvoiceStatus {
    INSUFFICIENTFUNDS,
    CURRENCYMISMATCH,
    CUSTOMERNOTFOUND,
    NETWORKFAIL,
    PENDING,
    PAID
}
