package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.DisplayMessage.ModerationMsg
import io.rudione.chatone.domain.model.DisplayMessage.ModerationMsg.ModerationAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatModerationEventsTest {

    private val timeout = ModerationMsg(
        id = "live", timestamp = 10_000L, channel = "chan", text = "",
        action = ModerationAction.TIMEOUT, targetUser = "alice", duration = 1,
        serverTimestamp = 10_000L
    )

    @Test
    fun redeliveredTimeoutIsTheSameEventEvenAfterALongDelay() {
        val repeated = timeout.copy(id = "repeated", timestamp = 60_000L)
        assertTrue(ChatModerationEvents.isSameEvent(timeout, repeated))
    }

    @Test
    fun aNewTimeoutForTheSameDurationIsPreserved() {
        val next = timeout.copy(id = "next", timestamp = 10_100L, serverTimestamp = 10_100L)
        assertFalse(ChatModerationEvents.isSameEvent(timeout, next))
    }

    @Test
    fun timeoutsWithDifferentDurationsArePreservedWithoutServerTimestamps() {
        val first = timeout.copy(serverTimestamp = null)
        val second = first.copy(id = "next", duration = 600)
        assertFalse(ChatModerationEvents.isSameEvent(first, second))
    }

    @Test
    fun channelAndLoginComparisonIgnoresCaseAndChannelPrefix() {
        assertTrue(ChatModerationEvents.isSameEvent(timeout, timeout.copy(id = "other", channel = "#CHAN", targetUser = "ALICE")))
        assertFalse(ChatModerationEvents.isSameEvent(timeout, timeout.copy(id = "other", channel = "elsewhere")))
        assertFalse(ChatModerationEvents.isSameEvent(timeout, timeout.copy(id = "other", targetUser = "bob")))
    }

    @Test
    fun bansAndTimeoutsAreDifferentActions() {
        assertFalse(ChatModerationEvents.isSameEvent(timeout, timeout.copy(id = "ban", action = ModerationAction.BAN)))
    }

    @Test
    fun deletingDifferentMessagesByTheSameUserPreservesBothEvents() {
        val first = timeout.copy(action = ModerationAction.DELETE, targetMessageId = "message-1")
        assertFalse(ChatModerationEvents.isSameEvent(first, first.copy(id = "other", targetMessageId = "message-2")))
    }

    @Test
    fun repeatedDeletionUsesMessageIdEvenWhenTheAuthorIsMissing() {
        val first = timeout.copy(action = ModerationAction.DELETE, targetMessageId = "message-1")
        val repeated = first.copy(id = "other", timestamp = 60_000L, targetUser = null)
        assertTrue(ChatModerationEvents.isSameEvent(first, repeated))
    }

    @Test
    fun missingMessageIdsDoNotCollapseUnrelatedDeletions() {
        val first = timeout.copy(action = ModerationAction.DELETE)
        assertFalse(ChatModerationEvents.isSameEvent(first, first.copy(id = "other")))
    }

    @Test
    fun repeatedClearIsSuppressedButANewClearIsPreserved() {
        val first = timeout.copy(action = ModerationAction.CLEAR, targetUser = null, duration = null)
        assertTrue(ChatModerationEvents.isSameEvent(first, first.copy(id = "repeat")))
        assertFalse(ChatModerationEvents.isSameEvent(first, first.copy(id = "next", serverTimestamp = 10_001L)))
    }

    @Test
    fun missingServerTimestampUsesOnlyAShortFallbackWindow() {
        val first = timeout.copy(serverTimestamp = null)
        assertTrue(ChatModerationEvents.isSameEvent(first, first.copy(id = "repeat", timestamp = 10_500L)))
        assertFalse(ChatModerationEvents.isSameEvent(first, first.copy(id = "next", timestamp = 11_001L)))
    }

    @Test
    fun unrelatedAutoModResolutionsAreNotCollapsed() {
        val first = timeout.copy(action = ModerationAction.AUTOMOD_ALLOWED, targetUser = null)
        assertFalse(ChatModerationEvents.isSameEvent(first, first.copy(id = "other")))
    }

    @Test
    fun aLiveCopyEnrichesTheHistoryRowWithoutChangingItsIdentity() {
        val history = timeout.copy(id = "history", moderatorLogin = null)
        val live = timeout.copy(id = "live", moderatorLogin = "moderator", text = "Timed out by moderator")
        val merged = ChatModerationEvents.enrich(history, live)
        assertEquals("history", merged.id)
        assertEquals(history.timestamp, merged.timestamp)
        assertEquals("moderator", merged.moderatorLogin)
        assertEquals(live.text, merged.text)
        assertEquals(merged, ChatModerationEvents.enrich(merged, timeout))
    }
}
