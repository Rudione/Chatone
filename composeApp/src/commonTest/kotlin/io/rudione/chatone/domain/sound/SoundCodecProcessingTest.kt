package io.rudione.chatone.domain.sound

import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SoundCodecProcessingTest {

    private fun tone(ms: Int, rate: Int = 44_100, channels: Int = 1, silenceBeforeMs: Int = 0, silenceAfterMs: Int = 0, level: Float = 0.5f): PcmAudio {
        val lead = rate * silenceBeforeMs / 1000
        val body = rate * ms / 1000
        val tail = rate * silenceAfterMs / 1000
        val frames = lead + body + tail
        val out = FloatArray(frames * channels)
        for (f in lead until lead + body) {
            val v = (sin(2 * kotlin.math.PI * 440 * f / rate) * level).toFloat()
            for (c in 0 until channels) out[f * channels + c] = v
        }
        return PcmAudio(rate, channels, out)
    }

    @Test
    fun wavRoundTripKeepsFormatAndSamples() {
        val source = tone(200, channels = 2)
        val decoded = assertNotNull(WavCodec.decode(WavCodec.encode(source)))
        assertEquals(44_100, decoded.sampleRate)
        assertEquals(2, decoded.channels)
        assertEquals(source.frames, decoded.frames)
        for (i in source.samples.indices step 97) assertTrue(abs(source.samples[i] - decoded.samples[i]) < 1f / 16_000)
    }

    @Test
    fun wavDecoderSkipsExtraChunksAndReadsFloat() {
        val floats = floatArrayOf(0.25f, -0.5f, 0.75f)
        val data = ByteArray(floats.size * 4)
        floats.forEachIndexed { i, v ->
            val bits = v.toRawBits()
            for (b in 0 until 4) data[i * 4 + b] = (bits shr (8 * b)).toByte()
        }
        val list = "LIST".encodeToByteArray() + le(5) + byteArrayOf(1, 2, 3, 4, 5, 0)
        val fmt = "fmt ".encodeToByteArray() + le(16) + le16(3) + le16(1) + le(8000) + le(32000) + le16(4) + le16(32)
        val body = "WAVE".encodeToByteArray() + fmt + list + "data".encodeToByteArray() + le(data.size) + data
        val bytes = "RIFF".encodeToByteArray() + le(body.size) + body
        val decoded = assertNotNull(WavCodec.decode(bytes))
        assertEquals(8000, decoded.sampleRate)
        assertEquals(floats.toList(), decoded.samples.toList())
    }

    @Test
    fun wavDecoderRejectsOtherFilesAndHonoursDurationCap() {
        assertNull(WavCodec.decode("ID3 not a wave".encodeToByteArray()))
        val capped = assertNotNull(WavCodec.decode(WavCodec.encode(tone(1000)), maxDurationMs = 250))
        assertEquals(250L, capped.durationMs)
    }

    @Test
    fun clampEditCapsLengthAndFades() {
        val edit = SoundProcessing.clampEdit(SoundEdit(startMs = 1_000, endMs = 20_000, fadeInMs = 4_000, fadeOutMs = -5), 30_000)
        assertEquals(1_000L, edit.startMs)
        assertEquals(1_000L + SoundLimits.MAX_CLIP_MS, edit.endMs)
        assertEquals(SoundLimits.MAX_FADE_MS, edit.fadeInMs)
        assertEquals(0, edit.fadeOutMs)
    }

    @Test
    fun clampEditKeepsMinimumLengthAndFullSourceWhenEndIsUnset() {
        val tight = SoundProcessing.clampEdit(SoundEdit(startMs = 990, endMs = 1_000), 1_000)
        assertTrue(tight.endMs - tight.startMs >= SoundLimits.MIN_CLIP_MS)
        assertEquals(800L, SoundProcessing.clampEdit(SoundEdit(), 800).endMs)
    }

    @Test
    fun defaultEditTrimsSilenceAround() {
        val audio = tone(300, silenceBeforeMs = 500, silenceAfterMs = 700)
        val edit = SoundProcessing.defaultEdit(audio)
        assertTrue(edit.startMs in 480L..500L, "start=${edit.startMs}")
        assertTrue(edit.endMs in 800L..900L, "end=${edit.endMs}")
        assertEquals(0, edit.fadeOutMs)
    }

    @Test
    fun defaultEditAddsFadeWhenSoundIsCutAtLimit() {
        val edit = SoundProcessing.defaultEdit(tone(9_000, rate = 8_000))
        assertEquals(SoundLimits.MAX_CLIP_MS, edit.endMs - edit.startMs)
        assertTrue(edit.fadeOutMs > 0)
    }

    @Test
    fun renderSlicesAppliesFadesAndNormalizes() {
        val audio = tone(1_000, level = 0.2f)
        val out = SoundProcessing.render(audio, SoundEdit(startMs = 100, endMs = 600, fadeInMs = 50, normalize = true))
        assertEquals(500L, out.durationMs)
        assertEquals(0f, out.samples.first())
        val peak = out.samples.maxOf { abs(it) }
        assertTrue(peak in 0.85f..0.9f, "peak=$peak")
        assertTrue(abs(out.samples.last()) < 0.01f)
    }

    @Test
    fun renderSoftClipsLoudGain() {
        val out = SoundProcessing.render(tone(200, level = 0.9f), SoundEdit(gain = 2f))
        assertTrue(out.samples.all { abs(it) <= 1f })
    }

    @Test
    fun renderDownmixesAndResamples() {
        val surround = PcmAudio(96_000, 6, FloatArray(96_000 * 6 / 10) { 0.1f })
        val out = SoundProcessing.render(surround, SoundEdit())
        assertEquals(2, out.channels)
        assertEquals(SoundLimits.MAX_RENDER_RATE, out.sampleRate)
        assertEquals(100L, out.durationMs)
    }

    @Test
    fun peaksAreNormalizedToLoudestBucket() {
        val audio = PcmAudio(1_000, 1, FloatArray(1_000) { i -> if (i < 500) 0.1f else 0.4f })
        val peaks = SoundProcessing.peaks(audio, 4)
        assertEquals(listOf(0.25f, 0.25f, 1f, 1f), peaks)
    }

    @Test
    fun synthPresetsAreShortAndQuiet() {
        BuiltInSound.entries.mapNotNull { SoundSynth.render(it) }.forEach { audio ->
            assertTrue(audio.durationMs in 100L..1_500L)
            assertTrue(audio.samples.maxOf { abs(it) } <= 0.7f)
            assertTrue(abs(audio.samples.last()) < 0.01f)
        }
    }

    @Test
    fun customIdsAndExtensions() {
        val id = MentionSoundIds.newCustomId(1_700_000_000_000, -5)
        assertTrue(MentionSoundIds.isCustom(id))
        assertTrue(SoundLimits.isSupported("C:\\Users\\me\\Ping.MP3"))
        assertTrue(!SoundLimits.isSupported("/tmp/readme.txt"))
    }

    private fun le(value: Int) = byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte())
    private fun le16(value: Int) = byteArrayOf(value.toByte(), (value shr 8).toByte())
}
