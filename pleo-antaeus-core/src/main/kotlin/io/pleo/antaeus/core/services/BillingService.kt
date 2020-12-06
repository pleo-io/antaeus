package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.BillNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*


class BillingService(
    private val dal: AntaeusDal,
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {
// TODO - Add code e.g. here

    fun fetchAll(): List<Bill> {
        return dal.fetchBills()
    }

    fun fetch(id: Int): Bill {
        return dal.fetchBill(id) ?: throw BillNotFoundException(id)
    }

    fun chargeAll(): String {

        val allInvoices = invoiceService.fetchAll()
        allInvoices.forEach {
            if (it.status == InvoiceStatus.PENDING) {
                var status = BillingStatus.PAYMENTFAILED
                var failureReason = "dummy"
                val currentCustomer = customerService.fetch(it.customerId)
                try {
                    if (it.amount.currency != currentCustomer.currency) {
                        failureReason = "Currency Mismatch between Invoice and Customer"
                        throw CurrencyMismatchException(it.id, it.customerId)
                    }
                    if (paymentProvider.charge(it)) {
                        status = BillingStatus.PAYMENTSUCCESFUL
                        failureReason = "None"
                        dal.updateInvoiceStatus(it.id,status=InvoiceStatus.PAID)
                    } else {

                        status = BillingStatus.PAYMENTFAILED
                        failureReason = "Something wrong at the Payment Gateway"
                    }


                } finally {
                    dal.createBill(
                            invoiceId = it.id,
                            status = status,
                            failureReason = failureReason

                    )


                }


            }
        }
        return "Monthly check complete"


    }

}
