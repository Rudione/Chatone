package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.remote.RecentHistoryEvent
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.IrcEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChatHistoryEventsTest {

    private val toDisplay: (ChatMessage) -> DisplayMessage.PrivMsg = { message -> privMsg(message.id, message.timestamp) }

    private fun privMsg(id: String, timestamp: Long, rewardId: String? = null, rewardName: String? = null) =
        DisplayMessage.PrivMsg(
            id = id, timestamp = timestamp, channel = "chan", userId = "1", username = "u",
            displayName = "U", tokens = emptyList(), color = null, badges = emptyList(),
            isModerator = false, isSubscriber = false, isVip = false, isBroadcaster = false,
            isMention = false, isAction = false, customRewardId = rewardId, rewardName = rewardName
        )

    @Test
    fun timeoutKeepsItsOwnTimestamp() {
        val event = RecentHistoryEvent(1_000L, IrcEvent.ClearChat("chan", "alice", 600, mapOf("target-user-id" to "5")))
        val message = assertIs<DisplayMessage.ModerationMsg>(ChatHistoryEvents.toDisplay(event, toDisplay))
        assertEquals(1_000L, message.timestamp)
        assertEquals(DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT, message.action)
        assertEquals("hist_mod_5_1000", message.id)
    }

    @Test
    fun clearChatWithoutTargetBecomesClear() {
        val event = RecentHistoryEvent(2_000L, IrcEvent.ClearChat("chan", null, null))
        val message = assertIs<DisplayMessage.ModerationMsg>(ChatHistoryEvents.toDisplay(event, toDisplay))
        assertEquals(DisplayMessage.ModerationMsg.ModerationAction.CLEAR, message.action)
    }

    @Test
    fun liveCopyOfTheSameBanIsADuplicate() {
        val live = DisplayMessage.ModerationMsg(
            id = "mod_live", timestamp = 10_500L, channel = "chan", text = "",
            action = DisplayMessage.ModerationMsg.ModerationAction.BAN, targetUser = "Alice"
        )
        val history = live.copy(id = "hist", timestamp = 10_000L, targetUser = "alice")
        assertTrue(ChatHistoryEvents.isDuplicate(history, listOf(live)))
        assertFalse(ChatHistoryEvents.isDuplicate(history.copy(timestamp = 60_000L), listOf(live)))
    }

    @Test
    fun liveAndHistoryCopiesMatchInBothArrivalOrders() {
        val event = RecentHistoryEvent(10_000L, IrcEvent.ClearChat(
            "chan", "alice", 1, mapOf("tmi-sent-ts" to "10000")
        ))
        val history = assertIs<DisplayMessage.ModerationMsg>(ChatHistoryEvents.toDisplay(event, toDisplay))
        val live = history.copy(id = "live", channel = "#CHAN", timestamp = 12_000L)
        assertEquals(10_000L, history.serverTimestamp)
        assertTrue(ChatHistoryEvents.isDuplicate(live, listOf(history)))
        assertTrue(ChatHistoryEvents.isDuplicate(history, listOf(live)))
        assertEquals(emptyList(), ChatHistoryEvents.newMessages(listOf(history), listOf(live)))
    }

    @Test
    fun duplicatesInsideOneHistoryBatchAreRemoved() {
        val event = RecentHistoryEvent(1_000L, IrcEvent.ClearChat(
            "chan", "alice", 1, mapOf("tmi-sent-ts" to "1000")
        ))
        val first = assertIs<DisplayMessage.ModerationMsg>(ChatHistoryEvents.toDisplay(event, toDisplay))
        assertEquals(listOf(first), ChatHistoryEvents.newMessages(listOf(first, first.copy(id = "copy")), emptyList()))
    }

    @Test
    fun historyKeepsSeparateDeletionsAndRemovesOnlyTheRepeatedMessageId() {
        fun deletion(messageId: String, timestamp: Long) = assertIs<DisplayMessage.ModerationMsg>(
            ChatHistoryEvents.toDisplay(
                RecentHistoryEvent(timestamp, IrcEvent.ClearMsg("chan", messageId, "text", "alice")), toDisplay
            )
        )
        val first = deletion("message-1", 10_000L)
        val second = deletion("message-2", 10_001L)
        assertEquals("message-1", first.targetMessageId)
        assertEquals(listOf(first, second), ChatHistoryEvents.newMessages(
            listOf(first, second, first.copy(id = "repeat", timestamp = 20_000L)), emptyList()
        ))
        assertEquals(listOf(second), ChatHistoryEvents.newMessages(listOf(first, second), listOf(first)))
    }

    @Test
    fun rewardTitlesReplaceTheGenericLabel() {
        val messages = listOf(privMsg("a", 1, "r1", "Channel Points Reward"), privMsg("b", 2))
        val updated = ChatHistoryEvents.withRewardTitles(messages, mapOf("r1" to "Hydrate"))
        assertEquals("Hydrate", (updated[0] as DisplayMessage.PrivMsg).rewardName)
        assertSame(messages[1], updated[1])
        assertSame(updated, ChatHistoryEvents.withRewardTitles(updated, mapOf("r1" to "Hydrate")))
    }
}
