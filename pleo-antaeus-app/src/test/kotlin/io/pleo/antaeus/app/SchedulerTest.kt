package io.pleo.antaeus.app

import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.*
import org.junit.jupiter.api.Assertions.*


class SchedulerTest {

    @Test
    fun `should schedule an function execution to the first day of every month`() {
        val testDateString = "15/11/2025"
        val testDate: Date = SimpleDateFormat("dd/MM/yyyy").parse(testDateString)

        val assertDateString = "01/12/2025"
        val assertDate: Date = SimpleDateFormat("dd/MM/yyyy").parse(assertDateString)
        assertEquals(monthlyScheduleFunction(testDate) { println("testing") }, assertDate)
    }


    @Test
    fun `should return first day of the next month if already first day of a month`() {
        val testDateString = "01/11/2025"
        val testDate: Date = SimpleDateFormat("dd/MM/yyyy").parse(testDateString)

        val assertDateString = "01/12/2025"
        val assertDate: Date = SimpleDateFormat("dd/MM/yyyy").parse(assertDateString)
        assertEquals(monthlyScheduleFunction(testDate) { println("testing") }, assertDate)
    }

}