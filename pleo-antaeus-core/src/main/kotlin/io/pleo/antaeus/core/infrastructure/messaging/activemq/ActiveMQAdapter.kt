package io.pleo.antaeus.core.infrastructure.messaging.activemq

import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProducer
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.ScheduledMessage
import javax.jms.Session

class ActiveMQAdapter: JmsProducer {
    private val connectionFactory = ActiveMQConnectionFactory()

    override fun send(destination: String, message: String, delay: Long) {
        // TODO: use a connection pool here
        val connection = connectionFactory.createConnection()!!
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)!!
        val producer = session.createProducer(session.createQueue(destination))!!
        val payload = session.createTextMessage(message)!!
        payload.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay)
        producer.send(session.createTextMessage(message)!!)
        connection.close()
    }
}

