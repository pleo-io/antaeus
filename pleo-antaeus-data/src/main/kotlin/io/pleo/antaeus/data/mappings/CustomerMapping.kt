/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data.mappings

import io.pleo.antaeus.data.tables.CustomerTable
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCustomer(): Customer = Customer(
    id = this[CustomerTable.id],
    currency = Currency.valueOf(this[CustomerTable.currency])
)
