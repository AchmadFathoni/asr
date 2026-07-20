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
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { rescheduleAll(context) } finally { pendingResult.finish() }
            }
            return
        }

        val title = intent.getStringExtra("title") ?: return
        val body = intent.getStringExtra("body") ?: return
        val type = intent.getStringExtra("type")

        if (type == "habit") {
            val entityId = intent.getLongExtra("entityId", 0L)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val koin = GlobalContext.getOrNull() ?: return@launch
                    val habitRepo = koin.get<HabitRepo>()
                    val habit = habitRepo.getHabitById(entityId) ?: return@launch
                    if (habit.shouldShowToday(LocalDate.now())) {
                        showNotification(context, title, body)
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
    }

    private suspend fun rescheduleAll(context: Context) {
        val koin = GlobalContext.getOrNull() ?: return
        val scheduler = koin.get<AlarmScheduler>()
        val taskRepo = koin.get<com.asr.core.task.TaskRepo>()
        val habitRepo = koin.get<HabitRepo>()

        MidnightRefreshReceiver.scheduleNext(context)
        taskRepo.getTasksFlow().first().filter { it.reminderTime != null }.forEach { scheduler.schedule(it) }
        habitRepo.getHabitsFlow().first().filter { it.reminderTime != null }.forEach { scheduler.schedule(it) }
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
