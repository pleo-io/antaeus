package io.pleo.antaeus.core.infrastructure.messaging.activemq

import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProvider
import mu.KotlinLogging
import org.apache.activemq.ScheduledMessage
import javax.jms.Session

class ActiveMQAdapter: JmsProvider {
    private val logger = KotlinLogging.logger {}

    override fun send(destination: String, message: String, delay: Long) {
        // use a connection pool here
        val connection = ConnectionFactory().getConnectionFactory().createConnection()!!
        try {
            logger.info("Delaying message: '$message' for destination: '$destination' by: '$delay' ms")
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)!!
            val producer = session.createProducer(session.createQueue(destination))!!
            val payload = session.createTextMessage(message)!!
            payload.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay)
            producer.send(payload)
            logger.info("Message: '$message' for destination: '$destination' delayed for: '$delay' ms")
        } finally {
            connection.close()
        }
    }
}

