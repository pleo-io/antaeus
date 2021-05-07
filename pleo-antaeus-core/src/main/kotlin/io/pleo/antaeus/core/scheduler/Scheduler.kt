package io.pleo.antaeus.core.scheduler

import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Scheduler(private val task: Runnable) {
    private val logger = KotlinLogging.logger {}
    private val executor = Executors.newScheduledThreadPool(1)!!

    fun scheduleExecution(every: Every) {

        val taskWrapper = Runnable {
            task.run()
        }

        executor.scheduleWithFixedDelay(taskWrapper, every.n, every.n, every.unit)
    }


    fun stop() {
        executor.shutdown()

        try {
            executor.awaitTermination(1, TimeUnit.HOURS)
        } catch (e: InterruptedException) {
            logger.warn(e) { "Unexpected error during scheduler termination occurred." }
        }

    }
}

data class Every(val n: Long, val unit: TimeUnit)