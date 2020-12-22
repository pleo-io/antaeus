package io.pleo.antaeus.app;

import mu.KotlinLogging
import java.util.*
import java.util.Calendar.*

private val logger = KotlinLogging.logger {}

/*
    Method to be used as a scheduler, any executable function can be passed in as a parameter
 */
//Todo: Implement a more generic scheduler in which it is possible to define other execution frequencies(eg. every 15 days or every 2 months)
internal fun monthlyScheduleFunction(date: Date, function: () -> Unit): Date {

    val timer = Timer()
    val firstDayOfNextMonth = firstDayOfNextMonth(date)
    //Todo: Find a better way to calculate the next first day of the month
    val timeInMillisUntilNextMonth = 2592000000L // this should be 1 month in millis

    timer.schedule(object : TimerTask() {
        override fun run() {
            function()
        }
    }, firstDayOfNextMonth, timeInMillisUntilNextMonth)

    logger.info("First execution was scheduled to $firstDayOfNextMonth")
    return firstDayOfNextMonth
}

private fun firstDayOfNextMonth(date: Date): Date {
    val today = getInstance()
    today.time = date
    val nextDay: Calendar = getInstance()
    nextDay.clear()
    nextDay[YEAR] = today[YEAR]
    nextDay[MONTH] = today[MONTH] + 1
    nextDay[DAY_OF_MONTH] = 1 // 1st day of the month
    return nextDay.time
}