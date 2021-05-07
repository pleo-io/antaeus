package io.pleo.antaeus.core.scheduler

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.junit.jupiter.api.Test
import java.lang.Runnable
import java.util.concurrent.TimeUnit
import io.pleo.antaeus.models.factories.createInvoice as createTestInvoice

class SchedulerTest {
    private val dal = mockk<AntaeusDal> {
        coEvery { fetchInvoices() } returns (0..9).map { createTestInvoice() }
    }
    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `run scheduled task`() = runBlocking {
        val task = Runnable {
            runBlocking {
                launch(Dispatchers.Default) {
                    val producer = invoiceGenerator(invoiceService)
                    repeat(5) {
                        billingProcessor(it, producer)
                    }
                }
            }
        }
        val scheduler = Scheduler(task)
        scheduler.scheduleExecution(Every(2, TimeUnit.SECONDS))
        delay(15000)
        scheduler.stop()
        println("test started")
    }

}

fun CoroutineScope.billingProcessor(id: Int, channel: ReceiveChannel<Invoice>) = launch {
    for (msg in channel) {
        println("${Thread.currentThread().name} Processor #$id received $msg")
        delay(100)
    }
}

fun CoroutineScope.invoiceGenerator(invoiceService: InvoiceService) = produce {
    val invoices = invoiceService.fetchAll()
    println("invoices $invoices")
    invoices.forEach { send(it) }
}