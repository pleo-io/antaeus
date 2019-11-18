package io.pleo.antaeus.core.infrastructure.messaging.jms

import io.pleo.antaeus.core.infrastructure.messaging.activemq.connection.ConnectionFactory
import mu.KotlinLogging
import javax.jms.JMSException
import javax.jms.MessageListener
import javax.jms.Session

/**
 * Abstract class extending a [MessageListener] implementing an
 * asynchronous message listener that JMS can push a message to.
 *
 */
abstract class MessagingMessageListener: MessageListener {

    private val logger = KotlinLogging.logger {}

    abstract val queueName: String

    @Throws(JMSException::class)
    fun listen() {
        val connection = ConnectionFactory.getConnectionFactory().createConnection()!!
        try {
            logger.debug { "Starting connection with to queue=${this.queueName}" }
            connection.start()
            logger.debug { "Connection to queue=${this.queueName} started" }
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val consumer = session.createConsumer(session.createQueue(queueName))
            consumer.messageListener = this
        } finally {
            // closing the connection kills the worker.
            // connection.close()
        }
    }
}
