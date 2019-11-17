package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.models.Schedule

/**
 * An interface for the scheduling service
 */
interface Scheduler {

    fun schedule(message: String, schedule: Schedule) : Boolean
}
