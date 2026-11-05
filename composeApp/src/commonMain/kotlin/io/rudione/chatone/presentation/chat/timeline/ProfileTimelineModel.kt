package io.rudione.chatone.presentation.chat.timeline

import androidx.compose.runtime.Immutable
import io.rudione.chatone.data.remote.ArchivedChatLine
import io.rudione.chatone.data.remote.GqlUsercardMessage
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.chat.localEpochDay
import kotlinx.datetime.TimeZone

@Immutable
sealed interface TimelineEntry {
    val id: String
    val timestamp: Long

    data class Live(val message: DisplayMessage.PrivMsg) : TimelineEntry {
        override val id: String get() = message.id
        override val timestamp: Long get() = message.timestamp
    }

    data class Past(override val id: String, val text: String, override val timestamp: Long, val deleted: Boolean) : TimelineEntry

    data class Event(val line: ArchivedChatLine) : TimelineEntry {
        override val id: String get() = line.id
        override val timestamp: Long get() = line.timestampMs
    }
}

@Immutable
sealed interface TimelineRow {
    val key: String

    data class Day(override val key: String, val epochDay: Long) : TimelineRow
    data class Message(override val key: String, val message: DisplayMessage.PrivMsg) : TimelineRow
    data class Event(override val key: String, val line: ArchivedChatLine) : TimelineRow
}

object ProfileTimeline {

    fun merge(
        session: List<DisplayMessage.PrivMsg>,
        local: List<ChatMessage>,
        remote: List<GqlUsercardMessage>,
        archive: List<ArchivedChatLine>
    ): List<TimelineEntry> {
        val seen = HashSet<String>()
        val out = ArrayList<TimelineEntry>(session.size + local.size + remote.size + archive.size)
        fun add(entry: TimelineEntry) {
            if (entry.id.isEmpty() || seen.add(entry.id)) out += entry
        }
        session.forEach { add(TimelineEntry.Live(it)) }
        local.forEach { add(TimelineEntry.Past(it.id, it.message, it.timestamp, deleted = false)) }
        remote.forEach { add(TimelineEntry.Past(it.id, it.text, it.sentAtEpochMs ?: 0L, it.isDeleted)) }
        archive.forEach { line ->
            if (line.kind == ArchivedChatLine.Kind.MESSAGE) add(TimelineEntry.Past(line.id, line.text, line.timestampMs, line.isDeleted))
            else add(TimelineEntry.Event(line))
        }
        out.sortBy { it.timestamp }
        return out
    }

    fun rows(
        entries: List<TimelineEntry>,
        zone: TimeZone,
        render: (TimelineEntry.Past) -> DisplayMessage.PrivMsg
    ): List<TimelineRow> {
        val out = ArrayList<TimelineRow>(entries.size + 8)
        var lastDay: Long? = null
        entries.forEach { entry ->
            if (entry.timestamp > 0L) {
                val day = localEpochDay(entry.timestamp, zone)
                if (day != lastDay) {
                    lastDay = day
                    out += TimelineRow.Day("day_$day", day)
                }
            }
            out += when (entry) {
                is TimelineEntry.Live -> TimelineRow.Message("m_${entry.id}", entry.message)
                is TimelineEntry.Past -> TimelineRow.Message("m_${entry.id}", render(entry))
                is TimelineEntry.Event -> TimelineRow.Event("e_${entry.id}_${entry.timestamp}", entry.line)
            }
        }
        return out
    }
}
