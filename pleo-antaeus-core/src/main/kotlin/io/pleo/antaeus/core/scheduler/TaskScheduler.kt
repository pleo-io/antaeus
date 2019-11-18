package io.pleo.antaeus.core.scheduler

import io.pleo.antaeus.models.Schedule

/**
 * An interface for a task scheduler
 */
interface TaskScheduler {

    fun schedule(destination: String, payload: Any, schedule: Schedule) : Boolean
}
