package com.asr.desktop

import com.asr.core.interfaces.SoundPlayer
import com.asr.ui.di.DefaultSoundPlayer
import com.asr.ui.di.UIModules
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [UIModules::class])
@ComponentScan("com.asr.desktop")
class DesktopAppModule {
    @Single
    fun provideSoundPlayer(): SoundPlayer = DefaultSoundPlayer()
}
