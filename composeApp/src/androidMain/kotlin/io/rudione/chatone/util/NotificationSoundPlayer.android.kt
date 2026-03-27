package io.rudione.chatone.util

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator

actual object NotificationSoundPlayer {
    actual fun playMentionSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {
            // Silent fail
        }
    }
}
