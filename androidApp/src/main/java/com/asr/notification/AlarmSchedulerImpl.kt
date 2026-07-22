package com.asr.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        NotifDebugLog.init(context)
    }

    override fun schedule(habit: Habit) {
        habit.reminderTime?.let { timeStr ->
            val time = try { LocalTime.parse(timeStr) } catch (_: Exception) {
                NotifDebugLog.w("ASR_BUGTRACE", "schedule(habit) FAIL: bad time parse timeStr=$timeStr habitId=${habit.id} title=${habit.title}")
                return
            }
            val id = habit.id.toInt() + 10000
            scheduledIds.add(id)

            val today = LocalDate.now()
            val targetDate = habit.nextOccurrenceFrom(today)
            val nowMs = System.currentTimeMillis()
            val triggerTime = buildCalendar(targetDate, time).timeInMillis
            val deltaMs = triggerTime - nowMs

            val finalTrigger: Long
            val decisionTag: String
            if (triggerTime <= nowMs) {
                // ponytail: skip to next occurrence, never loop on same day
                decisionTag = "SKIP_TO_NEXT"
                val next = habit.nextOccurrenceFrom(LocalDate.fromEpochDays(today.toEpochDays() + 1))
                finalTrigger = buildCalendar(next, time).timeInMillis
            } else {
                decisionTag = "FUTURE"
                finalTrigger = triggerTime
            }

            NotifDebugLog.d("ASR_BUGTRACE", "schedule(habit) id=$id title=${habit.title} " +
                "freq=${habit.frequencyType} daysOfMonth=${habit.daysOfMonth} daysOfWeek=${habit.daysOfWeek} " +
                "reminder=$timeStr today=$today targetDate=$targetDate " +
                "triggerTime=$triggerTime nowMs=$nowMs deltaMs=$deltaMs " +
                "decision=$decisionTag finalTrigger=$finalTrigger " +
                "finalDate=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(finalTrigger))}")

            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val intent = Intent(context, BootReceiver::class.java).apply {
                putExtra("title", habit.title)
                putExtra("body", "Time to do: ${habit.title}")
                putExtra("type", "habit")
                putExtra("entityId", habit.id)
            }
            val pending = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val info = AlarmManager.AlarmClockInfo(finalTrigger, null)
            alarmManager.setAlarmClock(info, pending)
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
            set(java.util.Calendar.MILLISECOND, 0)
            NotifDebugLog.d("ASR_BUGTRACE", "buildCalendar date=$date time=$time resultMs=$timeInMillis zone=${timeZone.id} hasDST=${timeZone.observesDaylightTime()}")
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
        NotifDebugLog.d("ASR_BUGTRACE", "cancel(habit) id=$id title=${habit.title} freq=${habit.frequencyType} daysOfMonth=${habit.daysOfMonth}")
        cancelAlarm(id)
    }

    override fun cancel(task: Task) {
        val id = task.id.toInt()
        scheduledIds.remove(id)
        NotifDebugLog.d("ASR_BUGTRACE", "cancel(task) id=$id title=${task.title}")
        cancelAlarm(id)
    }

    override fun cancelAll() {
        val ids = scheduledIds.toSet()
        NotifDebugLog.d("ASR_BUGTRACE", "cancelAll ids=$ids")
        ids.forEach { cancelAlarm(it) }
        scheduledIds.clear()
    }

    private fun scheduleAlarm(id: Int, title: String, body: String, timeStr: String, type: String? = null, entityId: Long = 0) {
        val time = try { LocalTime.parse(timeStr) } catch (_: Exception) {
            NotifDebugLog.w("ASR_BUGTRACE", "scheduleAlarm FAIL: bad time parse timeStr=$timeStr id=$id")
            return
        }
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

        val nowMs = System.currentTimeMillis()
        val triggerTime = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, time.hour)
            set(java.util.Calendar.MINUTE, time.minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMs) {
                setTimeInMillis(nowMs + 30_000)
                NotifDebugLog.d("ASR_BUGTRACE", "scheduleAlarm(id=$id title=$title) TRIGGER_PASSED: originalTime=$timeStr nowMs=$nowMs → bumped +30s")
            }
        }.timeInMillis

        NotifDebugLog.d("ASR_BUGTRACE", "scheduleAlarm(id=$id title=$title) timeStr=$timeStr " +
            "triggerTime=$triggerTime nowMs=$nowMs deltaMs=${triggerTime - nowMs} " +
            "triggerDate=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(triggerTime))}")

        val info = AlarmManager.AlarmClockInfo(triggerTime, null)
        alarmManager.setAlarmClock(info, pending)
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
