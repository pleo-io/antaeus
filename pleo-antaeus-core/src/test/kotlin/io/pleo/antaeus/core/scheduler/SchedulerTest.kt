package io.pleo.antaeus.core.scheduler

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*
import io.pleo.antaeus.core.common.factories.createInvoice as createTestInvoice

class SchedulerTest {
    private val scheduler = Scheduler()
    private val dal = mockk<AntaeusDal> {
        coEvery { fetchInvoices() } returns (0..9).map { createTestInvoice() }
    }
    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `run scheduled task`() = runBlocking {

        scheduler.schedule(Date()) {
            val producer = invoiceGenerator(invoiceService)
            repeat(5) {
                billingProcessor(it, producer)
            }
        }
        println("test started")
    }
}

fun CoroutineScope.billingProcessor(id: Int, channel: ReceiveChannel<Invoice>) = launch {
    for (msg in channel) {
        println("${Thread.currentThread().name} Processor #$id received $msg")
    }
}

fun CoroutineScope.invoiceGenerator(invoiceService: InvoiceService) = produce {
    val invoices = invoiceService.fetchAll()
    println("invoices $invoices")
    invoices.forEach { send(it) }
}