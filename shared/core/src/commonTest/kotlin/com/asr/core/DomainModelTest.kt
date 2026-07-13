package com.asr.core

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.task.Task
import com.asr.core.tag.Tag
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainModelTest {
    @Test
    fun taskDefaultsToNotDone() {
        val task = Task(title = "Test task")
        assertFalse(task.isDone)
        assertEquals("Test task", task.title)
    }

    @Test
    fun habitDefaultsToDaily() {
        val habit = Habit(title = "Test habit")
        assertEquals(HabitFrequency.DAILY, habit.frequencyType)
        assertEquals(1, habit.frequencyCount)
    }

    @Test
    fun habitRecordDefaultsToNotDone() {
        val record = HabitRecord(
            habitId = 1L,
            date = LocalDate(2026, 7, 13),
        )
        assertEquals(HabitState.NOT_DONE, record.state)
    }

    @Test
    fun taskHasParentIdForSubtasks() {
        val parent = Task(id = 1, title = "Parent")
        val child = Task(id = 2, title = "Child", parentId = 1)
        assertEquals(1L, child.parentId)
    }

    @Test
    fun habitStatesAreCorrect() {
        assertEquals(3, HabitState.entries.size)
        assertTrue(HabitState.entries.contains(HabitState.SKIPPED))
    }
}
