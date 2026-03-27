package io.rudione.chatone.util

import javax.sound.sampled.*
import kotlin.math.PI
import kotlin.math.sin

actual object NotificationSoundPlayer {
    actual fun playMentionSound() {
        try {
            // Generate a short "ping" tone programmatically
            val sampleRate = 44100f
            val durationMs = 150
            val frequency = 880.0 // A5 note
            val numSamples = (sampleRate * durationMs / 1000).toInt()
            val buffer = ByteArray(numSamples * 2) // 16-bit mono

            for (i in 0 until numSamples) {
                val t = i / sampleRate.toDouble()
                val fade = if (i < numSamples / 10) i.toDouble() / (numSamples / 10)
                else (numSamples - i).toDouble() / (numSamples * 0.8)
                val sample = (sin(2.0 * PI * frequency * t) * 0.5 * fade.coerceIn(0.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                buffer[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
            }

            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val clip = AudioSystem.getClip()
            clip.open(format, buffer, 0, buffer.size)
            clip.start()
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                }
            }
        } catch (_: Exception) {
            // Silent fail if audio not available
        }
    }
}
