package com.asr.ui.di

import com.asr.core.interfaces.SoundPlayer
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class TestSoundPlayerModule {
    @Single(binds = [SoundPlayer::class])
    fun provideSoundPlayer(): SoundPlayer = object : SoundPlayer {
        override fun play(pitch: Float) {}
    }
}
