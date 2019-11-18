package io.pleo.antaeus.models

import java.time.ZoneId
import java.util.TimeZone

/**
 * Domain class that holds schedule configuration
 */
data class Schedule(
        val cronExpression: String,
        val timeZone: TimeZone? = TimeZone.getTimeZone(ZoneId.of("UTC"))
)
