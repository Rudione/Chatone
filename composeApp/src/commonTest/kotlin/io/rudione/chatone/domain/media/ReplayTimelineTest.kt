package io.rudione.chatone.domain.media

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayTimelineTest {

    private fun line(id: String, offset: Int) =
        ReplayLine(id = id, offsetSeconds = offset, userId = "u", login = "u", displayName = "u", colorHex = null, text = id)

    private fun page(vararg offsets: Int, prefix: String = "m") = offsets.mapIndexed { i, o -> line("$prefix$o-$i", o) }

    @Test
    fun firstFetchStartsBeforeThePlayhead() {
        val timeline = ReplayTimeline()
        assertEquals(70, timeline.nextFetch(100))
        assertFalse(timeline.primed)
    }

    @Test
    fun keepsFetchingUntilTheLookaheadIsCovered() {
        val timeline = ReplayTimeline()
        val from = timeline.nextFetch(100)!!
        timeline.accept(from, page(70, 80, 95))
        assertEquals(95, timeline.nextFetch(100))
        timeline.accept(95, page(95, 110, 130, prefix = "n"))
        assertNull(timeline.nextFetch(100))
        assertTrue(timeline.primed)
    }

    @Test
    fun aFullPageInsideOneSecondStillMovesForward() {
        val timeline = ReplayTimeline()
        timeline.nextFetch(30)
        timeline.accept(0, page(0, 0, 0))
        assertEquals(1, timeline.nextFetch(30))
    }

    @Test
    fun anEmptyPageSkipsAhead() {
        val timeline = ReplayTimeline()
        timeline.nextFetch(30)
        timeline.accept(0, emptyList())
        assertNull(timeline.nextFetch(30))
        assertEquals(60, timeline.nextFetch(45))
    }

    @Test
    fun linesRepeatedAcrossPagesAppearOnce() {
        val timeline = ReplayTimeline()
        timeline.nextFetch(100)
        val first = page(70, 95)
        timeline.accept(70, first)
        val fresh = timeline.accept(95, listOf(first.last(), line("x", 96)))
        assertEquals(listOf("x"), fresh.map { it.id })
        assertEquals(3, timeline.lines.size)
    }

    @Test
    fun seekingPastTheLoadedChatStartsOver() {
        val timeline = ReplayTimeline()
        timeline.nextFetch(100)
        timeline.accept(70, page(70, 130))
        assertEquals(570, timeline.nextFetch(600))
        assertTrue(timeline.lines.isEmpty())
        assertFalse(timeline.primed)
    }

    @Test
    fun seekingBeforeTheLoadedChatStartsOver() {
        val timeline = ReplayTimeline()
        timeline.nextFetch(100)
        timeline.accept(70, page(70, 130))
        assertEquals(0, timeline.nextFetch(10))
        assertTrue(timeline.lines.isEmpty())
    }

    @Test
    fun seekingInsideTheLoadedChatKeepsIt() {
        val timeline = ReplayTimeline()
        timeline.nextFetch(100)
        timeline.accept(70, page(70, 90, 130))
        assertNull(timeline.nextFetch(85))
        assertNull(timeline.nextFetch(105))
        assertEquals(3, timeline.lines.size)
    }

    @Test
    fun aStalePageIsIgnored() {
        val timeline = ReplayTimeline()
        timeline.nextFetch(100)
        assertTrue(timeline.accept(40, page(40, 50)).isEmpty())
        assertTrue(timeline.lines.isEmpty())
        assertEquals(70, timeline.nextFetch(100))
    }

    @Test
    fun oldLinesAreTrimmedButUpcomingOnesStay() {
        val timeline = ReplayTimeline(keepBehind = 2)
        timeline.nextFetch(100)
        timeline.accept(70, page(70, 80, 90, 100, 110, 120))
        assertEquals(listOf(90, 100, 110, 120), timeline.lines.map { it.offsetSeconds })
        assertEquals(59, timeline.nextFetch(89))
    }

    @Test
    fun linesOfOneSecondAreSpreadAcrossIt() {
        val times = page(5, 5, 5, 6).revealTimesMs()
        assertContentEquals(longArrayOf(5_000, 5_333, 5_666, 6_000), times)
    }
}
