package com.asr.widget

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.asr.core.now
import com.asr.data.database.HabitRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class WidgetActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent.action
        val appWidgetId = intent.getIntExtra("appWidgetId", -1)
        Log.d("Widget", "Activity action=$action widgetId=$appWidgetId")

        when (action) {
            TodayWidgetProvider.ACTION_TOGGLE_TASK -> {
                val taskId = intent.getLongExtra("task_id", 0)
                if (taskId > 0) toggleTask(taskId, appWidgetId)
            }
            TodayWidgetProvider.ACTION_INCREMENT_HABIT -> {
                val habitId = intent.getLongExtra("habit_id", 0)
                if (habitId > 0) incrementHabit(habitId, appWidgetId)
            }
        }
    }

    private fun toggleTask(taskId: Long, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getDatabase(applicationContext)
                val task = db.taskDao().getTaskById(taskId) ?: return@launch
                db.taskDao().upsertTask(task.copy(isDone = !task.isDone))
                Log.d("Widget", "ToggleTask id=$taskId done=${!task.isDone}")
                if (widgetId > 0) TodayWidgetProvider.refreshWidget(applicationContext, widgetId)
            } finally {
                finishAndRemoveTask()
            }
        }
    }

    private fun incrementHabit(habitId: Long, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getDatabase(applicationContext)
                val today = LocalDate.now()
                val todayEpoch = today.toEpochDays()
                val habitEntity = db.habitDao().getHabitById(habitId) ?: return@launch
                val recordEntity = db.habitDao().getRecordForDate(habitId, todayEpoch)
                val periodStartEpoch = when (habitEntity.frequencyType) {
                    "WEEKLY" -> todayEpoch - (today.dayOfWeek.ordinal)
                    "MONTHLY" -> LocalDate(today.year, today.month, 1).toEpochDays()
                    "YEARLY" -> LocalDate(today.year, 1, 1).toEpochDays()
                    else -> todayEpoch
                }
                val periodTotal = db.habitDao().getPeriodTotalCount(habitId, periodStartEpoch, todayEpoch) ?: 0
                val newCount = (recordEntity?.count ?: 0) + 1
                val newPeriodTotal = periodTotal + 1
                val newState = if (newPeriodTotal >= habitEntity.frequencyCount) "DONE" else "NOT_DONE"
                db.habitDao().upsertRecord(
                    HabitRecordEntity(
                        id = recordEntity?.id ?: 0,
                        habitId = habitId,
                        date = todayEpoch,
                        state = newState,
                        count = newCount,
                    )
                )
                Log.d("Widget", "IncrementHabit id=$habitId count=$newCount periodTotal=$newPeriodTotal")
                if (widgetId > 0) TodayWidgetProvider.refreshWidget(applicationContext, widgetId)
            } finally {
                finishAndRemoveTask()
            }
        }
    }
}
