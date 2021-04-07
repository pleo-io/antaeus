/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.dals.CustomerDal
import io.pleo.antaeus.models.Customer

class CustomerService(private val customerDal: CustomerDal) {
    fun fetchAll(): List<Customer> {
        return customerDal.fetchCustomers()
    }

    @Throws(CustomerNotFoundException::class)
    fun fetch(id: Int): Customer {
        return customerDal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }
}
