package io.pleo.antaeus.models

/**
 * Domain class that holds schedule configuration
 */
data class Schedule(val cronExpression: String = "0 0 0 1 * ?")
