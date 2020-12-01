package io.pleo.antaeus.core.services.config

import io.pleo.antaeus.models.Schedule

object ServiceConfiguration {
    val billingScheme: Schedule by lazy {
        Schedule.MONTHLY
    }

    val invoiceTroubleShootingHeartbeatMs: Long by lazy {
        when(billingScheme) {
            Schedule.MONTHLY -> 24*60*60*60*1000L //24h
        }
    }

    val retryInvoiceBillingCount: Int by lazy { 3 }
    val retryInvoiceBillingIntervalMs: Long by lazy { 3*60*1000L } //3 min
}