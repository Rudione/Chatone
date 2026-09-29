package io.rudione.chatone.presentation.stream

import androidx.compose.foundation.layout.size
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.rounded.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Immutable
data class StreamHeaderHint(
    val latencySeconds: Float?,
    val playerShowsLive: Boolean
)

private val NoHint = StreamHeaderHint(latencySeconds = null, playerShowsLive = false)
private val LivePhases = setOf(StreamPhase.PLAYING, StreamPhase.BUFFERING, StreamPhase.PAUSED)

@Composable
fun rememberStreamHeaderHint(channelLogin: String): StreamHeaderHint? {
    val host = LocalStreamPlayerHost.current ?: return null
    val hints = remember(host, channelLogin) {
        combine(host.state, host.latencySeconds) { state, latency ->
            val watched = state.channelLogin.equals(channelLogin, ignoreCase = true)
            StreamHeaderHint(
                latencySeconds = latency.takeIf { watched && state.phase == StreamPhase.PLAYING },
                playerShowsLive = watched && state.phase in LivePhases
            )
        }.distinctUntilChanged()
    }
    val hint by hints.collectAsState(NoHint)
    return hint
}

@Composable
fun streamLatencyText(seconds: Float, compact: Boolean): String {
    val appStrings = LocalStrings.current
    val strings = appStrings.player
    return appStrings.format(
        if (compact) strings.latencyCompact else strings.latency,
        StreamFormatting.decimal(seconds, 2, strings.decimalSeparator)
    )
}

@Composable
fun StreamWatchButton(channelLogin: String, buttonSize: Dp, iconSize: Dp) {
    val host = LocalStreamPlayerHost.current ?: return
    val state by host.state.collectAsState()
    val watching = state.channelLogin.equals(channelLogin, ignoreCase = true)
    val strings = LocalStrings.current.player
    ChatoneIconButton(
        onClick = {
            host.onEvent(if (watching) StreamPlayerEvent.Close else StreamPlayerEvent.Open(channelLogin))
        },
        modifier = Modifier.size(buttonSize)
    ) {
        Icon(
            imageVector = Icons.Rounded.LiveTv,
            contentDescription = if (watching) strings.close else strings.watch,
            tint = if (watching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}
