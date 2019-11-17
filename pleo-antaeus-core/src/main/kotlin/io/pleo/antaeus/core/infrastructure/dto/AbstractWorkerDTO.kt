package io.pleo.antaeus.core.infrastructure.dto

import com.squareup.moshi.Json
import java.time.OffsetDateTime

abstract class AbstractWorkerDTO(
        @Json(name = "occurredAt")
        private val occurredAt: OffsetDateTime? = null
)
