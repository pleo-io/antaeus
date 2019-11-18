package io.pleo.antaeus.core.scheduler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProvider
import io.pleo.antaeus.models.Schedule
import java.lang.Exception
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import org.junit.jupiter.api.Test

class DelayedTaskSchedulerTest {

    private val timeZoneString = "GMT+03:00"
    private val timeZone = TimeZone.getTimeZone(timeZoneString)

    private val jmsProvider = mockk<JmsProvider> {
        every { send(any(), any(), any()) } just Runs
    }

    private val delayedTaskScheduler = DelayedTaskScheduler(
            clock = Clock.fixed(frozenOffsetDateTime().toInstant(), timeZone.toZoneId()),
            schedulingProvider = jmsProvider
    )

    @Test
    fun `correct scheduling date and delay calculated`() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val expectedDelay = (
                this.frozenOffsetDateTime(
                        year = currentYear,
                        month = 2,
                        day = 1
                ).toInstant().toEpochMilli()- this.frozenOffsetDateTime().toInstant().toEpochMilli()
        )
        assert(
                delayedTaskScheduler.schedule(
                        destination = "my-destination",
                        payload = "Payload",
                        schedule = Schedule(cronExpression = "0 0 0 1 * ?", timeZone = timeZone)
                )
        )

        verify(exactly = 1) { jmsProvider.send(any(), any(), any()) }
    }

    @Test
    fun `errors are handled`() {
        every { jmsProvider.send(any(), any(), any()) } throws Exception("Houston! We have a problem!")

        assert(
            !delayedTaskScheduler.schedule(
                    destination = "my-destination",
                    payload = "Payload",
                    schedule = Schedule(cronExpression = "0 0 0 1 * ?", timeZone = timeZone)
            )
        )

        verify(exactly = 1) { jmsProvider.send(any(), any(), any()) }
    }

    private fun frozenOffsetDateTime(
            year: Int = Calendar.getInstance().get(Calendar.YEAR),
            month: Int = 1,
            day: Int = 15,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            nanos: Int = 0,
            zone: ZoneId = timeZone.toZoneId()
    ): OffsetDateTime {
        // frozen zoned date time for use in freezing time in tests
        return OffsetDateTime.from(ZonedDateTime.of(year, month, day, hour, minute, second, nanos, zone))
    }
}
