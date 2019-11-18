package io.pleo.antaeus.core.workers

import io.pleo.antaeus.core.infrastructure.dto.AbstractWorkerDTO
import io.pleo.antaeus.core.infrastructure.messaging.activemq.Config
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.*

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

    @Throws(JMSException::class)
    fun main(args: Array<String>) {
        // Getting JMS connection from the server
        val connectionFactory = ActiveMQConnectionFactory(
                Config.brokerUsername,
                Config.brokerPassword,
                Config.brokerUrl
        )
        val connection = connectionFactory.createConnection()
        connection.start()

        // Creating session for sending messages
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

        val destination = session.createQueue("invoice-billing")

        // MessageConsumer is used for receiving (consuming) messages
        val consumer = session.createConsumer(destination)

        // Here we receive the message.
        val message = consumer.receive()

        this.onMessage(message)

        connection.close()
    }
}
