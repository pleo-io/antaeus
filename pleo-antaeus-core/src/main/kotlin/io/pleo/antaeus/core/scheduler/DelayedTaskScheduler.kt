package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProvider
import io.pleo.antaeus.core.infrastructure.util.json.JsonSerializationHelper
import io.pleo.antaeus.models.Schedule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import mu.KotlinLogging
import org.quartz.CronExpression

/**
 * An implementation of TaskScheduler based on delayed scheduled queueing
 */
class DelayedTaskScheduler(
        private val clock: Clock,
        private val schedulingProvider: JmsProvider
): TaskScheduler {
    // UTC time zone for standardized application of time calculations
    private val utcTimeZone = TimeZone.getTimeZone(ZoneId.of("UTC"))

    private val logger = KotlinLogging.logger {}

    override fun schedule(destination: String, payload: Any, schedule: Schedule): Boolean {
        return try {
            val timeToSchedule = this.scheduleDateTimeFromCron(schedule.cronExpression)
            val message = JsonSerializationHelper.serializeToJson(payload)
            logger.info {
                "Task with message: '$message' and destination: '$destination' scheduled for: '$timeToSchedule'"
            }
            this.schedulingProvider.send(destination, message = message, delay = this.calculateDelayMs(timeToSchedule))
            true
        } catch (ex: Throwable) {
            logger.error(ex) {"An error: '${ex.message}' has occurred while scheduling"}
            false
        }
    }

    private fun scheduleDateTimeFromCron(expression: String): Date {
        return CronExpression(expression).getNextValidTimeAfter(this.currentDateTime())
    }

    private fun calculateDelayMs(scheduleTime: Date): Long {
        return scheduleTime.toInstant().toEpochMilli() - currentDateTime().toInstant().toEpochMilli()
    }

    private fun currentDateTime(): Date {
        return Date.from(Instant.now(this.clock.withZone(utcTimeZone.toZoneId())))
    }
}
