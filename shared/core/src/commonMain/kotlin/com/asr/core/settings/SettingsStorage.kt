package com.asr.core.settings

interface SettingsStorage {
    fun getTheme(): ThemeOption
    fun setTheme(theme: ThemeOption)
}
