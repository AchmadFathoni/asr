package com.asr.widget

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.asr.core.now
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
                if (habitId > 0) toggleHabit(habitId, appWidgetId)
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

    private fun toggleHabit(habitId: Long, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getDatabase(applicationContext)
                val today = LocalDate.now()
                val todayEpoch = today.toEpochDays()
                val recordEntity = db.habitDao().getRecordForDate(habitId, todayEpoch)
                val isDone = recordEntity?.state == "DONE"
                val newState = if (isDone) "NOT_DONE" else "DONE"
                val newCount = if (newState == "DONE") 1 else 0
                db.habitDao().upsertRecordForDate(habitId, todayEpoch, newState, newCount)
                Log.d("Widget", "ToggleHabit id=$habitId state=$newState")
                if (widgetId > 0) TodayWidgetProvider.refreshWidget(applicationContext, widgetId)
            } finally {
                finishAndRemoveTask()
            }
        }
    }
}
