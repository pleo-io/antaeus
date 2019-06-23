package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.math.BigDecimal

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {
    // TODO - Add code e.g. here
    fun chargeInvoices() {
        val billableInvoices = invoiceService.fetchAll().filter { it.status == InvoiceStatus.PENDING }.distinct()
        billableInvoices.forEach {
            val customer = customerService.fetch(it.customerId)
            validateInvoiceCurrency(it, customer)
            validateValueRange(it)
            // TODO write validate payment function
              validatePayment()
        }
        println("Total amount of pending invoices is now ${invoiceService.fetchAll().filter {it.status == InvoiceStatus.PENDING}.count()}")
    }

    private fun validateInvoiceCurrency(invoice: Invoice, customer: Customer) {
        if (invoice.amount.currency != customer.currency) throw CurrencyMismatchException(invoice.customerId, customer.id)
//        when {
//            invoice.amount.currency != customer.currency -> throw CurrencyMismatchException(invoice.customerId, customer.id)
//        }
        //TODO Change if to when maybe combine?
    }

    private fun validateValueRange(invoice: Invoice) {
        if (invoice.amount.value !in BigDecimal.ONE..BigDecimal.valueOf(500)) throw MoneyValueOutOfRangeException(invoice.id)
    }

    private fun validatePayment() {

    }
}
