package io.pleo.antaeus.core.infrastructure.messaging.activemq

import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProducer
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.ScheduledMessage
import javax.jms.Session

class ActiveMQAdapter: JmsProducer {
    private val logger = KotlinLogging.logger {}

    private val connectionFactory = ActiveMQConnectionFactory(
            Config.brokerUsername,
            Config.brokerPassword,
            Config.brokerUrl
    )

    override fun send(destination: String, message: String, delay: Long) {
        logger.info("Delaying message: '$message' for destination: '$destination' by: '$delay' ms")
        // TODO: use a connection pool here
        val connection = connectionFactory.createConnection()!!
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)!!
        val producer = session.createProducer(session.createQueue(destination))!!
        val payload = session.createTextMessage(message)!!
        payload.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay)
        producer.send(session.createTextMessage(message)!!)
        logger.info("Message: '$message' for destination: '$destination' delayed for: '$delay' ms")
        connection.close()
    }
}

