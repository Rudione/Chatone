package io.rudione.chatone.data.sound

import android.app.Application
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import io.rudione.chatone.domain.sound.PcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.File
import java.nio.ByteOrder

actual val mediaStreamChoiceSupported: Boolean = true

actual fun soundsRootDirectory(): String =
    File(KoinPlatform.getKoin().get<Application>().filesDir, "sounds").absolutePath

actual suspend fun decodePlatformAudio(path: String, maxDurationMs: Long): PcmAudio? =
    withContext(Dispatchers.IO) { runCatching { MediaCodecDecoder(path, maxDurationMs).decode() }.getOrNull() }

private class MediaCodecDecoder(private val path: String, private val maxDurationMs: Long) {
    private var sampleRate = 0
    private var channels = 0
    private var encoding = AudioFormat.ENCODING_PCM_16BIT
    private var samples = FloatArray(1 shl 16)
    private var count = 0

    fun decode(): PcmAudio? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME) ?: return null)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            pump(extractor, decoder)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
        if (sampleRate <= 0 || channels <= 0 || count < channels) return null
        val frames = count / channels
        return PcmAudio(sampleRate, channels, samples.copyOf(frames * channels))
    }

    private fun pump(extractor: MediaExtractor, codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var idle = 0
        while (idle < MAX_IDLE_POLLS) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buffer = codec.getInputBuffer(inputIndex) ?: return
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outputIndex >= 0 -> {
                    idle = 0
                    codec.getOutputBuffer(outputIndex)?.let { buffer ->
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        append(buffer.order(ByteOrder.nativeOrder()))
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || reachedLimit()) return
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val output = codec.outputFormat
                    sampleRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        encoding = output.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                }
                else -> idle++
            }
        }
    }

    private fun append(buffer: java.nio.ByteBuffer) {
        if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
            val floats = buffer.asFloatBuffer()
            ensure(floats.remaining())
            while (floats.hasRemaining()) samples[count++] = floats.get()
        } else {
            val shorts = buffer.asShortBuffer()
            ensure(shorts.remaining())
            while (shorts.hasRemaining()) samples[count++] = shorts.get() / 32768f
        }
    }

    private fun ensure(extra: Int) {
        if (count + extra <= samples.size) return
        var size = samples.size
        while (size < count + extra) size *= 2
        samples = samples.copyOf(size)
    }

    private fun reachedLimit(): Boolean =
        sampleRate > 0 && channels > 0 && count / channels >= sampleRate * maxDurationMs / 1000L

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val MAX_IDLE_POLLS = 600
    }
}
