package com.asr.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asr.core.habit.Habit
import com.asr.core.habit.nextOccurrenceFrom
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.now
import com.asr.core.task.Task
import kotlinx.datetime.LocalDate
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
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Reminders for tasks and habits" }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun schedule(habit: Habit) {
        habit.reminderTime?.let { timeStr ->
            val time = try { LocalTime.parse(timeStr) } catch (_: Exception) { return }
            val id = habit.id.toInt() + 10000
            scheduledIds.add(id)

            val today = LocalDate.now()
            val targetDate = habit.nextOccurrenceFrom(today)

            val triggerTime = buildCalendar(targetDate, time).timeInMillis
            val finalTrigger = if (triggerTime <= System.currentTimeMillis()) {
                if (targetDate == today) {
                    System.currentTimeMillis() + 30_000
                } else {
                    val next = habit.nextOccurrenceFrom(LocalDate.fromEpochDays(today.toEpochDays() + 1))
                    buildCalendar(next, time).timeInMillis
                }
            } else triggerTime

            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, BootReceiver::class.java).apply {
                putExtra("title", habit.title)
                putExtra("body", "Time to do: ${habit.title}")
                putExtra("type", "habit")
                putExtra("entityId", habit.id)
            }
            val pending = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            Log.d("ASR_Reminder", "habit alarm: id=$id title=${habit.title} targetDate=$targetDate triggerTime=$finalTrigger")
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTrigger, pending)
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, finalTrigger, pending)
            }
        }
    }

    private fun buildCalendar(date: LocalDate, time: LocalTime): java.util.Calendar =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, date.year)
            set(java.util.Calendar.MONTH, date.month.ordinal)
            set(java.util.Calendar.DAY_OF_MONTH, date.day)
            set(java.util.Calendar.HOUR_OF_DAY, time.hour)
            set(java.util.Calendar.MINUTE, time.minute)
            set(java.util.Calendar.SECOND, 0)
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

    private fun scheduleAlarm(id: Int, title: String, body: String, timeStr: String, type: String? = null, entityId: Long = 0) {
        val time = try { LocalTime.parse(timeStr) } catch (_: Exception) { return }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, BootReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
            if (type != null) {
                putExtra("type", type)
                putExtra("entityId", entityId)
            }
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerTime = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, time.hour)
            set(java.util.Calendar.MINUTE, time.minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                setTimeInMillis(System.currentTimeMillis() + 30_000)
            }
        }.timeInMillis

        Log.d("ASR_Reminder", "scheduleAlarm: id=$id title=$title timeStr=$timeStr triggerTime=$triggerTime")
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        } catch (_: SecurityException) {
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
