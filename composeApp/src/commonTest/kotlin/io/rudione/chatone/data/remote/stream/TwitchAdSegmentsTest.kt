package io.rudione.chatone.data.remote.stream

import io.rudione.chatone.data.remote.stream.TwitchAdBreakTestPlaylists.live
import io.rudione.chatone.data.remote.stream.TwitchAdBreakTestPlaylists.withMidroll
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TwitchAdSegmentsTest {

    @Test
    fun liveSegmentsAreNotAds() {
        assertFalse(TwitchAdSegments.hasAds(live(firstSequence = 6697, firstSecond = 0)))
    }

    @Test
    fun stitchedSegmentsAreAds() {
        assertTrue(TwitchAdSegments.hasAds(withMidroll(firstSequence = 6697, firstSecond = 0)))
    }

    @Test
    fun sessionsOnTheSameTimelineShareSequenceNumbers() {
        val primary = live(firstSequence = 6697, firstSecond = 0)
        val backup = live(firstSequence = 6698, firstSecond = 2, host = "edge-backup")
        assertTrue(TwitchAdSegments.shareTimeline(primary, backup))
    }

    @Test
    fun sessionWithItsOwnNumberingDoesNotShareTheTimeline() {
        val renumbered = live(firstSequence = 19, firstSecond = 0)
        val global = live(firstSequence = 6697, firstSecond = 0)
        assertFalse(TwitchAdSegments.shareTimeline(renumbered, global))
    }

    @Test
    fun sameSequenceAtADifferentTimeIsNotTheSameTimeline() {
        val first = live(firstSequence = 6697, firstSecond = 0)
        val shifted = live(firstSequence = 6697, firstSecond = 30)
        assertFalse(TwitchAdSegments.shareTimeline(first, shifted))
    }

    @Test
    fun onlyAbsoluteHttpsSegmentsAreAccepted() {
        assertTrue(TwitchAdSegments.hasOnlyAbsoluteHttpsSegments(live(firstSequence = 1, firstSecond = 0)))
        val relative = live(firstSequence = 1, firstSecond = 0).replace("https://edge-live.hls.ttvnw.net/v1/segment/1.ts", "1.ts")
        assertFalse(TwitchAdSegments.hasOnlyAbsoluteHttpsSegments(relative))
        assertFalse(TwitchAdSegments.hasOnlyAbsoluteHttpsSegments("#EXTM3U\n#EXT-X-TARGETDURATION:5\n"))
    }
}
