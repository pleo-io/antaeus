package io.pleo.antaeus.core.infrastructure.messaging.jms

/**
 * Interface for a JMS producer
 */
interface JmsProvider {

    fun send(destination: String, message: String, delay: Long)
}
