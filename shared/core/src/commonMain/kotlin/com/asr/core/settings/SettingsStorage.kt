package com.asr.core.settings

interface SettingsStorage {
    fun getTheme(): ThemeOption
    fun setTheme(theme: ThemeOption)
    fun getPunishmentAcknowledgedDate(): String?
    fun setPunishmentAcknowledgedDate(date: String?)
}
