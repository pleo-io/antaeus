package io.pleo.antaeus.core.infrastructure.messaging.activemq

import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.ConnectionFactory

/**
 * ActiveMQ connection factory
 */
class ConnectionFactory {

    private val brokerConfig = Config

    /**
     * Returns a ConnectionFactory object
     */
    fun getConnectionFactory(): ConnectionFactory {
        return ActiveMQConnectionFactory(
                brokerConfig.brokerUsername,
                brokerConfig.brokerPassword,
                brokerConfig.brokerUrl
        )
    }
}
