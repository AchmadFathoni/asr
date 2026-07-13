package com.asr.core

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HabitTrackingTest {
    @Test
    fun habitStatesHaveThreeValues() {
        assertEquals(3, HabitState.entries.size)
    }

    @Test
    fun notDoneIsDefault() {
        assertEquals("NOT_DONE", HabitState.NOT_DONE.name)
    }

    @Test
    fun doneStateIsCorrect() {
        assertEquals("DONE", HabitState.DONE.name)
    }

    @Test
    fun skippedStateIsCorrect() {
        assertEquals("SKIPPED", HabitState.SKIPPED.name)
    }

    @Test
    fun recordCountStartsAtZero() {
        val record = HabitRecord(
            habitId = 1,
            date = LocalDate(2026, 7, 13),
            state = HabitState.NOT_DONE,
        )
        assertEquals(0, record.count)
    }

    @Test
    fun skippedRecordPreservesCount() {
        val record = HabitRecord(
            id = 1,
            habitId = 1,
            date = LocalDate(2026, 7, 13),
            state = HabitState.SKIPPED,
            count = 3,
        )
        assertEquals(3, record.count)
        assertEquals(HabitState.SKIPPED, record.state)
    }

    @Test
    fun recordsForDifferentHabitsAreIndependent() {
        val r1 = HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13))
        val r2 = HabitRecord(habitId = 2, date = LocalDate(2026, 7, 13))
        assertEquals(1L, r1.habitId)
        assertEquals(2L, r2.habitId)
    }

    @Test
    fun recordsForDifferentDatesAreIndependent() {
        val r1 = HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13))
        val r2 = HabitRecord(habitId = 1, date = LocalDate(2026, 7, 14))
        assertEquals(LocalDate(2026, 7, 13), r1.date)
        assertEquals(LocalDate(2026, 7, 14), r2.date)
    }

    @Test
    fun habitCountDefaultsToOne() {
        val habit = Habit(title = "H")
        assertEquals(1, habit.frequencyCount)
    }
}
