package com.asr.data.repository

import android.content.Context
import com.asr.core.settings.SettingsRepo
import com.asr.core.settings.ThemeOption
import org.koin.core.annotation.Single

@Single(binds = [SettingsRepo::class])
class SettingsRepository(context: Context) : SettingsRepo {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getTheme(): ThemeOption {
        val raw = prefs.getString(KEY_THEME, null) ?: return ThemeOption.SYSTEM
        return try { ThemeOption.valueOf(raw) } catch (_: IllegalArgumentException) { ThemeOption.SYSTEM }
    }

    override fun setTheme(theme: ThemeOption) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "asr_settings"
        private const val KEY_THEME = "theme"
    }
}
