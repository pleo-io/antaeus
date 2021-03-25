package io.pleo.antaeus.core.exceptions

class BillNotFoundException(id: Int) : EntityNotFoundException("Bill", id)