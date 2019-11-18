package io.pleo.antaeus.core.infrastructure.messaging.activemq

import io.pleo.antaeus.core.infrastructure.messaging.activemq.connection.ConnectionFactory
import io.pleo.antaeus.core.infrastructure.messaging.jms.JmsProvider
import javax.jms.Session
import mu.KotlinLogging
import org.apache.activemq.ScheduledMessage

/**
 * An ActiveMQ adapter implementing a [JmsProvider].
 *
 * It handles establishing connections and sessions, creating a producer
 * as well as a payload to send to ActiveMQ.
 *
 * Despite ActiveMQ support cron expression based scheduling, with the
 * ScheduledMessage.AMQ_SCHEDULED_CRON property, using a delay (in ms)
 * is favored because it's much more generic and not specific to the
 * cron syntax hence can support a wider schedule definition DSL.
 *
 * The cron expression is first evaluated into a date-time object from
 * which a delay (the difference in time between the current time and
 * the target time) is calculated.
 */
class ActiveMQAdapter: JmsProvider {
    private val logger = KotlinLogging.logger {}

    /**
     * Send a message to a destination with a delay in ms to the provider.
     */
    override fun send(destination: String, message: String, delay: Long) {
        // TODO: use a connection pool here with decorated connection so that connections auto-close when yielded
        val connection = ConnectionFactory.getConnectionFactory().createConnection()!!
        try {
            logger.info { "Delaying message='$message' for destination='$destination' by $delay ms" }
            val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)!!
            val producer = session.createProducer(session.createQueue(destination))!!
            val payload = session.createTextMessage(message)!!
            payload.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay)
            producer.send(payload)
            logger.info { "Message='$message' for destination='$destination' delayed for $delay ms" }
        } finally {
            connection.close()
        }
    }
}

