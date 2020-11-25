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
import kotlin.random.Random


class BillingService(private val paymentProvider: PaymentProvider, private val dal: AntaeusDal) {

    // Return the number of milliseconds between now and the next first of the month
    fun getMillisBetweenNextFirstOfTheMonth() : Long {
        val c = Calendar.getInstance()
        when {
            // AT 0:01 on the first of the month
            c.get(Calendar.DAY_OF_MONTH) != 1 && c.get(Calendar.HOUR) != 0 && c.get(Calendar.SECOND) != 1 -> {
                val currentTime = c.timeInMillis
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.add(Calendar.MONTH, 1)
                val futureTime = c.timeInMillis
                return futureTime - currentTime
            }
        }
        return 0
    }

    fun payPendingInvoices(): List<Invoice>{
        // In a real life scenario, you need to have a timestamp in the db proving when exactly invoices were paid off.
        // A proof is required in most case and in one like this it's crucial, so I'd store it in the DB
        // Also there would be a check to confirm the payment has been made and has been approved
        val pendingInvoices = dal.fetchInvoicesPending()

        for (invoice in pendingInvoices) {
            dal.payInvoice(invoice.id)
        }
        return pendingInvoices
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

