package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.infrastructure.dto.InvoiceBillingWorkerDTO
import io.pleo.antaeus.core.scheduler.TaskScheduler
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Schedule
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory


private const val CRON_EXPRESSION = "1 * * * * ?" // run every hour

private const val JOB_NAME = "billingSchedulingJob"

private const val TRIGGER_NAME = "billingSchedulingTrigger"

class BillingService(
        private val invoiceService: InvoiceService,
        private val taskScheduler: TaskScheduler,
        private val schedule: Schedule
) {
    private val scheduler: Scheduler = StdSchedulerFactory().scheduler
    private val job: JobDetail
    private val trigger: Trigger

    init {
        job = JobBuilder
                .newJob(BillingSchedulingJob::class.java)
                .withIdentity(JOB_NAME)
                .build()

        trigger = TriggerBuilder.newTrigger()
                .withIdentity(TRIGGER_NAME)
                .withSchedule(CronScheduleBuilder.cronScheduleNonvalidatedExpression(CRON_EXPRESSION))
                .usingJobData(
                        JobDataMap(
                                mapOf(
                                        "invoiceService" to invoiceService,
                                        "taskScheduler" to taskScheduler,
                                        "schedule" to schedule
                                )
                        )
                )
                .forJob(job)
                .build()
    }

    fun scheduleBilling(): Boolean {
        try {
            when {
                !scheduler.isStarted -> {
                    scheduler.scheduleJob(job, trigger)
                    scheduler.start()
                }
                else -> scheduler.rescheduleJob(TriggerKey.triggerKey(TRIGGER_NAME), trigger)
            }
            return true
        }
        catch (e: Exception) {
            throw e
        }
    }

    class BillingSchedulingJob : Job {
        private val destination = "invoice-billing"

        override fun execute(context: JobExecutionContext) {

            val invoiceService = context.mergedJobDataMap["invoiceService"] as InvoiceService
            val taskScheduler = context.mergedJobDataMap["taskScheduler"] as TaskScheduler
            val schedule = context.mergedJobDataMap["schedule"] as Schedule

            try {
                invoiceService.fetchAllPending().forEach {
                    val scheduled = taskScheduler.schedule(
                            destination = destination,
                            payload = InvoiceBillingWorkerDTO(invoiceId = it.id),
                            schedule = schedule)

                    if (scheduled) invoiceService.updateStatus(it.id, InvoiceStatus.SCHEDULED)
                }
            }
            catch (e: Exception) {
                throw JobExecutionException(e, true)
            }
        }
    }
}
