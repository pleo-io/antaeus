package io.pleo.antaeus.core.infrastructure.messaging.activemq.connection

import io.pleo.antaeus.core.infrastructure.messaging.activemq.BrokerConfig
import javax.jms.ConnectionFactory
import org.apache.activemq.ActiveMQConnectionFactory

/**
 * ActiveMQ connection factory
 */
object ConnectionFactory {

    /**
     * Returns a [ConnectionFactory] object
     */
    fun getConnectionFactory(): ConnectionFactory {
        return ActiveMQConnectionFactory(
                BrokerConfig.brokerUsername,
                BrokerConfig.brokerPassword,
                BrokerConfig.brokerUrl
        )
    }
}
