package io.rudione.chatone.presentation.stream.lowlatency

import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.domain.stream.TwitchServingInfo
import io.rudione.chatone.presentation.stream.IosPlaybackAudioSession
import io.rudione.chatone.presentation.stream.PlaylistFetch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LowLatencyStreamPlaybackEngineTest {

    private val noAds = object : StreamAdBreakSource {
        override suspend fun adFreeMediaPlaylist(channelLogin: String, variant: StreamVariant, supportedCodecs: String): String? = null
    }
    private val player = FakeLowLatencyPlayer()
    private val gateway = StreamPlaylistGateway(source = { PlaylistFetch(404, null) })
    private var now = START_MS

    @Test
    fun loadsThroughTheLocalGatewayAndPlaysOnceTheSourceIsReady() = runTest {
        val engine = engine()
        engine.load(MANIFEST, StreamQualitySelection.Auto, lowLatency = true)
        assertTrue(player.loadedUrl.orEmpty().startsWith("http://127.0.0.1:"), "loaded ${player.loadedUrl}")
        assertTrue(player.loadedUrl.orEmpty().endsWith("/master.m3u8"))
        assertEquals("load(lowLatency=true)", player.calls.last())
        assertEquals(StreamPhase.BUFFERING, engine.status.value.phase)

        player.startPlaying()
        assertEquals(listOf("quality(null, seamless=false)", "play"), player.calls.takeLast(2))
        assertEquals(StreamPhase.PLAYING, engine.status.value.phase)
        engine.release()
    }

    @Test
    fun pinsTheChosenQualityBeforePlaybackStarts() = runTest {
        val engine = engine()
        engine.load(MANIFEST, StreamQualitySelection.Fixed("720p60"), lowLatency = false)
        assertEquals("load(lowLatency=false)", player.calls.last())
        player.emit(LowLatencyPlayerState.READY)
        assertEquals(listOf("quality(720p60, seamless=false)", "play"), player.calls.takeLast(2))
        engine.release()
    }

    @Test
    fun switchesQualityWithoutInterruptingPlayback() = runTest {
        val engine = playingEngine()
        engine.selectQuality(StreamQualitySelection.AudioOnly)
        assertEquals("quality(audio_only, seamless=true)", player.calls.last())
        assertTrue(engine.status.value.audioOnly)
        engine.selectQuality(StreamQualitySelection.Auto)
        assertEquals("quality(null, seamless=true)", player.calls.last())
        assertFalse(engine.status.value.audioOnly)
        engine.release()
    }

    @Test
    fun pauseKeepsTheStreamLoadedAndResumeJoinsLive() = runTest {
        val engine = playingEngine()
        engine.setPlaying(false)
        player.emit(LowLatencyPlayerState.IDLE)
        assertEquals("pause", player.calls.last { it == "pause" || it == "play" })
        assertEquals(StreamPhase.PAUSED, engine.status.value.phase)

        engine.setPlaying(true)
        assertEquals("play", player.calls.last())
        player.startPlaying()
        assertEquals(StreamPhase.PLAYING, engine.status.value.phase)
        assertEquals(1, player.calls.count { it.startsWith("load") }, "resume must not reload the stream")
        engine.release()
    }

    @Test
    fun followsPauseAndResumeMadeFromSystemControls() = runTest {
        val engine = playingEngine()
        now += 5_000
        player.emit(LowLatencyPlayerState.IDLE)
        assertEquals(StreamPhase.PAUSED, engine.status.value.phase)

        now += 5_000
        player.startPlaying()
        assertEquals(StreamPhase.PLAYING, engine.status.value.phase)
        engine.release()
    }

    @Test
    fun keepsBufferingWhenTheNetworkDropsInsteadOfShowingPause() = runTest {
        val engine = playingEngine()
        now += 5_000
        player.loseNetwork()
        player.emit(LowLatencyPlayerState.IDLE)
        assertEquals(StreamPhase.BUFFERING, engine.status.value.phase)
        engine.release()
    }

    @Test
    fun reportsTheEndOfTheStreamWhenThePlaylistIsGone() = runTest {
        val engine = playingEngine()
        player.fail(status = 404, reason = "ErrorNotAvailable")
        assertEquals(StreamPhase.ENDED, engine.status.value.phase)
        engine.release()
    }

    @Test
    fun mapsPlayerFailuresToRetryableAndTerminalKinds() = runTest {
        val expected = mapOf(
            Triple(0, "ErrorNetworkIO", "MasterPlaylist") to StreamErrorKind.NETWORK,
            Triple(0, "ErrorTimeout", "Segment") to StreamErrorKind.NETWORK,
            Triple(403, "ErrorAuthorization", "MediaPlaylist") to StreamErrorKind.UNKNOWN,
            Triple(0, "ErrorNotSupported", "Decode") to StreamErrorKind.UNSUPPORTED_CODEC,
            Triple(0, "ErrorInvalidData", "Segment") to StreamErrorKind.UNKNOWN
        )
        expected.forEach { (failure, kind) ->
            val engine = playingEngine()
            player.fail(failure.first, failure.second, failure.third)
            assertEquals(StreamPhase.ERROR, engine.status.value.phase, failure.toString())
            assertEquals(kind, engine.status.value.errorKind, failure.toString())
            engine.release()
        }
    }

    @Test
    fun reloadClearsAnEarlierFailure() = runTest {
        val engine = playingEngine()
        player.fail(status = 0, reason = "ErrorNetworkIO")
        engine.load(MANIFEST, StreamQualitySelection.Auto, lowLatency = true)
        assertEquals(StreamPhase.BUFFERING, engine.status.value.phase)
        assertNull(engine.status.value.errorKind)
        engine.release()
    }

    @Test
    fun reportsNetworkFailureWhenPlaybackNeverStarts() = runTest {
        val engine = engine()
        engine.load(MANIFEST, StreamQualitySelection.Auto, lowLatency = true)
        elapse(11_000)
        assertEquals(StreamPhase.BUFFERING, engine.status.value.phase)
        elapse(2_000)
        assertEquals(StreamPhase.ERROR, engine.status.value.phase)
        assertEquals(StreamErrorKind.NETWORK, engine.status.value.errorKind)
        engine.release()
    }

    @Test
    fun speedsUpGentlyWhenSlightlyBehindLive() = runTest {
        val engine = playingEngine()
        player.latencyMs = 3_000
        player.bufferedMs = 2_800
        elapse(1_500)
        val rate = player.calls.last { it.startsWith("rate") }.removePrefix("rate(").removeSuffix(")").toFloat()
        assertTrue(rate > 1f && rate <= 1.1f, "rate $rate")

        player.latencyMs = 1_600
        player.bufferedMs = 1_450
        elapse(1_500)
        assertEquals("rate(1.0)", player.calls.last { it.startsWith("rate") })
        engine.release()
    }

    @Test
    fun neverSpeedsUpWithAnAlmostEmptyBuffer() = runTest {
        val engine = playingEngine()
        player.latencyMs = 4_000
        player.bufferedMs = 400
        elapse(3_000)
        assertTrue(player.calls.none { it.startsWith("rate") && it != "rate(1.0)" }, player.calls.toString())
        engine.release()
    }

    @Test
    fun rejoinsLiveWhenFarBehindForSeveralSeconds() = runTest {
        val engine = playingEngine()
        val before = player.calls.size
        player.latencyMs = 9_000
        player.bufferedMs = 8_000
        elapse(2_000)
        assertFalse(player.calls.drop(before).contains("pause"), "a short lag must not interrupt playback")
        elapse(2_500)
        assertEquals(listOf("pause", "play"), player.calls.drop(before).filter { it == "pause" || it == "play" })
        engine.release()
    }

    @Test
    fun jumpToLiveOnRequestOnlyWhenActuallyBehind() = runTest {
        val engine = playingEngine()
        val before = player.calls.size
        player.latencyMs = 1_900
        engine.seekToLive()
        assertEquals(emptyList(), player.calls.drop(before))
        player.latencyMs = 6_000
        engine.seekToLive()
        assertEquals(listOf("pause", "play"), player.calls.drop(before).filter { it == "pause" || it == "play" })
        engine.release()
    }

    @Test
    fun metricsExposeLatencyOnTheServerClock() = runTest {
        val engine = playingEngine()
        player.latencyMs = 1_750
        player.qualityName = "720p60"
        player.videoWidth = 1280
        player.videoHeight = 720
        val metrics = assertNotNull(engine.metrics())
        assertEquals(now + CLOCK_OFFSET_MS - 1_750, metrics.playbackWallClockMs)
        assertEquals(1280, metrics.downloadWidth)
        assertTrue(metrics.lowLatencyActive)
        elapse(600)
        assertEquals(3_000_000L, engine.status.value.activeBandwidth)
        assertEquals(720, engine.status.value.activeHeight)
        engine.release()
    }

    @Test
    fun pinsTheLastWorkingQualityWhenFramesOfTheNewOneKeepFailing() = runTest {
        val engine = playingEngine()
        player.qualityName = "1080p60 (source)"
        player.decodedFrames = 600
        elapse(1_000)
        failDecoding("720p60")
        assertEquals("quality(1080p60 (source), seamless=false)", player.calls.last { it.startsWith("quality") })
        assertEquals(StreamPhase.PLAYING, engine.status.value.phase)
        engine.release()
    }

    @Test
    fun aBriefDecodeHiccupDoesNotChangeQuality() = runTest {
        val engine = playingEngine()
        val before = player.calls.count { it.startsWith("quality") }
        player.qualityName = "720p60"
        player.failedFrames = 3
        elapse(500)
        player.decodedFrames = 120
        elapse(3_000)
        assertEquals(before, player.calls.count { it.startsWith("quality") })
        engine.release()
    }

    @Test
    fun keepsAvoidingTheBrokenQualityAfterAReloadOfTheSameChannel() = runTest {
        val engine = playingEngine()
        failDecoding("720p60")
        engine.load(MANIFEST, StreamQualitySelection.Auto, lowLatency = true)
        player.failedFrames = 0
        player.emit(LowLatencyPlayerState.READY)
        assertEquals("quality(1080p60 (source), seamless=false)", player.calls.last { it.startsWith("quality") })
        engine.release()
    }

    @Test
    fun givesAnotherChannelAFreshStart() = runTest {
        val engine = playingEngine()
        failDecoding("720p60")
        engine.load(manifest("another"), StreamQualitySelection.Auto, lowLatency = true)
        player.failedFrames = 0
        player.emit(LowLatencyPlayerState.READY)
        assertEquals("quality(null, seamless=false)", player.calls.last { it.startsWith("quality") })
        engine.release()
    }

    @Test
    fun reportsAnUnsupportedStreamWhenNoQualityCanBeDecoded() = runTest {
        val engine = playingEngine()
        failDecoding("720p60")
        failDecoding("1080p60 (source)")
        assertEquals(StreamPhase.ERROR, engine.status.value.phase)
        assertEquals(StreamErrorKind.UNSUPPORTED_CODEC, engine.status.value.errorKind)
        engine.release()
    }

    @Test
    fun ignoresDecodeFailuresWhileListeningToAudioOnly() = runTest {
        val engine = playingEngine()
        engine.selectQuality(StreamQualitySelection.AudioOnly)
        failDecoding("audio_only")
        assertEquals("quality(audio_only, seamless=true)", player.calls.last { it.startsWith("quality") })
        engine.release()
    }

    @Test
    fun releaseClosesThePlayerAndStopsServingPlaylists() = runTest {
        val engine = playingEngine()
        val url = assertNotNull(player.loadedUrl)
        val port = url.removePrefix("http://127.0.0.1:").substringBefore('/').toInt()
        assertEquals(200, LoopbackClient.get(port, url.substringAfter(":$port"))?.status)
        engine.release()
        assertTrue(player.closed)
        assertTrue(LoopbackClient.awaitRefused(port))
        assertEquals(StreamPhase.IDLE, engine.status.value.phase)
    }

    private fun TestScope.engine() = LowLatencyStreamPlaybackEngine(
        player = player,
        gateway = gateway,
        adBreakSource = noAds,
        supportedCodecs = "h264",
        audioSession = IosPlaybackAudioSession(),
        scope = backgroundScope,
        nowMs = { now }
    )

    private fun TestScope.playingEngine(): LowLatencyStreamPlaybackEngine {
        val engine = engine()
        engine.load(MANIFEST, StreamQualitySelection.Auto, lowLatency = true)
        player.latencyMs = 1_600
        player.bufferedMs = 1_450
        player.startPlaying()
        return engine
    }

    private fun TestScope.failDecoding(quality: String) {
        player.qualityName = quality
        repeat(4) {
            player.failedFrames += 30
            player.decodedFrames += 30
            elapse(500)
        }
    }

    private fun TestScope.elapse(millis: Long) {
        var remaining = millis
        while (remaining > 0) {
            val step = minOf(STEP_MS, remaining)
            now += step
            advanceTimeBy(step)
            runCurrent()
            remaining -= step
        }
    }

    private companion object {
        const val START_MS = 1_794_000_000_000L
        const val CLOCK_OFFSET_MS = 250L
        const val STEP_MS = 500L
        val MANIFEST = manifest("channel")

        fun manifest(channelLogin: String) = StreamManifest(
            channelLogin = channelLogin,
            playlist = """
                #EXTM3U
                #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="chunked",NAME="1080p60 (source)",AUTOSELECT=YES,DEFAULT=YES
                #EXT-X-STREAM-INF:BANDWIDTH=8000000,RESOLUTION=1920x1080,VIDEO="chunked",FRAME-RATE=60.000
                https://video.ttvnw.net/v1/playlist/source.m3u8
                #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="720p60",NAME="720p60",AUTOSELECT=YES,DEFAULT=YES
                #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,VIDEO="720p60",FRAME-RATE=60.000
                https://video.ttvnw.net/v1/playlist/720.m3u8
                #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="audio_only",NAME="audio_only",AUTOSELECT=NO,DEFAULT=NO
                #EXT-X-STREAM-INF:BANDWIDTH=160000,CODECS="mp4a.40.2",VIDEO="audio_only"
                https://video.ttvnw.net/v1/playlist/audio.m3u8
            """.trimIndent(),
            variants = listOf(
                StreamVariant("chunked", "1080p60 (source)", "https://video.ttvnw.net/v1/playlist/source.m3u8", 8_000_000, 1920, 1080, 60f, "avc1.64002A,mp4a.40.2"),
                StreamVariant("720p60", "720p60", "https://video.ttvnw.net/v1/playlist/720.m3u8", 3_000_000, 1280, 720, 60f, "avc1.4D401F,mp4a.40.2"),
                StreamVariant("audio_only", "audio_only", "https://video.ttvnw.net/v1/playlist/audio.m3u8", 160_000, 0, 0, 0f, "mp4a.40.2")
            ),
            serving = TwitchServingInfo(),
            playSessionId = "test",
            clockOffsetMs = CLOCK_OFFSET_MS
        )
    }
}
