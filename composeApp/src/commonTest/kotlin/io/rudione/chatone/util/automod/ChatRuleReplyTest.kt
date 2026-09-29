package io.rudione.chatone.util.automod

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatRuleReplyTest {

    @Test
    fun templateReceivesUserAndChannel() {
        assertEquals(
            "@Viewer please slow down in #somechannel",
            ChatRuleReplyFormatter.format("@{user} please slow down in #{CHANNEL}", "Viewer", "somechannel")
        )
    }

    @Test
    fun whitespaceIsCollapsedAndLengthCapped() {
        assertEquals("a b", ChatRuleReplyFormatter.format("  a \n   b  ", "u", "c"))
        assertEquals(500, ChatRuleReplyFormatter.format("x".repeat(600), "u", "c").length)
    }

    @Test
    fun mentionFallsBackToLoginForLocalizedDisplayNames() {
        assertEquals("Viewer", ChatRuleReplyFormatter.mentionFor("Viewer", "viewer"))
        assertEquals("viewer", ChatRuleReplyFormatter.mentionFor("Зритель", "viewer"))
        assertEquals("Зритель", ChatRuleReplyFormatter.mentionFor("Зритель", ""))
    }

    @Test
    fun throttleHoldsRepliesUntilCooldownPasses() {
        var now = 0L
        val throttle = ChatRuleReplyThrottle { now }
        assertTrue(throttle.tryAcquire("somechannel", "rule", 15))
        now = 14_999L
        assertFalse(throttle.tryAcquire("somechannel", "rule", 15))
        now = 15_000L
        assertTrue(throttle.tryAcquire("somechannel", "rule", 15))
    }

    @Test
    fun throttleIsScopedPerChannelAndRule() {
        val throttle = ChatRuleReplyThrottle { 0L }
        assertTrue(throttle.tryAcquire("first", "rule", 60))
        assertTrue(throttle.tryAcquire("second", "rule", 60))
        assertTrue(throttle.tryAcquire("first", "other", 60))
        assertFalse(throttle.tryAcquire("FIRST", "rule", 60))
    }
}
