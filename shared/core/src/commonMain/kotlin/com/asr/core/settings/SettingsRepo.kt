package com.asr.core.settings

enum class ThemeOption { SYSTEM, LIGHT, DARK }

interface SettingsRepo {
    fun getTheme(): ThemeOption
    fun setTheme(theme: ThemeOption)
    fun getPunishmentAcknowledgedDate(): String?
    fun setPunishmentAcknowledgedDate(date: String?)
}
