package io.rudione.chatone.domain.sound

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.tanh

object SoundProcessing {
    private const val NORMALIZE_PEAK = 0.89f
    private const val ANTI_CLICK_MS = 4
    private const val SILENCE_DB = -42f
    private const val LEAD_IN_MS = 8L
    private const val TAIL_MS = 90L
    private const val CAP_FADE_MS = 140

    fun clampEdit(edit: SoundEdit, sourceMs: Long): SoundEdit {
        val total = sourceMs.coerceAtLeast(0L)
        val end = (if (edit.endMs <= 0L) total else edit.endMs).coerceIn(0L, total)
        val start = edit.startMs.coerceIn(0L, (end - SoundLimits.MIN_CLIP_MS).coerceAtLeast(0L))
        val cappedEnd = min(end, start + SoundLimits.MAX_CLIP_MS)
        val length = (cappedEnd - start).toInt()
        return edit.copy(
            startMs = start,
            endMs = cappedEnd,
            fadeInMs = edit.fadeInMs.coerceIn(0, min(SoundLimits.MAX_FADE_MS, length / 2)),
            fadeOutMs = edit.fadeOutMs.coerceIn(0, min(SoundLimits.MAX_FADE_MS, length / 2)),
            gain = edit.gain.coerceIn(0.1f, 2f)
        )
    }

    fun defaultEdit(audio: PcmAudio): SoundEdit {
        val total = audio.durationMs
        val audible = audibleRange(audio) ?: (0L..total)
        val start = (audible.first - LEAD_IN_MS).coerceAtLeast(0L)
        val naturalEnd = (audible.last + TAIL_MS).coerceAtMost(total)
        val capped = naturalEnd - start > SoundLimits.MAX_CLIP_MS
        return clampEdit(
            SoundEdit(startMs = start, endMs = naturalEnd, fadeOutMs = if (capped) CAP_FADE_MS else 0),
            total
        )
    }

    fun audibleRange(audio: PcmAudio, thresholdDb: Float = SILENCE_DB): LongRange? {
        val threshold = 10f.pow(thresholdDb / 20f)
        val channels = audio.channels
        var first = -1
        var last = -1
        for (frame in 0 until audio.frames) {
            var loud = false
            for (c in 0 until channels) {
                if (abs(audio.samples[frame * channels + c]) > threshold) {
                    loud = true
                    break
                }
            }
            if (loud) {
                if (first < 0) first = frame
                last = frame
            }
        }
        if (first < 0) return null
        return (first * 1000L / audio.sampleRate)..((last + 1) * 1000L / audio.sampleRate)
    }

    fun render(source: PcmAudio, edit: SoundEdit): PcmAudio {
        val prepared = resample(downmix(source))
        val safe = clampEdit(edit, prepared.durationMs)
        val channels = prepared.channels
        val startFrame = prepared.frameAt(safe.startMs)
        val endFrame = prepared.frameAt(safe.endMs).coerceAtLeast(startFrame)
        val frames = endFrame - startFrame
        val out = prepared.samples.copyOfRange(startFrame * channels, endFrame * channels)
        val scale = (if (safe.normalize) normalizeScale(out) else 1f) * safe.gain
        val fadeIn = max(safe.fadeInMs, ANTI_CLICK_MS) * prepared.sampleRate / 1000
        val fadeOut = max(safe.fadeOutMs, ANTI_CLICK_MS) * prepared.sampleRate / 1000
        for (frame in 0 until frames) {
            val envelope = min(ramp(frame, fadeIn), ramp(frames - 1 - frame, fadeOut))
            for (c in 0 until channels) {
                val index = frame * channels + c
                out[index] = softClip(out[index] * scale * envelope)
            }
        }
        return PcmAudio(prepared.sampleRate, channels, out)
    }

    fun peaks(audio: PcmAudio, buckets: Int = SoundLimits.PEAK_BUCKETS): List<Float> {
        if (buckets <= 0 || audio.frames == 0) return List(buckets.coerceAtLeast(0)) { 0f }
        val channels = audio.channels
        val raw = FloatArray(buckets)
        for (b in 0 until buckets) {
            val from = (b.toLong() * audio.frames / buckets).toInt()
            val to = (((b + 1).toLong() * audio.frames / buckets).toInt()).coerceAtLeast(from + 1).coerceAtMost(audio.frames)
            var peak = 0f
            for (frame in from until to) {
                for (c in 0 until channels) peak = max(peak, abs(audio.samples[frame * channels + c]))
            }
            raw[b] = peak
        }
        val top = raw.maxOrNull()?.takeIf { it > 0f } ?: return raw.toList()
        return raw.map { (it / top * 100f).toInt() / 100f }
    }

    fun downmix(audio: PcmAudio): PcmAudio {
        if (audio.channels <= 2) return audio
        val frames = audio.frames
        val out = FloatArray(frames * 2)
        for (frame in 0 until frames) {
            out[frame * 2] = audio.samples[frame * audio.channels]
            out[frame * 2 + 1] = audio.samples[frame * audio.channels + 1]
        }
        return PcmAudio(audio.sampleRate, 2, out)
    }

    fun resample(audio: PcmAudio, maxRate: Int = SoundLimits.MAX_RENDER_RATE): PcmAudio {
        if (audio.sampleRate <= maxRate || audio.frames == 0) return audio
        val channels = audio.channels
        val ratio = audio.sampleRate.toDouble() / maxRate
        val frames = (audio.frames / ratio).toInt()
        val out = FloatArray(frames * channels)
        for (frame in 0 until frames) {
            val position = frame * ratio
            val left = position.toInt().coerceAtMost(audio.frames - 1)
            val right = (left + 1).coerceAtMost(audio.frames - 1)
            val t = (position - left).toFloat()
            for (c in 0 until channels) {
                val a = audio.samples[left * channels + c]
                val b = audio.samples[right * channels + c]
                out[frame * channels + c] = a + (b - a) * t
            }
        }
        return PcmAudio(maxRate, channels, out)
    }

    private fun normalizeScale(samples: FloatArray): Float {
        var peak = 0f
        for (s in samples) peak = max(peak, abs(s))
        return if (peak < 1e-4f) 1f else NORMALIZE_PEAK / peak
    }

    private fun ramp(position: Int, length: Int): Float =
        if (length <= 0 || position >= length) 1f else (position.coerceAtLeast(0).toFloat() / length)

    private fun softClip(x: Float): Float {
        val magnitude = abs(x)
        if (magnitude <= 0.9f) return x
        return sign(x) * (0.9f + 0.1f * tanh((magnitude - 0.9f) / 0.1f))
    }
}
