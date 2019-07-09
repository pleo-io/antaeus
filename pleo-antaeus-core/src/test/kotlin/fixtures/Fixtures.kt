package fixtures

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

class Fixtures {
    companion object {
        fun createPendingInvoice() : Invoice {
            return Invoice(id = nextInt(), customerId = nextInt(),
                    amount = Money(BigDecimal.valueOf(nextLong()), Currency.EUR),
                    status = InvoiceStatus.PENDING)
        }

        fun createPaidInvoice() : Invoice {
            return Invoice(id = nextInt(), customerId = nextInt(),
                    amount = Money(BigDecimal.valueOf(nextLong()), Currency.EUR),
                    status = InvoiceStatus.PAID)
        }

        fun createErrorInvoice() : Invoice {
            return Invoice(id = nextInt(), customerId = nextInt(),
                    amount = Money(BigDecimal.valueOf(nextLong()), Currency.EUR),
                    status = InvoiceStatus.ERROR)
        }
    }
}

