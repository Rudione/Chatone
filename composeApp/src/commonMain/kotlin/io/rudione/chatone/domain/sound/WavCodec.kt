package io.rudione.chatone.domain.sound

import kotlin.math.roundToInt

object WavCodec {
    private const val FORMAT_PCM = 1
    private const val FORMAT_FLOAT = 3
    private const val FORMAT_EXTENSIBLE = 0xFFFE

    fun isWav(bytes: ByteArray): Boolean =
        bytes.size >= 12 && ascii(bytes, 0) == "RIFF" && ascii(bytes, 8) == "WAVE"

    fun encode(audio: PcmAudio): ByteArray {
        val channels = audio.channels.coerceAtLeast(1)
        val frames = audio.frames
        val dataSize = frames * channels * 2
        val out = ByteArray(44 + dataSize)
        putAscii(out, 0, "RIFF")
        putInt(out, 4, 36 + dataSize)
        putAscii(out, 8, "WAVE")
        putAscii(out, 12, "fmt ")
        putInt(out, 16, 16)
        putShort(out, 20, FORMAT_PCM)
        putShort(out, 22, channels)
        putInt(out, 24, audio.sampleRate)
        putInt(out, 28, audio.sampleRate * channels * 2)
        putShort(out, 32, channels * 2)
        putShort(out, 34, 16)
        putAscii(out, 36, "data")
        putInt(out, 40, dataSize)
        var offset = 44
        for (i in 0 until frames * channels) {
            val value = (audio.samples[i] * 32768f).roundToInt().coerceIn(-32768, 32767)
            out[offset] = value.toByte()
            out[offset + 1] = (value shr 8).toByte()
            offset += 2
        }
        return out
    }

    fun decode(bytes: ByteArray, maxDurationMs: Long = Long.MAX_VALUE): PcmAudio? {
        if (!isWav(bytes)) return null
        var offset = 12
        var format = -1
        var channels = 0
        var sampleRate = 0
        var bits = 0
        while (offset + 8 <= bytes.size) {
            val id = ascii(bytes, offset)
            val size = int(bytes, offset + 4).toLong() and 0xffffffffL
            val body = offset + 8
            when (id) {
                "fmt " -> {
                    if (body + 16 > bytes.size) return null
                    format = short(bytes, body)
                    channels = short(bytes, body + 2)
                    sampleRate = int(bytes, body + 4)
                    bits = short(bytes, body + 14)
                    if (format == FORMAT_EXTENSIBLE && size >= 26 && body + 26 <= bytes.size) format = short(bytes, body + 24)
                }
                "data" -> {
                    if (format < 0 || channels <= 0 || sampleRate <= 0) return null
                    val end = (body + size).coerceAtMost(bytes.size.toLong()).toInt()
                    return readSamples(bytes, body, end, format, channels, sampleRate, bits, maxDurationMs)
                }
            }
            offset = (body + size + (size and 1L)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
        return null
    }

    private fun readSamples(
        bytes: ByteArray,
        start: Int,
        end: Int,
        format: Int,
        channels: Int,
        sampleRate: Int,
        bits: Int,
        maxDurationMs: Long
    ): PcmAudio? {
        val width = bits / 8
        if (width !in 1..4) return null
        if (format != FORMAT_PCM && !(format == FORMAT_FLOAT && width == 4)) return null
        val available = (end - start) / (width * channels)
        val cap = if (maxDurationMs == Long.MAX_VALUE) Long.MAX_VALUE else maxDurationMs * sampleRate / 1000L
        val frames = available.toLong().coerceAtMost(cap).toInt()
        val samples = FloatArray(frames * channels)
        var offset = start
        for (i in samples.indices) {
            samples[i] = when {
                format == FORMAT_FLOAT -> Float.fromBits(int(bytes, offset))
                width == 1 -> ((bytes[offset].toInt() and 0xff) - 128) / 128f
                width == 2 -> short(bytes, offset).toShort() / 32768f
                width == 3 -> {
                    val raw = (bytes[offset].toInt() and 0xff) or
                        ((bytes[offset + 1].toInt() and 0xff) shl 8) or
                        (bytes[offset + 2].toInt() shl 16)
                    raw / 8388608f
                }
                else -> int(bytes, offset) / 2147483648f
            }
            offset += width
        }
        return PcmAudio(sampleRate, channels, samples)
    }

    private fun ascii(bytes: ByteArray, offset: Int): String =
        if (offset + 4 > bytes.size) "" else CharArray(4) { (bytes[offset + it].toInt() and 0xff).toChar() }.concatToString()

    private fun short(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun int(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            (bytes[offset + 3].toInt() shl 24)

    private fun putAscii(out: ByteArray, offset: Int, text: String) {
        text.forEachIndexed { i, c -> out[offset + i] = c.code.toByte() }
    }

    private fun putShort(out: ByteArray, offset: Int, value: Int) {
        out[offset] = value.toByte()
        out[offset + 1] = (value shr 8).toByte()
    }

    private fun putInt(out: ByteArray, offset: Int, value: Int) {
        out[offset] = value.toByte()
        out[offset + 1] = (value shr 8).toByte()
        out[offset + 2] = (value shr 16).toByte()
        out[offset + 3] = (value shr 24).toByte()
    }
}
