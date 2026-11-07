package io.rudione.chatone.presentation.chat.mentions

import io.rudione.chatone.domain.mention.MentionReadMark
import io.rudione.chatone.domain.mention.MentionReadStore
import io.rudione.chatone.domain.mention.MentionRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MentionJumpStateTest {

    private class FakeStore(initial: Map<String, MentionReadMark> = emptyMap()) : MentionReadStore {
        val marks = initial.toMutableMap()
        var saves = 0

        override fun mark(channelLogin: String): MentionReadMark? = marks[channelLogin]

        override fun save(channelLogin: String, mark: MentionReadMark) {
            saves++
            marks[channelLogin] = mark
        }
    }

    private var now = 1_000L

    private fun ref(id: String, ts: Long, login: String = "user_$id") = MentionRef(id, ts, login, login.uppercase())

    private fun state(store: MentionReadStore) = MentionJumpState("chan", store, clock = { now }, lingerMs = 10_000L)

    @Test
    fun firstVisitStartsFromTheNewestMessageSoHistoryIsNotCounted() {
        val store = FakeStore()
        val jump = state(store)
        jump.sync(listOf(ref("old", 100)), newestTimestampMs = 500)
        assertEquals(0, jump.count)
        assertEquals(500, store.marks["chan"]?.watermarkMs)
        jump.sync(listOf(ref("old", 100), ref("new", 600)), newestTimestampMs = 600)
        assertEquals(listOf("new"), jump.pending.map { it.messageId })
    }

    @Test
    fun savedMarkMakesMissedMentionsPending() {
        val store = FakeStore(mapOf("chan" to MentionReadMark(200)))
        val jump = state(store)
        jump.sync(listOf(ref("seen", 150), ref("missed1", 250), ref("missed2", 300)), newestTimestampMs = 320)
        assertEquals(2, jump.count)
        assertNull(jump.lingerEndsAtMs)
    }

    @Test
    fun seenWhileFollowingLingersForTenSecondsThenGoes() {
        val jump = state(FakeStore(mapOf("chan" to MentionReadMark(0))))
        jump.sync(listOf(ref("a", 10)), newestTimestampMs = 10)
        jump.markSeen(setOf("a"), live = true)
        assertTrue(jump.pending.isEmpty())
        assertEquals(1, jump.count)
        assertEquals(11_000, jump.lingerEndsAtMs)
        now = 10_999
        jump.expire()
        assertEquals(1, jump.count)
        now = 11_000
        jump.expire()
        assertEquals(0, jump.count)
    }

    @Test
    fun seenWhileScrolledIsConsumedWithoutLingering() {
        val jump = state(FakeStore(mapOf("chan" to MentionReadMark(0))))
        jump.sync(listOf(ref("a", 10), ref("b", 20)), newestTimestampMs = 20)
        jump.markSeen(setOf("a"), live = false)
        assertEquals(listOf("b"), jump.pending.map { it.messageId })
        assertEquals(1, jump.count)
    }

    @Test
    fun readMentionsNeverComeBackAfterReload() {
        val store = FakeStore(mapOf("chan" to MentionReadMark(0)))
        val first = state(store)
        val mentions = listOf(ref("a", 10), ref("b", 20), ref("c", 30))
        first.sync(mentions, newestTimestampMs = 30)
        first.consume("c")
        first.markSeen(setOf("a"), live = false)
        val reloaded = state(store)
        reloaded.sync(mentions, newestTimestampMs = 30)
        assertEquals(listOf("b"), reloaded.pending.map { it.messageId })
    }

    @Test
    fun nearestGoesToTheClosestThenOlder() {
        val jump = state(FakeStore(mapOf("chan" to MentionReadMark(0))))
        jump.sync(listOf(ref("a", 10), ref("b", 20), ref("c", 30)), newestTimestampMs = 30)
        val index = mapOf("a" to 5, "b" to 50, "c" to 90)
        val first = jump.nearest({ index[it] ?: -1 }, anchorIndex = 99)
        assertEquals("c", first?.messageId)
        jump.consume("c")
        assertEquals("b", jump.nearest({ index[it] ?: -1 }, anchorIndex = 99)?.messageId)
        assertEquals("a", jump.nearest({ index[it] ?: -1 }, anchorIndex = 27)?.messageId)
    }

    @Test
    fun nearestFallsBackToLingeringMentions() {
        val jump = state(FakeStore(mapOf("chan" to MentionReadMark(0))))
        jump.sync(listOf(ref("a", 10)), newestTimestampMs = 10)
        jump.markSeen(setOf("a"), live = true)
        assertEquals("a", jump.nearest({ 3 }, anchorIndex = 0)?.messageId)
        jump.consume("a")
        assertEquals(0, jump.count)
    }

    @Test
    fun consumeAllReturnsAuthorsInOrderAndClearsEverything() {
        val store = FakeStore(mapOf("chan" to MentionReadMark(0)))
        val jump = state(store)
        jump.sync(
            listOf(ref("a", 10, "user1"), ref("b", 20, "user2"), ref("c", 30, "user1"), ref("d", 40, "user3")),
            newestTimestampMs = 40
        )
        jump.markSeen(setOf("d"), live = true)
        assertEquals(listOf("USER1", "USER2", "USER3").map { it.lowercase() }, jump.authors().map { it.lowercase() })
        val consumed = jump.consumeAll()
        assertEquals(listOf("a", "b", "c", "d"), consumed.map { it.messageId })
        assertEquals(0, jump.count)
        assertEquals(40, store.marks["chan"]?.watermarkMs)
    }

    @Test
    fun twoScreensOfOneChannelShareReads() {
        val store = FakeStore(mapOf("chan" to MentionReadMark(0)))
        val mentions = listOf(ref("a", 10), ref("b", 20), ref("c", 30))
        val first = state(store)
        val second = state(store)
        first.sync(mentions, newestTimestampMs = 30)
        second.sync(mentions, newestTimestampMs = 30)
        first.consume("c")
        second.consume("a")
        first.sync(mentions, newestTimestampMs = 30)
        assertEquals(listOf("b"), first.pending.map { it.messageId })
        assertEquals(listOf("b"), second.pending.map { it.messageId })
    }

    @Test
    fun lingeringDropsWhenTheMessageLeavesTheList() {
        val jump = state(FakeStore(mapOf("chan" to MentionReadMark(0))))
        jump.sync(listOf(ref("a", 10)), newestTimestampMs = 10)
        jump.markSeen(setOf("a"), live = true)
        jump.sync(emptyList(), newestTimestampMs = 20)
        assertEquals(0, jump.count)
    }
}
