package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.DisplayMessage
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatDayBreaksTest {

    private val day = 86_400_000L
    private val now = 1_790_700_000_000L

    private fun msg(id: String, ts: Long) = DisplayMessage.SystemMsg(
        id = id,
        timestamp = ts,
        channel = "chan",
        text = id,
        type = DisplayMessage.SystemMsg.SystemType.INFO
    )

    @Test
    fun todaysChatHasNoDividers() {
        val index = ChatDayIndex(TimeZone.UTC)
        assertEquals(emptyMap(), index.breaks(listOf(msg("a", now - 1_000), msg("b", now)), now))
    }

    @Test
    fun dividersMarkTheFirstMessageOfEachOlderDay() {
        val index = ChatDayIndex(TimeZone.UTC)
        val today = localEpochDay(now, TimeZone.UTC)
        val breaks = index.breaks(
            listOf(msg("old1", now - 40 * day), msg("old2", now - 40 * day + 60_000), msg("y", now - day), msg("t", now)),
            now
        )
        assertEquals(mapOf("old1" to today - 40, "y" to today - 1, "t" to today), breaks)
    }

    @Test
    fun labelsAreRelativeForRecentDaysAndOmitTheCurrentYear() {
        val today = localEpochDay(now, TimeZone.UTC)
        assertEquals(DayLabel.Today, dayLabelOf(today, today))
        assertEquals(DayLabel.Yesterday, dayLabelOf(today - 1, today))
        assertEquals(DayLabel.Date(19, 3, null), dayLabelOf(today - 194, today))
        assertEquals(DayLabel.Date(29, 9, 2025), dayLabelOf(today - 365, today))
    }
}
