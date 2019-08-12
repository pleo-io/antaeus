package io.pleo.antaeus.core.services

import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.PaymentNotDoneException
import io.pleo.antaeus.core.external.PaymentProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BillingServiceTest {

    private val paymentProvider = mockk<PaymentProvider>()
    private val billingService = BillingService(paymentProvider)

    @Test
    fun `will throw if scheduling was not done`() {
        assertThrows<PaymentNotDoneException> {
            billingService.schedulePayment(404)
        }
    }
}