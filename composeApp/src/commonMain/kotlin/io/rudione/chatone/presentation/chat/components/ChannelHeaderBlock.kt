package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.data.remote.dto.ChannelData
import io.rudione.chatone.data.repository.StreamerModeController
import io.rudione.chatone.presentation.components.expressive.ClosingTap
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
import io.rudione.chatone.presentation.channelmenu.ChannelMenuItem
import io.rudione.chatone.presentation.channelmenu.ChannelMenuModel
import io.rudione.chatone.presentation.channelmenu.ChannelOptionsMenu
import io.rudione.chatone.icons.lucide.Globe
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessagesSquare
import io.rudione.chatone.icons.lucide.Play

internal fun formatStreamUptime(
    startedAt: String,
    hourUnit: String,
    minuteUnit: String
): String {
    return try {
        val start = kotlin.time.Instant.parse(startedAt)
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
    liveStream: ChannelData?,
    connectionStatus: String,
    isConnected: Boolean,
    channelMenu: ChannelMenuModel,
    singleLine: Boolean = false,
    compactLatency: Boolean = false
) {
    var showStreamMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val isLive = liveStream != null
    val s = LocalStrings.current
    val phoneStyle = remember { currentFormFactor() == DeviceFormFactor.PHONE }
    val playerHost = LocalStreamPlayerHost.current
    val streamLauncher = rememberStreamLauncher()
    val streamHint = if (phoneStyle) rememberStreamHeaderHint(channelLogin) else null
    val shownLive = isLive || streamHint?.playerShowsLive == true
    var infoOpen by remember { mutableStateOf(false) }
    val closingInfo = remember { ClosingTap() }
    val infoAvailable = phoneStyle && liveStream != null && streamHint?.watched == true

    val headerCore: @Composable () -> Unit = {
        val headerModifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .onSecondaryClick { showStreamMenu = true }
            .pointerInput(closingInfo) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    closingInfo.pressed()
                }
            }
            .clickable(enabled = isLive || infoAvailable, interactionSource = null, indication = null) {
                when {
                    infoAvailable -> {
                        val wasOpen = infoOpen || closingInfo.consume()
                        infoOpen = !wasOpen
                    }
                    playerHost != null -> playerHost.onEvent(StreamPlayerEvent.Open(channelLogin))
                    else -> uriHandler.openUri("https://twitch.tv/$channelLogin")
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
        if (isLive && !phoneStyle) {
            LiquidGlassRichTooltipBox(
                tooltipContent = {
                    val streamerModeState by koinInject<StreamerModeController>()
                        .state.collectAsState()
                    val hideThumbs = streamerModeState.enabled && streamerModeState.options.hideThumbnails
                    StreamInfoContent(
                        liveStream = liveStream,
                        showPreview = !hideThumbs,
                        footer = s.streamTooltipHint,
                        modifier = Modifier.width(280.dp)
                    )
                }
            ) {
                headerCore()
            }
        } else {
            headerCore()
        }

        if (infoOpen && infoAvailable && liveStream != null) {
            StreamInfoPopup(
                liveStream = liveStream,
                onDismiss = {
                    if (infoOpen) {
                        infoOpen = false
                        closingInfo.closed()
                    }
                }
            )
        }

        ChannelOptionsMenu(
            expanded = showStreamMenu,
            onDismissRequest = { showStreamMenu = false },
            model = channelMenu
        ) { close ->
            val login = channelLogin.lowercase()
            ChannelMenuItem(s.streamOpenInBrowser, Lucide.Globe) {
                close()
                uriHandler.openUri("https://www.twitch.tv/$login")
            }
            ChannelMenuItem(s.screenChat.menuPopoutChat, Lucide.MessagesSquare) {
                close()
                uriHandler.openUri("https://www.twitch.tv/popout/$login/chat?popout=")
            }
            ChannelMenuItem(s.screenChat.menuCopyLink, Lucide.Link) {
                close()
                clipboard.setText(AnnotatedString("https://www.twitch.tv/$login"))
            }
            StreamOpenQuality.entries.forEach { quality ->
                ChannelMenuItem(s.streamPlayerQuality.replace("{0}", quality.label), Lucide.Play) {
                    close()
                    streamLauncher.open(channelLogin, quality)
                }
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
    liveStream: ChannelData?,
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
