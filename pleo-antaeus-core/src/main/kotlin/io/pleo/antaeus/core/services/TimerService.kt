package io.pleo.antaeus.core.services

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.*

private val logger = KotlinLogging.logger {}

class TimerService(private val billingService: BillingService) {
    suspend fun setup() {
        logger.info {  "Timer is in service!" }
        val today = LocalDate.now()
        val nextMonthToday = today.plusMonths(1)
        val firstDayOfTheNextMonth = nextMonthToday.withDayOfMonth(1)
        val sleepDuration = Duration.between(today.atStartOfDay(), firstDayOfTheNextMonth.atStartOfDay()).toSeconds()
        // Sleep until the first day of the next month
        delay(sleepDuration)
        billingService.start()
        setup()
    }
}