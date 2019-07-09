package io.pleo.antaeus.core.exceptions

class InsufficientFundsException(invoiceId: Int, customerId: Int) :
        Exception("Customer '$customerId' did have sufficient balance to pay invoice '$invoiceId' '")
