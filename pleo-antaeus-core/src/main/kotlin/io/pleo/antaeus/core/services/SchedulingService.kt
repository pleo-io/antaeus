package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.external.PaymentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

class SchedulingService(private val billingService: BillingService) {
    private var job: Job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job )

    fun cancel() {
        job.cancel()
    }

    fun schedule() = scope.launch { // launching the coroutine
        var initialDelay = 0
        var cadence = 24*60*60*1000L
        val calendar = Calendar.getInstance()

        fixedRateTimer(
                name = "billing service scheduller",
                initialDelay = initialDelay.toLong(),
                period = cadence.toLong(),
                daemon = false) {
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            if (dayOfMonth == 1){
                logger.info { "Scheduling billing service" }
                billingService.billInvoices("PENDING")
            }
        }
    }
}
