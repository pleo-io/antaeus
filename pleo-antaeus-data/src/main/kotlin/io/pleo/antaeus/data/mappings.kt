/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toInvoice(): Invoice = Invoice(
        id = this[InvoiceTable.id],
        amount = Money(
                value = this[InvoiceTable.value],
                currency = Currency.valueOf(this[InvoiceTable.currency])
        ),
        status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
        customerId = this[InvoiceTable.customerId],
        targetDate = this[InvoiceTable.targetDate].toDate(),
        createdAt = this[InvoiceTable.createdAt].toDate()
)

fun ResultRow.toInvoicePayment(): InvoicePayment = InvoicePayment(
        id = this[InvoicePaymentTable.id],
        amount = Money(
                value = this[InvoicePaymentTable.value],
                currency = Currency.valueOf(this[InvoicePaymentTable.currency])
        ),
        paymentDate = this[InvoicePaymentTable.paymentDate].toDate(),
        success = this[InvoicePaymentTable.success],
        invoiceId = this[InvoicePaymentTable.invoiceId]
)

fun ResultRow.toCustomer(): Customer = Customer(
        id = this[CustomerTable.id],
        currency = Currency.valueOf(this[CustomerTable.currency])
)
