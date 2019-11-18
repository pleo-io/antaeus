package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProvider
import io.pleo.antaeus.core.infrastructure.util.json.JsonSerializationHelper
import io.pleo.antaeus.models.Schedule
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import mu.KotlinLogging
import org.quartz.CronExpression

/**
 * An implementation of TaskScheduler based on delayed scheduled queueing.
 * Applies a supplied Schedule to generate a delay for use in scheduling a
 * task which is delegated to a JmsProvider.
 *
 * Constructor Dependency Injection favored to allow flexibility in testing.
 * The Clock dependency is rather important in this regard as it can be
 * mocked with a fixed date time to allow a known time reference in tests.
 *
 * TimeZones are an important consideration since we want to delay a task
 * with precision of local times. For instance, if we have customers in
 * a GMT+0300 timezone and the desired billing time is the first of every
 * month e.g. 1 January 00:00:00, we want to delay this billing task in
 * such a way that that local time is achieved despite having servers
 * elsewhere.
 */
class DelayedTaskScheduler(
        private val clock: Clock,
        private val schedulingProvider: JmsProvider
): TaskScheduler {
    private val logger = KotlinLogging.logger {}

    private val utcTimeZone = TimeZone.getTimeZone(ZoneId.of("UTC"))

    override fun schedule(destination: String, payload: Any, schedule: Schedule): Boolean {
        return try {
            // TODO: apply timezones
            val timeZone = schedule.timeZone ?: utcTimeZone
            val timeToSchedule = this.scheduleDateTimeFromCron(schedule.cronExpression)
            val message = JsonSerializationHelper.serializeToJson(payload)
            logger.info { "Task message='$message' and destination='$destination' scheduled for='$timeToSchedule'" }
            this.schedulingProvider.send(
                    destination = destination,
                    message = message,
                    delay = this.calculateDelayMs(timeToSchedule)
            )
            true
        } catch (ex: Throwable) {
            logger.error(ex) {"An error='${ex.message}' has occurred while scheduling"}
            false
        }
    }

    private fun calculateDelayMs(scheduleTime: OffsetDateTime): Long {
        return scheduleTime.toInstant().toEpochMilli() - this.currentDateTime().toInstant().toEpochMilli()
    }

    private fun scheduleDateTimeFromCron(expression: String): OffsetDateTime {
        val currentDateTime = Date.from(this.currentDateTime().toInstant())
        return CronExpression(expression)
                .getNextValidTimeAfter(currentDateTime)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
    }

    private fun currentDateTime(): OffsetDateTime {
        return OffsetDateTime.now(this.clock)
    }
}
