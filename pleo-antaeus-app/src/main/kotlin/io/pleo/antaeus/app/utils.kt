import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTimeZone
import org.joda.time.MutableDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

fun nextMonthDate(date: Date): Date {
    val mutableDateTime = MutableDateTime(date, DateTimeZone.UTC)
    mutableDateTime.addMonths(1)
    mutableDateTime.dayOfMonth = 1
    mutableDateTime.millisOfDay = 0
    return mutableDateTime.toDate()
}

// This will create all schemas and setup initial data
suspend internal fun setupInitialData(dal: AntaeusDal)  {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                    amount = Money(
                            value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                            currency = customer.currency
                    ),
                    customer = customer,
                    status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID,
                    createdAt = Date(),
                    targetDate = nextMonthDate(Date())
                )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            if (Random.nextInt(100) < 5) {
                throw NetworkException()
            }
            return Random.nextBoolean()
        }
    }
}
