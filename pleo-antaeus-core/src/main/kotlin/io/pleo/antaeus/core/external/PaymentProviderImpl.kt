package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import kotlin.random.Random

class PaymentProviderImpl(
    private val customerService: CustomerService
) : PaymentProvider {

    private val log = KotlinLogging.logger {}

    /*
        Charge a customer's account the amount from the invoice.

        Returns:
          `True` when the customer account was successfully charged the given amount.
          `False` when the customer account balance did not allow the charge.

        Throws:
          `CustomerNotFoundException`: when no customer has the given id.
          `CurrencyMismatchException`: when the currency does not match the customer account.
          `NetworkException`: when a network error happens.
     */
    @Throws(CurrencyMismatchException::class, CustomerNotFoundException::class, NetworkException::class)
    override fun charge(invoice: Invoice): Boolean {

        // Simulate a network issue
        if (networkIssue()) {
            log.error("Network issue")
            throw NetworkException()
        }

        // Check that invoice needs to be paid
        if (invoice.status == InvoiceStatus.PAID) {
            // TODO throw a specific exception
            log.error("Invoice ${invoice.id} is already paid!")
            return true
        }

        // Check that customer exists
        val customer = customerService.fetch(invoice.customerId) // Throws CustomerNotFound customer id does not exists

        // Check that customer and invoice currency matches
        if (customer.currency != invoice.amount.currency) {
            log.error("Currency mismatch between Invoice ${invoice.id} and customer ${customer.id}")
            throw CurrencyMismatchException(invoice.id, customer.id)
        }

        // Simulate an insufficient customer account balance
        return !insufficientFunds()
    }

    fun networkIssue(): Boolean {
        return Random.nextInt(1, 21) == 20 // 5% network issue
    }

    fun insufficientFunds(): Boolean {
        return Random.nextInt(1, 11) > 8 // 20% insufficient funds
    }
}
