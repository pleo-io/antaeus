package io.pleo.antaeus.core.exceptions

class CustomerNotFoundException(id: Int) : EntityNotFoundException("Customer", id)
