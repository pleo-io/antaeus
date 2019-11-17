package io.pleo.antaeus.core.infrastructure.dto

import com.squareup.moshi.Json

data class InvoiceBillingWorkerDTO(
        @Json(name = "invoiceId")
        val invoiceId: Int
): AbstractWorkerDTO()
