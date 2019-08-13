package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.timerTask

class BillingService(private val paymentProvider: PaymentProvider,
                     private val invoiceService: InvoiceService) {

   fun schedulePayment() {
      //Set to run once a day
      Timer("schedulePayment").schedule(
              timerTask { executePayment(invoiceService) },
              0,
              86400000)
   }

   private fun executePayment(invoiceService: InvoiceService) {
      if (LocalDateTime.now().dayOfMonth == 1) {
         for (invoice in invoiceService.fetchUnpaid()) {
            if (paymentProvider.charge(invoice)) {
               invoiceService.updateInvoiceStatus(invoice.id, true)
            }
         }
      }
   }
}