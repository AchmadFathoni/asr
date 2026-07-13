package com.asr.core

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateTimeEdgeCasesTest {
    @Test
    fun epochDaysConversionIsReversible() {
        val date = LocalDate(2026, 7, 13)
        val epochDays = date.toEpochDays().toLong()
        val restored = LocalDate.fromEpochDays(epochDays.toInt())
        assertEquals(date, restored)
    }

    @Test
    fun dateComparisonWorks() {
        val today = LocalDate(2026, 7, 13)
        val yesterday = LocalDate(2026, 7, 12)
        assertTrue(today > yesterday)
    }

    @Test
    fun monthBoundaryWorks() {
        val endOfMonth = LocalDate(2026, 3, 31)
        val startOfNextMonth = LocalDate(2026, 4, 1)
        assertEquals(1, startOfNextMonth.toEpochDays() - endOfMonth.toEpochDays())
    }

    @Test
    fun weekPeriodCalculationWorks() {
        val start = LocalDate(2026, 7, 6)
        val end = LocalDate(2026, 7, 12)
        val days = end.toEpochDays() - start.toEpochDays()
        assertEquals(6, days)
    }

    @Test
    fun monthPeriodCalculationWorks() {
        val start = LocalDate(2026, 6, 13)
        val end = LocalDate(2026, 7, 13)
        assertTrue(end > start)
    }
}
