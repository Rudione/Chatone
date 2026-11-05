package io.rudione.chatone.presentation.stream

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.rudione.chatone.data.remote.TwitchLiveStatusClient
import io.rudione.chatone.data.remote.stream.TwitchAdBreakResolver
import io.rudione.chatone.data.remote.stream.TwitchPlaybackClient
import io.rudione.chatone.domain.stream.StreamManifestResult
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamQualitySelection
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.Foundation.timeIntervalSinceDate
import platform.posix.getenv
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import platform.AVFoundation.currentTime
import platform.CoreMedia.CMTimeGetSeconds

@OptIn(ExperimentalForeignApi::class)
class IosTwitchLiveIntegrationTest {

    @Test
    fun liveTwitchChannelPlaysThroughPlaylistProxy() {
        if (getenv("CHATONE_IOS_INTEGRATION") == null) return
        val http = HttpClient(Darwin)
        val playback = TwitchPlaybackClient(httpClient = { http })
        val live = await { TwitchLiveStatusClient(http).liveStreams(CANDIDATES) }
        val login = live?.keys?.firstOrNull()
        assertNotNull(login, "no candidate channel is live")
        val result = await { playback.fetchManifest(login, "h264") }
        val manifest = (result as? StreamManifestResult.Ready)?.manifest
        assertNotNull(manifest, "manifest for $login: $result")
        val engine = IosStreamPlaybackEngine(TwitchAdBreakResolver(playback), "h264", IosPlaybackAudioSession())
        try {
            engine.load(manifest, StreamQualitySelection.Auto, lowLatency = true)
            assertTrue(spinUntil(40.0) { engine.status.value.phase == StreamPhase.PLAYING }, "$login: ${engine.status.value}")
            spinUntil(6.0) { false }
            val before = CMTimeGetSeconds(engine.player.currentTime())
            spinUntil(4.0) { false }
            val advanced = CMTimeGetSeconds(engine.player.currentTime()) - before
            assertTrue(advanced >= MIN_PROGRESS_SECONDS, "playback stalled, advanced $advanced s")
            val metrics = engine.metrics()
            val wallClock = metrics?.playbackWallClockMs
            assertNotNull(wallClock, "no program date time: $metrics")
            assertTrue((metrics.downloadHeight) > 0, "no video: $metrics")
            val latency = (Clock.System.now().toEpochMilliseconds() + manifest.clockOffsetMs - wallClock) / 1000.0
            assertTrue(latency in 0.0..MAX_LATENCY_SECONDS, "latency $latency s")
        } finally {
            engine.release()
            http.close()
        }
    }

    private fun <T> await(block: suspend () -> T): T {
        val deferred = CoroutineScope(Dispatchers.Main).async { block() }
        spinUntil(30.0) { deferred.isCompleted }
        return deferred.getCompleted()
    }

    private fun spinUntil(seconds: Double, condition: () -> Boolean): Boolean {
        val deadline = NSDate.dateWithTimeIntervalSinceNow(seconds)
        while (NSDate().timeIntervalSinceDate(deadline) < 0) {
            if (condition()) return true
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.1))
        }
        return condition()
    }

    private companion object {
        const val MIN_PROGRESS_SECONDS = 3.0
        const val MAX_LATENCY_SECONDS = 9.0
        val CANDIDATES = listOf("eslcs", "riotgames", "kaicenat", "xqc", "ibai", "jynxzi", "caseoh_", "tarik", "shroud", "summit1g", "zackrawrr", "hasanabi", "auronplay", "gaules", "esl_dota2", "lirik")
    }
}
