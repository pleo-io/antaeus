package io.pleo.antaeus.core.workers.billing.validator

import io.pleo.antaeus.core.workers.interceptor.PreExecutionValidationInterceptor
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ValidateInvoiceStatusInterceptorTest {

    private val interceptor: PreExecutionValidationInterceptor<Invoice> = ValidateInvoiceStatusInterceptor()

    @Test
    fun `validation passes if invoice status is not PAID`() {
        val invoice = Invoice(
                id = 1,
                customerId = 1,
                amount = Money(value = BigDecimal.valueOf(100), currency = Currency.DKK),
                status = InvoiceStatus.PENDING
        )
        assert(interceptor.validate(invoice))
    }

    @Test
    fun `validation fails if invoice status is PAID`() {
        val invoice = Invoice(
                id = 1,
                customerId = 1,
                amount = Money(value = BigDecimal.valueOf(100), currency = Currency.DKK),
                status = InvoiceStatus.PAID
        )
        assert(!interceptor.validate(invoice))
    }
}
