package io.pleo.antaeus.core.scheduler

import dev.inmo.krontab.builder.buildSchedule
import dev.inmo.krontab.doInfinity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val FIRST_DAY_OF_MONTH = 0

object RecurringTaskScheduler {
    fun scheduleOnEveryFirstDayOfMonth(block: () -> Unit) {
        val buildSchedule = buildSchedule {
            dayOfMonth {
                each(FIRST_DAY_OF_MONTH)
            }
        }
        GlobalScope.launch {
            buildSchedule.doInfinity { block() }
        }
    }
}