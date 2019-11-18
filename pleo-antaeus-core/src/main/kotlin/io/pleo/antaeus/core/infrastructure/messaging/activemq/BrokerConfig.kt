package io.pleo.antaeus.core.infrastructure.messaging.activemq

/**
 * Environment driven ActiveMQ broker configuration.
 */
object BrokerConfig {
    val brokerUrl: String = System.getenv("BROKER_URL")!!
    val brokerUsername: String = System.getenv("BROKER_USERNAME")!!
    val brokerPassword: String = System.getenv("BROKER_PASSWORD")!!
}
