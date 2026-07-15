package com.asr.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import com.asr.R
import com.asr.core.interfaces.SoundPlayer

class AndroidSoundPlayer(private val context: Context) : SoundPlayer {
    override fun play(pitch: Float) {
        val player = MediaPlayer.create(context, R.raw.done) ?: return
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            if (pitch != 1f) player.setPlaybackParams(PlaybackParams().setPitch(pitch))
            player.setOnCompletionListener { player.release() }
            player.start()
        } catch (e: Exception) {
            player.release()
            Log.e("SoundPlayer", "playback failed", e)
        }
    }
}
