package io.rudione.chatone.presentation.laboratory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.lab_all_channels
import chatone.composeapp.generated.resources.lab_channels
import chatone.composeapp.generated.resources.lab_chart
import chatone.composeapp.generated.resources.lab_chart_recent
import chatone.composeapp.generated.resources.lab_clear
import chatone.composeapp.generated.resources.lab_clear_text
import chatone.composeapp.generated.resources.lab_clear_title
import chatone.composeapp.generated.resources.lab_days
import chatone.composeapp.generated.resources.lab_empty
import chatone.composeapp.generated.resources.lab_experiments
import chatone.composeapp.generated.resources.lab_experiments_hint
import chatone.composeapp.generated.resources.lab_gifs
import chatone.composeapp.generated.resources.lab_hours_minutes
import chatone.composeapp.generated.resources.lab_input_glow
import chatone.composeapp.generated.resources.lab_local_only
import chatone.composeapp.generated.resources.lab_minutes
import chatone.composeapp.generated.resources.lab_range_all
import chatone.composeapp.generated.resources.lab_range_month
import chatone.composeapp.generated.resources.lab_range_week
import chatone.composeapp.generated.resources.lab_sessions
import chatone.composeapp.generated.resources.lab_short_duration
import chatone.composeapp.generated.resources.lab_smooth
import chatone.composeapp.generated.resources.lab_top_channels
import chatone.composeapp.generated.resources.lab_total_time
import chatone.composeapp.generated.resources.lab_tracking
import chatone.composeapp.generated.resources.lab_tracking_hint
import chatone.composeapp.generated.resources.ui_close
import io.rudione.chatone.data.repository.WatchHistoryRepository
import io.rudione.chatone.domain.laboratory.ChannelWatchTime
import io.rudione.chatone.domain.laboratory.WatchDay
import io.rudione.chatone.domain.laboratory.WatchInsights
import io.rudione.chatone.icons.lucide.Activity
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.ChatoneChip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import io.rudione.chatone.presentation.components.FitText
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.profile.editor.ProfileEditorEmbedded
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val LabPanelShape = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)

