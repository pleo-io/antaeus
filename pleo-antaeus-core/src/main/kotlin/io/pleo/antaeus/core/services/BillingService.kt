package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.infrastructure.dto.InvoiceBillingWorkerTaskDTO
import io.pleo.antaeus.core.scheduler.TaskScheduler
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Schedule
import mu.KotlinLogging
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.time.OffsetDateTime

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
                .withSchedule(CronScheduleBuilder.cronScheduleNonvalidatedExpression(billingSchedulingJobCron))
                .usingJobData(
                        JobDataMap(
                                mapOf(
                                        "destinationQueue" to destinationQueue,
                                        "invoiceService" to invoiceService,
                                        "customerService" to customerService,
                                        "taskScheduler" to taskScheduler,
                                        "globalBillingSchedule" to globalBillingSchedule
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
