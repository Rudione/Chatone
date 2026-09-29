package io.rudione.chatone.presentation.chat.presence

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class ChatPresence(
    val dayCounts: Map<LocalDate, Int>,
    val hourCounts: List<Int>,
    val total: Int,
    val activeDays: Int,
    val longestStreak: Int,
    val currentStreak: Int,
    val peakHour: Int?,
    val favoriteWeekday: DayOfWeek?
) {
    companion object {
        val Empty = ChatPresence(emptyMap(), List(24) { 0 }, 0, 0, 0, 0, null, null)
    }
}

class ChatPresenceAccumulator(
    private val zone: TimeZone,
    private val today: LocalDate,
    private val firstDay: LocalDate = ChatPresenceMath.gridStart(today)
) {
    private val days = HashMap<LocalDate, Int>()
    private val hours = IntArray(24)
    private var total = 0

    fun add(batch: LongArray) {
        batch.forEach { timestamp ->
            val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(zone)
            if (local.date < firstDay || local.date > today) return@forEach
            days[local.date] = (days[local.date] ?: 0) + 1
            hours[local.hour]++
            total++
        }
    }

    fun snapshot(): ChatPresence {
        if (total == 0) return ChatPresence.Empty

        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        days.keys.sorted().forEach { date ->
            run = if (previous?.plus(1, DateTimeUnit.DAY) == date) run + 1 else 1
            longest = maxOf(longest, run)
            previous = date
        }

        var current = 0
        var cursor = if (today in days) today else today.minus(1, DateTimeUnit.DAY)
        while (cursor in days) {
            current++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }

        val weekdayTotals = IntArray(7)
        days.forEach { (date, count) -> weekdayTotals[date.dayOfWeek.isoDayNumber - 1] += count }
        val favorite = weekdayTotals.indices.maxByOrNull { weekdayTotals[it] }
            ?.takeIf { weekdayTotals[it] > 0 }
            ?.let { DayOfWeek.entries[it] }

        return ChatPresence(
            dayCounts = HashMap(days),
            hourCounts = hours.toList(),
            total = total,
            activeDays = days.size,
            longestStreak = longest,
            currentStreak = current,
            peakHour = hours.indices.maxByOrNull { hours[it] }?.takeIf { hours[it] > 0 },
            favoriteWeekday = favorite
        )
    }
}

object ChatPresenceMath {

    const val WEEKS = 53

    fun level(count: Int): Int = when {
        count <= 0 -> 0
        count <= 10 -> 1
        count <= 50 -> 2
        count <= 99 -> 3
        else -> 4
    }

    fun gridStart(today: LocalDate): LocalDate {
        val yearAgo = today.minus((WEEKS - 1) * 7, DateTimeUnit.DAY)
        return yearAgo.minus(yearAgo.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
    }

    fun dateAt(start: LocalDate, week: Int, weekday: Int): LocalDate =
        start.plus(week * 7 + weekday, DateTimeUnit.DAY)
}
