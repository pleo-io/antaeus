/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import io.pleo.antaeus.core.scheduler.Scheduler
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.workers.BillingProcessor
import io.pleo.antaeus.data.*
import io.pleo.antaeus.rest.AntaeusRest
import it.justwrote.kjob.InMem
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.kjob
import it.justwrote.kjob.kron.KronModule
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

// TODO fix up crontab
object MonthlyBillingJob : KronJob("monthly-billing-job", "0 0 1 * * *")

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, InvoicePaymentTable, CustomerTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = "")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    runBlocking {
        // Insert example data in the database.
        setupInitialData(dal = dal)
    }

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

    val billingProcessor = BillingProcessor(billingService, invoiceService)

    val kjob = kjob(InMem) {
        extension(KronModule)
    }.start()

    val kronScheduler = Scheduler(MonthlyBillingJob, kjob)

    kronScheduler.schedule { date ->
        billingProcessor.process(date)
    }

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}
