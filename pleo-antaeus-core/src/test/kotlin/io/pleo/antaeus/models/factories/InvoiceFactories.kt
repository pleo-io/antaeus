package io.pleo.antaeus.models.factories

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

// TODO remove this factory in favour to the same one located in the pleo-antaeus-models module
// TODO use common test fixtures
var idCounter = AtomicInteger(1)

fun nextId() = idCounter.getAndIncrement()
fun randomCurrency() = Currency.values()[Random.nextInt(0, Currency.values().size)]
fun randomAmount() = BigDecimal(Random.nextDouble(10.0, 500.0))
fun randomStatus() = InvoiceStatus.values()[Random.nextInt(0, InvoiceStatus.values().size)]

fun createInvoice(id: Int = nextId(),
                  customerId: Int = 1,
                  amount: Money = Money(
                          value = randomAmount(),
                          currency = randomCurrency()
                  ),
                  status: InvoiceStatus = randomStatus(),
                  targetDate: Date = Date(),
                  createdAt: Date = Date()
) = Invoice(id, customerId, amount, status, targetDate, createdAt)