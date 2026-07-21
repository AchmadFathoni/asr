package com.asr.core.settings

class SharedSettingsRepo(private val storage: SettingsStorage) : SettingsRepo {
    override fun getTheme(): ThemeOption = storage.getTheme()
    override fun setTheme(theme: ThemeOption) = storage.setTheme(theme)
    override fun getPunishmentAcknowledgedDate(): String? = storage.getPunishmentAcknowledgedDate()
    override fun setPunishmentAcknowledgedDate(date: String?) = storage.setPunishmentAcknowledgedDate(date)
}