@Composable
internal fun LaboratoryContent(settings: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val repository = koinInject<WatchHistoryRepository>()
    val records by repository.records.collectAsState()
    val enabled by repository.enabled.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    LaboratoryPager { selected ->
        if (selected == 1) {
            ProfileEditorEmbedded(Modifier.fillMaxSize())
            return@LaboratoryPager
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (selected) {
                0 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WatchOverview(records)
                    SettingsGroup(stringResource(Res.string.lab_tracking)) {
                        SwitchRow(
                            stringResource(Res.string.lab_tracking),
                            stringResource(Res.string.lab_tracking_hint),
                            enabled,
                            repository::setEnabled
                        )
                        Text(
                            stringResource(Res.string.lab_local_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                        if (records.isNotEmpty()) TextButton(onClick = { confirmClear = true }) {
                            Text(stringResource(Res.string.lab_clear), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                else -> SettingsGroup(stringResource(Res.string.lab_experiments)) {
                    Text(stringResource(Res.string.lab_experiments_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
                    SwitchRow(stringResource(Res.string.lab_smooth), null, settings.smoothChatEnabled) { onEvent(SettingsEvent.OnSmoothChatEnabledChanged(it)) }
                    SwitchRow(stringResource(Res.string.lab_gifs), null, settings.showChatGifs) { onEvent(SettingsEvent.OnShowChatGifsChanged(it)) }
                    SwitchRow(stringResource(Res.string.lab_input_glow), null, settings.chatInputEventGlow) { onEvent(SettingsEvent.OnChatInputGlowChanged(it)) }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        shape = LabPanelShape,
        title = { Text(stringResource(Res.string.lab_clear_title)) },
        text = { Text(stringResource(Res.string.lab_clear_text)) },
        confirmButton = {
            TextButton(onClick = { repository.clear(); confirmClear = false }) { Text(stringResource(Res.string.lab_clear)) }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
internal fun WatchOverview(records: List<WatchDay>) {
    var range by remember { mutableIntStateOf(7) }
    var showAllChannels by remember { mutableStateOf(false) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val insights = remember(records, range, today) { WatchInsights.calculate(records, today, range.takeUnless { it == 365 }) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(7 to Res.string.lab_range_week, 30 to Res.string.lab_range_month, 365 to Res.string.lab_range_all).forEach { (days, label) ->
                ChatoneChip(stringResource(label), { range = days }, selected = range == days)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(LabPanelShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Lucide.Activity, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.lab_total_time), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedContent(targetState = durationLabel(insights.totalMs), label = "watchTotal") { total ->
                FitText(total, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, marquee = false)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WatchMetric(insights.activeDays, stringResource(Res.string.lab_days), Modifier.weight(1f))
                WatchMetric(insights.sessions, stringResource(Res.string.lab_sessions), Modifier.weight(1f))
                WatchMetric(insights.channels.size, stringResource(Res.string.lab_channels), Modifier.weight(1f))
            }
            if (insights.totalMs == 0L) {
                Text(stringResource(Res.string.lab_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                key(range) { WatchDayChart(insights.daily, range == 365) }
            }
        }
        if (insights.channels.isNotEmpty()) SettingsGroup(stringResource(Res.string.lab_top_channels)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                insights.channels.take(12).forEachIndexed { index, channel ->
                    WatchChannelRow(channel, insights.totalMs, index, channel.channel)
                }
                if (insights.channels.size > 12) {
                    TextButton(onClick = { showAllChannels = true }) { Text(stringResource(Res.string.lab_all_channels)) }
                }
            }
        }
    }
    if (showAllChannels) AlertDialog(
        onDismissRequest = { showAllChannels = false },
        shape = LabPanelShape,
        title = { Text(stringResource(Res.string.lab_top_channels)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(insights.channels, key = { _, row -> row.channel }) { index, channel ->
                    WatchChannelRow(channel, insights.totalMs, index, channel.channel)
                }
            }
        },
        confirmButton = { TextButton(onClick = { showAllChannels = false }) { Text(stringResource(Res.string.ui_close)) } }
    )
}

@Composable
private fun WatchMetric(value: Int, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        FitText(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, marquee = false, maxLines = 2)
    }
}

@Composable
private fun WatchChannelRow(channel: ChannelWatchTime, totalMs: Long, index: Int, name: String) {
    val fraction by animateFloatAsState(channel.durationMs.toFloat() / totalMs.coerceAtLeast(1L), tween(350), label = "channelShare")
    val accent = when (index % 3) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FitText(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, marquee = false, modifier = Modifier.weight(1f))
            Text(durationLabel(channel.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${channel.sharePercent}%", style = MaterialTheme.typography.labelSmall, color = accent)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).drawBehind {
            drawRect(track)
            drawRect(accent, size = Size(size.width * fraction, size.height))
        })
    }
}

@Composable
private fun WatchDayChart(daily: List<Pair<String, Long>>, showRecentLabel: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val maximum = daily.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val label = stringResource(if (showRecentLabel) Res.string.lab_chart_recent else Res.string.lab_chart)
    var selected by remember(daily) { mutableIntStateOf(daily.indexOfLast { it.second > 0L }.coerceAtLeast(0)) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f, tween(400), label = "watchChart")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FitText(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, marquee = false, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${shortDate(daily[selected].first)} · ${durationLabel(daily[selected].second)}", style = MaterialTheme.typography.labelSmall)
        }
        Canvas(Modifier.fillMaxWidth().height(64.dp).pointerInput(daily) {
            detectTapGestures { position ->
                selected = (position.x / size.width * daily.size).toInt().coerceIn(daily.indices)
            }
        }.semantics {
            contentDescription = "$label: ${daily.joinToString { "${it.first}: ${it.second / 60_000L}" }}"
        }) {
            val gap = 3.dp.toPx()
            val width = ((size.width - gap * (daily.size - 1)) / daily.size).coerceAtLeast(1f)
            daily.forEachIndexed { index, (_, watched) ->
                val height = (size.height * watched.toFloat() / maximum * reveal).coerceAtLeast(3.dp.toPx())
                drawRoundRect(
                    accent.copy(alpha = if (watched == 0L) 0.15f else if (index == selected) 1f else 0.55f),
                    Offset(index * (width + gap), size.height - height),
                    Size(width, height),
                    CornerRadius(3.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(daily.firstOrNull()?.first?.let(::shortDate).orEmpty(), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(daily.lastOrNull()?.first?.let(::shortDate).orEmpty(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun durationLabel(durationMs: Long): String {
    val minutes = durationMs / 60_000L
    return when {
        durationMs in 1 until 60_000L -> stringResource(Res.string.lab_short_duration)
        minutes < 60 -> stringResource(Res.string.lab_minutes, minutes.toInt())
        else -> stringResource(Res.string.lab_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}

private fun shortDate(date: String): String = "${date.takeLast(2)}.${date.substring(5, 7)}"
