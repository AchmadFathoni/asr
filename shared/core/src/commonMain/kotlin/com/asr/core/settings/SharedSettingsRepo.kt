package com.asr.core.settings

class SharedSettingsRepo(private val storage: SettingsStorage) : SettingsRepo {
    override fun getTheme(): ThemeOption = storage.getTheme()
    override fun setTheme(theme: ThemeOption) = storage.setTheme(theme)
}
