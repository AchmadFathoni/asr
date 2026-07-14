package com.asr.ui.di

import com.asr.core.interfaces.SoundPlayer
import org.koin.core.annotation.Single

@Single(binds = [SoundPlayer::class])
class DefaultSoundPlayer : SoundPlayer {
    override fun play() {}
}
