package com.asr.core

import com.asr.core.habit.HabitFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertTrue

class HabitFrequencyTest {
    @Test
    fun dailyFrequencyHasCorrectName() {
        assertEquals("DAILY", HabitFrequency.DAILY.name)
    }

    @Test
    fun weeklyFrequencyHasCorrectName() {
        assertEquals("WEEKLY", HabitFrequency.WEEKLY.name)
    }

    @Test
    fun monthlyFrequencyHasCorrectName() {
        assertEquals("MONTHLY", HabitFrequency.MONTHLY.name)
    }

    @Test
    fun yearlyFrequencyHasCorrectName() {
        assertEquals("YEARLY", HabitFrequency.YEARLY.name)
    }

    @Test
    fun allFrequenciesAreValid() {
        val frequencyStrings = setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
        HabitFrequency.entries.forEach {
            assertTrue(frequencyStrings.contains(it.name))
        }
    }

    @Test
    fun fourFrequencyTypesExist() {
        assertEquals(4, HabitFrequency.entries.size)
    }
}
