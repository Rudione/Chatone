package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.theme.i18n.ChatTimelineStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

internal class ChatDayIndex(private val zone: TimeZone = TimeZone.currentSystemDefault()) {
    private val dayById = HashMap<String, Long>()

    fun breaks(messages: List<DisplayMessage>, nowMs: Long): Map<String, Long> {
        if (dayById.size > messages.size * 2 + CACHE_SLACK) {
            val alive = messages.mapTo(HashSet(messages.size)) { it.id }
            dayById.keys.retainAll(alive)
        }
        var previous = localEpochDay(nowMs, zone)
        val result = HashMap<String, Long>()
        for (message in messages) {
            val day = dayById.getOrPut(message.id) { localEpochDay(message.timestamp, zone) }
            if (day != previous) result[message.id] = day
            previous = day
        }
        return result
    }

    private companion object {
        const val CACHE_SLACK = 512
    }
}

internal fun localEpochDay(epochMs: Long, zone: TimeZone = TimeZone.currentSystemDefault()): Long =
    Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone).date.toEpochDays().toLong()

internal fun chatDayLabel(strings: ChatTimelineStrings, epochDay: Long, todayEpochDay: Long): String {
    val date = LocalDate.fromEpochDays(epochDay)
    val today = LocalDate.fromEpochDays(todayEpochDay)
    return when (epochDay) {
        todayEpochDay -> strings.today
        todayEpochDay - 1 -> strings.yesterday
        else -> strings.dayLabel(date.day, date.month.ordinal + 1, date.year.takeIf { it != today.year })
    }
}

@Composable
internal fun ChatDayBreakFrame(epochDay: Long?, content: @Composable () -> Unit) {
    if (epochDay == null) {
        content()
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            ChatDayDivider(epochDay)
            content()
        }
    }
}

@Composable
private fun ChatDayDivider(epochDay: Long) {
    val strings = LocalStrings.current.timeline
    val today = localEpochDay(Clock.System.now().toEpochMilliseconds())
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = lineColor)
        Text(
            chatDayLabel(strings, epochDay, today),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 9.dp, vertical = 2.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = lineColor)
    }
}
