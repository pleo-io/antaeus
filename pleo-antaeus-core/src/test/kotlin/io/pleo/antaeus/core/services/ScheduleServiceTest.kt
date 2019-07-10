package io.pleo.antaeus.core.services

import fixtures.Fixtures.Companion.createPaidInvoice
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import reactor.core.publisher.Flux

internal class ScheduleServiceTest {

    private val billingService = mockk<BillingService>()

    @Test
    fun scheduleInvoiceJob() {

        // given
        every { billingService.chargePendingInvoices() } returns Flux.fromIterable(listOf(createPaidInvoice()))
        val scheduleService = ScheduleService(billingService)

        // when
        scheduleService.scheduleInvoiceJob(1)

        //prevent jvm from shutting down
        Thread.sleep(3000)

        // then
        verify { billingService.chargePendingInvoices() }
    }
}
