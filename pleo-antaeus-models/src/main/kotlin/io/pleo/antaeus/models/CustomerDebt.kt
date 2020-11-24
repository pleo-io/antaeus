package io.pleo.antaeus.models

data class CustomerDebt(
        private val cId: Int,
        private val amountMoney: List<Money>
){
    val customerId: Int = cId
    var amount: List<Money> = amountMoney
}
