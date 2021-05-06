package io.pleo.antaeus.core.scheduler

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*

class Scheduler() {
    suspend fun schedule(time: Date, action: suspend () -> Unit) {
        coroutineScope {
            launch {
                action()
            }
        }
        println("scheduled task")
    }

}