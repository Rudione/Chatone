package io.rudione.chatone.presentation.chat.timeline

import io.rudione.chatone.data.remote.ArchivedChatLine
import io.rudione.chatone.data.remote.GqlUsercardMessage
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileTimelineTest {

    private val day = 86_400_000L

    private fun live(id: String, ts: Long) = DisplayMessage.PrivMsg(
        id = id, timestamp = ts, channel = "c", userId = "1", username = "u", displayName = "U",
        tokens = emptyList(), color = null, badges = emptyList(), isModerator = false, isSubscriber = false,
        isVip = false, isBroadcaster = false, isMention = false, isAction = false
    )

    private fun local(id: String, ts: Long) = ChatMessage(
        id = id, channelId = "9", channelName = "c", userId = "1", username = "u", displayName = "U", message = "l$id", timestamp = ts
    )

    @Test
    fun mergesAllSourcesChronologicallyWithoutDuplicates() {
        val entries = ProfileTimeline.merge(
            session = listOf(live("s1", 50 * day)),
            local = listOf(local("s1", 50 * day), local("l1", 40 * day)),
            remote = listOf(GqlUsercardMessage("r1", "hi", 30 * day, "c", false, null)),
            archive = listOf(
                ArchivedChatLine("a1", "old", 10 * day, ArchivedChatLine.Kind.MESSAGE),
                ArchivedChatLine("r1", "hi", 30 * day, ArchivedChatLine.Kind.MESSAGE),
                ArchivedChatLine("t1", "", 20 * day, ArchivedChatLine.Kind.TIMEOUT, durationSeconds = 600)
            )
        )
        assertEquals(listOf("a1", "t1", "r1", "l1", "s1"), entries.map { it.id })
        assertTrue(entries.last() is TimelineEntry.Live)
        assertTrue(entries[1] is TimelineEntry.Event)
    }

    @Test
    fun insertsADayBreakOnlyWhenTheDateChanges() {
        val entries = ProfileTimeline.merge(
            session = listOf(live("a", 10 * day + 1_000), live("b", 10 * day + 2_000), live("c", 12 * day)),
            local = emptyList(), remote = emptyList(), archive = emptyList()
        )
        val rows = ProfileTimeline.rows(entries, TimeZone.UTC) { error("no past entries") }
        assertEquals(listOf("day_10", "m_a", "m_b", "day_12", "m_c"), rows.map { it.key })
    }
}
