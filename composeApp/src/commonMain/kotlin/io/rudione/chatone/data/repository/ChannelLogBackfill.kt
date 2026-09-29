package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.BestLogsClient
import io.rudione.chatone.data.remote.IrcHistoryParser
import io.rudione.chatone.data.remote.LogDay
import io.rudione.chatone.data.remote.RecentHistoryEvent
import io.rudione.chatone.data.remote.RecentMessagesResult
import io.rudione.chatone.domain.model.ChatMessage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class ChannelLogBackfill(private val client: BestLogsClient) {

    suspend fun messagesBefore(channelId: String, beforeMs: Long, wanted: Int): RecentMessagesResult.Success? {
        if (wanted <= 0) return null
        val days = client.channelDays(channelId) ?: return null
        val messages = ArrayList<ChatMessage>()
        val events = ArrayList<RecentHistoryEvent>()
        val deleted = HashSet<String>()
        var requests = 0
        for (day in days) {
            if (messages.size >= wanted || requests >= MAX_DAY_REQUESTS) break
            if (day.startMs() >= beforeMs) continue
            requests++
            val lines = client.channelDayRawLines(channelId, day) ?: break
            val parsed = IrcHistoryParser.parse(lines)
            messages.addAll(0, parsed.messages.filter { it.timestamp < beforeMs })
            events.addAll(0, parsed.events.filter { it.timestamp < beforeMs })
            deleted += parsed.deletedMessageIds
        }
        if (messages.isEmpty() && events.isEmpty()) return null
        val kept = messages.takeLast(wanted)
        val oldestKept = kept.firstOrNull()?.timestamp ?: Long.MIN_VALUE
        return RecentMessagesResult.Success(
            messages = kept,
            deletedMessageIds = deleted,
            events = events.filter { it.timestamp >= oldestKept }
        )
    }

    private fun LogDay.startMs(): Long =
        LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    private companion object {
        const val MAX_DAY_REQUESTS = 30
    }
}
