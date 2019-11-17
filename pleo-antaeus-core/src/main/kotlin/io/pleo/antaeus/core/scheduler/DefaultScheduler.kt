package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProducer
import io.pleo.antaeus.models.Schedule
import java.time.Clock
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import org.quartz.CronExpression
import java.time.OffsetDateTime

// UTC time zone for standardized application of time calculations
private val UTC_TIME_ZONE = TimeZone.getTimeZone(ZoneId.of("UTC"))

class DefaultScheduler(
        private val clock: Clock,
        private val activeMQAdapter: JmsProducer
): Scheduler {

    override fun schedule(message: String, schedule: Schedule): Boolean {
        return try {
            val delay = this.calculateDelayMs(schedule)
            this.activeMQAdapter.send("billing", message = message, delay = delay)
            true
        } catch (ex: Throwable) {
            false
        }
    }

    fun calculateDelayMs(schedule: Schedule): Long {
        return scheduleDateTime(schedule.cronExpression)
                .atZoneSimilarLocal(UTC_TIME_ZONE.toZoneId())
                .toInstant()
                .toEpochMilli() - currentDateTime().toInstant().toEpochMilli()
    }

    private fun scheduleDateTime(expression: String): OffsetDateTime {
        val dateNow = Date(
                this.currentDateTime().atZoneSimilarLocal(UTC_TIME_ZONE.toZoneId()).toInstant().toEpochMilli()
        )
        val cronExpression = CronExpression(expression)
        return OffsetDateTime.ofInstant(
                cronExpression.getNextValidTimeAfter(dateNow).toInstant(),
                cronExpression.timeZone.toZoneId()
        )
    }

    private fun currentDateTime(): OffsetDateTime {
        return OffsetDateTime.now(this.clock.withZone(UTC_TIME_ZONE.toZoneId()))
    }
}
