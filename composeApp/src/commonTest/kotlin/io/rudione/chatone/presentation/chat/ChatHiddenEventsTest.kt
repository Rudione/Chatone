package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.remote.dto.PollData
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.domain.model.DisplayMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatHiddenEventsTest {

    private val pinned = DisplayMessage.PrivMsg(
        id = "pin-1",
        timestamp = 0L,
        channel = "demo",
        userId = "1",
        username = "streamhelper",
        displayName = "StreamHelper",
        tokens = emptyList(),
        color = null,
        badges = emptyList(),
        isModerator = true,
        isSubscriber = false,
        isVip = false,
        isBroadcaster = false,
        isMention = false,
        isAction = false
    )
    private val poll = PollData(id = "poll-1", title = "Next map?", status = "ACTIVE")
    private val prediction = PredictionData(id = "pred-1", title = "Win?", status = "ACTIVE")
    private val live = ChatState(pinnedMessage = pinned, livePoll = poll, livePrediction = prediction)

    @Test
    fun hidesEverythingLiveWhenNothingIsHidden() {
        val hidden = live.withHiddenEventsToggled(memory = 50)
        assertTrue(hidden.pinLocallyHidden)
        assertEquals(setOf("poll-1", "pred-1"), hidden.hiddenEventIds)
        assertTrue(hidden.hasHiddenEvents())
    }

    @Test
    fun restoresEverythingWhenAnythingIsHidden() {
        val onlyPollHidden = live.copy(hiddenEventIds = setOf("poll-1", "older"))
        val restored = onlyPollHidden.withHiddenEventsToggled(memory = 50)
        assertFalse(restored.pinLocallyHidden)
        assertEquals(setOf("older"), restored.hiddenEventIds)
        assertFalse(restored.hasHiddenEvents())
    }

    @Test
    fun aHiddenPinCountsOnlyWhileThereIsAPin() {
        assertFalse(ChatState(pinLocallyHidden = true).hasHiddenEvents())
        assertTrue(live.copy(pinLocallyHidden = true).hasHiddenEvents())
    }

    @Test
    fun keepsTheHiddenIdMemoryBounded() {
        val crowded = ChatState(livePoll = poll, hiddenEventIds = (1..50).map { "old-$it" }.toSet())
        val hidden = crowded.withHiddenEventsToggled(memory = 50)
        assertEquals(50, hidden.hiddenEventIds.size)
        assertTrue("poll-1" in hidden.hiddenEventIds)
    }
}
