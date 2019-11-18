package io.pleo.antaeus.core.workers.interceptor

/**
 * Pre execution validation interceptor interface
 */
interface PreExecutionValidationInterceptor<T> {

    fun validate(context: T): Boolean
}
