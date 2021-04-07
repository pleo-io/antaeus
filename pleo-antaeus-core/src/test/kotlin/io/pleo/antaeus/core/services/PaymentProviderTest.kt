package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProviderImpl
import io.pleo.antaeus.data.dals.CustomerDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PaymentProviderTest {

    // Work as expected
    private val invoice1 = Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PENDING)
    private val customer1 = Customer(id = 1, currency = Currency.EUR)

    // Throws CustomerNotFoundException
    private val invoice2 = Invoice(id = 2, customerId = 404, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PENDING)

    // Throws CurrencyMismatchException
    private val invoice3 = Invoice(id = 3, customerId = 2, amount = Money(BigDecimal.ONE, Currency.EUR), status = InvoiceStatus.PENDING)
    private val customer2 = Customer(id = 1, currency = Currency.DKK)


    private val customerDal = mockk<CustomerDal> {
        every { fetchCustomer(1) } returns customer1
        every { fetchCustomer(2) } returns customer2
        every { fetchCustomer(404) } returns null
    }

    private val customerService = CustomerService(customerDal)
    private val spiedInstance = PaymentProviderImpl(customerService)
    private val paymentProvider = spyk(spiedInstance) {
        every { networkIssue() } returns false // No network issue for testing
        every { insufficientFunds() } returns false // No insufficient fund for testing
    }

    @Test
    fun `charge invoice1 with success`() {
        Assertions.assertTrue(paymentProvider.charge(invoice1))
    }

    @Test
    fun `charge invoice2 throws CustomerNotFoundException`() {
        assertThrows<CustomerNotFoundException> {
            paymentProvider.charge(invoice2)
        }
    }

    @Test
    fun `charge invoice2 throws CurrencyMismatchException`() {
        assertThrows<CurrencyMismatchException> {
            paymentProvider.charge(invoice3)
        }
    }
}
