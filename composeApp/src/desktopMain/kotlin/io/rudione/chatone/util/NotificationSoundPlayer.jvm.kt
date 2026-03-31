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
        // Run on a background thread so we never block the UI/AWT thread
        Thread {
            try {
                if (customSoundPath.isNotBlank()) {
                    val file = File(customSoundPath)
                    if (file.exists() && file.canRead()) {
                        playCustomSound(file, volume)
                    } else {
                        // File missing or unreadable — fall back to default
                        playDefaultTone(volume)
                    }
                } else {
                    playDefaultTone(volume)
                }
            } catch (_: Exception) {
                // Silent fail — try default as last resort
                try { playDefaultTone(volume) } catch (_: Exception) {}
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun applyVolume(clip: Clip, volume: Float) {
        try {
            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl ?: return
            val dB = (20.0 * Math.log10(volume.toDouble().coerceIn(0.01, 1.0))).toFloat()
            gain.value = dB.coerceIn(gain.minimum, gain.maximum)
        } catch (_: Exception) {}
    }

    private fun playCustomSound(file: File, volume: Float) {
        var audioStream: AudioInputStream? = null
        var clip: Clip? = null
        try {
            audioStream = AudioSystem.getAudioInputStream(file)

            // Convert to PCM if needed (e.g. MP3 via SPI)
            val baseFormat = audioStream.format
            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate.takeIf { it > 0 } ?: 44100f,
                16,
                baseFormat.channels.takeIf { it > 0 } ?: 2,
                baseFormat.channels.takeIf { it > 0 }?.times(2) ?: 4,
                baseFormat.sampleRate.takeIf { it > 0 } ?: 44100f,
                false
            )

            val pcmStream = try {
                AudioSystem.getAudioInputStream(decodedFormat, audioStream)
            } catch (_: Exception) {
                // Already PCM or conversion not available — use original
                audioStream
            }

            clip = AudioSystem.getClip()
            clip.open(pcmStream)
            applyVolume(clip, volume)

            val finalClip = clip
            val finalStream = audioStream
            finalClip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    finalClip.close()
                    try { finalStream.close() } catch (_: Exception) {}
                }
            }
            finalClip.start()
        } catch (e: Exception) {
            // Close resources and fall back to default
            try { clip?.close() } catch (_: Exception) {}
            try { audioStream?.close() } catch (_: Exception) {}
            playDefaultTone(volume)
        }
    }

    private fun playDefaultTone(volume: Float) {
        val sampleRate = 44100f
        val durationMs = 150
        val frequency = 880.0
        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val buffer = ByteArray(numSamples * 2)
        val vol = volume.toDouble().coerceIn(0.0, 1.0)

        for (i in 0 until numSamples) {
            val t = i / sampleRate.toDouble()
            val fade = if (i < numSamples / 10) {
                i.toDouble() / (numSamples / 10)
            } else {
                (numSamples - i).toDouble() / (numSamples * 0.8)
            }
            val sample = (sin(2.0 * PI * frequency * t) * 0.5 * vol *
                    fade.coerceIn(0.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
        }

        val format = AudioFormat(sampleRate, 16, 1, true, false)
        val clip = AudioSystem.getClip()
        clip.open(format, buffer, 0, buffer.size)
        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP) {
                clip.close()
            }
        }
        clip.start()
    }
}