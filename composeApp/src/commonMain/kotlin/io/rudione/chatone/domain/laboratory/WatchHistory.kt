package io.rudione.chatone.domain.laboratory

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class WatchDay(
    val date: String,
    val channel: String,
    val durationMs: Long,
    val sessions: Int,
    val lastWatchedAtMs: Long
)

data class ChannelWatchTime(val channel: String, val durationMs: Long, val sessions: Int, val sharePercent: Int = 0)

data class WatchInsights(
    val totalMs: Long,
    val activeDays: Int,
    val sessions: Int,
    val channels: List<ChannelWatchTime>,
    val daily: List<Pair<String, Long>>
) {
    companion object {
        fun calculate(records: List<WatchDay>, today: LocalDate, days: Int?): WatchInsights {
            val start = today.plus(-(days ?: RETENTION_DAYS) + 1, DateTimeUnit.DAY)
            val selected = records.filter { it.date >= start.toString() && it.date <= today.toString() }
            val channels = selected.groupBy { it.channel }.map { (login, rows) ->
                ChannelWatchTime(login, rows.sumOf { it.durationMs }, rows.sumOf { it.sessions })
            }.sortedWith(compareByDescending<ChannelWatchTime> { it.durationMs }.thenBy { it.channel })
            val shares = watchPercentages(channels.map { it.durationMs })
            val ranked = channels.mapIndexed { index, channel -> channel.copy(sharePercent = shares[index]) }
            val totals = selected.groupBy { it.date }.mapValues { (_, rows) -> rows.sumOf { it.durationMs } }
            val chartDays = (days ?: 30).coerceAtMost(30)
            val daily = (chartDays - 1 downTo 0).map { offset ->
                val day = today.plus(-offset, DateTimeUnit.DAY).toString()
                day to (totals[day] ?: 0L)
            }
            return WatchInsights(
                totalMs = channels.sumOf { it.durationMs },
                activeDays = totals.size,
                sessions = channels.sumOf { it.sessions },
                channels = ranked,
                daily = daily
            )
        }
    }
}

internal const val RETENTION_DAYS = 365

fun recordWatchInterval(
    records: List<WatchDay>,
    channel: String,
    startedAtMs: Long,
    durationMs: Long,
    newSession: Boolean,
    zone: TimeZone
): List<WatchDay> {
    val login = channel.trim().removePrefix("#").lowercase()
    if (!login.matches(Regex("[a-z0-9_]{1,25}")) || durationMs <= 0L) return records
    val end = startedAtMs + durationMs
    val today = Instant.fromEpochMilliseconds(end).toLocalDateTime(zone).date
    val oldest = today.plus(-RETENTION_DAYS + 1, DateTimeUnit.DAY).toString()
    val ledger = records.filter { it.date >= oldest }.associateByTo(LinkedHashMap()) { it.date to it.channel }
    var cursor = startedAtMs
    var first = true
    while (cursor < end) {
        val date = Instant.fromEpochMilliseconds(cursor).toLocalDateTime(zone).date
        val nextDay = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
        val until = minOf(end, nextDay)
        val key = date.toString() to login
        val previous = ledger[key]
        ledger[key] = WatchDay(
            date = key.first,
            channel = login,
            durationMs = (previous?.durationMs ?: 0L) + until - cursor,
            sessions = (previous?.sessions ?: 0) + if (first && newSession) 1 else 0,
            lastWatchedAtMs = until
        )
        first = false
        cursor = until
    }
    return ledger.values.sortedWith(compareByDescending<WatchDay> { it.date }.thenByDescending { it.lastWatchedAtMs }.thenBy { it.channel }).take(MAX_WATCH_ROWS)
}

internal const val MAX_WATCH_ROWS = 12_000

internal fun watchPercentages(durations: List<Long>): List<Int> {
    val total = durations.sum()
    if (total <= 0L) return durations.map { 0 }
    val shares = durations.map { (it * 100 / total).toInt() }.toMutableList()
    val missing = 100 - shares.sum()
    durations.indices.sortedByDescending { durations[it] * 100 % total }.take(missing).forEach { shares[it]++ }
    return shares
}
