package io.pleo.antaeus.core.exceptions

class PaymentNotDoneException(id: Int) : Exception("Payment on user '$id' was not done")