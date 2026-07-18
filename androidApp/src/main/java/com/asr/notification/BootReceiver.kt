package com.asr.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.asr.core.interfaces.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0
        val notification = NotificationCompat.Builder(context, AlarmSchedulerImpl.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    flags,
                )
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private suspend fun rescheduleAll(context: Context) {
        val koin = GlobalContext.getOrNull() ?: return
        val scheduler = koin.get<AlarmScheduler>()
        val taskRepo = koin.get<com.asr.core.task.TaskRepo>()
        val habitRepo = koin.get<com.asr.core.habit.HabitRepo>()

        taskRepo.getTasksFlow().first().filter { it.reminderTime != null }.forEach { scheduler.schedule(it) }
        habitRepo.getHabitsFlow().first().filter { it.reminderTime != null }.forEach { scheduler.schedule(it) }
    }
}
