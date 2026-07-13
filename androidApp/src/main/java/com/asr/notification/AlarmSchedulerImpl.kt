package com.asr.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
            scheduleAlarm(
                id = habit.id.toInt() + 10000,
                title = habit.title,
                body = "Time to do: ${habit.title}",
                timeStr = timeStr,
                repeating = true,
            )
        }
    }

    override fun schedule(task: Task) {
        task.reminderTime?.let { timeStr ->
            scheduleAlarm(
                id = task.id.toInt(),
                title = task.title,
                body = task.description.ifBlank { "Reminder: ${task.title}" },
                timeStr = timeStr,
                repeating = false,
            )
        }
    }

    override fun cancel(habit: Habit) {
        cancelAlarm(habit.id.toInt() + 10000)
    }

    override fun cancel(task: Task) {
        cancelAlarm(task.id.toInt())
    }

    override fun cancelAll() {
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, BootReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(pending)
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerTime = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, time.hour)
            set(java.util.Calendar.MINUTE, time.minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        if (repeating) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, triggerTime,
                AlarmManager.INTERVAL_DAY, pending,
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        }
    }

    private fun cancelAlarm(id: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, BootReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }
}
