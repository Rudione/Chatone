@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package io.rudione.chatone.data.sound

import io.rudione.chatone.domain.sound.PcmAudio
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatFloat32
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual val mediaStreamChoiceSupported: Boolean = false

actual fun soundsRootDirectory(): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: NSTemporaryDirectory()
    return "${base.trimEnd('/')}/sounds"
}

actual suspend fun decodePlatformAudio(path: String, maxDurationMs: Long): PcmAudio? = withContext(Dispatchers.Default) {
    runCatching {
        val file = AVAudioFile(
            forReading = NSURL.fileURLWithPath(path),
            commonFormat = AVAudioPCMFormatFloat32,
            interleaved = false,
            error = null
        )
        val format = file.processingFormat
        val rate = format.sampleRate
        val channels = format.channelCount.toInt()
        val limit = (rate * maxDurationMs / 1000.0).toLong()
        val frames = minOf(file.length, limit)
        if (frames <= 0L || channels <= 0 || rate <= 0.0) return@runCatching null
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = frames.toUInt()) ?: return@runCatching null
        if (!file.readIntoBuffer(buffer, frameCount = frames.toUInt(), error = null)) return@runCatching null
        val read = buffer.frameLength.toInt()
        val data = buffer.floatChannelData ?: return@runCatching null
        val out = FloatArray(read * channels)
        for (c in 0 until channels) {
            val channel = data[c] ?: continue
            for (i in 0 until read) out[i * channels + c] = channel[i]
        }
        PcmAudio(rate.toInt(), channels, out)
    }.getOrNull()
}
