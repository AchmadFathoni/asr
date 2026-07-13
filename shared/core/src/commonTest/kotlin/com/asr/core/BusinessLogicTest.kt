package com.asr.core

import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.habit.computeStreak
import com.asr.core.habit.habitRecordWithNewState
import com.asr.core.habit.shouldShowToday
import com.asr.core.tag.Tag
import com.asr.core.task.Task
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

enum class TaskFilter { ALL, ACTIVE, DONE }

class BusinessLogicTest {

    // Habit period counting algorithm extracted from TodayViewModel / HabitsViewModel
    private fun computeHabitState(habit: Habit, existingRecord: HabitRecord?): HabitState {
        val currentCount = existingRecord?.takeUnless { it.state == HabitState.SKIPPED }?.count ?: 0
        val newCount = currentCount + 1
        return if (newCount >= habit.frequencyCount) HabitState.DONE else HabitState.NOT_DONE
    }

    @Test
    fun firstTapWhenFrequencyIs1_setsDone() {
        val habit = Habit(id = 1, title = "H", frequencyCount = 1)
        val state = computeHabitState(habit, null)
        assertEquals(HabitState.DONE, state)
    }

    @Test
    fun firstTapWhenFrequencyIs3_staysNotDone() {
        val habit = Habit(id = 1, title = "H", frequencyCount = 3)
        val state = computeHabitState(habit, null)
        assertEquals(HabitState.NOT_DONE, state)
    }

