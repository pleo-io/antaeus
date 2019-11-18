package io.pleo.antaeus.core.exceptions

import io.pleo.antaeus.models.Currency

class CurrencyNotSupportedException(currency: Currency) : Exception("Currency='${currency.name}' is not supported")
