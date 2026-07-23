package com.asr.app.util

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import com.asr.R
import com.asr.core.interfaces.SoundPlayer

class AndroidSoundPlayer(private val context: Context) : SoundPlayer {
    override fun play(pitch: Float) {
        val player = MediaPlayer.create(context, R.raw.done) ?: return
        try {
            if (pitch != 1f) player.setPlaybackParams(PlaybackParams().setPitch(pitch))
            player.setOnCompletionListener { player.release() }
            player.start()
        } catch (e: Exception) {
            player.release()
            Log.e("SoundPlayer", "playback failed", e)
        }
    }
}
