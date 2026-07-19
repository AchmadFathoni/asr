package com.asr.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.asr.core.interfaces.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single(binds = [WidgetUpdater::class])
class AndroidWidgetUpdater(private val context: Context) : WidgetUpdater {
    override fun notifyDataChanged() {
        CoroutineScope(Dispatchers.IO).launch {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val provider = ComponentName(appContext, TodayWidgetProvider::class.java)
            val appWidgetIds = manager.getAppWidgetIds(provider)
            for (appWidgetId in appWidgetIds) {
                TodayWidgetProvider.refreshWidget(appContext, appWidgetId)
            }
        }
    }
}
