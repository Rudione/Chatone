package io.rudione.chatone.presentation.profile

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_calendar_empty
import chatone.composeapp.generated.resources.channel_page_calendar_hint
import chatone.composeapp.generated.resources.channel_page_calendar_less
import chatone.composeapp.generated.resources.channel_page_calendar_more
import chatone.composeapp.generated.resources.channel_page_calendar_title
import chatone.composeapp.generated.resources.channel_page_day_streams
import chatone.composeapp.generated.resources.channel_page_stat_average
import chatone.composeapp.generated.resources.channel_page_stat_hours
import chatone.composeapp.generated.resources.channel_page_stat_streams
import chatone.composeapp.generated.resources.channel_page_stat_top_category
import chatone.composeapp.generated.resources.channel_page_views
import chatone.composeapp.generated.resources.channel_page_weekdays_short
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.profile.ChannelVideo
import io.rudione.chatone.domain.profile.StreamCalendar
import io.rudione.chatone.presentation.chat.compactCount
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val Cell = 16.dp
private val Gap = 3.dp

@Composable
internal fun StreamCalendarCard(calendar: StreamCalendar, onOpenVideo: (ChannelVideo) -> Unit, modifier: Modifier = Modifier) {
    var selected by remember(calendar) { mutableStateOf(calendar.days.keys.maxOrNull()) }
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(Res.string.channel_page_calendar_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (calendar.days.isEmpty()) {
            Text(stringResource(Res.string.channel_page_calendar_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Text(stringResource(Res.string.channel_page_calendar_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                stringArrayResource(Res.array.channel_page_weekdays_short).forEach { day ->
                    Box(Modifier.height(Cell), contentAlignment = Alignment.CenterStart) {
                        Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState(Int.MAX_VALUE)),
                horizontalArrangement = Arrangement.spacedBy(Gap)
            ) {
                repeat(calendar.weeks) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                        repeat(7) { weekday ->
                            val date = calendar.dateAt(week, weekday)
                            DayCell(
                                count = calendar.days[date]?.size ?: 0,
                                max = calendar.maxPerDay,
                                future = date > calendar.today,
                                selected = date == selected,
                                onClick = { selected = if (selected == date) null else date }
                            )
                        }
                    }
                }
            }
        }
        Legend(calendar.maxPerDay)
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn(tween(180)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                        (fadeOut(tween(100)) + shrinkVertically(tween(160)))
            },
            label = "calendarDay"
        ) { date ->
            if (date != null) DayStreams(date, calendar.days[date].orEmpty(), onOpenVideo)
        }
        Stats(calendar)
    }
}

@Composable
private fun DayCell(count: Int, max: Int, future: Boolean, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.22f else 1f, spring(dampingRatio = 0.5f), label = "dayScale")
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val accent = MaterialTheme.colorScheme.primary
    val color = when {
        future -> Color.Transparent
        count == 0 -> base
        else -> lerp(accent.copy(alpha = 0.35f), accent, (count.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f))
    }
    Box(
        Modifier.size(Cell)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .then(if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)) else Modifier)
            .clickable(enabled = !future, onClick = onClick)
    )
}

@Composable
private fun Legend(max: Int) {
    val accent = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.channel_page_calendar_less), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(0f, 0.34f, 0.67f, 1f).forEach { level ->
            Box(
                Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(
                    if (level == 0f || max == 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    else lerp(accent.copy(alpha = 0.35f), accent, level)
                )
            )
        }
        Text(stringResource(Res.string.channel_page_calendar_more), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayStreams(date: LocalDate, videos: List<ChannelVideo>, onOpenVideo: (ChannelVideo) -> Unit) {
    val zone = TimeZone.currentSystemDefault()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            pluralStringResource(Res.plurals.channel_page_day_streams, videos.size, formatDay(date), videos.size),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        videos.forEach { video ->
            val time = video.createdAtMs?.let {
                val local = Instant.fromEpochMilliseconds(it).toLocalDateTime(zone)
                "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onOpenVideo(video) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(video.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(time, video.category.takeIf { it.isNotBlank() }, stringResource(Res.string.channel_page_views, compactCount(video.views))).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(formatDuration(video.lengthSeconds), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Stats(calendar: StreamCalendar) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(stringResource(Res.string.channel_page_stat_streams), calendar.totalStreams.toString(), Modifier.weight(1f))
        StatTile(stringResource(Res.string.channel_page_stat_hours), formatHours(calendar.totalSeconds), Modifier.weight(1f))
        StatTile(stringResource(Res.string.channel_page_stat_average), formatDuration(calendar.averageSeconds.toInt()), Modifier.weight(1f))
    }
    calendar.topCategory?.let { StatTile(stringResource(Res.string.channel_page_stat_top_category), it, Modifier.fillMaxWidth()) }
}

@Composable
private fun StatTile(caption: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun formatDay(date: LocalDate): String {
    val day = date.day.toString().padStart(2, '0')
    val month = (date.month.ordinal + 1).toString().padStart(2, '0')
    return "$day.$month.${date.year}"
}
