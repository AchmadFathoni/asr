package com.asr.data.repository

import android.content.Context
import com.asr.core.settings.SettingsRepo
import org.koin.core.annotation.Single

@Single(binds = [SettingsRepo::class])
class SettingsRepository(context: Context) : SettingsRepo {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    override fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    companion object {
        private const val PREFS_NAME = "asr_settings"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
