package com.asr.widget

import com.asr.core.now
import com.asr.data.database.HabitEntity
import com.asr.data.database.HabitRecordEntity
import com.asr.data.database.TaskEntity
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetItemListTest {

    private val today = LocalDate.now()
    private val todayEpoch = today.toEpochDays()

    @Test
    fun `undone task appears in list`() {
        val tasks = listOf(TaskEntity(id = 1, title = "Buy milk"))
        val items = TodayViewsFactory.buildItemList(tasks, emptyList(), emptyList(), today)
        assertTrue(items.any { it is TodayViewsFactory.ListItem.TaskItem && it.task.title == "Buy milk" })
    }

    @Test
    fun `done task excluded`() {
        val tasks = listOf(TaskEntity(id = 1, title = "Done", isDone = true))
        val items = TodayViewsFactory.buildItemList(tasks, emptyList(), emptyList(), today)
        assertTrue(items.none { it is TodayViewsFactory.ListItem.TaskItem })
    }

    @Test
    fun `all done message when only done tasks exist`() {
        val tasks = listOf(TaskEntity(id = 1, title = "Done", isDone = true))
        val items = TodayViewsFactory.buildItemList(tasks, emptyList(), emptyList(), today)
        assertTrue(items.any { it is TodayViewsFactory.ListItem.Label && it.text == "All done for today!" })
    }

    @Test
    fun `nothing message when no data`() {
        val items = TodayViewsFactory.buildItemList(emptyList(), emptyList(), emptyList(), today)
        assertTrue(items.any { it is TodayViewsFactory.ListItem.Label && it.text.contains("Nothing for today") })
    }

    @Test
    fun `unscheduled habit shows nothing message not all done`() {
        val otherDow = ((today.dayOfWeek.ordinal + 1) % 7) + 1
        val habits = listOf(HabitEntity(id = 1, title = "Off day", frequencyType = "WEEKLY", daysOfWeek = otherDow.toString()))
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, emptyList(), today)
        assertTrue(items.any { it is TodayViewsFactory.ListItem.Label && it.text.contains("Nothing for today") })
        assertTrue(items.none { it is TodayViewsFactory.ListItem.Label && it.text == "All done for today!" })
    }

    @Test
    fun `period count excludes off-schedule records`() {
        val today = LocalDate(2026, 7, 13) // Monday
        val tuesday = LocalDate(2026, 7, 14).toEpochDays()
        val habits = listOf(HabitEntity(id = 1, title = "Mon habit", frequencyType = "WEEKLY", frequencyCount = 1, daysOfWeek = "1"))
        val records = listOf(HabitRecordEntity(habitId = 1, date = tuesday.toLong(), state = "DONE"))
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, records, today)
        val habitItem = items.filterIsInstance<TodayViewsFactory.ListItem.HabitItem>().first()
        assertEquals(0, habitItem.periodCount)
    }

    @Test
    fun `period target collapses clamped days in february`() {
        val today = LocalDate(2025, 2, 28) // non-leap February
        val habits = listOf(HabitEntity(id = 1, title = "Payday", frequencyType = "MONTHLY", frequencyCount = 2, daysOfMonth = "30,31"))
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, emptyList(), today)
        val habitItem = items.filterIsInstance<TodayViewsFactory.ListItem.HabitItem>().first()
        assertEquals(1, habitItem.periodTarget)
    }

    @Test
    fun `daily habit without today record appears`() {
        val habits = listOf(HabitEntity(id = 1, title = "Exercise", frequencyType = "DAILY"))
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, emptyList(), today)
        assertTrue(items.any { it is TodayViewsFactory.ListItem.HabitItem && it.habit.title == "Exercise" })
    }

    @Test
    fun `done habit excluded`() {
        val habits = listOf(HabitEntity(id = 1, title = "Exercise", frequencyType = "DAILY"))
        val records = listOf(HabitRecordEntity(habitId = 1, date = todayEpoch.toLong(), state = "DONE"))
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, records, today)
        assertTrue(items.none { it is TodayViewsFactory.ListItem.HabitItem })
    }

    @Test
    fun `header label is always first`() {
        val tasks = listOf(TaskEntity(id = 1, title = "Buy milk"))
        val items = TodayViewsFactory.buildItemList(tasks, emptyList(), emptyList(), today)
        assertEquals(TodayViewsFactory.ListItem.Label("Today To-do List", TodayViewsFactory.LabelStyle.HEADER), items.first())
    }

    @Test
    fun `overdue task appears`() {
        val yesterday = todayEpoch - 1
        val tasks = listOf(TaskEntity(id = 1, title = "Overdue", dueDate = yesterday.toLong()))
        val items = TodayViewsFactory.buildItemList(tasks, emptyList(), emptyList(), today)
        assertTrue(items.any { it is TodayViewsFactory.ListItem.TaskItem && it.task.title == "Overdue" })
    }

    @Test
    fun `future task excluded`() {
        val tomorrow = todayEpoch + 1
        val tasks = listOf(TaskEntity(id = 1, title = "Future", dueDate = tomorrow.toLong()))
        val items = TodayViewsFactory.buildItemList(tasks, emptyList(), emptyList(), today)
        assertTrue(items.none { it is TodayViewsFactory.ListItem.TaskItem })
    }

    @Test
    fun `streak counts consecutive done days`() {
        val today = LocalDate(2026, 7, 14)
        val habits = listOf(HabitEntity(id = 1, title = "Exercise", frequencyType = "DAILY"))
        val records = listOf(
            HabitRecordEntity(habitId = 1, date = LocalDate(2026, 7, 12).toEpochDays(), state = "DONE"),
            HabitRecordEntity(habitId = 1, date = LocalDate(2026, 7, 13).toEpochDays(), state = "DONE"),
        )
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, records, today)
        val habitItem = items.filterIsInstance<TodayViewsFactory.ListItem.HabitItem>().first()
        assertEquals(2, habitItem.streak)
    }

    @Test
    fun `streak counts from yesterday when today pending`() {
        val today = LocalDate(2026, 7, 13)
        val habits = listOf(HabitEntity(id = 1, title = "Exercise", frequencyType = "DAILY"))
        val records = listOf(
            HabitRecordEntity(habitId = 1, date = LocalDate(2026, 7, 12).toEpochDays(), state = "DONE"),
        )
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, records, today)
        val habitItem = items.filterIsInstance<TodayViewsFactory.ListItem.HabitItem>().first()
        assertEquals(1, habitItem.streak)
    }

    @Test
    fun `streak is zero when no records`() {
        val habits = listOf(HabitEntity(id = 1, title = "Exercise", frequencyType = "DAILY"))
        val items = TodayViewsFactory.buildItemList(emptyList(), habits, emptyList(), today)
        val habitItem = items.filterIsInstance<TodayViewsFactory.ListItem.HabitItem>().first()
        assertEquals(0, habitItem.streak)
    }
}
