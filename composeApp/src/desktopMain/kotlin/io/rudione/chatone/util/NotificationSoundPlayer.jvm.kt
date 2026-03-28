package io.rudione.chatone.util

import java.io.File
import javax.sound.sampled.*
import kotlin.math.PI
import kotlin.math.sin

actual object NotificationSoundPlayer {
    actual fun playMentionSound() {
        playMentionSound(volume = 0.8f, customSoundPath = "")
    }

    actual fun playMentionSound(volume: Float, customSoundPath: String) {
        try {
            if (customSoundPath.isNotBlank()) {
                playCustomSound(customSoundPath, volume)
            } else {
                playDefaultTone(volume)
            }
        } catch (_: Exception) {
            // Silent fail if audio not available
        }
    }

    private fun playCustomSound(path: String, volume: Float) {
        val file = File(path)
        if (!file.exists()) {
            playDefaultTone(volume)
            return
        }
        val audioStream = AudioSystem.getAudioInputStream(file)
        val clip = AudioSystem.getClip()
        clip.open(audioStream)
        // Set volume
        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
        if (gainControl != null) {
            val dB = (20.0 * Math.log10(volume.toDouble().coerceIn(0.01, 1.0))).toFloat()
            gainControl.value = dB.coerceIn(gainControl.minimum, gainControl.maximum)
        }
        clip.start()
        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                clip.close()
                audioStream.close()
            }
        }
    }

    private fun playDefaultTone(volume: Float) {
        val sampleRate = 44100f
        val durationMs = 150
        val frequency = 880.0 // A5 note
        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val buffer = ByteArray(numSamples * 2) // 16-bit mono
        val vol = volume.toDouble().coerceIn(0.0, 1.0)

        for (i in 0 until numSamples) {
            val t = i / sampleRate.toDouble()
            val fade = if (i < numSamples / 10) i.toDouble() / (numSamples / 10)
            else (numSamples - i).toDouble() / (numSamples * 0.8)
            val sample = (sin(2.0 * PI * frequency * t) * 0.5 * vol * fade.coerceIn(0.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
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
    }
}
