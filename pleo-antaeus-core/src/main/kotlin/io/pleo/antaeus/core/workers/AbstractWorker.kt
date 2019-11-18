package io.pleo.antaeus.core.workers

import io.pleo.antaeus.core.infrastructure.dto.AbstractWorkerDTO
import io.pleo.antaeus.core.infrastructure.messaging.jms.MessagingMessageListener
import java.lang.Exception
import javax.jms.Message
import mu.KotlinLogging

abstract class AbstractWorker<WorkerDTO: AbstractWorkerDTO>: MessagingMessageListener() {
    private val logger = KotlinLogging.logger {}

    abstract fun handle(workerDTO: WorkerDTO)

    override fun onMessage(message: Message?) {
        try {
            val payload: WorkerDTO = (message as WorkerDTO)
            logger.info { "Message: '$payload' has been received. Invoking consumer handle" }
            this.handle(payload)
        } catch (ex: Exception) {
            logger.error(ex) { "An error: '${ex.message}' has occurred" }
        }
    }
}
