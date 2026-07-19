package com.asr.widget

import android.content.Context
import android.content.res.Configuration

fun isWidgetDark(context: Context): Boolean {
    val prefs = context.getSharedPreferences("asr_settings", Context.MODE_PRIVATE)
    val theme = prefs.getString("theme", "SYSTEM") ?: "SYSTEM"
    return when (theme) {
        "DARK" -> true
        "LIGHT" -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
