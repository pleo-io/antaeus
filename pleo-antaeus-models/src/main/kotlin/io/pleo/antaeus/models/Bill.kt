package io.pleo.antaeus.models

import javax.print.attribute.standard.JobStateReason

data class Bill(
        val id: Int,
        val invoiceId: Int,
        val status: BillingStatus,
        val failureReason: String

)


