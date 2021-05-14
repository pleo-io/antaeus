package io.pleo.antaeus.core.services

import io.mockk.coEvery
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerServiceTest {
    private val dal = mockk<AntaeusDal> {
        coEvery { fetchCustomer(404) } returns null
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `will throw if customer is not found`() = runBlockingTest {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }
}
