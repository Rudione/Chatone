package io.rudione.chatone.presentation.stream

import com.russhwolf.settings.PreferencesSettings
import io.rudione.chatone.data.repository.StreamPlayerPreferencesRepository
import io.rudione.chatone.domain.stream.PlaybackMetrics
import io.rudione.chatone.domain.stream.PlaybackStatus
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamManifestResult
import io.rudione.chatone.domain.stream.StreamManifestSource
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.domain.stream.TwitchServingInfo
import io.rudione.chatone.domain.stream.UnsupportedStreamPlaybackEngineFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StreamPlayerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val node = Preferences.userRoot().node("chatone-test-stream-${System.nanoTime()}")
    private val preferences = StreamPlayerPreferencesRepository(PreferencesSettings(node))
    private val source = FakeManifestSource()
    private val factory = FakeEngineFactory()
    private var now = 10_000L

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        node.removeNode()
    }

    private fun TestScope.withViewModel(
        engineFactory: StreamPlaybackEngineFactory = factory,
        block: TestScope.(StreamPlayerViewModel) -> Unit
    ) {
        val viewModel = StreamPlayerViewModel(source, preferences, engineFactory, nowMs = { now })
        try {
            block(viewModel)
        } finally {
            viewModel.onEvent(StreamPlayerEvent.Close)
            runCurrent()
        }
    }

    @Test
    fun openingALiveChannelStartsPlaybackWithSavedQuality() = runTest(dispatcher) {
        preferences.save(preferences.load().copy(preferredQuality = "720p60"))
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("Streamer"))
            assertEquals(StreamPhase.LOADING, viewModel.state.value.phase)
            runCurrent()

            val engine = factory.created.single()
            assertEquals("streamer", source.calls.single())
            assertEquals(StreamQualitySelection.Fixed("720p60"), engine.loads.single().second)
            assertTrue(engine.loads.single().third)

            engine.status.value = PlaybackStatus(phase = StreamPhase.PLAYING, videoWidth = 1280, videoHeight = 720, activeHeight = 720, activeBandwidth = 3_400_000)
            runCurrent()
            val state = viewModel.state.value
            assertEquals(StreamPhase.PLAYING, state.phase)
            assertEquals("720p60", state.activeVariant?.groupId)
            assertEquals(listOf("chunked", "720p60", "audio_only"), state.variants.map { it.groupId })
        }
    }

    @Test
    fun offlineChannelDoesNotCreateAnEngine() = runTest(dispatcher) {
        source.result = StreamManifestResult.Offline
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            assertEquals(StreamPhase.OFFLINE, viewModel.state.value.phase)
            assertTrue(factory.created.isEmpty())

            source.result = StreamManifestResult.Ready(manifest("streamer"))
            viewModel.onEvent(StreamPlayerEvent.StreamWentLive)
            runCurrent()
            assertEquals(1, factory.created.size)
        }
    }

    @Test
    fun networkFailuresRetryWithBackoffThenGiveUp() = runTest(dispatcher) {
        source.result = StreamManifestResult.Failed(StreamErrorKind.NETWORK)
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            assertEquals(StreamPhase.LOADING, viewModel.state.value.phase)
            repeat(5) { advanceTimeBy(25_000) }
            runCurrent()
            assertEquals(6, source.calls.size)
            assertEquals(StreamPhase.ERROR, viewModel.state.value.phase)
            assertEquals(StreamErrorKind.NETWORK, viewModel.state.value.errorKind)
        }
    }

    @Test
    fun terminalErrorsAreNotRetried() = runTest(dispatcher) {
        source.result = StreamManifestResult.Failed(StreamErrorKind.GEOBLOCKED)
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(1, source.calls.size)
            assertEquals(StreamErrorKind.GEOBLOCKED, viewModel.state.value.errorKind)
        }
    }

    @Test
    fun followingAnotherChannelSwitchesAndLeavingClosesThePlayer() = runTest(dispatcher) {
        preferences.save(preferences.load().copy(autoOpen = false))
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("ignored"))
            runCurrent()
            assertTrue(source.calls.isEmpty())

            viewModel.onEvent(StreamPlayerEvent.Open("first"))
            runCurrent()
            source.result = StreamManifestResult.Ready(manifest("second"))
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("second"))
            runCurrent()
            val engine = factory.created.single()
            assertEquals(listOf("first", "second"), source.calls)
            assertEquals("second", engine.loads.last().first.channelLogin)

            viewModel.onEvent(StreamPlayerEvent.FollowChannel(null))
            runCurrent()
            assertFalse(viewModel.state.value.isOpen)
            assertTrue(engine.released)
        }
    }

    @Test
    fun followingAnOfflineChannelKeepsTheStreamAndFlagsTheForeignChat() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("first"))
            runCurrent()
            val engine = factory.created.single()
            engine.status.value = PlaybackStatus(phase = StreamPhase.PLAYING)
            runCurrent()

            source.result = StreamManifestResult.Offline
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("second"))
            runCurrent()
            val state = viewModel.state.value
            assertEquals("first", state.channelLogin)
            assertEquals("second", state.chatChannelLogin)
            assertTrue(state.showsOtherChannelNotice)
            assertEquals(1, engine.loads.size)
            assertFalse(engine.released)

            viewModel.onEvent(StreamPlayerEvent.DismissChannelNotice)
            assertFalse(viewModel.state.value.showsOtherChannelNotice)

            viewModel.onEvent(StreamPlayerEvent.FollowChannel("first"))
            runCurrent()
            assertNull(viewModel.state.value.chatChannelLogin)
            assertEquals(listOf("first", "second"), source.calls)
        }
    }

    @Test
    fun aBrokenStreamIsReplacedInsteadOfKeptForAnOfflineChat() = runTest(dispatcher) {
        source.result = StreamManifestResult.Failed(StreamErrorKind.GEOBLOCKED)
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("first"))
            runCurrent()
            source.result = StreamManifestResult.Offline
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("second"))
            runCurrent()
            val state = viewModel.state.value
            assertEquals("second", state.channelLogin)
            assertEquals(StreamPhase.OFFLINE, state.phase)
            assertNull(state.chatChannelLogin)
        }
    }

    @Test
    fun qualityChoiceIsForwardedAndRemembered() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            viewModel.onEvent(StreamPlayerEvent.SelectQuality(StreamQualitySelection.AudioOnly))
            assertEquals(listOf<StreamQualitySelection>(StreamQualitySelection.AudioOnly), factory.created.single().selections)
            assertEquals("audio_only", preferences.load().preferredQuality)
        }
    }

    @Test
    fun latencyUsesTheServerCorrectedClock() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            val engine = factory.created.single()
            engine.metrics = metrics(playbackWallClockMs = 8_750L)
            engine.status.value = PlaybackStatus(phase = StreamPhase.PLAYING)
            runCurrent()
            advanceTimeBy(1_001)
            runCurrent()
            assertEquals(1.75f, viewModel.latencySeconds.value)

            viewModel.onEvent(StreamPlayerEvent.SetStatsVisible(true))
            val stats = viewModel.stats.value
            assertEquals("serving", stats?.servingId)
            assertEquals("1280x720", stats?.downloadResolution)
        }
    }

    @Test
    fun goingToBackgroundPausesAndComingBackJumpsToLive() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            val engine = factory.created.single()
            engine.status.value = PlaybackStatus(phase = StreamPhase.PLAYING)
            runCurrent()

            viewModel.onEvent(StreamPlayerEvent.AppVisibilityChanged(false))
            assertFalse(engine.playbackRequested)
            viewModel.onEvent(StreamPlayerEvent.AppVisibilityChanged(true))
            assertTrue(engine.playbackRequested)
            assertEquals(1, engine.seekedToLive)
        }
    }

    @Test
    fun followingALiveChannelOpensThePlayerByDefault() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("Streamer"))
            runCurrent()
            val state = viewModel.state.value
            assertEquals("streamer", state.channelLogin)
            assertEquals(1, factory.created.size)
            assertEquals("streamer", factory.created.single().loads.single().first.channelLogin)
        }
    }

    @Test
    fun offlineChannelOpensOnlyOnceItGoesLive() = runTest(dispatcher) {
        source.result = StreamManifestResult.Offline
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("streamer"))
            runCurrent()
            assertFalse(viewModel.state.value.isOpen)
            assertTrue(factory.created.isEmpty())

            source.result = StreamManifestResult.Ready(manifest("streamer"))
            viewModel.onEvent(StreamPlayerEvent.StreamWentLive)
            runCurrent()
            assertEquals("streamer", viewModel.state.value.channelLogin)
            assertEquals(1, factory.created.size)
        }
    }

    @Test
    fun closingThePlayerKeepsItClosedOnOtherChannels() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("first"))
            runCurrent()
            viewModel.onEvent(StreamPlayerEvent.Close)
            assertFalse(preferences.load().autoOpen)

            source.result = StreamManifestResult.Ready(manifest("second"))
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("second"))
            runCurrent()
            assertFalse(viewModel.state.value.isOpen)
            assertEquals(listOf("first"), source.calls)

            viewModel.onEvent(StreamPlayerEvent.Open("second"))
            runCurrent()
            assertTrue(preferences.load().autoOpen)
            assertEquals("second", viewModel.state.value.channelLogin)
        }
    }

    @Test
    fun backDismissHidesThePlayerOnlyForTheCurrentChannel() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("first"))
            runCurrent()
            viewModel.onEvent(StreamPlayerEvent.Dismiss)
            assertFalse(viewModel.state.value.isOpen)
            assertTrue(preferences.load().autoOpen)

            viewModel.onEvent(StreamPlayerEvent.StreamWentLive)
            runCurrent()
            assertFalse(viewModel.state.value.isOpen)

            source.result = StreamManifestResult.Ready(manifest("second"))
            viewModel.onEvent(StreamPlayerEvent.FollowChannel("second"))
            runCurrent()
            assertEquals("second", viewModel.state.value.channelLogin)
        }
    }

    @Test
    fun openingWithAQualityPlaysAndRemembersIt() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer", StreamPlayerPreferences.QUALITY_AUDIO_ONLY))
            runCurrent()
            val engine = factory.created.single()
            assertEquals(StreamQualitySelection.AudioOnly, engine.loads.single().second)
            assertEquals(StreamPlayerPreferences.QUALITY_AUDIO_ONLY, preferences.load().preferredQuality)

            engine.status.value = PlaybackStatus(phase = StreamPhase.PLAYING)
            runCurrent()
            viewModel.onEvent(StreamPlayerEvent.Open("streamer", "720p60"))
            runCurrent()
            assertEquals(listOf<StreamQualitySelection>(StreamQualitySelection.Fixed("720p60")), engine.selections)
            assertEquals(1, engine.loads.size)
        }
    }

    @Test
    fun reloadRefreshesTheManifestWithoutTearingDownPlayback() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            val engine = factory.created.single()
            engine.status.value = PlaybackStatus(phase = StreamPhase.PLAYING)
            runCurrent()
            engine.setPlaying(false)

            viewModel.onEvent(StreamPlayerEvent.Reload)
            assertEquals(1, engine.seekedToLive)
            assertTrue(engine.playbackRequested)
            runCurrent()

            assertEquals(1, factory.created.size)
            assertFalse(engine.released)
            assertEquals(2, engine.loads.size)
            assertEquals(listOf("streamer", "streamer"), source.calls)
            assertEquals(StreamPhase.PLAYING, viewModel.state.value.phase)
        }
    }

    @Test
    fun failedReloadKeepsTheCurrentSession() = runTest(dispatcher) {
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            val engine = factory.created.single()
            engine.status.value = PlaybackStatus(phase = StreamPhase.PLAYING)
            runCurrent()

            source.result = StreamManifestResult.Failed(StreamErrorKind.NETWORK)
            viewModel.onEvent(StreamPlayerEvent.Reload)
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(1, engine.loads.size)
            assertFalse(engine.released)
            assertEquals(2, source.calls.size)
            assertEquals(StreamPhase.PLAYING, viewModel.state.value.phase)
        }
    }

    @Test
    fun reloadReopensAStreamThatNeedsAttention() = runTest(dispatcher) {
        source.result = StreamManifestResult.Offline
        withViewModel { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            assertEquals(StreamPhase.OFFLINE, viewModel.state.value.phase)

            source.result = StreamManifestResult.Ready(manifest("streamer"))
            viewModel.onEvent(StreamPlayerEvent.Reload)
            runCurrent()

            assertEquals(1, factory.created.size)
            assertEquals(1, factory.created.single().loads.size)
        }
    }

    @Test
    fun unsupportedPlatformsIgnoreOpenRequests() = runTest(dispatcher) {
        withViewModel(engineFactory = UnsupportedStreamPlaybackEngineFactory) { viewModel ->
            viewModel.onEvent(StreamPlayerEvent.Open("streamer"))
            runCurrent()
            assertNull(viewModel.state.value.channelLogin)
            assertTrue(source.calls.isEmpty())
        }
    }

    private fun manifest(login: String) = StreamManifest(
        channelLogin = login,
        playlist = "#EXTM3U",
        variants = listOf(
            StreamVariant("chunked", "1080p60 (source)", "https://x/1.m3u8", 6_000_000, 1920, 1080, 60f, "avc1,mp4a.40.2"),
            StreamVariant("720p60", "720p60", "https://x/2.m3u8", 3_400_000, 1280, 720, 60f, "avc1,mp4a.40.2"),
            StreamVariant("audio_only", "audio_only", "https://x/3.m3u8", 160_000, 0, 0, 0f, "mp4a.40.2")
        ),
        serving = TwitchServingInfo(servingId = "serving"),
        playSessionId = "session",
        clockOffsetMs = 500L
    )

    private fun metrics(playbackWallClockMs: Long) = PlaybackMetrics(
        downloadWidth = 1280,
        downloadHeight = 720,
        renderWidth = 1280,
        renderHeight = 720,
        downloadBitrateKbps = 3000,
        bandwidthEstimateKbps = 9000,
        framesPerSecond = 60f,
        droppedFrames = 0,
        bufferMs = 1_500,
        playbackWallClockMs = playbackWallClockMs,
        codecs = "avc1,mp4a.40.2",
        lowLatencyActive = true,
        renderSurface = "Test",
        backendVersion = "Test"
    )

    private inner class FakeManifestSource : StreamManifestSource {
        var result: StreamManifestResult = StreamManifestResult.Ready(manifest("streamer"))
        val calls = mutableListOf<String>()

        override suspend fun fetchManifest(channelLogin: String, supportedCodecs: String): StreamManifestResult {
            calls += channelLogin
            return result
        }
    }

    private class FakeEngineFactory : StreamPlaybackEngineFactory {
        val created = mutableListOf<FakeEngine>()
        override val isSupported: Boolean = true
        override val supportedCodecs: String = "h264"
        override fun create(): StreamPlaybackEngine = FakeEngine().also(created::add)
    }

    private class FakeEngine : StreamPlaybackEngine {
        override val status = MutableStateFlow(PlaybackStatus())
        val loads = mutableListOf<Triple<StreamManifest, StreamQualitySelection, Boolean>>()
        val selections = mutableListOf<StreamQualitySelection>()
        var playbackRequested = true
        var released = false
        var seekedToLive = 0
        var metrics: PlaybackMetrics? = null

        override fun load(manifest: StreamManifest, selection: StreamQualitySelection, lowLatency: Boolean) {
            loads += Triple(manifest, selection, lowLatency)
            playbackRequested = true
        }

        override fun selectQuality(selection: StreamQualitySelection) {
            selections += selection
        }

        override fun setPlaying(playing: Boolean) {
            playbackRequested = playing
        }

        override fun setMuted(muted: Boolean) = Unit

        override fun seekToLive() {
            seekedToLive++
        }

        override fun metrics(): PlaybackMetrics? = metrics

        override fun release() {
            released = true
        }
    }
}
