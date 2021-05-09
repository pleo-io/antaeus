package io.pleo.antaeus.core.scheduler

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

const val PROCESSORS_NUMBER = 8

class SchedulerTest {
    private val logger = KotlinLogging.logger {}

    private val tables = arrayOf(InvoiceTable, InvoicePaymentTable, CustomerTable)

    private val db by lazy { Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;", "org.h2.Driver", "root", "") }
    private val dal = AntaeusDal(db = db)


    private val invoiceService = InvoiceService(dal = dal)

    private val paymentProvider = mockk<PaymentProvider> {
        coEvery { charge(any()) } returns true
    }

    private val billingService = BillingService(paymentProvider, invoiceService)

    @BeforeEach
    fun before() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // Drop all existing tables to ensure a clean slate on each run
            SchemaUtils.drop(*tables)
            // Create all tables
            SchemaUtils.create(*tables)
            runBlocking {
                setupInitialData(dal = dal, customersNum = 100, invoicesPerCustomerNum = 10)
            }
        }
    }

    @Test
    fun `run scheduled task`() = runBlocking(Dispatchers.Default) {
        val scheduler = Scheduler()
        val job = scheduler.scheduleExecution(nextMonthDate()) { date ->
            val producer = invoiceGenerator(date, invoiceService)
            repeat(PROCESSORS_NUMBER) {
                billingProcessor(it, producer)
            }
        }
        job.join()
        println("test ended")
//        assertEquals(0, dal.invoicesByStatusAndTargetDateCount(InvoiceStatus.PENDING.toString(), nextMonthDate()))
    }

    private fun CoroutineScope.billingProcessor(id: Int, channel: ReceiveChannel<Invoice>) = launch {
        for (invoice in channel) {
            try {
                logger.info { "${Thread.currentThread().name} Processor #$id received $invoice" }
                billingService.chargeInvoice(invoice)
            } catch (e: Exception) {
                logger.error(e) { "Unexpected Error: Invoice Charge" }
            }
        }
    }

    fun CoroutineScope.invoiceGenerator(targetDate: Date, invoiceService: InvoiceService) = produce {
        val invoices = invoiceService.fetchByStatusAndTargetDate(InvoiceStatus.PENDING, targetDate)
        logger.info { "invoices ${invoices.map { it.id }}" }
        invoices.forEach { send(it) }
    }
}