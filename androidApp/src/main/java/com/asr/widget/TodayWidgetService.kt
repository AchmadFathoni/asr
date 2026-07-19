package com.asr.widget

import android.content.Intent
import android.widget.RemoteViewsService

class TodayWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 0
        )
        return TodayViewsFactory(applicationContext, appWidgetId)
    }
}
