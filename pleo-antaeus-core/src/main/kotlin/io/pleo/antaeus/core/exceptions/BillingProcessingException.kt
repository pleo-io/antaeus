package io.pleo.antaeus.core.exceptions

class BillingProcessingException(invoiceId: Int, customerId: Int, cause: Throwable?) :
        Exception("Billing of invoice '$invoiceId' for customer '$customerId' has failed", cause)
