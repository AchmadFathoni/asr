package com.asr.di

import com.asr.app.util.AndroidSoundPlayer
import com.asr.core.interfaces.SoundPlayer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppModuleTest {
    @Test
    fun `appModule binds SoundPlayer to AndroidSoundPlayer`() {
        val soundPlayer = AppModule().provideSoundPlayer(RuntimeEnvironment.getApplication())
        assertTrue("SoundPlayer must be AndroidSoundPlayer", soundPlayer is AndroidSoundPlayer)
    }
}
