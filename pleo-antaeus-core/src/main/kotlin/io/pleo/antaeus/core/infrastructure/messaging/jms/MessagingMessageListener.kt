package io.pleo.antaeus.core.infrastructure.messaging.jms

import io.pleo.antaeus.core.infrastructure.messaging.activemq.ConnectionFactory
import javax.jms.JMSException
import javax.jms.MessageListener
import javax.jms.Session

abstract class MessagingMessageListener: MessageListener {

    private val queueName: String = "invoice-billing"

    @Throws(JMSException::class)
    fun main(args: Array<String>) {
        val connection = ConnectionFactory().getConnectionFactory().createConnection()!!
        try {
            connection.start()
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val destination = session.createQueue(queueName)
            val consumer = session.createConsumer(destination)
            val message = consumer.receive()
            this.onMessage(message)
        } finally {
            connection.close()
        }
    }
}
