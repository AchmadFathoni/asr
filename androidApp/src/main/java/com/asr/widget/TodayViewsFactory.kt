package com.asr.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color as GColor
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.asr.R
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.habit.shouldShowToday
import com.asr.core.now
import com.asr.core.sortedByPinAndDate
import com.asr.core.sortedByPinAndTime
import com.asr.core.task.Task
import com.asr.data.database.Converters
import com.asr.data.database.HabitEntity
import com.asr.data.database.TaskEntity
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class TodayViewsFactory(
    private val context: android.content.Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<ListItem> = emptyList()
    private var isDark: Boolean = false

    override fun onCreate() {}

    override fun onDataSetChanged() {
        try {
            isDark = isWidgetDark(context)
            items = buildItems(context)
        } catch (e: Exception) {
            Log.e("Widget", "onDataSetChanged failed", e)
            items = listOf(ListItem.Label("Error loading data", LabelStyle.MESSAGE))
        }
    }

    override fun getCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return when (val item = items[position]) {
            is ListItem.Label -> item.text.hashCode().toLong()
            is ListItem.TaskItem -> item.task.id + 1000
            is ListItem.HabitItem -> item.habit.id + 10000
        }
    }

    override fun getViewTypeCount(): Int = 3

    override fun hasStableIds(): Boolean = true

    override fun getViewAt(position: Int): RemoteViews {
        return when (val item = items[position]) {
            is ListItem.Label -> buildLabelView(item)
            is ListItem.TaskItem -> buildTaskView(item)
            is ListItem.HabitItem -> buildHabitView(item)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDestroy() {}

    // ── view builders ──────────────────────────────────────────────────────

    private fun buildLabelView(item: ListItem.Label): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_label_row)
        views.setTextViewText(R.id.label_text, item.text)
        views.setTextColor(R.id.label_text, when (item.style) {
            LabelStyle.HEADER -> textPrimary()
            LabelStyle.SECTION_TASKS -> 0xFF4CAF50.toInt()
            LabelStyle.SECTION_HABITS -> 0xFF2196F3.toInt()
            LabelStyle.MESSAGE -> textDim()
            else -> textPrimary()
        })
        views.setFloat(R.id.label_text, "setTextSize", when (item.style) {
            LabelStyle.HEADER -> 16f
            LabelStyle.MESSAGE -> 14f
            else -> 12f
        })
        return views
    }

    private fun buildTaskView(item: ListItem.TaskItem): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_row)
        views.setTextViewText(R.id.task_title, item.task.title)
        views.setTextColor(R.id.task_title, if (item.isParent) textDim() else textPrimary())

        val checkText = if (item.task.isDone) "\u2611" else "\u2610"
        views.setTextViewText(R.id.task_check, checkText)
        views.setTextColor(R.id.task_check, 0xFF4CAF50.toInt())

        if (!item.isParent && !item.task.isDone) {
            val fillInIntent = Intent().apply {
                action = TodayWidgetProvider.ACTION_TOGGLE_TASK
                putExtra("task_id", item.task.id)
                putExtra("appWidgetId", appWidgetId)
            }
            views.setOnClickFillInIntent(R.id.task_row, fillInIntent)
        }

        return views
    }

    private fun buildHabitView(item: ListItem.HabitItem): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_habit_row)
        views.setTextViewText(R.id.habit_title, item.habit.title)
        views.setTextColor(R.id.habit_title, textPrimary())
        views.setTextViewText(
            R.id.habit_count,
            "${item.record?.count ?: 0} / ${item.habit.frequencyCount}"
        )
        views.setTextColor(R.id.habit_count, textDim())

        val fillInIntent = Intent().apply {
            action = TodayWidgetProvider.ACTION_INCREMENT_HABIT
            putExtra("habit_id", item.habit.id)
            putExtra("appWidgetId", appWidgetId)
        }
        views.setOnClickFillInIntent(R.id.habit_row, fillInIntent)

        return views
    }

    // ── theme helpers ──────────────────────────────────────────────────────

    private fun textPrimary(): Int = if (isDark) GColor.WHITE else GColor.BLACK
    private fun textDim(): Int = if (isDark) 0xFFB0B0B0.toInt() else 0xFF666666.toInt()

    // ── sealed item model ──────────────────────────────────────────────────

    sealed class ListItem {
        data class Label(val text: String, val style: LabelStyle) : ListItem()
        data class TaskItem(val task: Task, val isParent: Boolean) : ListItem()
        data class HabitItem(val habit: Habit, val record: HabitRecord?) : ListItem()
    }

    enum class LabelStyle { HEADER, SECTION_TASKS, SECTION_HABITS, MESSAGE }

    // ── data loading ───────────────────────────────────────────────────────

    companion object {
        fun buildItems(context: android.content.Context): List<ListItem> {
            val db = getDatabase(context)
            val today = LocalDate.now()
            val todayEpoch = today.toEpochDays()

            lateinit var allTaskEntities: List<TaskEntity>
            lateinit var allHabitEntities: List<HabitEntity>
            lateinit var allRecordEntities: List<com.asr.data.database.HabitRecordEntity>

            runBlocking {
                allTaskEntities = db.taskDao().getAllTasks()
                allHabitEntities = db.habitDao().getAllHabits()
                allRecordEntities = db.habitDao().getAllRecords()
            }

            val parentIds = allTaskEntities.mapNotNull { it.parentId }.toSet()

            val tasks = allTaskEntities
                .filter { !it.isDone && (it.dueDate == null || it.dueDate <= todayEpoch) }
                .map { it.toTask() }
                .sortedByPinAndDate()

            val allHabits = allHabitEntities.map { it.toHabit() }
            val todayRecords = allRecordEntities
                .filter { it.date == todayEpoch }
                .map { it.toHabitRecord() }
                .associateBy { it.habitId }

            val habits = allHabits
                .filter { it.shouldShowToday(today) && (todayRecords[it.id]?.state != HabitState.DONE) }
                .sortedByPinAndTime()

            val allDone = tasks.isEmpty() && habits.isEmpty() &&
                (allTaskEntities.any { it.isDone } || allHabitEntities.isNotEmpty())

            val result = mutableListOf<ListItem>()
            result.add(ListItem.Label("Today", LabelStyle.HEADER))

            if (allDone) {
                result.add(ListItem.Label("All done for today!", LabelStyle.MESSAGE))
                return result
            }

            if (tasks.isEmpty() && habits.isEmpty()) {
                result.add(
                    ListItem.Label(
                        "Nothing for today.\nAdd tasks or habits in the app.",
                        LabelStyle.MESSAGE
                    )
                )
                return result
            }

            if (tasks.isNotEmpty()) {
                result.add(ListItem.Label("TASKS", LabelStyle.SECTION_TASKS))
                tasks.forEach { task ->
                    result.add(ListItem.TaskItem(task, task.id in parentIds))
                }
            }

            if (habits.isNotEmpty()) {
                result.add(ListItem.Label("HABITS", LabelStyle.SECTION_HABITS))
                habits.forEach { habit ->
                    result.add(ListItem.HabitItem(habit, todayRecords[habit.id]))
                }
            }

            return result
        }
    }
}

private fun TaskEntity.toTask() = Task(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    dueDate = dueDate?.let { Converters.dateFromTimestamp(it) },
    parentId = parentId,
    isPinned = isPinned,
    reminderTime = reminderTime,
)

private fun HabitEntity.toHabit() = Habit(
    id = id,
    title = title,
    description = description,
    frequencyType = HabitFrequency.valueOf(frequencyType),
    frequencyCount = frequencyCount,
    daysOfWeek = if (daysOfWeek.isNotBlank()) daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    else emptySet(),
    daysOfMonth = if (daysOfMonth.isNotBlank()) daysOfMonth.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    else emptySet(),
    yearlyDates = if (yearlyDates.isNotBlank()) yearlyDates.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    else emptySet(),
    isPinned = isPinned,
    reminderTime = reminderTime,
)

private fun com.asr.data.database.HabitRecordEntity.toHabitRecord() = HabitRecord(
    id = id,
    habitId = habitId,
    date = Converters.dateFromTimestamp(date),
    state = HabitState.valueOf(state),
    count = count,
)
