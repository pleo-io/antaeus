package io.pleo.antaeus.core.scheduler

import it.justwrote.kjob.KJob
import it.justwrote.kjob.KronJob
import it.justwrote.kjob.job.JobExecutionType
import it.justwrote.kjob.kron.Kron
import mu.KotlinLogging
import java.util.*

class Scheduler(private val kronJob: KronJob, val kjob: KJob) {
    private val logger1 = KotlinLogging.logger {}

    fun schedule(action: suspend (date: Date) -> Unit) {
        kjob(Kron).kron(kronJob) {
            executionType = JobExecutionType.NON_BLOCKING
            maxRetries = 3
            execute {
                action(Date())
            }.onError {
                // errors will automatically logged but we might want to do some metrics or something
                logger1.error { "Oh no! Our code for $jobName with id $jobId failed us with the exception $error" }
            }.onComplete {
                // Also we can define a block that gets executed when our execution has been finished
                logger1.info { "Everything for job $jobName with id $jobId worked out as planed!" }
                logger1.info { "The execution took ${time().toMillis()}ms" }
            }
        }
    }
}
