package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.core.infrastructure.messaging.activemq.ActiveMQAdapter
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.*

class DelayedTaskSchedulerTest {

    private val timeZoneString = "UTC"
    private val timeZone = TimeZone.getTimeZone(timeZoneString)

    private val defaultScheduler = DelayedTaskScheduler(
            clock = Clock.fixed(frozenOffsetDateTime().toInstant(), timeZone.toZoneId()),
            schedulingProvider = ActiveMQAdapter()
    )

    @Test
    fun `correct scheduling date and delay calculated`() {
        val expected = this.frozenOffsetDateTime(Calendar.getInstance().get(Calendar.YEAR), 2, 1)
//        assert(defaultScheduler.scheduleDateTimeFromCron("0 0 0 1 * ?").toInstant() == expected.toInstant())
    }

    @Test
    fun `sent`() {}

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
