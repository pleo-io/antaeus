package io.pleo.antaeus.core.infrastructure.messaging.activemq

object Config {
    val brokerUrl: String = System.getenv("BROKER_URL") + ":" + System.getenv("BROKER_PORT")
    val brokerUsername: String = System.getenv("BROKER_USERNAME")
    val brokerPassword: String = System.getenv("BROKER_PASSWORD")
}
