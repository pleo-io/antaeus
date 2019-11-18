package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Money

interface CurrencyConversionProvider {

    /*
        Convert an amount money to an equivalent in the target currency.

        Returns:
          `Money`: the converted amount

        Throws:
          `CurrencyNotSupportedException`: a custom exception used when the target Currency is unknown.
    **/

    fun convert(amount: Money, target: Currency): Money
}
