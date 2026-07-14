package com.asr.app.util

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.RingtoneManager
import android.net.Uri
import com.asr.core.interfaces.SoundPlayer

class AndroidSoundPlayer(private val context: Context) : SoundPlayer {
    override fun play(pitch: Float) {
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
            if (pitch != 1f) setPlaybackParams(PlaybackParams().setPitch(pitch))
            setOnCompletionListener { release() }
            start()
        }
    }
}
