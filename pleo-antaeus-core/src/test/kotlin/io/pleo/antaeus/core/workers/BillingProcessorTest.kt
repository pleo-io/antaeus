package io.pleo.antaeus.core.workers

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.scheduler.Scheduler
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.InvoiceStatus
import it.justwrote.kjob.InMem
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.kjob
import it.justwrote.kjob.kron.KronModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

object MinuteBillingJob : KronJob("minute-billing-job", "0 * * ? * * *")

class BillingProcessorTest {
    private val now = Date()

    private val tables = arrayOf(InvoiceTable, InvoicePaymentTable, CustomerTable)
    private val db by lazy { Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;", "org.h2.Driver", "root", "") }

    private val dal = AntaeusDal(db = db)
    private val invoiceService = InvoiceService(dal = dal)

    private fun createBillingProcessor(paymentProvider: PaymentProvider): BillingProcessor {
        val billingService = BillingService(paymentProvider, invoiceService)
        return BillingProcessor(billingService, invoiceService)
    }

    private fun createPaymentProviderMock(block: () -> Boolean): PaymentProvider {
        return mockk {
            coEvery { charge(any()) } returns block()
        }
    }

    val kjob = kjob(InMem) {
        extension(KronModule)
    }

    @BeforeEach
    fun before() {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // Drop all existing tables to ensure a clean slate on each run
            SchemaUtils.drop(*tables)
            // Create all tables
            SchemaUtils.create(*tables)
            runBlocking {
                setupInitialData(
                    dal = dal,
                    targetDate = now
                )
            }
        }
    }

    @BeforeEach
    fun startKjob() {
        kjob.start()
    }

    @AfterEach
    fun stopKjob() {
        kjob.shutdown()
    }

    @Test
    fun `successful billing`() = runBlocking {
        val billingProcessor = createBillingProcessor(createPaymentProviderMock { true })

        billingProcessor.process(now)

        assertEquals(
            0, dal.countFetchInvoicesBy(
                status = InvoiceStatus.PENDING.toString(),
                targetDate = now
            )
        )
    }

    @Test
    fun `billing failure`() = runBlocking {
        val billingProcessor = createBillingProcessor(createPaymentProviderMock { false })

        billingProcessor.process(now)

        assertEquals(
            10, dal.countFetchInvoicesBy(
                status = InvoiceStatus.PENDING.toString(),
                targetDate = now
            )
        )
    }

    @Test
    fun `run billing on schedule`() = runBlocking {
        val billingProcessor = createBillingProcessor(createPaymentProviderMock { true })

        val scheduler = Scheduler(kjob)
        scheduler.kron(MinuteBillingJob) { date ->
            withContext(Dispatchers.Default) {
                billingProcessor.process(date)
            }
        }
        delay(66_000)
        assertEquals(
            0, dal.countFetchInvoicesBy(
                status = InvoiceStatus.PENDING.toString(),
                targetDate = now
            )
        )
    }

}