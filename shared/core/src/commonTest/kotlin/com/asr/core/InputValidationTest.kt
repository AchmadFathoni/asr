package com.asr.core

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.task.Task
import com.asr.core.tag.Tag
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InputValidationTest {
    @Test
    fun veryLongTitleShouldBeAllowed() {
        val longTitle = "A".repeat(10000)
        val task = Task(title = longTitle)
        assertEquals(longTitle, task.title)
    }

    @Test
    fun emptyTitleIsAllowed() {
        val task = Task(title = "")
        assertEquals("", task.title)
    }

    @Test
    fun whitespaceTitleIsAllowed() {
        val task = Task(title = "   ")
        assertEquals("   ", task.title)
    }

    @Test
    fun nullParentIdOnRootTask() {
        val task = Task(title = "Root")
        assertEquals(null, task.parentId)
    }

    @Test
    fun deeplyNestedTasksWork() {
        fun buildNested(depth: Int, parentId: Long? = null): List<Task> {
            if (depth == 0) return emptyList()
            val task = Task(id = depth.toLong(), title = "Depth $depth", parentId = parentId)
            return listOf(task) + buildNested(depth - 1, task.id)
        }
        val tasks = buildNested(100)
        assertEquals(100, tasks.size)
        assertEquals(100L, tasks[1].parentId)
    }

    @Test
    fun orphanChildIsAllowed() {
        val child = Task(title = "Orphan", parentId = 999L)
        assertEquals(999L, child.parentId)
    }

    @Test
    fun reminderTimeFormatsArePreserved() {
        val task = Task(title = "Task", reminderTime = "14:30")
        assertEquals("14:30", task.reminderTime)
    }

    @Test
    fun taskDescriptionHandlesEmptyString() {
        val task = Task(title = "T", description = "")
        assertEquals("", task.description)
    }

    @Test
    fun taskDescriptionHandlesMultiLineText() {
        val desc = "Line 1\nLine 2\nLine 3"
        val task = Task(title = "T", description = desc)
        assertEquals(desc, task.description)
    }

    @Test
    fun taskDescriptionHandlesSpecialCharacters() {
        val desc = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~"
        val task = Task(title = "T", description = desc)
        assertEquals(desc, task.description)
    }

    @Test
    fun taskDescriptionHandlesEmoji() {
        val desc = "✅🔥🚀"
        val task = Task(title = "T", description = desc)
        assertEquals(desc, task.description)
    }

    @Test
    fun habitCountZeroIsAllowed() {
        val habit = Habit(title = "H", frequencyCount = 0)
        assertEquals(0, habit.frequencyCount)
    }

    @Test
    fun habitCountNegativeIsAllowed() {
        val habit = Habit(title = "H", frequencyCount = -1)
        assertEquals(-1, habit.frequencyCount)
    }

    @Test
    fun habitCountLargeValueIsAllowed() {
        val habit = Habit(title = "H", frequencyCount = 9_999_999)
        assertEquals(9_999_999, habit.frequencyCount)
    }

    @Test
    fun emptyReminderTimeIsNullWhenBlank() {
        val task = Task(title = "T", reminderTime = "   ".ifBlank { null })
        assertEquals(null, task.reminderTime)
    }

    @Test
    fun recordCountExceedingFrequencyIsAllowed() {
        val record = HabitRecord(
            id = 1,
            habitId = 1,
            date = LocalDate(2026, 7, 13),
            state = HabitState.DONE,
            count = 99,
        )
        assertEquals(99, record.count)
    }

    @Test
    fun tagNameSpecialCharsArePreserved() {
        val tag = Tag(id = 1, name = "!@# Work & Home")
        assertEquals("!@# Work & Home", tag.name)
    }

    @Test
    fun tagNameEmojiIsPreserved() {
        val tag = Tag(id = 1, name = "🚀 Important")
        assertEquals("🚀 Important", tag.name)
    }
}
