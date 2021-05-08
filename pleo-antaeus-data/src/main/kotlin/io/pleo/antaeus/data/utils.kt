package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
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
suspend fun setupInitialData(dal: AntaeusDal, customersNum: Int = 100, invoicesPerCustomerNum: Int = 10)  {
    val customers = (1..customersNum).mapNotNull {
        dal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..invoicesPerCustomerNum).forEach {
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

