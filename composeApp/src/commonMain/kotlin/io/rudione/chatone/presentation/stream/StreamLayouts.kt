package io.rudione.chatone.presentation.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.presentation.chat.ChatSurfaceStyle
import io.rudione.chatone.presentation.chat.LocalChatSurfaceStyle
import io.rudione.chatone.presentation.components.ChatoneWindowSize
import io.rudione.chatone.presentation.components.LocalWindowSize
import kotlin.math.roundToInt

@Composable
fun PortraitStreamPlayer(ui: MobileStreamUi, modifier: Modifier = Modifier) {
    ui.Player(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(StreamPlayerUiState.DEFAULT_ASPECT_RATIO),
        fullscreen = false
    )
}

@Composable
fun LandscapeStreamLayout(
    ui: MobileStreamUi,
    chat: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val host = ui.host
    val state by host.state.collectAsState()
    val preferences = state.preferences
    val mode = preferences.landscapeChatMode
    var fraction by remember { mutableFloatStateOf(preferences.sideChatFraction) }
    LaunchedEffect(preferences.sideChatFraction) { fraction = preferences.sideChatFraction }
    var totalWidthPx by remember { mutableIntStateOf(0) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { totalWidthPx = it.width }
    ) {
        ui.Player(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            fullscreen = true,
            overlay = if (mode == LandscapeChatMode.OVERLAY) {
                {
                    OverlayChatWindow(
                        bounds = preferences.overlayBounds,
                        opacity = preferences.overlayOpacity,
                        messageOpacity = preferences.overlayMessageOpacity,
                        locked = preferences.overlayLocked,
                        onBoundsCommitted = { host.onEvent(StreamPlayerEvent.SetOverlayBounds(it)) },
                        onLockedChange = { host.onEvent(StreamPlayerEvent.SetOverlayLocked(it)) },
                        onModeChange = { host.onEvent(StreamPlayerEvent.SetLandscapeChatMode(it)) },
                        content = chat
                    )
                }
            } else {
                null
            }
        )
        if (mode == LandscapeChatMode.SIDE) {
            SideChatHandle(
                onDrag = { delta ->
                    if (totalWidthPx > 0) {
                        fraction = (fraction - delta / totalWidthPx).coerceIn(
                            StreamPlayerPreferences.MIN_SIDE_CHAT_FRACTION,
                            StreamPlayerPreferences.MAX_SIDE_CHAT_FRACTION
                        )
                    }
                },
                onDragFinished = { host.onEvent(StreamPlayerEvent.SetSideChatFraction(fraction)) }
            )
            SideChatPane(fraction = { fraction }, chat = chat)
        }
    }
}

private val SideHandleWidth = 6.dp
private val SideGripWidth = 2.dp
private val SideHandleReachOverPlayer = 10.dp
private val SideChatStyle = ChatSurfaceStyle(showWatchButton = false, scrollableHeader = true)

@Composable
private fun SideChatHandle(onDrag: (Float) -> Unit, onDragFinished: () -> Unit) {
    val drag by rememberUpdatedState(onDrag)
    val finish by rememberUpdatedState(onDragFinished)
    Box(
        modifier = Modifier
            .width(SideHandleWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .layout { measurable, constraints ->
                val reach = SideHandleReachOverPlayer.roundToPx()
                val placeable = measurable.measure(
                    Constraints.fixed(constraints.maxWidth + reach, constraints.maxHeight)
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(-reach, 0)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { finish() },
                    onDragCancel = { finish() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        drag(dragAmount)
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            Modifier
                .padding(end = (SideHandleWidth - SideGripWidth) / 2)
                .width(SideGripWidth)
                .height(28.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun SideChatPane(fraction: () -> Float, chat: @Composable (Modifier) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .layout { measurable, constraints ->
                val totalWidth = constraints.maxWidth + SideHandleWidth.roundToPx()
                val chatWidth = (totalWidth * fraction()).roundToInt().coerceIn(0, constraints.maxWidth)
                val placeable = measurable.measure(Constraints.fixed(chatWidth, constraints.maxHeight))
                layout(chatWidth, constraints.maxHeight) { placeable.place(0, 0) }
            }
    ) {
        CompositionLocalProvider(
            LocalWindowSize provides ChatoneWindowSize.Compact,
            LocalChatSurfaceStyle provides SideChatStyle
        ) {
            chat(Modifier.fillMaxSize())
        }
    }
}
