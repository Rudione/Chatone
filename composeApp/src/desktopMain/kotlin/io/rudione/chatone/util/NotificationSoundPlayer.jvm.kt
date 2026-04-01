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
        Thread {
            try {
                if (customSoundPath.isNotBlank()) {
                    val file = File(customSoundPath)
                    if (file.exists() && file.canRead()) {
                        val played = tryPlayFile(file, volume)
                        if (!played) playDefaultTone(volume)
                    } else {
                        playDefaultTone(volume)
                    }
                } else {
                    playDefaultTone(volume)
                }
            } catch (_: Exception) {
                try { playDefaultTone(volume) } catch (_: Exception) {}
            }
        }.also { it.isDaemon = true }.start()
    }

    /**
     * Attempts to play an audio file.
     * Returns true if playback started successfully, false if unsupported/error.
     */
    private fun tryPlayFile(file: File, volume: Float): Boolean {
        var rawStream: AudioInputStream? = null
        var clip: Clip? = null
        return try {
            rawStream = AudioSystem.getAudioInputStream(file)
            val sourceFormat = rawStream.format

            // Build a PCM target format from source properties
            val channels = sourceFormat.channels.takeIf { it > 0 } ?: 2
            val sampleRate = sourceFormat.sampleRate.takeIf { it > 0f } ?: 44100f
            val targetFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                channels,
                channels * 2,
                sampleRate,
                false
            )

            // Try to convert to PCM (works for WAV natively, MP3 needs SPI plugin)
            val pcmStream = if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                AudioSystem.getAudioInputStream(targetFormat, rawStream)
            } else {
                rawStream // Use as-is if already PCM or conversion not available
            }

            clip = AudioSystem.getClip()
            clip.open(pcmStream)
            applyVolume(clip, volume)

            val finalClip = clip
            val finalRaw = rawStream
            finalClip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    finalClip.close()
                    try { finalRaw.close() } catch (_: Exception) {}
                }
            }
            finalClip.start()
            true
        } catch (e: Exception) {
            try { clip?.close() } catch (_: Exception) {}
            try { rawStream?.close() } catch (_: Exception) {}
            false
        }
    }

    private fun applyVolume(clip: Clip, volume: Float) {
        try {
            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl ?: return
            val safeVolume = volume.coerceIn(0.0001f, 1.0f)
            val dB = (20.0 * Math.log10(safeVolume.toDouble())).toFloat()
            gain.value = dB.coerceIn(gain.minimum, gain.maximum)
        } catch (_: Exception) {}
    }

    private fun playDefaultTone(volume: Float) {
        try {
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
                }.coerceIn(0.0, 1.0)
                val sample = (sin(2.0 * PI * frequency * t) * 0.5 * vol * fade * Short.MAX_VALUE)
                    .toInt().toShort()
                buffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                buffer[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
            }

            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val clip = AudioSystem.getClip()
            clip.open(format, buffer, 0, buffer.size)
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) clip.close()
            }
            clip.start()
        } catch (_: Exception) {}
    }
}