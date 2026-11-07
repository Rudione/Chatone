package io.rudione.chatone.domain.mention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MentionReadMarkTest {

    private fun ref(id: String, ts: Long) = MentionRef(id, ts, "user_$id", "User$id")

    @Test
    fun mentionsUpToTheWatermarkAreRead() {
        val mark = MentionReadMark.startingAt(100)
        assertTrue(mark.isRead(ref("a", 100)))
        assertFalse(mark.isRead(ref("b", 101)))
    }

    @Test
    fun readingEverythingMovesTheWatermarkAndForgetsIds() {
        val known = listOf(ref("a", 110), ref("b", 120))
        val next = MentionReadMark.startingAt(100).withRead(listOf("a", "b"), known)
        assertEquals(120, next.watermarkMs)
        assertTrue(next.readIds.isEmpty())
        assertTrue(next.unread(known).isEmpty())
    }

    @Test
    fun readingTheNewestKeepsOlderOnesUnread() {
        val known = listOf(ref("a", 110), ref("b", 120), ref("c", 130))
        val next = MentionReadMark.startingAt(100).withRead(listOf("c"), known)
        assertEquals(109, next.watermarkMs)
        assertEquals(setOf("c"), next.readIds)
        assertEquals(listOf("a", "b"), next.unread(known).map { it.messageId })
    }

    @Test
    fun watermarkStopsRightBeforeTheOldestUnread() {
        val known = listOf(ref("a", 110), ref("b", 120), ref("c", 130))
        val next = MentionReadMark.startingAt(100).withRead(listOf("a", "c"), known)
        assertEquals(119, next.watermarkMs)
        assertEquals(setOf("c"), next.readIds)
        assertEquals(listOf("b"), next.unread(known).map { it.messageId })
    }

    @Test
    fun sameMillisecondMentionsStaySeparate() {
        val known = listOf(ref("a", 110), ref("b", 110))
        val next = MentionReadMark.startingAt(100).withRead(listOf("a"), known)
        assertEquals(listOf("b"), next.unread(known).map { it.messageId })
    }

    @Test
    fun watermarkNeverMovesBack() {
        val known = listOf(ref("a", 50))
        val next = MentionReadMark.startingAt(100).withRead(listOf("a"), known)
        assertEquals(100, next.watermarkMs)
    }

    @Test
    fun idsOfMessagesThatLeftTheListAreDropped() {
        val next = MentionReadMark(100, setOf("gone")).withRead(listOf("a"), listOf(ref("a", 110)))
        assertEquals(110, next.watermarkMs)
        assertTrue(next.readIds.isEmpty())
    }

    @Test
    fun emptyReadIsANoOp() {
        val mark = MentionReadMark(100, setOf("x"))
        assertEquals(mark, mark.withRead(emptyList(), listOf(ref("x", 120))))
    }
}
