package io.pleo.antaeus.core.schedulers

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvalidCronException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal


class BillingSchedulerTest {

    private val billingService: BillingService = mockk() {
        every { chargePendingInvoices() } returns listOf(
            Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PAID)
        )
    }

    private val billingScheduler: BillingScheduler = BillingScheduler(billingService)

    private val everySec = "0/1 * * ? * *"
    private val every2Sec = "0/2 * * ? * *"

    @Test
    fun `start successfully`() {
        assert(billingScheduler.start(everySec))
        //Thread.sleep(3000) // Useful for manual execution
    }

    @Test
    fun `start and reschedule successfully`() {
        assert(billingScheduler.start(everySec))
        //Thread.sleep(3000) // Useful for manual execution

        assert(billingScheduler.start(every2Sec))
        //Thread.sleep(5000) // Useful for manual execution
    }

    @Test
    fun `fail to schedule with invalid cron`(){
        assertThrows<InvalidCronException> {
            billingScheduler.start("invalidExpression")
        }
    }
}
