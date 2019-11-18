/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.app.config.AppConfiguration
import io.pleo.antaeus.core.infrastructure.messaging.activemq.ActiveMQAdapter
import io.pleo.antaeus.core.scheduler.DefaultScheduler
import io.pleo.antaeus.core.scheduler.TaskScheduler
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.workers.billing.InvoiceBillingWorker
import io.pleo.antaeus.core.workers.billing.validator.ValidateInvoiceStatusInterceptor
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.Schedule
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.sql.Connection
import java.time.Clock

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(
                AppConfiguration.databaseUrl,
                AppConfiguration.databaseDriver,
                AppConfiguration.databaseUser,
                AppConfiguration.databasePassword)
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

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)
    val scheduler: TaskScheduler = DefaultScheduler(Clock.systemUTC(), ActiveMQAdapter())

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(invoiceService, scheduler, Schedule())

    billingService.scheduleBilling()

    InvoiceBillingWorker(
            invoiceService = invoiceService,
            preExecutionValidatorChain = listOf(ValidateInvoiceStatusInterceptor()),
            paymentProvider = paymentProvider
    )

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}

