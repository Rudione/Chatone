package io.rudione.chatone.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.TwitchEventSubClient
import io.rudione.chatone.data.remote.TwitchPubSubClient
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.flow.merge
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.components.ChatoneIconButton

private enum class MonitorMode { LIVE, AUTOMOD }

private data class MonitorLine(
    val id: String,
    val channel: String,
    val channelAvatar: String,
    val username: String,
    val color: String?,
    val text: String,
    val reason: String?
)

fun isMonitorLogin(login: String?): Boolean =
    login == "/live" || login == "/automod"

fun monitorChannelMeta(state: MainState): Map<String, ChannelTab> =
    (state.openChannels + state.unfolderedChannels + state.folders.flatMap { it.channels })
        .associateBy { it.login.lowercase().removePrefix("#") }

@Composable
fun MonitorFeedScreen(
    login: String,
    channelMeta: Map<String, ChannelTab>,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null
) {
    val s = LocalStrings.current
    val mode = if (login == "/automod") MonitorMode.AUTOMOD else MonitorMode.LIVE
    val chatRepository: ChatRepository = koinInject()
    val eventSub: TwitchEventSubClient = koinInject()
    val pubSub: TwitchPubSubClient = koinInject()

    val liveLogins = remember(channelMeta) {
        channelMeta.values.filter { it.isLive }.map { it.login.lowercase().removePrefix("#") }.toSet()
    }
    fun avatarFor(ch: String) = channelMeta[ch.lowercase().removePrefix("#")]?.profileImageUrl.orEmpty()

    var lines by remember(login, liveLogins) { mutableStateOf(listOf<MonitorLine>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(login, liveLogins) {
        if (mode == MonitorMode.LIVE) {
            chatRepository.messages.collect { m ->
                val ch = m.channelName.lowercase().removePrefix("#")
                if (liveLogins.isEmpty() || ch in liveLogins) {
                    lines = (lines + MonitorLine(m.id, ch, avatarFor(ch), m.displayName.ifBlank { m.username }, m.color, m.message, null))
                        .takeLast(300)
                }
            }
        }
    }
    LaunchedEffect(login) {
        if (mode == MonitorMode.AUTOMOD) {
            merge(eventSub.events, pubSub.events).collect { ev ->
                when (ev) {
                    is IrcEvent.AutoModHeld -> {
                        val ch = ev.channel.lowercase().removePrefix("#")
                        lines = (lines + MonitorLine(
                            ev.msgId, ch, avatarFor(ch), ev.displayName.ifBlank { ev.username },
                            ev.color, ev.message, ev.reasonCategory
                        )).takeLast(300)
                    }
                    is IrcEvent.AutoModResolved -> {
                        val ch = ev.channel.lowercase().removePrefix("#")
                        lines = (lines + MonitorLine(
                            ev.msgId + "_r", ch, avatarFor(ch), ev.resolvedBy, null,
                            "→ ${ev.action}", "resolved"
                        )).takeLast(300)
                    }
                    else -> {}
                }
            }
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (onMenuClick != null) {
                ChatoneIconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Icon(
                if (mode == MonitorMode.AUTOMOD) Icons.Outlined.Shield else Icons.Outlined.Wifi,
                null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)
            )
            Text(login, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        if (lines.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    if (mode == MonitorMode.LIVE) s.openChannelLiveDesc else s.openChannelAutomodDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
            ) {
                items(lines, key = { it.id }) { line -> MonitorRow(line) }
            }
        }

        ReadOnlyInputBar(hint = s.monitorChannelInputHint)
    }
}

@Composable
private fun MonitorRow(line: MonitorLine) {
    val nameColor = remember(line.color) {
        runCatching { Color(line.color!!.removePrefix("#").toLong(16) or 0xFF000000L) }
            .getOrDefault(Color(0xFF9146FF))
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (line.channelAvatar.isNotEmpty()) {
            AsyncImage(
                model = line.channelAvatar,
                contentDescription = null,
                modifier = Modifier.size(20.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            buildString {
                append("#${line.channel}  ")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.username,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = nameColor
                )
                if (line.reason != null) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            line.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                line.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReadOnlyInputBar(hint: String) {
    Row(
        Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp)
        )
    }
}
