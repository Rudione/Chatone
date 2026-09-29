package io.rudione.chatone.presentation.chat.presence

import io.rudione.chatone.data.remote.parseLogMessageTimestamp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatPresenceTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        LocalDateTime(year, month, day, hour, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()

    @Test
    fun levelsFollowTheAgreedBuckets() {
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4, 4), listOf(0, 1, 10, 11, 50, 51, 99, 100, 5000).map(ChatPresenceMath::level))
    }

    @Test
    fun logLinesAreParsedAsUtcAndModerationRowsSkipped() {
        assertEquals(at(2026, 9, 1, 4) + (48 * 60 + 25) * 1000L, parseLogMessageTimestamp("[2026-09-01 04:48:25] #forsen ambatusam: +ed"))
        assertNull(parseLogMessageTimestamp("[2026-09-01 05:57:52] #forsen ambatusam: ambatusam has been timed out for 10 seconds"))
        assertNull(parseLogMessageTimestamp("[2026-09-01 05:57:52] #forsen ambatusam: ambatusam has been banned"))
        assertNull(parseLogMessageTimestamp("garbage"))
        assertNull(parseLogMessageTimestamp("[2026-13-01 04:48:25] #forsen ambatusam: bad month"))
    }

    @Test
    fun accumulatorCountsDaysStreaksAndPeaks() {
        val today = LocalDate(2026, 9, 28)
        val accumulator = ChatPresenceAccumulator(TimeZone.UTC, today)
        accumulator.add(longArrayOf(at(2026, 9, 20, 21), at(2026, 9, 21, 21), at(2026, 9, 22, 21), at(2026, 9, 22, 9), at(2026, 9, 22, 21)))
        accumulator.add(longArrayOf(at(2026, 9, 27, 21), at(2026, 9, 28, 21), at(2024, 1, 1)))
        val presence = accumulator.snapshot()
        assertEquals(7, presence.total)
        assertEquals(5, presence.activeDays)
        assertEquals(3, presence.longestStreak)
        assertEquals(2, presence.currentStreak)
        assertEquals(21, presence.peakHour)
        assertEquals(3, presence.dayCounts[LocalDate(2026, 9, 22)])
        assertEquals(DayOfWeek.TUESDAY, presence.favoriteWeekday)
    }

    @Test
    fun gridStartsOnAMondayAboutAYearBack() {
        val start = ChatPresenceMath.gridStart(LocalDate(2026, 9, 28))
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(LocalDate(2025, 9, 29), start)
        assertEquals(LocalDate(2026, 9, 28), ChatPresenceMath.dateAt(start, 52, 0))
    }

    @Test
    fun emptyInputGivesEmptyPresence() {
        val presence = ChatPresenceAccumulator(TimeZone.UTC, LocalDate(2026, 9, 28)).snapshot()
        assertEquals(ChatPresence.Empty, presence)
    }
}
