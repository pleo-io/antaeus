package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.CustomerDebt
import java.time.LocalDateTime
import java.util.*


class BillingService(private val paymentProvider: PaymentProvider, private val dal: AntaeusDal) {

    // Example to help me remember syntax
    //fun test(name: String, age: Int): String {
    //    return "Happy ${age}th birthday, $name!"
    //}

    fun payPendingInvoices(): String {
        // In a real life scenario, you need to have a timestamp in the db proving when exactly invoices were paid off.
        // A proof is required in most case and in a case like this one it's crucial.
        val pendingInvoices = dal.fetchInvoicesPending()

        for (invoice in pendingInvoices) {
            dal.payInvoice(invoice.id)
        }
        return "All invoices were paid"
    }

    fun fetchInvoicesPerCustomer() : List<CustomerDebt>  {
        // Had no idea if a customer can or can't have multiple invoices in different currencies.
        val pendingInvoices = dal.fetchInvoicesPending()

        val listCustomerDebt = mutableListOf<CustomerDebt>()

        for (invoice in pendingInvoices) {

            if (listCustomerDebt.none { it.customerId == invoice.customerId}){
                val newCustomerDebt = CustomerDebt(invoice.customerId, mutableListOf(Money(invoice.amount.value, invoice.amount.currency)))
                listCustomerDebt.add(newCustomerDebt)
            }
            else {
                val index = listCustomerDebt.indexOfFirst{ it.customerId == invoice.customerId}
                listCustomerDebt[index].amount = listCustomerDebt[index].amount + mutableListOf(Money(invoice.amount.value, invoice.amount.currency))
            }
        }
        return listCustomerDebt
    }
}

