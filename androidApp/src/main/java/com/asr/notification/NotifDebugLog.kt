package com.asr.notification

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotifDebugLog {
    private var context: Context? = null
    private var logFile: File? = null
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        this.context = context.applicationContext
        logFile = File(context.filesDir, "notif_debug.log")
    }

    private fun isEnabled(): Boolean = context
        ?.getSharedPreferences("asr_settings", Context.MODE_PRIVATE)
        ?.getBoolean("notif_debug", false) ?: false

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        if (!isEnabled()) return
        logFile?.let { f ->
            try {
                f.appendText("[${sdf.format(Date())}] $tag: $msg\n")
            } catch (_: Exception) {}
        }
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        if (!isEnabled()) return
        logFile?.let { f ->
            try {
                f.appendText("[${sdf.format(Date())}] W/$tag: $msg\n")
            } catch (_: Exception) {}
        }
    }
}