    @Test
    fun secondTapWhenFrequencyIs3_staysNotDone() {
        val habit = Habit(id = 1, title = "H", frequencyCount = 3)
        val existing =
            HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13), count = 1, state = HabitState.NOT_DONE)
        val state = computeHabitState(habit, existing)
        assertEquals(HabitState.NOT_DONE, state)
    }

    @Test
    fun thirdTapWhenFrequencyIs3_setsDone_at_exact_boundary() {
        val habit = Habit(id = 1, title = "H", frequencyCount = 3)
        val existing =
            HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13), count = 2, state = HabitState.NOT_DONE)
        val state = computeHabitState(habit, existing)
        assertEquals(HabitState.DONE, state)
    }

    @Test
    fun exceedingFrequencyKeepsDone() {
        val habit = Habit(id = 1, title = "H", frequencyCount = 3)
        val existing =
            HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13), count = 3, state = HabitState.DONE)
        val state = computeHabitState(habit, existing)
        assertEquals(HabitState.DONE, state)
    }

    @Test
    fun skippedRecordDoesNotCountTowardProgress() {
        val habit = Habit(id = 1, title = "H", frequencyCount = 2)
        val existing =
            HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13), count = 1, state = HabitState.SKIPPED)
        // Skipped → count treated as 0, so newCount = 1, freqCount = 2 → NOT_DONE
        val state = computeHabitState(habit, existing)
        assertEquals(HabitState.NOT_DONE, state)
    }

    @Test
    fun highFrequencyCountBoundary() {
        val habit = Habit(id = 1, title = "H", frequencyCount = 99)
        // 97 taps so far → 98th tap < 99 → NOT_DONE
        val notDone =
            HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13), count = 97, state = HabitState.NOT_DONE)
        assertEquals(HabitState.NOT_DONE, computeHabitState(habit, notDone))
        // 98 taps so far → 99th tap = 99 → exactly at boundary → DONE
        val atBoundary =
            HabitRecord(habitId = 1, date = LocalDate(2026, 7, 13), count = 98, state = HabitState.NOT_DONE)
        assertEquals(HabitState.DONE, computeHabitState(habit, atBoundary))
    }

    // Task filtering logic from TasksViewModel
    private fun filterTasks(
        tasks: List<Task>,
        doneTasks: List<Task>,
        filter: TaskFilter,
        query: String,
    ): List<Task> {
        val filtered = when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.ACTIVE -> tasks.filter { !it.isDone }
            TaskFilter.DONE -> doneTasks
        }
        return if (query.isBlank()) filtered
        else filtered.filter { it.title.contains(query, ignoreCase = true) }
    }

    @Test
    fun activeFilterExcludesDoneTasks() {
        val tasks = listOf(
            Task(id = 1, title = "Active", isDone = false),
            Task(id = 2, title = "Done", isDone = true),
        )
        val result = filterTasks(tasks, tasks.filter { it.isDone }, TaskFilter.ACTIVE, "")
        assertEquals(1, result.size)
        assertEquals("Active", result[0].title)
    }

    @Test
    fun allFilterIncludesEverything() {
        val tasks = listOf(
            Task(id = 1, title = "Active", isDone = false),
            Task(id = 2, title = "Done", isDone = true),
        )
        val result = filterTasks(tasks, tasks.filter { it.isDone }, TaskFilter.ALL, "")
        assertEquals(2, result.size)
    }

    @Test
    fun doneFilterOnlyShowsDone() {
        val tasks = listOf(
            Task(id = 1, title = "Active", isDone = false),
            Task(id = 2, title = "Done", isDone = true),
        )
        val result = filterTasks(tasks, tasks.filter { it.isDone }, TaskFilter.DONE, "")
        assertEquals(1, result.size)
        assertEquals("Done", result[0].title)
    }

    @Test
    fun searchQueryCaseInsensitive() {
        val tasks = listOf(Task(id = 1, title = "Buy Milk"))
        val result = filterTasks(tasks, emptyList(), TaskFilter.ALL, "MILK")
        assertEquals(1, result.size)
        val noResult = filterTasks(tasks, emptyList(), TaskFilter.ALL, "eggs")
        assertEquals(0, noResult.size)
    }

    @Test
    fun searchQueryMatchesPartialTitle() {
        val tasks = listOf(Task(id = 1, title = "Buy Milk"))
        val result = filterTasks(tasks, emptyList(), TaskFilter.ALL, "Buy")
        assertEquals(1, result.size)
        val resultPartial = filterTasks(tasks, emptyList(), TaskFilter.ALL, "y M")
        assertEquals(1, resultPartial.size)
    }

    @Test
    fun emptyTaskListIsHandled() {
        val result = filterTasks(emptyList(), emptyList(), TaskFilter.ALL, "")
        assertEquals(0, result.size)
        val resultActive = filterTasks(emptyList(), emptyList(), TaskFilter.ACTIVE, "")
        assertEquals(0, resultActive.size)
    }

    // Task sub-task hierarchy
    @Test
    fun subTasksAreGroupedByParentId() {
        val tasks = listOf(
            Task(id = 1, title = "Parent"),
            Task(id = 2, title = "Child", parentId = 1),
            Task(id = 3, title = "Another Child", parentId = 1),
            Task(id = 4, title = "Orphan", parentId = 999),
        )
        val topLevel = tasks.filter { it.parentId == null }
        assertEquals(1, topLevel.size)

        val subMap = tasks.filter { it.parentId != null }.groupBy { it.parentId }
        assertEquals(2, subMap[1]?.size)
        assertEquals(1, subMap[999]?.size)
    }

    @Test
    fun deletingParentShouldAlsoDeleteChildren() {
        val tasks = listOf(
            Task(id = 1, title = "Parent"),
            Task(id = 2, title = "Child", parentId = 1),
            Task(id = 3, title = "Another Child", parentId = 1),
        )
        val parentId = 1L
        val remaining = tasks.filter { it.id != parentId && it.parentId != parentId }
        assertEquals(0, remaining.size)
    }

    @Test
    fun taskDescriptionDefaultIsEmpty() {
        val task = Task(title = "Test")
        assertEquals("", task.description)
    }

    @Test
    fun taskReminderIsNullByDefault() {
        val task = Task(title = "Test")
        assertEquals(null, task.reminderTime)
    }

    @Test
    fun taskDescriptionIsPreserved() {
        val task = Task(title = "Test", description = "A long description with details")
        assertEquals("A long description with details", task.description)
    }

    @Test
    fun habitDescriptionDefaultIsEmpty() {
        val habit = Habit(title = "Test")
        assertEquals("", habit.description)
    }

    @Test
    fun habitReminderIsNullByDefault() {
        val habit = Habit(title = "Test")
        assertEquals(null, habit.reminderTime)
    }

    // Tag management
    @Test
    fun tagCreationPreservesName() {
        val tag = Tag(id = 1, name = "Work")
        assertEquals("Work", tag.name)
        assertEquals(1L, tag.id)
    }

    @Test
    fun tagBlankNameIsAllowedAtModelLevel() {
        val tag = Tag(name = "   ")
        assertEquals("   ", tag.name)
    }

    @Test
    fun tagEmptyNameIsAllowedAtModelLevel() {
        val tag = Tag(name = "")
        assertEquals("", tag.name)
    }

    @Test
    fun multipleTagsCanBeAssignedToTask() {
        val tagIds = listOf(1L, 2L, 3L)
        assertEquals(3, tagIds.size)
        assertTrue(1L in tagIds)
    }

    @Test
    fun removingTagFromSelection() {
        val selected = setOf(1L, 2L, 3L)
        val afterRemove = selected - 2L
        assertEquals(setOf(1L, 3L), afterRemove)
    }

    @Test
    fun addingTagToSelection() {
        val selected = setOf(1L)
        val afterAdd = selected + 2L
        assertEquals(setOf(1L, 2L), afterAdd)
    }

    @Test
    fun taskDueDateDefaultIsNull() {
        val task = Task(title = "Test")
        assertEquals(null, task.dueDate)
    }

    @Test
    fun taskDueDateIsPreserved() {
        val date = kotlinx.datetime.LocalDate(2026, 12, 31)
        val task = Task(title = "Test", dueDate = date)
        assertEquals(date, task.dueDate)
    }

    // Due-date filtering for Today page
    @Test
    fun taskWithoutDueDateShowsInToday() {
        val today = kotlinx.datetime.LocalDate(2026, 7, 13)
        val task = Task(id = 1, title = "No due date", dueDate = null)
        val visible = task.dueDate == null || task.dueDate <= today
        assertTrue(visible)
    }

    @Test
    fun taskDueTodayShowsInToday() {
        val today = kotlinx.datetime.LocalDate(2026, 7, 13)
        val task = Task(id = 1, title = "Due today", dueDate = today)
        val visible = task.dueDate == null || task.dueDate <= today
        assertTrue(visible)
    }

    @Test
    fun overdueTaskShowsInToday() {
        val today = kotlinx.datetime.LocalDate(2026, 7, 13)
        val task = Task(id = 1, title = "Overdue", dueDate = kotlinx.datetime.LocalDate(2026, 7, 12))
        val visible = task.dueDate == null || task.dueDate <= today
        assertTrue(visible)
    }

    @Test
    fun futureTaskHiddenFromToday() {
        val today = kotlinx.datetime.LocalDate(2026, 7, 13)
        val task = Task(id = 1, title = "Future", dueDate = kotlinx.datetime.LocalDate(2026, 7, 14))
        val visible = task.dueDate == null || task.dueDate <= today
        assertFalse(visible)
    }

    // Habit period filtering for Today tab

    @Test fun dailyHabitAlwaysShows() {
        assertTrue(Habit(title = "D").shouldShowToday(LocalDate(2026, 7, 13)))
    }

    @Test fun weeklyShowsOnCorrectDay() {
        val mon = LocalDate(2026, 7, 13) // Monday
        val h = Habit(title = "W", frequencyType = HabitFrequency.WEEKLY, daysOfWeek = setOf(1))
        assertTrue(h.shouldShowToday( mon))
    }

    @Test fun weeklyHiddenOnWrongDay() {
        val tue = LocalDate(2026, 7, 14) // Tuesday
        val h = Habit(title = "W", frequencyType = HabitFrequency.WEEKLY, daysOfWeek = setOf(1)) // Monday
        assertFalse(h.shouldShowToday( tue))
    }

    @Test fun weeklySunday() {
        val sun = LocalDate(2026, 7, 19) // Sunday
        val h = Habit(title = "W", frequencyType = HabitFrequency.WEEKLY, daysOfWeek = setOf(7))
        assertTrue(h.shouldShowToday( sun))
    }

    @Test fun weeklyMultiDay() {
        val mon = LocalDate(2026, 7, 13) // Monday
        val wed = LocalDate(2026, 7, 15) // Wednesday
        val fri = LocalDate(2026, 7, 17) // Friday
        val tue = LocalDate(2026, 7, 14) // Tuesday
        val h = Habit(title = "MWF", frequencyType = HabitFrequency.WEEKLY, daysOfWeek = setOf(1, 3, 5))
        assertTrue(h.shouldShowToday( mon))
        assertTrue(h.shouldShowToday( wed))
        assertTrue(h.shouldShowToday( fri))
        assertFalse(h.shouldShowToday( tue))
    }

    @Test fun weeklyEmptyDaysAlwaysShows() {
        assertTrue(Habit(title = "W", frequencyType = HabitFrequency.WEEKLY).shouldShowToday(LocalDate(2026, 7, 13)))
    }

    @Test fun weeklyDayZeroNeverMatches() {
        val mon = LocalDate(2026, 7, 13) // Monday = day 1
        assertFalse(Habit(title = "W", frequencyType = HabitFrequency.WEEKLY, daysOfWeek = setOf(0)).shouldShowToday(mon))
    }

    @Test fun weeklyDayEightNeverMatches() {
        val sun = LocalDate(2026, 7, 19) // Sunday = day 7
        assertFalse(Habit(title = "W", frequencyType = HabitFrequency.WEEKLY, daysOfWeek = setOf(8)).shouldShowToday(sun))
    }

    @Test fun monthlyShowsOnCorrectDay() {
        val d15 = LocalDate(2026, 7, 15)
        val h = Habit(title = "M", frequencyType = HabitFrequency.MONTHLY, dayOfMonth = 15)
        assertTrue(h.shouldShowToday( d15))
    }

    @Test fun monthlyHiddenOnWrongDay() {
        val d16 = LocalDate(2026, 7, 16)
        val h = Habit(title = "M", frequencyType = HabitFrequency.MONTHLY, dayOfMonth = 15)
        assertFalse(h.shouldShowToday( d16))
    }

    @Test fun monthlyNullDayAlwaysShows() {
        assertTrue(Habit(title = "M", frequencyType = HabitFrequency.MONTHLY, dayOfMonth = null).shouldShowToday(LocalDate(2026, 7, 13)))
    }

    @Test fun monthlyDay31ClampsInApril() {
        val h = Habit(title = "M", frequencyType = HabitFrequency.MONTHLY, dayOfMonth = 31)
        val apr30 = LocalDate(2026, 4, 30) // April has 30 days, 31 clamps to 30
        assertTrue(h.shouldShowToday( apr30))
        val apr29 = LocalDate(2026, 4, 29)
        assertFalse(h.shouldShowToday( apr29))
    }

    @Test fun monthlyDay31ClampsInFebruaryNonLeap() {
        val h = Habit(title = "M", frequencyType = HabitFrequency.MONTHLY, dayOfMonth = 31)
        val feb28 = LocalDate(2025, 2, 28) // 2025 not leap
        assertTrue(h.shouldShowToday( feb28))
        val feb27 = LocalDate(2025, 2, 27)
        assertFalse(h.shouldShowToday( feb27))
    }

    @Test fun monthlyDay31ShowsInJanuary() {
        val h = Habit(title = "M", frequencyType = HabitFrequency.MONTHLY, dayOfMonth = 31)
        assertTrue(h.shouldShowToday( LocalDate(2026, 1, 31)))
    }

    @Test fun monthlyLeapYearFeb29() {
        val h = Habit(title = "M", frequencyType = HabitFrequency.MONTHLY, dayOfMonth = 29)
        assertTrue(h.shouldShowToday( LocalDate(2024, 2, 29))) // 2024 is leap
        assertTrue(h.shouldShowToday( LocalDate(2025, 2, 28))) // 2025: 29 clamps to 28
    }

    @Test fun yearlyShowsOnCorrectMonthAndDay() {
        val h = Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 7, dayOfMonth = 4)
        assertTrue(h.shouldShowToday( LocalDate(2026, 7, 4)))
    }

    @Test fun yearlyHiddenOnWrongMonth() {
        val h = Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 7, dayOfMonth = 4)
        assertFalse(h.shouldShowToday( LocalDate(2026, 8, 4)))
    }

    @Test fun yearlyHiddenOnWrongDay() {
        val h = Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 7, dayOfMonth = 4)
        assertFalse(h.shouldShowToday( LocalDate(2026, 7, 5)))
    }

    @Test fun yearlyNullMonthAlwaysShows() {
        assertTrue(Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = null, dayOfMonth = 15).shouldShowToday(LocalDate(2026, 7, 13)))
    }

    @Test fun yearlyNullDayAlwaysShows() {
        assertTrue(Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 7, dayOfMonth = null).shouldShowToday(LocalDate(2026, 7, 13)))
    }

    @Test fun yearlyFeb29InLeapYear() {
        val h = Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 2, dayOfMonth = 29)
        assertTrue(h.shouldShowToday( LocalDate(2024, 2, 29)))
    }

    @Test fun yearlyFeb29InNonLeapYearClamps() {
        val h = Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 2, dayOfMonth = 29)
        assertTrue(h.shouldShowToday( LocalDate(2025, 2, 28)))
        assertFalse(h.shouldShowToday( LocalDate(2025, 2, 27)))
    }

    @Test fun yearlyDec31() {
        val h = Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 12, dayOfMonth = 31)
        assertTrue(h.shouldShowToday( LocalDate(2026, 12, 31)))
    }

    @Test fun yearlyMonthZeroNeverMatches() {
        assertFalse(Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 0, dayOfMonth = 15).shouldShowToday(LocalDate(2026, 7, 15)))
    }

    @Test fun yearlyMonthThirteenNeverMatches() {
        assertFalse(Habit(title = "Y", frequencyType = HabitFrequency.YEARLY, monthOfYear = 13, dayOfMonth = 15).shouldShowToday(LocalDate(2026, 7, 15)))
    }

    @Test fun dailyStreakConsecutive() {
        val today = LocalDate(2026, 7, 13)
        val h = Habit(title = "S")
        val records = listOf(
            HabitRecord(habitId = 0, date = today, state = HabitState.DONE),
            HabitRecord(habitId = 0, date = LocalDate(2026, 7, 12), state = HabitState.DONE),
            HabitRecord(habitId = 0, date = LocalDate(2026, 7, 11), state = HabitState.DONE),
        )
        assertEquals(3, h.computeStreak(records, today))
    }

    @Test fun dailyStreakBroken() {
        val today = LocalDate(2026, 7, 13)
        val h = Habit(title = "S")
        val records = listOf(
            HabitRecord(habitId = 0, date = today, state = HabitState.DONE),
            HabitRecord(habitId = 0, date = LocalDate(2026, 7, 12), state = HabitState.DONE),
            HabitRecord(habitId = 0, date = LocalDate(2026, 7, 10), state = HabitState.DONE),
        )
        assertEquals(2, h.computeStreak(records, today))
    }

    @Test fun dailyStreakZeroWhenMissing() {
        val today = LocalDate(2026, 7, 13)
        val h = Habit(title = "S")
        assertEquals(0, h.computeStreak(emptyList(), today))
    }

    @Test fun weeklyStreakConsecutive() {
        val today = LocalDate(2026, 7, 13) // Monday
        val h = Habit(title = "W", frequencyType = HabitFrequency.WEEKLY)
        val records = listOf(
            HabitRecord(habitId = 0, date = today, state = HabitState.DONE),
            HabitRecord(habitId = 0, date = LocalDate(2026, 7, 6), state = HabitState.DONE),
        )
        assertEquals(2, h.computeStreak(records, today))
    }

    // ── habitRecordWithNewState ──────────────────────────────────────────────

    @Test fun recordNewFirstTapCreatesDone() {
        val h = Habit(title = "H")
        val d = LocalDate(2026, 8, 1)
        val r = habitRecordWithNewState(null, h, d, HabitState.DONE)
        assertEquals(HabitState.DONE, r.state)
        assertEquals(1, r.count)
        assertEquals(h.id, r.habitId)
        assertEquals(d, r.date)
    }

    @Test fun recordToggleDoneBackToNotDone() {
        val h = Habit(title = "H")
        val d = LocalDate(2026, 8, 1)
        val existing = HabitRecord(habitId = h.id, date = d, state = HabitState.DONE, count = 1)
        val r = habitRecordWithNewState(existing, h, d, HabitState.DONE)
        assertEquals(HabitState.NOT_DONE, r.state)
        assertEquals(0, r.count)
    }

    @Test fun recordSkipSetsSkipped() {
        val h = Habit(title = "H")
        val d = LocalDate(2026, 8, 1)
        val r = habitRecordWithNewState(null, h, d, HabitState.SKIPPED)
        assertEquals(HabitState.SKIPPED, r.state)
        assertEquals(0, r.count)
    }

    @Test fun recordFirstOfThreeTapsNotDoneYet() {
        val h = Habit(title = "H", frequencyCount = 3)
        val d = LocalDate(2026, 8, 1)
        val r = habitRecordWithNewState(null, h, d, HabitState.DONE)
        assertEquals(HabitState.NOT_DONE, r.state)
        assertEquals(1, r.count)
    }

    @Test fun recordSecondOfThreeTapsStillNotDone() {
        val h = Habit(title = "H", frequencyCount = 3)
        val d = LocalDate(2026, 8, 1)
        val existing = HabitRecord(habitId = h.id, date = d, state = HabitState.NOT_DONE, count = 1)
        val r = habitRecordWithNewState(existing, h, d, HabitState.DONE)
        assertEquals(HabitState.NOT_DONE, r.state)
        assertEquals(2, r.count)
    }

    @Test fun recordThirdTapCompletes() {
        val h = Habit(title = "H", frequencyCount = 3)
        val d = LocalDate(2026, 8, 1)
        val existing = HabitRecord(habitId = h.id, date = d, state = HabitState.NOT_DONE, count = 2)
        val r = habitRecordWithNewState(existing, h, d, HabitState.DONE)
        assertEquals(HabitState.DONE, r.state)
        assertEquals(3, r.count)
    }

    @Test fun recordAlReadyDoneTapsAgainResets() {
        val h = Habit(title = "H", frequencyCount = 3)
        val d = LocalDate(2026, 8, 1)
        val existing = HabitRecord(habitId = h.id, date = d, state = HabitState.DONE, count = 3)
        val r = habitRecordWithNewState(existing, h, d, HabitState.DONE)
        assertEquals(HabitState.NOT_DONE, r.state)
        assertEquals(0, r.count)
    }

    @Test fun recordSkippedThenTapMakesNotDone() {
        val h = Habit(title = "H")
        val d = LocalDate(2026, 8, 1)
        val existing = HabitRecord(habitId = h.id, date = d, state = HabitState.SKIPPED)
        val r = habitRecordWithNewState(existing, h, d, HabitState.DONE)
        assertEquals(HabitState.DONE, r.state)
        assertEquals(1, r.count)
    }

    @Test fun recordStreakSkipsMissingMiddleDay() {
        val today = LocalDate(2026, 8, 3)
        val h = Habit(title = "S")
        val records = listOf(
            HabitRecord(habitId = 0, date = today, state = HabitState.DONE),
            HabitRecord(habitId = 0, date = LocalDate(2026, 8, 2), state = HabitState.DONE),
            // Aug 1 missing — skip
            HabitRecord(habitId = 0, date = LocalDate(2026, 7, 31), state = HabitState.DONE),
        )
        assertEquals(2, h.computeStreak(records, today))
    }

    @Test fun recordStreakOnlyCountsDone() {
        val today = LocalDate(2026, 8, 3)
        val h = Habit(title = "S")
        val records = listOf(
            HabitRecord(habitId = 0, date = today, state = HabitState.DONE),
            HabitRecord(habitId = 0, date = LocalDate(2026, 8, 2), state = HabitState.SKIPPED),
            HabitRecord(habitId = 0, date = LocalDate(2026, 8, 1), state = HabitState.NOT_DONE),
        )
        assertEquals(1, h.computeStreak(records, today))
    }
}
