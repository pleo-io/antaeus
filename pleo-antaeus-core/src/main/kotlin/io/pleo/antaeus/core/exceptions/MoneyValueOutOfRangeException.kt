package io.pleo.antaeus.core.exceptions

class MoneyValueOutOfRangeException(invoiceId: Int) :
        Exception("The invoice $invoiceId has an amount outside the acceptable range'")
