package com.asr.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.asr.R

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        MidnightRefreshReceiver.scheduleNext(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetId)
        }
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "com.asr.widget.TOGGLE_TASK"
        const val ACTION_INCREMENT_HABIT = "com.asr.widget.INCREMENT_HABIT"

        fun updateWidget(context: Context, appWidgetId: Int) {
            val views = buildRemoteViews(context, appWidgetId)
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
            // notifyAppWidgetViewDataChanged(int[], int) is deprecated but its
            // replacement (int[], RemoteViews) requires building a RemoteViews
            // just to extract the view ID — no functional difference.
            @Suppress("DEPRECATION")
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                intArrayOf(appWidgetId), R.id.widget_list
            )
        }

        fun refreshWidget(context: Context, appWidgetId: Int) {
            // Same deprecation situation as updateWidget — replacement
            // requires building RemoteViews just for the view ID lookup.
            @Suppress("DEPRECATION")
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                intArrayOf(appWidgetId), R.id.widget_list
            )
        }
    }
}

private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_today_layout)

    val isDark = isWidgetDark(context)
    val bgColor = if (isDark) 0x80000000.toInt() else 0x80FFFFFF.toInt()
    views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

    val adapterIntent = Intent(context, TodayWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
    }
    // Android 15 deprecates setRemoteAdapter(Intent) in favor of
    // RemoteCollectionItems, which only supports static snapshots.
    // This widget requires a dynamic Room-backed collection that
    // refreshes via notifyAppWidgetViewDataChanged() — no
    // feature-equivalent replacement exists yet.
    @Suppress("DEPRECATION")
    views.setRemoteAdapter(R.id.widget_list, adapterIntent)

    val templateIntent = Intent(context, WidgetActionActivity::class.java)
    val templatePendingIntent = PendingIntent.getActivity(
        context, 0, templateIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    views.setPendingIntentTemplate(R.id.widget_list, templatePendingIntent)

    return views
}
