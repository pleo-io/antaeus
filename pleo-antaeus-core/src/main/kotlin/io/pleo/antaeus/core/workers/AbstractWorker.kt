package io.pleo.antaeus.core.workers

import io.pleo.antaeus.core.infrastructure.dto.AbstractWorkerTaskDTO
import io.pleo.antaeus.core.infrastructure.messaging.jms.MessagingMessageListener
import io.pleo.antaeus.core.infrastructure.util.json.JsonSerializationHelper
import java.lang.Exception
import javax.jms.Message
import javax.jms.TextMessage
import mu.KotlinLogging

/**
 * An abstract worker extending the MessagingMessageListener class
 * that defines a skeleton for a concrete worker class.
 */
abstract class AbstractWorker<WorkerDTO: AbstractWorkerTaskDTO>(
        private val workerDTOClass: Class<WorkerDTO>
): MessagingMessageListener() {
    private val logger = KotlinLogging.logger {}

    abstract fun handle(workerDTO: WorkerDTO)

    override fun onMessage(message: Message?) {
        try {
            val messageText = (message as TextMessage).text
            logger.info { "Message='$messageText' has been received." }
            val workerDTO = this.messageUnmarshaller(messageText) as WorkerDTO
            logger.info { "Invoking listener handler with dto='$workerDTO'" }
            this.handle(workerDTO)
        } catch (ex: Exception) {
            logger.error(ex) { "An error='${ex.message}' has occurred" }
        }
    }

    private fun messageUnmarshaller(message: String): Any {
        return JsonSerializationHelper.deserializeFromJson(message, workerDTOClass)
    }
}
