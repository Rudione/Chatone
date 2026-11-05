package io.rudione.chatone.domain.profile

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreamCalendarTest {

    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 10, 28)

    private fun video(id: String, date: LocalDate, hours: Int, category: String, kind: VideoKind = VideoKind.ARCHIVE) = ChannelVideo(
        id = id,
        title = id,
        lengthSeconds = hours * 3600,
        views = 0,
        createdAtMs = date.atStartOfDayIn(zone).toEpochMilliseconds() + 3_600_000,
        thumbnailUrl = "",
        category = category,
        categoryArtUrl = "",
        kind = kind
    )

    @Test
    fun gridStartsOnMondayAndCoversTenWeeks() {
        val calendar = StreamCalendar.build(emptyList(), today, zone)
        assertEquals(DayOfWeek.MONDAY, calendar.firstDay.dayOfWeek)
        assertEquals(StreamCalendar.WINDOW_WEEKS, calendar.weeks)
        assertTrue(calendar.days.isEmpty())
        assertNull(calendar.topCategory)
    }

    @Test
    fun groupsArchivesPerDayAndSummarises() {
        val videos = listOf(
            video("a", today, 2, "Dota 2"),
            video("b", today, 4, "Dota 2"),
            video("c", LocalDate(2026, 10, 20), 3, "Just Chatting"),
            video("h", today, 1, "Dota 2", VideoKind.HIGHLIGHT),
            video("old", LocalDate(2026, 1, 1), 5, "Minecraft")
        )
        val calendar = StreamCalendar.build(videos, today, zone)
        assertEquals(listOf("a", "b"), calendar.days[today]?.map { it.id })
        assertEquals(3, calendar.totalStreams)
        assertEquals(9L * 3600, calendar.totalSeconds)
        assertEquals(3L * 3600, calendar.averageSeconds)
        assertEquals(2, calendar.maxPerDay)
        assertEquals("Dota 2", calendar.topCategory)
        assertEquals(listOf("Dota 2", "Just Chatting", "Minecraft"), calendar.categories.map { it.name })
    }

    @Test
    fun dateAtWalksWeeksThenWeekdays() {
        val calendar = StreamCalendar.build(emptyList(), today, zone)
        assertEquals(calendar.firstDay, calendar.dateAt(0, 0))
        assertEquals(today, calendar.dateAt(calendar.weeks - 1, today.dayOfWeek.ordinal))
    }
}
