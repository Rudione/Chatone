package io.rudione.chatone.util

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import java.io.File

actual object NotificationSoundPlayer {
    actual fun playMentionSound() {
        playMentionSound(volume = 0.8f, customSoundPath = "")
    }

    actual fun playMentionSound(volume: Float, customSoundPath: String) {
        try {
            if (customSoundPath.isNotBlank() && File(customSoundPath).exists()) {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(customSoundPath)
                mediaPlayer.setVolume(volume, volume)
                mediaPlayer.setOnCompletionListener { it.release() }
                mediaPlayer.prepare()
                mediaPlayer.start()
            } else {
                val toneVolume = (volume * 100).toInt().coerceIn(0, 100)
                val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, toneVolume)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            }
        } catch (_: Exception) {
           
        }
    }
}
