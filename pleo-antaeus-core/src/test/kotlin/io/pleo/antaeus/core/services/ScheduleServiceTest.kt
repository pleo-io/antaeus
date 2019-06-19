package io.pleo.antaeus.core.services

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertTrue as assertTrue1

class ScheduleServiceTest {
    private val billingService = mockk<BillingService> {}
    private val scheduleService = ScheduleService(billingService = billingService)
    // Will be outdated in July
    private val secondsUntilNewMonth =  Duration.between(LocalDateTime.now(), LocalDateTime.of(2019, 7,1, 0,0)).seconds

    @Test
    fun `secondsUntilBilling gives correct seconds between now and start of next month`() {
        assertEquals(secondsUntilNewMonth, scheduleService.secondsUntilBilling())
    }
}