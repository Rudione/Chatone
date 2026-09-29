package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.presentation.components.onSecondaryClick
import io.rudione.chatone.presentation.stream.LocalStreamPlayerHost
import io.rudione.chatone.presentation.stream.StreamOpenQuality
import io.rudione.chatone.presentation.stream.StreamPlayerEvent
import io.rudione.chatone.presentation.stream.rememberStreamHeaderHint
import io.rudione.chatone.presentation.stream.rememberStreamLauncher
import io.rudione.chatone.presentation.stream.streamLatencyText
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlin.time.Clock
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

internal fun formatStreamUptime(
    startedAt: String,
    hourUnit: String,
    minuteUnit: String
): String {
    return try {
        val start = kotlinx.datetime.Instant.parse(startedAt)
        val elapsed = Clock.System.now() - start
        val h = elapsed.inWholeHours
        val m = elapsed.inWholeMinutes % 60
        if (h > 0) "${h}$hourUnit ${m}$minuteUnit" else "${m}$minuteUnit"
    } catch (_: Exception) {
        ""
    }
}

private val LiveRed = Color(0xFFE91916)

@Composable
internal fun ChannelHeaderBlock(
    channelLogin: String,
    channelDisplayName: String,
    liveStream: io.rudione.chatone.data.remote.dto.ChannelData?,
    connectionStatus: String,
    isConnected: Boolean,
    singleLine: Boolean = false,
    compactLatency: Boolean = false
) {
    var showStreamMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val isLive = liveStream != null
    val s = LocalStrings.current
    val phoneStyle = remember { currentFormFactor() == DeviceFormFactor.PHONE }
    val playerHost = LocalStreamPlayerHost.current
    val streamLauncher = rememberStreamLauncher()
    val streamHint = if (phoneStyle) rememberStreamHeaderHint(channelLogin) else null
    val shownLive = isLive || streamHint?.playerShowsLive == true

    val headerCore: @Composable () -> Unit = {
        val headerModifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .onSecondaryClick { showStreamMenu = true }
            .clickable(enabled = isLive, interactionSource = null, indication = null) {
                if (playerHost != null) {
                    playerHost.onEvent(StreamPlayerEvent.Open(channelLogin))
                } else {
                    uriHandler.openUri("https://twitch.tv/$channelLogin")
                }
            }
            .padding(horizontal = 4.dp, vertical = 1.dp)
        if (phoneStyle) {
            val secondary = streamHint?.latencySeconds?.let { streamLatencyText(it, compactLatency) }
                ?: liveStream?.startedAt?.takeIf { it.isNotEmpty() }
                    ?.let { formatStreamUptime(it, s.unitHourShort, s.unitMinuteShort) }
                    ?.takeIf { it.isNotEmpty() }
            PhoneChannelHeader(
                title = "#${channelDisplayName.ifBlank { channelLogin }}",
                live = shownLive,
                statusHint = connectionStatus,
                secondary = secondary,
                singleLine = singleLine,
                modifier = headerModifier
            )
        } else {
            DesktopChannelHeader(
                title = "#${channelDisplayName.ifBlank { channelLogin }}",
                liveStream = liveStream,
                connectionStatus = connectionStatus,
                isConnected = isConnected,
                modifier = headerModifier
            )
        }
    }

    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
        if (isLive) {
            LiquidGlassRichTooltipBox(
                tooltipContent = {
                    Column(modifier = Modifier.width(280.dp)) {
                        val streamerModeState by koinInject<io.rudione.chatone.data.repository.StreamerModeController>()
                            .state.collectAsState()
                        val hideThumbs = streamerModeState.enabled && streamerModeState.options.hideThumbnails
                        val preview = liveStream.thumbnailUrl
                            .replace("{width}", "320")
                            .replace("{height}", "180")
                        if (preview.isNotEmpty() && !hideThumbs) {
                            AsyncImage(
                                model = preview,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            liveStream.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (liveStream.gameName.isNotEmpty()) {
                                Text(
                                    liveStream.gameName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Text(
                                "👁 ${liveStream.viewerCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "⏱ ${formatStreamUptime(liveStream.startedAt, s.unitHourShort, s.unitMinuteShort)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.streamTooltipHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            ) {
                headerCore()
            }
        } else {
            headerCore()
        }

        ChatoneDropdownMenu(
            expanded = showStreamMenu,
            onDismissRequest = { showStreamMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(s.streamOpenInBrowser, style = MaterialTheme.typography.bodySmall) },
                onClick = {
                    showStreamMenu = false
                    uriHandler.openUri("https://twitch.tv/$channelLogin")
                }
            )
            StreamOpenQuality.entries.forEach { quality ->
                DropdownMenuItem(
                    text = { Text(s.streamPlayerQuality.replace("{0}", quality.label), style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        showStreamMenu = false
                        streamLauncher.open(channelLogin, quality)
                    }
                )
            }
        }
    }
}

@Composable
private fun PhoneChannelHeader(
    title: String,
    live: Boolean,
    statusHint: String,
    secondary: String?,
    singleLine: Boolean,
    modifier: Modifier
) {
    val secondaryStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontFeatureSettings = "tnum"
    )
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val titleRow: @Composable () -> Unit = {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(5.dp))
        LiquidGlassTooltipBox(tooltip = statusHint) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (live) LiveRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
            )
        }
    }
    if (singleLine || secondary == null) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            titleRow()
            if (secondary != null) {
                Spacer(Modifier.width(6.dp))
                Text(secondary, style = secondaryStyle, color = secondaryColor, maxLines = 1)
            }
        }
    } else {
        Column(modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) { titleRow() }
            Text(secondary, style = secondaryStyle, color = secondaryColor, maxLines = 1)
        }
    }
}

@Composable
private fun DesktopChannelHeader(
    title: String,
    liveStream: io.rudione.chatone.data.remote.dto.ChannelData?,
    connectionStatus: String,
    isConnected: Boolean,
    modifier: Modifier
) {
    val s = LocalStrings.current
    val isLive = liveStream != null
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            LiquidGlassTooltipBox(tooltip = connectionStatus) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape)
                        .background(if (isConnected) ChatoneTheme.extraColors.connected else MaterialTheme.colorScheme.error)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(4.dp).clip(CircleShape)
                    .background(
                        if (isLive) LiveRed
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
            )
            Spacer(Modifier.width(2.dp))
            Text(
                if (isLive) "LIVE" else s.chatOffline,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal,
                color = if (isLive) LiveRed
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            if (liveStream != null && liveStream.startedAt.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    formatStreamUptime(liveStream.startedAt, s.unitHourShort, s.unitMinuteShort),
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
