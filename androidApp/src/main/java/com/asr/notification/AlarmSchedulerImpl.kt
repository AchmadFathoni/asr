package com.asr.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.asr.core.habit.Habit
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.task.Task
import org.koin.core.annotation.Single
import java.time.LocalTime

@Single(binds = [AlarmScheduler::class])
class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {
    companion object {
        const val CHANNEL_ID = "asr_reminders"
        const val CHANNEL_NAME = "Task & Habit Reminders"
        private val scheduledIds = mutableSetOf<Int>()
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminders for tasks and habits" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun schedule(habit: Habit) {
        habit.reminderTime?.let { timeStr ->
            val id = habit.id.toInt() + 10000
            scheduledIds.add(id)
            scheduleAlarm(
                id = id,
                title = habit.title,
                body = "Time to do: ${habit.title}",
                timeStr = timeStr,
                repeating = true,
            )
        }
    }

    override fun schedule(task: Task) {
        task.reminderTime?.let { timeStr ->
            val id = task.id.toInt()
            scheduledIds.add(id)
            scheduleAlarm(
                id = id,
                title = task.title,
                body = task.description.ifBlank { "Reminder: ${task.title}" },
                timeStr = timeStr,
                repeating = false,
            )
        }
    }

    override fun cancel(habit: Habit) {
        val id = habit.id.toInt() + 10000
        scheduledIds.remove(id)
        cancelAlarm(id)
    }

    override fun cancel(task: Task) {
        val id = task.id.toInt()
        scheduledIds.remove(id)
        cancelAlarm(id)
    }

    override fun cancelAll() {
        val ids = scheduledIds.toSet()
        ids.forEach { cancelAlarm(it) }
        scheduledIds.clear()
    }

    private fun scheduleAlarm(id: Int, title: String, body: String, timeStr: String, repeating: Boolean) {
        val time = try { LocalTime.parse(timeStr) } catch (_: Exception) { return }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, BootReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val triggerTime = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, time.hour)
            set(java.util.Calendar.MINUTE, time.minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        Log.d("ASR_Reminder", "scheduleAlarm: id=$id title=$title timeStr=$timeStr repeating=$repeating triggerTime=$triggerTime")

        if (repeating) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, triggerTime,
                AlarmManager.INTERVAL_DAY, pending,
            )
            Log.d("ASR_Reminder", "setRepeating scheduled for $title at $triggerTime")
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pending)
            Log.d("ASR_Reminder", "set scheduled for $title at $triggerTime")
        }
    }

    private fun cancelAlarm(id: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, BootReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.cancel(pending)
    }
}
