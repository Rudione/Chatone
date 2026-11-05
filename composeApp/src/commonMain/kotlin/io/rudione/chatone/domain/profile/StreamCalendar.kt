package io.rudione.chatone.domain.profile

import androidx.compose.runtime.Immutable
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Immutable
data class StreamCalendar(
    val firstDay: LocalDate,
    val today: LocalDate,
    val days: Map<LocalDate, List<ChannelVideo>>,
    val totalStreams: Int,
    val totalSeconds: Long,
    val topCategory: String?,
    val categories: List<CategoryShare>
) {
    val weeks: Int get() = (today.toEpochDays() - firstDay.toEpochDays()).toInt() / 7 + 1
    val averageSeconds: Long get() = if (totalStreams == 0) 0 else totalSeconds / totalStreams
    val maxPerDay: Int get() = days.values.maxOfOrNull { it.size } ?: 0

    fun dateAt(week: Int, weekday: Int): LocalDate = firstDay.plus(week * 7 + weekday, DateTimeUnit.DAY)

    companion object {
        const val WINDOW_WEEKS = 10

        fun build(videos: List<ChannelVideo>, today: LocalDate, zone: TimeZone): StreamCalendar {
            val firstDay = today.minus((WINDOW_WEEKS - 1) * 7 + (today.dayOfWeek.isoDayNumber - 1), DateTimeUnit.DAY)
            val archives = videos.filter { it.kind == VideoKind.ARCHIVE }
            val days = archives
                .mapNotNull { video ->
                    video.createdAtMs?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date to video }
                }
                .filter { (date, _) -> date in firstDay..today }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, list) -> list.sortedBy { it.createdAtMs } }
            val inWindow = days.values.flatten()
            val categories = archives
                .filter { it.category.isNotBlank() }
                .groupBy { it.category }
                .map { (name, list) -> CategoryShare(name, list.firstOrNull { it.categoryArtUrl.isNotBlank() }?.categoryArtUrl.orEmpty(), list.size) }
                .sortedByDescending { it.streams }
            return StreamCalendar(
                firstDay = firstDay,
                today = today,
                days = days,
                totalStreams = inWindow.size,
                totalSeconds = inWindow.sumOf { it.lengthSeconds.toLong() },
                topCategory = categories.firstOrNull()?.name,
                categories = categories
            )
        }
    }
}
