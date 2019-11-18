package io.pleo.antaeus.core.infrastructure.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

abstract class AbstractWorkerTaskDTO(
        @JsonProperty(value = "scheduledOn")
        private val scheduledOn: OffsetDateTime? = null
)
