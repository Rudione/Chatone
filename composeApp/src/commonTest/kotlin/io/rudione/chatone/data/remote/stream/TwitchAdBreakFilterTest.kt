package io.rudione.chatone.data.remote.stream

import io.rudione.chatone.data.remote.stream.TwitchAdBreakTestPlaylists.live
import io.rudione.chatone.data.remote.stream.TwitchAdBreakTestPlaylists.withMidroll
import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.domain.stream.TwitchServingInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TwitchAdBreakFilterTest {

    private val variantUrl = "https://eun11.playlist.ttvnw.net/v1/playlist/720.m3u8"
    private val manifest = StreamManifest(
        channelLogin = "streamer",
        playlist = "#EXTM3U",
        variants = listOf(
            StreamVariant("720p60", "720p60", variantUrl, 3_400_000, 1280, 720, 60f, "avc1,mp4a.40.2")
        ),
        serving = TwitchServingInfo(),
        playSessionId = "session",
        clockOffsetMs = 0L
    )

    private class FakeSource(var playlist: String?) : StreamAdBreakSource {
        val requests = mutableListOf<String>()
        override suspend fun adFreeMediaPlaylist(
            channelLogin: String,
            variant: StreamVariant,
            supportedCodecs: String
        ): String? {
            requests += "$channelLogin/${variant.groupId}"
            return playlist
        }
    }

    @Test
    fun playlistsWithoutAdsPassThroughWithoutNetwork() = runTest {
        val source = FakeSource(live(6697, 0, host = "edge-backup"))
        val filter = TwitchAdBreakFilter(manifest, "h264", source)
        val primary = live(6697, 0)
        assertSame(primary, filter.filter(variantUrl, primary))
        assertTrue(source.requests.isEmpty())
    }

    @Test
    fun midrollIsReplacedWithTheAdFreeBackup() = runTest {
        val backup = live(6697, 0, host = "edge-backup")
        val source = FakeSource(backup)
        val filter = TwitchAdBreakFilter(manifest, "h264", source)
        assertEquals(backup, filter.filter(variantUrl, withMidroll(6697, 0)))
        assertEquals(listOf("streamer/720p60"), source.requests)
    }

    @Test
    fun staysOnTheBackupUntilThePrimaryTimelineMatchesAgain() = runTest {
        val source = FakeSource(live(6697, 0, host = "edge-backup"))
        val filter = TwitchAdBreakFilter(manifest, "h264", source)
        filter.filter(variantUrl, withMidroll(6697, 0))

        val renumberedPrimary = live(19, 20)
        source.playlist = live(6707, 20, host = "edge-backup")
        assertEquals(source.playlist, filter.filter(variantUrl, renumberedPrimary))

        val alignedPrimary = live(6708, 22)
        source.playlist = live(6708, 22, host = "edge-backup")
        assertSame(alignedPrimary, filter.filter(variantUrl, alignedPrimary))

        val later = live(6709, 24)
        val requestsBefore = source.requests.size
        assertSame(later, filter.filter(variantUrl, later))
        assertEquals(requestsBefore, source.requests.size)
    }

    @Test
    fun withoutABackupTheOriginalPlaylistIsKept() = runTest {
        val filter = TwitchAdBreakFilter(manifest, "h264", FakeSource(null))
        val withAds = withMidroll(6697, 0)
        assertSame(withAds, filter.filter(variantUrl, withAds))
    }

    @Test
    fun unknownPlaylistsAreNeverTouched() = runTest {
        val source = FakeSource(live(6697, 0, host = "edge-backup"))
        val filter = TwitchAdBreakFilter(manifest, "h264", source)
        val withAds = withMidroll(6697, 0)
        assertSame(withAds, filter.filter("data:application/x-mpegURL;base64,AAAA", withAds))
        assertTrue(source.requests.isEmpty())
    }
}
