package io.pleo.antaeus.core.workers.billing.validator

import io.pleo.antaeus.core.workers.interceptor.PreExecutionValidatorInterceptor
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class ValidateInvoiceStatusInterceptor: PreExecutionValidatorInterceptor<Invoice> {
    override fun handle(context: Invoice): Boolean {
        return context.status == InvoiceStatus.SCHEDULED
    }
}
