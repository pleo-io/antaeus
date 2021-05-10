package io.pleo.antaeus.core.exceptions

class InvoiceChargedException(invoiceId: Int):
    Exception("Invoice '$invoiceId' has been already charged")