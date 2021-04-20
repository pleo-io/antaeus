package io.pleo.antaeus.core.schedulers

import io.pleo.antaeus.core.exceptions.BillingSchedulerException
import io.pleo.antaeus.core.exceptions.InvalidCronException
import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

private val log = KotlinLogging.logger {}

private const val BILLING_SERVICE_KEY = "billingService"

/**
 * Quartz scheduler with a predefined job charging all pending invoices. Execute on the 1st of each month by default.
 * WARNING: This scheduler is non-clustered and must be started by calling a start method.
 */
class BillingScheduler(
    billingService: BillingService
) {

    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()
    private val triggerKey: TriggerKey = TriggerKey.triggerKey("billingTrigger")

    private val job: JobDetail

    init {
        val jobDataMap = JobDataMap()
        jobDataMap[BILLING_SERVICE_KEY] = billingService

        // Job will always be the same
        job = JobBuilder.newJob(BillingJob::class.java)
            .withIdentity("billingJob")
            .usingJobData(jobDataMap)
            .build()
    }

    /**
     * Schedule/Reschedule a BillingJob with the given quartz cron expression
     *
     * @return true if the scheduler is running with the given cron
     * @throws BillingSchedulerException SchedulerException while scheduling or starting
     * @throws InvalidCronException cronExp is invalid
     */
    @Throws(BillingSchedulerException::class, InvalidCronException::class)
    fun start(cronExp: String): Boolean {

        val cronSchedule: CronScheduleBuilder
        try {
            cronSchedule = CronScheduleBuilder.cronSchedule(cronExp)
        } catch (e: RuntimeException) {
            log.error("Invalid cron expression: $cronExp")
            throw InvalidCronException()
        }

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .withSchedule(cronSchedule)
            .build()

        try {
            // Schedule or reschedule the job
            if (scheduler.isStarted) {
                log.info("Rescheduling billing job with cron: $cronExp ...")
                scheduler.rescheduleJob(triggerKey, trigger)
            } else {
                log.info("Scheduling billing job with cron: $cronExp ...")
                scheduler.scheduleJob(job, trigger)
            }
            scheduler.start()
        } catch (e: SchedulerException) {
            log.error("Scheduler error occurred", e)
            throw BillingSchedulerException()
        }

        log.info("Billing scheduler using cron: $cronExp successfully started")
        return true
    }

    class BillingJob : Job {
        override fun execute(context: JobExecutionContext) {
            log.info("Scheduled billing job started: Charging pending invoices...")
            val billingService = context.mergedJobDataMap[BILLING_SERVICE_KEY] as BillingService
            billingService.chargePendingInvoices()
            log.info("Billing job done.")
        }
    }
}