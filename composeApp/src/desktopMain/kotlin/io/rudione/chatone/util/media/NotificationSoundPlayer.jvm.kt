
package io.rudione.chatone.util.media

import java.io.File
import javax.sound.sampled.*
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

actual object NotificationSoundPlayer {

    actual fun playMentionSound() {
        playMentionSound(volume = 0.8f, customSoundPath = "")
    }

    actual fun playMentionSound(volume: Float, customSoundPath: String) {
        Thread {
            try {
                val effectiveVolume = volume.coerceIn(0.0f, 1.0f)

                if (customSoundPath.isNotBlank()) {
                    val file = File(customSoundPath)
                    if (file.exists() && file.canRead()) {
                        val played = tryPlayFile(file, effectiveVolume)
                        if (!played) {
                            println("Failed to play custom sound, falling back to default")
                            playDefaultTone(effectiveVolume)
                        }
                    } else {
                        println("Custom sound file not found: $customSoundPath")
                        playDefaultTone(effectiveVolume)
                    }
                } else {
                    playDefaultTone(effectiveVolume)
                }
            } catch (e: Exception) {
                println("Error playing mention sound: ${e.message}")
                try { playDefaultTone(volume.coerceIn(0.0f, 1.0f)) } catch (_: Exception) {}
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun tryPlayFile(file: File, volume: Float): Boolean {
        var audioInputStream: AudioInputStream? = null
        var clip: Clip? = null
        return try {
            audioInputStream = AudioSystem.getAudioInputStream(file)
            clip = AudioSystem.getClip()
            clip.open(audioInputStream)

            try {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val dB = (20.0 * log10(volume.toDouble())).toFloat()
                gainControl.value = dB.coerceIn(gainControl.minimum, gainControl.maximum)
            } catch (e: IllegalArgumentException) {
                try {
                    val volumeControl = clip.getControl(FloatControl.Type.VOLUME) as FloatControl
                    volumeControl.value = volume.coerceIn(volumeControl.minimum, volumeControl.maximum)
                } catch (_: Exception) {}
            }

            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                    try { audioInputStream?.close() } catch (_: Exception) {}
                }
            }
            clip.start()
            true
        } catch (e: UnsupportedAudioFileException) {
            println("Unsupported audio format: ${e.message}")
            false
        } catch (e: Exception) {
            println("Failed to play audio file: ${e.message}")
            false
        } finally {
            if (clip?.isOpen == false) {
                try { audioInputStream?.close() } catch (_: Exception) {}
            }
        }
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
        } catch (e: Exception) {
            println("Failed to play default tone: ${e.message}")
        }
    }
}
