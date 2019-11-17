package io.pleo.antaeus.core.workers

import io.pleo.antaeus.core.infrastructure.dto.AbstractWorkerDTO
import mu.KotlinLogging
import javax.jms.JMSException
import javax.jms.Message
import javax.jms.MessageListener

abstract class AbstractWorker<WorkerDTO: AbstractWorkerDTO>: MessageListener {
    private val logger = KotlinLogging.logger {}

    abstract fun handle(workerDTO: WorkerDTO)

    override fun onMessage(message: Message?) {
        try {
            val payload: WorkerDTO = (message as WorkerDTO)
            logger.info { "Received message: '$payload'" }
            this.handle(payload)
        } catch (ex: JMSException) {
            logger.error("An error has occurred: '${ex.message}'", ex)
        }
    }
}
