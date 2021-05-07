package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.factories.createInvoice
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import setupInitialData
import java.util.*

class AntaeusDalTest {
    private val db by lazy { Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;", "org.h2.Driver", "root", "") }
    val tables = arrayOf(InvoiceTable, InvoicePaymentTable, CustomerTable)

    val dal = AntaeusDal(db = db)

    @BeforeEach
    fun before() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // Drop all existing tables to ensure a clean slate on each run
            SchemaUtils.drop(*tables)
            // Create all tables
            SchemaUtils.create(*tables)
            runBlocking {
                setupInitialData(dal = dal)
            }
        }
    }

    @Test
    internal fun `retrieve invoices`() = runBlocking {
        val invoices = dal.fetchInvoices();
        Assertions.assertEquals(1000, invoices.size);
    }

    @Test
    internal fun `create invoice`() = runBlocking {
        val newInvoice = createInvoice();
        val customer = Customer(newInvoice.customerId, newInvoice.amount.currency);

        val invoice = dal.createInvoice(amount = newInvoice.amount, customer = customer, targetDate = Date())
        Assertions.assertEquals(1001, invoice?.id);
    }
}