package io.pleo.antaeus.app

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import kotlin.random.Random

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            if (Random.nextInt(100) < 5) {
                throw NetworkException()
            }
            if (invoice.customerId == 1) {
                throw CustomerNotFoundException(invoice.customerId)
            }
            if (invoice.amount.currency == Currency.GBP) {
                throw CurrencyMismatchException(invoiceId = invoice.id, customerId = invoice.customerId)
            }
            return Random.nextBoolean()
        }
    }
}