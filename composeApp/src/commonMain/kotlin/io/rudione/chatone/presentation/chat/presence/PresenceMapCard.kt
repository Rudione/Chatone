package io.rudione.chatone.presentation.chat.presence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.icons.lucide.CalendarDays
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

private val WeekdayLabelWidth = 20.dp
private val CellGap = 2.dp
private val MonthLabelHeight = 14.dp
private val MinCell = 9.dp
private val MaxCell = 14.dp

@Composable
internal fun PresenceMapSection(state: UserPresenceState, modifier: Modifier = Modifier) {
    if (!state.isSupported) return
    val s = LocalStrings.current
    val accent = MaterialTheme.colorScheme.primary
    val arrow by animateFloatAsState(if (state.isOpen) 180f else 0f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.08f))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                .clickable(onClick = state::toggle)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Lucide.CalendarDays, contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
            Text(
                s.profilePresenceOpen,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Lucide.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp).rotate(arrow),
                tint = accent.copy(alpha = 0.8f)
            )
        }
        AnimatedVisibility(
            visible = state.isOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            PresenceMapCard(state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresenceMapCard(state: UserPresenceState) {
    val s = LocalStrings.current
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val presence = state.presence

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                s.profilePresenceTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                s.profilePresencePeriod,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            if (state.status == UserPresenceState.Status.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
            }
        }

        when (state.status) {
            UserPresenceState.Status.NotLogged -> PresenceNote(s.profileArchiveNotLogged)
            UserPresenceState.Status.Failed -> {
                PresenceNote(s.profileArchiveFailed)
                TextButton(onClick = state::retry) {
                    Text(s.profileArchiveRetry, style = MaterialTheme.typography.labelSmall)
                }
            }

            else -> {
                if (state.status == UserPresenceState.Status.Loading && state.totalMonths > 0) {
                    LinearProgressIndicator(
                        progress = { state.loadedMonths.toFloat() / state.totalMonths },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                    )
                    PresenceNote(
                        s.format(s.profilePresenceLoading, state.loadedMonths.toString(), state.totalMonths.toString())
                    )
                }
                PresenceHeatmap(
                    counts = presence.dayCounts,
                    today = state.today,
                    selected = selected,
                    onSelect = { date -> selected = if (date == selected) null else date }
                )
                SelectedDayLine(selected, presence.dayCounts)
                PresenceLegend()
                if (presence.total > 0) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PresenceStat(s.profilePresenceTotal, presence.total.toString())
                        PresenceStat(s.profilePresenceActiveDays, presence.activeDays.toString())
                        PresenceStat(s.profilePresenceBestStreak, s.format(s.profilePresenceDays, presence.longestStreak.toString()))
                        if (presence.currentStreak > 0) {
                            PresenceStat(
                                s.profilePresenceCurrentStreak,
                                s.format(s.profilePresenceDays, presence.currentStreak.toString())
                            )
                        }
                        presence.favoriteWeekday?.let { day ->
                            PresenceStat(s.profilePresenceFavoriteDay, s.profilePresenceWeekdays[day.isoDayNumber - 1])
                        }
                        presence.peakHour?.let { hour ->
                            PresenceStat(s.profilePresencePeakHour, "${hour.toString().padStart(2, '0')}:00")
                        }
                    }
                    HourHistogram(presence.hourCounts, presence.peakHour)
                } else if (state.status == UserPresenceState.Status.Ready) {
                    PresenceNote(s.profilePresenceEmpty)
                }
                if (state.partial && state.status == UserPresenceState.Status.Ready) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PresenceNote(s.profilePresencePartial)
                        TextButton(onClick = state::retry) {
                            Text(s.profileArchiveRetry, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun presenceLevelColors(): List<Color> {
    val accent = MaterialTheme.colorScheme.primary
    return listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        accent.copy(alpha = 0.28f),
        accent.copy(alpha = 0.5f),
        accent.copy(alpha = 0.74f),
        accent
    )
}

@Composable
private fun PresenceHeatmap(
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    selected: LocalDate?,
    onSelect: (LocalDate?) -> Unit
) {
    val s = LocalStrings.current
    val start = remember(today) { ChatPresenceMath.gridStart(today) }
    val weeks = ChatPresenceMath.WEEKS
    val levelColors = presenceLevelColors()
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val todayOutline = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))

    val fade = remember { Animatable(1f) }
    var fresh by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var known by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    LaunchedEffect(counts) {
        val added = counts.keys - known
        known = counts.keys
        if (added.isEmpty()) return@LaunchedEffect
        fresh = added
        fade.snapTo(0f)
        fade.animateTo(1f, tween(durationMillis = 420))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cell: Dp = (((maxWidth - WeekdayLabelWidth + CellGap) / weeks) - CellGap).coerceIn(MinCell, MaxCell)
        val gridWidth = (cell + CellGap) * weeks - CellGap
        val gridHeight = MonthLabelHeight + (cell + CellGap) * 7 - CellGap
        val scrollable = WeekdayLabelWidth + gridWidth > maxWidth
        val scroll = rememberScrollState()
        LaunchedEffect(scrollable, scroll.maxValue) {
            if (scrollable) scroll.scrollTo(scroll.maxValue)
        }

        Row {
            Canvas(modifier = Modifier.width(WeekdayLabelWidth).height(gridHeight)) {
                val step = (cell + CellGap).toPx()
                listOf(0, 2, 4).forEach { row ->
                    val label = measurer.measure(s.profilePresenceWeekdays[row], labelStyle)
                    val top = MonthLabelHeight.toPx() + row * step + (cell.toPx() - label.size.height) / 2f
                    drawText(label, topLeft = Offset(0f, top))
                }
            }
            Box(modifier = if (scrollable) Modifier.horizontalScroll(scroll) else Modifier) {
                Canvas(
                    modifier = Modifier
                        .width(gridWidth)
                        .height(gridHeight)
                        .pointerInput(start, cell) {
                            detectTapGestures { offset ->
                                val step = (cell + CellGap).toPx()
                                val column = (offset.x / step).toInt()
                                val row = ((offset.y - MonthLabelHeight.toPx()) / step).toInt()
                                if (column !in 0 until weeks || row !in 0..6 || offset.y < MonthLabelHeight.toPx()) {
                                    onSelect(null)
                                    return@detectTapGestures
                                }
                                val date = ChatPresenceMath.dateAt(start, column, row)
                                onSelect(date.takeIf { it <= today })
                            }
                        }
                ) {
                    val cellPx = cell.toPx()
                    val step = (cell + CellGap).toPx()
                    val top = MonthLabelHeight.toPx()
                    val radius = CornerRadius(cellPx * 0.22f)
                    var lastLabelColumn = -4
                    for (column in 0 until weeks) {
                        val weekStart = ChatPresenceMath.dateAt(start, column, 0)
                        if (column > 0 && weekStart.month != ChatPresenceMath.dateAt(start, column - 1, 0).month &&
                            column - lastLabelColumn >= 3
                        ) {
                            val label = measurer.measure(
                                s.profilePresenceMonthsShort[weekStart.month.ordinal],
                                labelStyle
                            )
                            drawText(label, topLeft = Offset(column * step, 0f))
                            lastLabelColumn = column
                        }
                        for (row in 0..6) {
                            val date = ChatPresenceMath.dateAt(start, column, row)
                            if (date > today) continue
                            val count = counts[date] ?: 0
                            val level = ChatPresenceMath.level(count)
                            val base = levelColors[level]
                            val color = if (level > 0 && date in fresh) lerp(levelColors[0], base, fade.value) else base
                            val origin = Offset(column * step, top + row * step)
                            drawRoundRect(color = color, topLeft = origin, size = Size(cellPx, cellPx), cornerRadius = radius)
                            if (date == selected) {
                                drawRoundRect(
                                    color = outline,
                                    topLeft = origin,
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = radius,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            } else if (date == today) {
                                drawRoundRect(
                                    color = todayOutline,
                                    topLeft = origin,
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = radius,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayLine(selected: LocalDate?, counts: Map<LocalDate, Int>) {
    val s = LocalStrings.current
    val text = selected?.let { date ->
        val count = counts[date] ?: 0
        if (count > 0) s.format(s.profilePresenceDay, formatPresenceDate(date), count.toString())
        else s.format(s.profilePresenceDayEmpty, formatPresenceDate(date))
    } ?: s.profilePresenceHint
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected != null) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected != null) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

@Composable
private fun PresenceLegend() {
    val colors = presenceLevelColors()
    val labels = listOf("0", "1–10", "11–50", "51–99", "100+")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(colors[index]))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun PresenceStat(caption: String, value: String) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(accent.copy(alpha = 0.1f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 1
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1
        )
    }
}

@Composable
private fun HourHistogram(hours: List<Int>, peak: Int?) {
    val s = LocalStrings.current
    val accent = MaterialTheme.colorScheme.primary
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    val max = (hours.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            s.profilePresenceHours,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(46.dp)) {
            val labelHeight = 12.dp.toPx()
            val chartHeight = size.height - labelHeight
            val gap = 2.dp.toPx()
            val barWidth = ((size.width - gap * 23) / 24f).coerceAtLeast(1f)
            hours.forEachIndexed { hour, count ->
                val height = (chartHeight * count / max).coerceAtLeast(if (count > 0) 2.dp.toPx() else 1.dp.toPx())
                val radius = minOf(barWidth * 0.3f, height / 2f, 3.dp.toPx())
                drawRoundRect(
                    color = if (hour == peak) accent else accent.copy(alpha = if (count > 0) 0.38f else 0.1f),
                    topLeft = Offset(hour * (barWidth + gap), chartHeight - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(radius)
                )
            }
            listOf(0, 6, 12, 18, 23).forEach { hour ->
                val label = measurer.measure(hour.toString().padStart(2, '0'), labelStyle)
                val x = (hour * (barWidth + gap) + barWidth / 2f - label.size.width / 2f)
                    .coerceIn(0f, size.width - label.size.width)
                drawText(label, topLeft = Offset(x, chartHeight + 1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun PresenceNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

private fun formatPresenceDate(date: LocalDate): String =
    "${date.day.toString().padStart(2, '0')}.${(date.month.ordinal + 1).toString().padStart(2, '0')}.${date.year}"
