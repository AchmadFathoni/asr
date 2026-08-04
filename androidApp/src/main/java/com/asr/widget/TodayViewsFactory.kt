package com.asr.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color as GColor
import android.view.Gravity
import android.view.View
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.asr.R
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.computeStreak
import com.asr.core.habit.periodStart
import com.asr.core.habit.periodTarget
import com.asr.core.habit.shouldShowToday
import com.asr.core.now
import com.asr.core.TodayItems
import com.asr.core.sortedByPinAndDate
import com.asr.core.sortedByPinAndTime
import com.asr.core.task.Task
import com.asr.data.database.Converters
import com.asr.data.database.HabitEntity
import com.asr.data.database.TaskEntity
import com.asr.data.storage.toDomain
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
        })
        views.setFloat(R.id.label_text, "setTextSize", when (item.style) {
            LabelStyle.HEADER -> 24f
            LabelStyle.SECTION_TASKS, LabelStyle.SECTION_HABITS -> 18f
            LabelStyle.MESSAGE -> 14f
        })
        if (item.style == LabelStyle.HEADER || item.style == LabelStyle.SECTION_TASKS || item.style == LabelStyle.SECTION_HABITS) {
            views.setInt(R.id.label_text, "setGravity", Gravity.CENTER)
        }
        return views
    }

    private fun buildTaskView(item: ListItem.TaskItem): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_row)
        views.setTextViewText(R.id.task_title, item.task.title)
        views.setTextColor(R.id.task_title, if (item.isParent) textDim() else textPrimary())
        if (item.indentDp > 0) {
            val density = context.resources.displayMetrics.density
            val startPx = ((12 + item.indentDp) * density).toInt()
            val verticalPx = (6 * density).toInt()
            val endPx = (12 * density).toInt()
            views.setViewPadding(R.id.task_row, startPx, verticalPx, endPx, verticalPx)
        }
        views.setOnClickFillInIntent(R.id.task_row, taskFillInIntent(item.task.id, appWidgetId))
        return views
    }

    private fun buildHabitView(item: ListItem.HabitItem): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_habit_row)
        views.setTextViewText(R.id.habit_title, item.habit.title)
        views.setTextColor(R.id.habit_title, textPrimary())
        views.setTextViewText(
            R.id.habit_count,
            "${item.periodCount} / ${item.periodTarget.coerceAtLeast(1)}"
        )
        views.setTextColor(R.id.habit_count, textDim())
        if (item.streak > 0) {
            views.setViewVisibility(R.id.habit_streak_row, View.VISIBLE)
            views.setTextViewText(R.id.habit_streak, item.streak.toString())
            views.setTextColor(R.id.habit_streak, 0xFF1E88E5.toInt())
        } else {
            views.setViewVisibility(R.id.habit_streak_row, View.GONE)
        }
        views.setOnClickFillInIntent(R.id.habit_row, habitFillInIntent(item.habit.id, appWidgetId))
        return views
    }

    // ── theme helpers ──────────────────────────────────────────────────────

    private fun textPrimary(): Int = if (isDark) GColor.WHITE else GColor.BLACK
    private fun textDim(): Int = if (isDark) 0xFFB0B0B0.toInt() else 0xFF666666.toInt()

    // ── sealed item model ──────────────────────────────────────────────────

    sealed class ListItem {
        data class Label(val text: String, val style: LabelStyle) : ListItem()
        data class TaskItem(val task: Task, val isParent: Boolean, val indentDp: Int = 0) : ListItem()
        data class HabitItem(val habit: Habit, val record: HabitRecord?, val periodCount: Int = 0, val periodTarget: Int = 0, val streak: Int = 0) : ListItem()
    }

    enum class LabelStyle { HEADER, SECTION_TASKS, SECTION_HABITS, MESSAGE }

    // ── data loading ───────────────────────────────────────────────────────

    companion object {
        fun buildItems(context: android.content.Context): List<ListItem> {
            val db = getDatabase(context)
            val today = LocalDate.now()

            lateinit var allTaskEntities: List<TaskEntity>
            lateinit var allHabitEntities: List<HabitEntity>
            lateinit var allRecordEntities: List<com.asr.data.database.HabitRecordEntity>

            runBlocking {
                allTaskEntities = db.taskDao().getAllTasks()
                allHabitEntities = db.habitDao().getAllHabits()
                allRecordEntities = db.habitDao().getAllRecords()
            }

            return buildItemList(allTaskEntities, allHabitEntities, allRecordEntities, today)
        }

        fun buildItemList(
            allTaskEntities: List<TaskEntity>,
            allHabitEntities: List<HabitEntity>,
            allRecordEntities: List<com.asr.data.database.HabitRecordEntity>,
            today: LocalDate,
        ): List<ListItem> {
            val todayEpoch = today.toEpochDays()
            val parentIds = allTaskEntities.mapNotNull { it.parentId }.toSet()

            val allHabits = allHabitEntities.map { it.toDomain() }
            val allRecords = allRecordEntities.map { it.toDomain() }
            val todayRecordList = allRecords.filter { it.date == today }
            val todayRecords = todayRecordList.associateBy { it.habitId }

            val tasks = TodayItems.tasks(
                allTaskEntities.map { it.toDomain() }, today
            ).sortedByPinAndDate()

            val habits = TodayItems.habits(
                allHabits, today, allRecords, todayRecordList
            ).sortedByPinAndTime()

            val dueTasks = allTaskEntities.map { it.toDomain() }.filter { val d = it.dueDate; d == null || d <= today }
            val scheduledHabits = allHabits.filter { it.shouldShowToday(today) }
            val allDone = tasks.isEmpty() && habits.isEmpty() &&
                (dueTasks.isNotEmpty() || scheduledHabits.isNotEmpty())

            val result = mutableListOf<ListItem>()
            result.add(ListItem.Label("Today To-do List", LabelStyle.HEADER))

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
                result.add(ListItem.Label("Tasks", LabelStyle.SECTION_TASKS))
                tasks.forEach { task ->
                    result.add(ListItem.TaskItem(task, task.id in parentIds, if (task.parentId != null) 24 else 0))
                }
            }

            if (habits.isNotEmpty()) {
                result.add(ListItem.Label("Habits", LabelStyle.SECTION_HABITS))
                habits.forEach { habit ->
                    val pStart = habit.periodStart(today)
                    val pStartEpoch = pStart.toEpochDays()
                    val periodCount = allRecordEntities
                        .filter { it.habitId == habit.id && it.date >= pStartEpoch && it.date <= todayEpoch && habit.shouldShowToday(Converters.dateFromTimestamp(it.date)) }
                        .sumOf { it.count }
                    val streak = habit.computeStreak(allRecords, today, requireToday = false)
                    result.add(ListItem.HabitItem(habit, todayRecords[habit.id], periodCount, habit.periodTarget(today), streak))
                }
            }

            return result
        }
    }
}

fun taskFillInIntent(taskId: Long, appWidgetId: Int) = Intent().apply {
    action = TodayWidgetProvider.ACTION_TOGGLE_TASK
    putExtra("task_id", taskId)
    putExtra("appWidgetId", appWidgetId)
}

fun habitFillInIntent(habitId: Long, appWidgetId: Int) = Intent().apply {
    action = TodayWidgetProvider.ACTION_INCREMENT_HABIT
    putExtra("habit_id", habitId)
    putExtra("appWidgetId", appWidgetId)
}
