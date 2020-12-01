package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Schedule

class ScheduleService {

    fun timeUntilNextBilling(schedule: Schedule): Long =
            when(schedule) {
                Schedule.MONTHLY -> 30*24*60*60*1000L //TODO provide actual calculation
            }
}