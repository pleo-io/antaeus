/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.joda.time.DateTime
import java.util.Date

class AntaeusDal(private val db: Database) {
    private val logger = KotlinLogging.logger {}

    suspend fun <T> withTransaction(context: CoroutineDispatcher = Dispatchers.Default, action: suspend Transaction.() -> T): T {
        return newSuspendedTransaction(context, db, action)
    }

    suspend fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return withTransaction(Dispatchers.IO) {
            // Returns the first invoice with matching id.
            InvoiceTable
                    .select { InvoiceTable.id.eq(id) }
                    .firstOrNull()
                    ?.toInvoice()
        }
    }

    suspend fun fetchInvoices(): List<Invoice> {
        return withTransaction(Dispatchers.IO) {
            InvoiceTable
                    .selectAll()
                    .map { it.toInvoice() }
        }
    }

    suspend fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING, targetDate: Date, createdAt: Date = Date()): Invoice? {
        val id = withTransaction {
            // Insert the invoice and returns its new id.
            InvoiceTable
                    .insert {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.status] = status.toString()
                        it[this.createdAt] = DateTime(createdAt)
                        it[this.targetDate] = DateTime(targetDate)
                        it[this.customerId] = customer.id
                    } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Int {
        return InvoiceTable
            .update({ InvoiceTable.id eq id }) {
                it[this.status] = status.toString()
            }
    }

    suspend fun fetchInvoicePayments(invoiceId: Int): List<InvoicePayment> {
        return withTransaction(Dispatchers.IO) {
            InvoicePaymentTable
                    .select { InvoicePaymentTable.invoiceId.eq(invoiceId) }
                    .map { it.toInvoicePayment() }
        }
    }

    suspend fun fetchInvoicePayment(id: Int): InvoicePayment? {
        return withTransaction(Dispatchers.IO) {
            InvoicePaymentTable
                    .select { InvoicePaymentTable.id eq id }
                    .firstOrNull()
                    ?.toInvoicePayment()
        }
    }

    suspend fun createInvoicePayment(amount: Money, invoice: Invoice, success: Boolean = false, paymentDate: Date = Date()): Int {
        val id = withTransaction {
            addLogger(StdOutSqlLogger)
            logger.info {"${this.hashCode()} create invoice payment: ${invoice.id}"}

            // Insert the invoice and returns its new id.
            InvoicePaymentTable
                    .insert {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.paymentDate] = DateTime(paymentDate)
                        it[this.success] = success
                        it[this.invoiceId] = invoice.id
                    } get InvoicePaymentTable.id
        }

        return id
    }

    fun updateInvoicePaymentStatus(id: Int, success: Boolean, paymentDate: Date = Date()): Int {
        return InvoicePaymentTable
            .update({ InvoicePaymentTable.id eq id }) {
                it[this.success] = success
                it[this.paymentDate] = DateTime(paymentDate)
            }
    }

    suspend fun fetchCustomer(id: Int): Customer? {
        return withTransaction(Dispatchers.IO) {
            CustomerTable
                    .select { CustomerTable.id.eq(id) }
                    .firstOrNull()
                    ?.toCustomer()
        }
    }

    suspend fun fetchCustomers(): List<Customer> {
        return withTransaction(Dispatchers.IO) {
            CustomerTable
                    .selectAll()
                    .map { it.toCustomer() }
        }
    }

    suspend fun createCustomer(currency: Currency): Customer? {
        val id = withTransaction {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
