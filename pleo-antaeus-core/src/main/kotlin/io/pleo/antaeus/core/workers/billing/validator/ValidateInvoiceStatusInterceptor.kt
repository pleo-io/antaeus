package io.pleo.antaeus.core.workers.billing.validator

import io.pleo.antaeus.core.workers.interceptor.PreExecutionValidationInterceptor
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

/**
 * An implementation of PreExecutionValidationInterceptor that validates
 * the correct status of an Invoice before proceeding to charge the
 * customer.
 */
class ValidateInvoiceStatusInterceptor: PreExecutionValidationInterceptor<Invoice> {
    override fun validate(context: Invoice): Boolean {
        return context.status != InvoiceStatus.PAID
    }
}
