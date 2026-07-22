package com.asr.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.asr.core.habit.HabitRepo
import com.asr.core.habit.shouldShowToday
import com.asr.core.interfaces.AlarmScheduler
import com.asr.core.now
import com.asr.widget.MidnightRefreshReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.context.GlobalContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver.onReceive action=${intent.action} extras=${intent.extras?.keySet()?.map { "$it=${intent.extras?.get(it)}" }}")

        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver: BOOT_COMPLETED, starting rescheduleAll")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { rescheduleAll(context) } finally { pendingResult.finish() }
            }
            return
        }

        val title = intent.getStringExtra("title") ?: return
        val body = intent.getStringExtra("body") ?: return
        val type = intent.getStringExtra("type")

        NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver: alarm fired type=$type title=$title now=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())}")

        if (type == "habit") {
            val entityId = intent.getLongExtra("entityId", 0L)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val koin = GlobalContext.getOrNull() ?: run {
                        NotifDebugLog.w("ASR_BUGTRACE", "BootReceiver habit: Koin not available, skipping")
                        return@launch
                    }
                    val habitRepo = koin.get<HabitRepo>()
                    val habit = habitRepo.getHabitById(entityId) ?: run {
                        NotifDebugLog.w("ASR_BUGTRACE", "BootReceiver habit: habitId=$entityId NOT FOUND, skipping")
                        return@launch
                    }
                    val today = LocalDate.now()
                    val todayRecord = habitRepo.getRecordForDate(entityId, today)
                    NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver habit: id=$entityId title=${habit.title} " +
                        "freq=${habit.frequencyType} daysOfMonth=${habit.daysOfMonth} daysOfWeek=${habit.daysOfWeek} " +
                        "today=$today recordState=${todayRecord?.state}")

                    if (todayRecord?.state == com.asr.core.habit.HabitState.DONE) {
                        NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver habit: id=$entityId already DONE today, skipping notification+reschedule")
                        return@launch
                    }

                    val shouldShow = habit.shouldShowToday(today)
                    NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver habit: id=$entityId shouldShowToday=$shouldShow")

                    if (shouldShow) {
                        showNotification(context, title, body)
                        val scheduler = koin.get<AlarmScheduler>()
                        scheduler.schedule(habit)
                        NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver habit: id=$entityId NOTIFICATION_SHOWN + rescheduled")
                    } else {
                        NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver habit: id=$entityId shouldShowToday=FALSE, skipping notification but rescheduling anyway")
                        val scheduler = koin.get<AlarmScheduler>()
                        scheduler.schedule(habit)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        showNotification(context, title, body)
        NotifDebugLog.d("ASR_BUGTRACE", "BootReceiver: non-habit notification shown: $title")
    }

    private suspend fun rescheduleAll(context: Context) {
        val startMs = System.currentTimeMillis()
        NotifDebugLog.d("ASR_BUGTRACE", "rescheduleAll START nowMs=$startMs now=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(startMs))}")
        val koin = GlobalContext.getOrNull() ?: return
        val scheduler = koin.get<AlarmScheduler>()
        val taskRepo = koin.get<com.asr.core.task.TaskRepo>()
        val habitRepo = koin.get<HabitRepo>()

        MidnightRefreshReceiver.scheduleNext(context)
        val tasks = taskRepo.getTasksFlow().first().filter { it.reminderTime != null }
        val habits = habitRepo.getHabitsFlow().first().filter { it.reminderTime != null }
        NotifDebugLog.d("ASR_BUGTRACE", "rescheduleAll: rescheduling ${tasks.size} tasks, ${habits.size} habits")
        tasks.forEach { scheduler.schedule(it) }
        habits.forEach {
            NotifDebugLog.d("ASR_BUGTRACE", "rescheduleAll habit: id=${it.id} title=${it.title} freq=${it.frequencyType} daysOfMonth=${it.daysOfMonth} reminder=${it.reminderTime}")
            scheduler.schedule(it)
        }
        NotifDebugLog.d("ASR_BUGTRACE", "rescheduleAll DONE elapsedMs=${System.currentTimeMillis() - startMs}")
    }

    private fun showNotification(context: Context, title: String, body: String) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("ASR_Reminder", "POST_NOTIFICATIONS not granted, suppressing notification for: $title")
            return
        }

        val notification = NotificationCompat.Builder(context, AlarmSchedulerImpl.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
