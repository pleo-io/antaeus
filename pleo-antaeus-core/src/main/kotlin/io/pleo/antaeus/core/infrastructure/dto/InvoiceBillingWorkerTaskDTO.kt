package io.pleo.antaeus.core.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class InvoiceBillingWorkerTaskDTO(
        @JsonProperty(value = "invoiceId")
        val invoiceId: Int,

        @JsonProperty(value = "scheduledOn")
        val scheduledOn: OffsetDateTime?
): AbstractWorkerTaskDTO(scheduledOn)
