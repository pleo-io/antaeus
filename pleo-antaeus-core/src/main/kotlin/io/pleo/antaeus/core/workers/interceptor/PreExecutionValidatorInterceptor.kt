package io.pleo.antaeus.core.workers.interceptor

interface PreExecutionValidatorInterceptor<T> {

    fun handle(context: T): Boolean
}
