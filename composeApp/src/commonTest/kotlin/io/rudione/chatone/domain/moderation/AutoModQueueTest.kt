package io.rudione.chatone.domain.moderation

import io.rudione.chatone.domain.model.IrcEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoModQueueTest {

    private fun held(id: String, channel: String = "1", at: Long = 0L) = IrcEvent.AutoModHeld(
        channel = channel, msgId = id, userId = "u", username = "user", displayName = "User", message = "text", heldAtMs = at
    )

    private fun resolved(id: String, channel: String = "1") =
        IrcEvent.AutoModResolved(channel = channel, msgId = id, resolvedBy = "mod", action = IrcEvent.AutoModResolved.ALLOWED)

    @Test
    fun heldMessagesWaitPerChannelUntilResolved() {
        val queue = AutoModQueue()
        assertTrue(queue.onHeld(held("a")))
        assertTrue(queue.onHeld(held("b", channel = "2")))
        assertFalse(queue.onHeld(held("a")))
        assertEquals(listOf("a"), queue.snapshot(0L)["1"]?.map { it.msgId })
        queue.onResolved(resolved("a"))
        assertEquals(setOf("2"), queue.snapshot(0L).keys)
        assertTrue(queue.isResolved("a"))
    }

    @Test
    fun aLateHoldForAResolvedMessageIsIgnored() {
        val queue = AutoModQueue()
        queue.onResolved(resolved("x"))
        assertFalse(queue.onHeld(held("x")))
    }

    @Test
    fun resolutionsWithoutChannelStillClear() {
        val queue = AutoModQueue()
        queue.onHeld(held("a", channel = "9"))
        queue.onResolved(resolved("a", channel = ""))
        assertTrue(queue.snapshot(0L).isEmpty())
    }

    @Test
    fun oldAndExcessMessagesAreDropped() {
        val queue = AutoModQueue(maxPerChannel = 2, ttlMs = 1_000L)
        queue.onHeld(held("a", at = 0L))
        queue.onHeld(held("b", at = 5_000L))
        queue.onHeld(held("c", at = 5_000L))
        assertEquals(listOf("b", "c"), queue.snapshot(5_500L)["1"]?.map { it.msgId })
        assertTrue(queue.snapshot(10_000L).isEmpty())
    }
}
