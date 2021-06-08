package io.pleo.antaeus.core.scheduler

import dev.inmo.krontab.doInfinity
import kotlinx.coroutines.runBlocking

object RecurringTaskScheduler {
    /**
     * krontab syntax is a bit different from crontab syntax.
     * That's why it has days of month on the forth place.
     */
    fun scheduleOnEveryFirstDayOfMonth(block: () -> Unit) {
        runBlocking {
            doInfinity(("* * * /1 *")) {
                block.invoke()
            }
        }
    }
}