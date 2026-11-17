package io.rudione.chatone.data.sound

import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.WavCodec
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

actual val mediaStreamChoiceSupported: Boolean = false

actual fun soundsRootDirectory(): String =
    File(System.getProperty("user.home"), ".chatone/sounds").absolutePath

actual suspend fun decodePlatformAudio(path: String, maxDurationMs: Long): PcmAudio? = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.isFile) return@withContext null
    when (file.extension.lowercase()) {
        "mp3" -> DesktopDecoders.mp3(file, maxDurationMs) ?: DesktopDecoders.afconvert(file, maxDurationMs)
        "wav", "aif", "aiff", "au" -> DesktopDecoders.javaSound(file, maxDurationMs) ?: DesktopDecoders.afconvert(file, maxDurationMs)
        else -> DesktopDecoders.afconvert(file, maxDurationMs)
    }
}

internal object DesktopDecoders {
    private const val AFCONVERT = "/usr/bin/afconvert"
    private const val CONVERT_TIMEOUT_S = 20L

    fun javaSound(file: File, maxDurationMs: Long): PcmAudio? = runCatching {
        AudioSystem.getAudioInputStream(file).use { source ->
            val format = source.format
            val channels = format.channels.coerceAtLeast(1)
            val rate = format.sampleRate.toInt()
            val target = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, 16, channels, channels * 2, format.sampleRate, false)
            AudioSystem.getAudioInputStream(target, source).use { pcm ->
                val limit = (rate.toLong() * maxDurationMs / 1000L * channels * 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                val bytes = pcm.readNBytes(limit)
                val samples = FloatArray(bytes.size / 2) { i ->
                    ((bytes[i * 2].toInt() and 0xff) or (bytes[i * 2 + 1].toInt() shl 8)).toShort() / 32768f
                }
                PcmAudio(rate, channels, samples)
            }
        }
    }.getOrNull()?.takeIf { it.frames > 0 }

    fun mp3(file: File, maxDurationMs: Long): PcmAudio? = runCatching {
        val bitstream = Bitstream(BufferedInputStream(FileInputStream(file)))
        val decoder = Decoder()
        var rate = 0
        var channels = 0
        var samples = FloatArray(1 shl 16)
        var count = 0
        try {
            while (true) {
                val header = bitstream.readFrame() ?: break
                val buffer = decoder.decodeFrame(header, bitstream) as SampleBuffer
                rate = buffer.sampleFrequency
                channels = buffer.channelCount
                val length = buffer.bufferLength
                if (count + length > samples.size) samples = samples.copyOf(maxOf(samples.size * 2, count + length))
                val data = buffer.buffer
                for (i in 0 until length) samples[count++] = data[i] / 32768f
                bitstream.closeFrame()
                if (channels > 0 && count / channels >= rate.toLong() * maxDurationMs / 1000L) break
            }
        } finally {
            runCatching { bitstream.close() }
        }
        if (rate <= 0 || channels <= 0) null else PcmAudio(rate, channels, samples.copyOf(count - count % channels))
    }.getOrNull()?.takeIf { it.frames > 0 }

    fun afconvert(file: File, maxDurationMs: Long): PcmAudio? {
        if (!System.getProperty("os.name", "").lowercase().contains("mac") || !File(AFCONVERT).canExecute()) return null
        val output = File.createTempFile("chatone-sound", ".wav")
        return try {
            val process = ProcessBuilder(AFCONVERT, "-f", "WAVE", "-d", "LEI16", file.absolutePath, output.absolutePath)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
            if (!process.waitFor(CONVERT_TIMEOUT_S, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() != 0) null else WavCodec.decode(output.readBytes(), maxDurationMs)
        } catch (_: Exception) {
            null
        } finally {
            output.delete()
        }
    }
}
