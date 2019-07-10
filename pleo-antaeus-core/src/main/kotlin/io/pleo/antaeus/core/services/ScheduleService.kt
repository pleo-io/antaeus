package io.pleo.antaeus.core.services

import com.github.shyiko.skedule.Schedule
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.time.ZonedDateTime
import kotlin.system.measureTimeMillis


class ScheduleService(private val billingService: BillingService) {

    private val logger = KotlinLogging.logger {}
    private val executor = ScheduledThreadPoolExecutor(1)

    // For the purpose of the challenge I changed put the job to run every minute. Ideally this would be some configured
    // property read at runtime of the Javalin app. If we really wanted to simulate the real app, we could pass:
    //
    // Schedule.parse("1 of month").next(ZonedDateTime.now()).toEpochSecond() - ZonedDateTime.now().toEpochSecond()
    // when it comes to reschedule the job on line 26.
    fun scheduleInvoiceJob(nextRun: Long) {
        val now = ZonedDateTime.now()
        executor.schedule(
                {
                    logger.info { "Starting invoice processing job" }
                    val time = measureTimeMillis {
                        chargeInvoices()
                    }
                    logger.info { "Finished invoice processing job in $time ms" }
                    scheduleInvoiceJob(Schedule.parse("every 1 minutes").next(now).toEpochSecond() - now.toEpochSecond())
                },
                nextRun,
                TimeUnit.SECONDS
        )
    }

    private fun chargeInvoices() {
        billingService.chargePendingInvoices()
                .groupBy(Invoice::status)
                .flatMap(Flux<Invoice>::collectList)
                .subscribe { printReport(it) }
    }

    private fun printReport(processedInvoices: List<Invoice>?) {
            logger.info { "${processedInvoices?.size} invoices were charged with status ${processedInvoices?.get(0)?.status?.name}." }
    }
}
