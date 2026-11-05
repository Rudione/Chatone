package io.rudione.chatone.presentation.stream

import io.rudione.chatone.domain.media.MediaSource
import io.rudione.chatone.domain.media.PlaybackPhase
import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.domain.stream.TwitchServingInfo
import io.rudione.chatone.presentation.media.IosMediaPlayback
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.timeIntervalSinceDate
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.getenv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosPlaybackIntegrationTest {

    private val noAds = object : StreamAdBreakSource {
        override suspend fun adFreeMediaPlaylist(channelLogin: String, variant: StreamVariant, supportedCodecs: String): String? = null
    }

    private val enabled: Boolean get() = getenv(INTEGRATION_FLAG) != null

    @Test
    fun livePlaylistPlaysThroughResourceLoaderInAutoAndFixedQuality() {
        if (!enabled) return
        val master = fetchText(MASTER_URL)
        val absolute = absolutizeMediaPlaylist(master, MASTER_URL)
        val variantUrl = absolute.lineSequence().first { it.startsWith("https://") && it.endsWith(".m3u8") }
        val manifest = StreamManifest(
            channelLogin = "bipbop",
            playlist = absolute,
            variants = listOf(StreamVariant("fixed", "fixed", variantUrl, 1_000_000, 640, 480, 30f, "avc1")),
            serving = TwitchServingInfo(),
            playSessionId = "test",
            clockOffsetMs = 0
        )
        val engine = IosStreamPlaybackEngine(noAds, "h264", IosPlaybackAudioSession())
        try {
            engine.load(manifest, StreamQualitySelection.Auto, lowLatency = false)
            assertTrue(spinUntil { engine.status.value.phase == StreamPhase.PLAYING }, "auto: ${engine.status.value}")
            assertTrue((engine.metrics()?.downloadWidth ?: 0) > 0, "auto metrics: ${engine.metrics()}")
            var frame: IntArray? = null
            assertTrue(spinUntil { engine.sampleFrame(64, 36).also { frame = it } != null }, "no video frame for ambient light")
            assertTrue(frame!!.any { it and 0xFFFFFF != 0 }, "ambient frame is black")

            engine.selectQuality(StreamQualitySelection.Fixed("fixed"))
            assertTrue(spinUntil { engine.status.value.phase == StreamPhase.PLAYING }, "fixed: ${engine.status.value}")

            engine.setPlaying(false)
            spinUntil(2.0) { false }
            assertEquals(StreamPhase.PAUSED, engine.status.value.phase)
        } finally {
            engine.release()
        }
    }

    @Test
    fun mediaPlaybackReachesReadyAndReportsDuration() {
        if (!enabled) return
        val playback = IosMediaPlayback(IosPlaybackAudioSession())
        try {
            playback.load(MediaSource("vod", VOD_URL, isStream = true), startMs = 0)
            assertTrue(spinUntil { playback.playing.value && playback.phase.value == PlaybackPhase.READY }, "vod: ${playback.phase.value}")
            assertTrue(playback.durationMs() > 0, "duration ${playback.durationMs()}")
            playback.seekTo(5_000)
            assertTrue(spinUntil { playback.positionMs() >= 5_000 }, "position ${playback.positionMs()}")
        } finally {
            playback.release()
        }
    }

    private fun spinUntil(seconds: Double = 40.0, condition: () -> Boolean): Boolean {
        val deadline = NSDate.dateWithTimeIntervalSinceNow(seconds)
        while (NSDate().timeIntervalSinceDate(deadline) < 0) {
            if (condition()) return true
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.1))
        }
        return condition()
    }

    private fun fetchText(url: String): String =
        NSString.stringWithContentsOfURL(NSURL.URLWithString(url)!!, encoding = NSUTF8StringEncoding, error = null)!!

    private companion object {
        const val INTEGRATION_FLAG = "CHATONE_IOS_INTEGRATION"
        const val MASTER_URL = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"
        const val VOD_URL = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/gear2/prog_index.m3u8"
    }
}
