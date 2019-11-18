package io.pleo.antaeus.core.infrastructure.util.json

import java.time.OffsetDateTime

internal class TestObject {
    var id: String? = null
    var name: String? = null
    var number: Int = 0
    var occurredAt: OffsetDateTime? = null
    var isActive: Boolean = false

    constructor(id: String, name: String, number: Int, occurredAt: OffsetDateTime, active: Boolean) {
        this.id = id
        this.name = name
        this.number = number
        this.occurredAt = occurredAt
        this.isActive = active
    }

    constructor() {}
}
