package com.asr.core.settings

interface SettingsRepo {
    fun isDarkMode(): Boolean
    fun setDarkMode(isDark: Boolean)
}
