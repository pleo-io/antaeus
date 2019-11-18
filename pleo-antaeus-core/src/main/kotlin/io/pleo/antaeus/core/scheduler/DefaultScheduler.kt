package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProducer
import io.pleo.antaeus.models.Schedule
import mu.KotlinLogging
import java.time.Clock
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import org.quartz.CronExpression
import java.time.Instant

// UTC time zone for standardized application of time calculations
private val UTC_TIME_ZONE = TimeZone.getTimeZone(ZoneId.of("UTC"))

class DefaultScheduler(
        private val clock: Clock,
        private val activeMQAdapter: JmsProducer
): TaskScheduler {

    private val logger = KotlinLogging.logger {}

    override fun schedule(destination: String, payload: Any, schedule: Schedule): Boolean {
        return try {
            val timeToSchedule = this.scheduleDateTimeFromCron(schedule.cronExpression)
            val message = payload.toString()
            logger.info("Task: '$message' for destination: '$destination' scheduled for: '$timeToSchedule'")
            this.activeMQAdapter.send(destination, message = message, delay = this.calculateDelayMs(timeToSchedule))
            true
        } catch (ex: Throwable) {
            logger.error("An error: '${ex.message}' has occurred while scheduling", ex)
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
        return Date.from(Instant.now(this.clock.withZone(UTC_TIME_ZONE.toZoneId())))
    }
}
