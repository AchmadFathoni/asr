package com.asr.widget

import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import com.asr.R
import com.asr.core.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class WidgetActionActivity : Activity() {
    companion object {
        private var sfx: MediaPlayer? = null
    }

    private fun playDone() {
        try {
            val p = sfx ?: MediaPlayer.create(this, R.raw.done).also { sfx = it }
            p.seekTo(0)
            p.start()
        } catch (_: Exception) { }
    }

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
                val nowDone = !task.isDone
                db.taskDao().upsertTask(task.copy(isDone = nowDone))
                Log.d("Widget", "ToggleTask id=$taskId done=$nowDone")
                if (nowDone) playDone()
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
                if (newState == "DONE") playDone()
                if (widgetId > 0) TodayWidgetProvider.refreshWidget(applicationContext, widgetId)
            } finally {
                finishAndRemoveTask()
            }
        }
    }
}
