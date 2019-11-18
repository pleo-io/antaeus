package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.infrastructure.dto.InvoiceBillingWorkerTaskDTO
import io.pleo.antaeus.core.scheduler.TaskScheduler
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Schedule
import java.time.OffsetDateTime
import mu.KotlinLogging
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.quartz.impl.StdSchedulerFactory

/**
 * A class wrapping a Quartz [Scheduler] to provide a mechanism to trigger scheduling
 * of invoices at a configured interval (every minute, hourly, daily) via a cron job.
 *
 * This class forms the entry point of the invoice billing scheduling mechanism.
 * The Job it starts schedules invoices that are still outstanding (i.e. those whose
 * status is not [InvoiceStatus.PAID] by delegating to the [TaskScheduler].
 *
 * An invoice that is successfully scheduled for billing is updated with a newly
 * introduced [InvoiceStatus.SCHEDULED]
 */
class BillingService(
        private val destinationQueue: String,
        private val billingSchedulingJobCron: String,
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService,
        private val taskScheduler: TaskScheduler,
        private val globalBillingSchedule: Schedule
) {
    private val triggerName = "billingSchedulingTrigger"
    private val jobName = "billingSchedulingJob"

    private val cronJobScheduler: Scheduler = StdSchedulerFactory().scheduler
    private val cronJob: JobDetail
    private val cronJobTrigger: Trigger

    init {
        cronJob = JobBuilder
                .newJob(BillingSchedulingJob::class.java)
                .withIdentity(jobName)
                .build()

        cronJobTrigger = TriggerBuilder
                .newTrigger()
                .withIdentity(triggerName)
                .withSchedule(CronScheduleBuilder.cronScheduleNonvalidatedExpression(this.billingSchedulingJobCron))
                .usingJobData(
                        JobDataMap(
                                mapOf(
                                        "destinationQueue" to this.destinationQueue,
                                        "invoiceService" to this.invoiceService,
                                        "customerService" to this.customerService,
                                        "taskScheduler" to this.taskScheduler,
                                        "globalBillingSchedule" to this.globalBillingSchedule
                                )
                        )
                )
                .forJob(cronJob)
                .build()
    }

    fun scheduleBilling(): Boolean {
        try {
            when {
                !cronJobScheduler.isStarted -> {
                    cronJobScheduler.scheduleJob(cronJob, cronJobTrigger)
                    cronJobScheduler.start()
                }
                else -> cronJobScheduler.rescheduleJob(TriggerKey.triggerKey(triggerName), cronJobTrigger)
            }
            return true
        }
        catch (e: Exception) {
            throw e
        }
    }

    class BillingSchedulingJob : Job {
        private val logger = KotlinLogging.logger {}

        override fun execute(context: JobExecutionContext) {
            val destinationQueue = context.mergedJobDataMap["destinationQueue"] as String
            val invoiceService = context.mergedJobDataMap["invoiceService"] as InvoiceService
            val customerService = context.mergedJobDataMap["customerService"] as CustomerService
            val taskScheduler = context.mergedJobDataMap["taskScheduler"] as TaskScheduler
            val globalBillingSchedule = context.mergedJobDataMap["globalBillingSchedule"] as Schedule

            try {
                val outstandingInvoices = invoiceService.fetchAllPending()
                if (outstandingInvoices.isEmpty())  {
                    logger.info("There are no pending invoices to schedule")
                    return
                }
                outstandingInvoices.forEach {
                    // precedence order: invoice schedule -> customer schedule -> global schedule
                    val billingSchedule = it.billingSchedule
                            ?: customerService.fetch(it.customerId).billingSchedule
                            ?: globalBillingSchedule

                    val scheduled = taskScheduler.schedule(
                            destination = destinationQueue,
                            payload = InvoiceBillingWorkerTaskDTO(
                                    invoiceId = it.id,
                                    scheduledOn = OffsetDateTime.now()
                            ),
                            schedule = billingSchedule
                    )
                    if (scheduled) invoiceService.updateStatus(it.id, InvoiceStatus.SCHEDULED)
                }
            }
            catch (ex: Exception) {
                logger.error(ex) { "An error: '${ex.message}' has occurred" }
                throw JobExecutionException(ex, true)
            }
        }
    }
}
