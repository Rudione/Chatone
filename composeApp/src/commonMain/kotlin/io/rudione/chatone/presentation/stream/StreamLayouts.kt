package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import io.rudione.chatone.presentation.chat.LocalChatSpill
import io.rudione.chatone.presentation.chat.ChatSpill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.presentation.chat.ChatSurfaceStyle
import io.rudione.chatone.presentation.chat.LocalChatSurfaceStyle
import io.rudione.chatone.presentation.components.ChatoneWindowSize
import io.rudione.chatone.presentation.components.LocalWindowSize
import io.rudione.chatone.presentation.components.StatusBarOverDarkContent
import io.rudione.chatone.presentation.stream.ambient.AmbienceViewer
import io.rudione.chatone.presentation.stream.ambient.AmbientEdge
import io.rudione.chatone.presentation.stream.ambient.AmbientGlow
import io.rudione.chatone.presentation.stream.ambient.AmbientInsetStrip
import io.rudione.chatone.presentation.stream.ambient.AmbientSurroundInsets
import io.rudione.chatone.presentation.stream.ambient.ambientSurround
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.roundToInt

@Composable
fun PortraitStreamPlayer(
    ui: MobileStreamUi,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    glowHeight: Dp = 0.dp,
    channelPanel: (@Composable (login: String, modifier: Modifier) -> Unit)? = null
) {
    val reveal = rememberChannelPanelReveal(ui, enabled = channelPanel != null)
    Box(modifier.zIndex(1f)) {
        Column {
            if (topInset > 0.dp) {
                StatusBarOverDarkContent()
                AmbientInsetStrip(
                    ambience = ui.ambience,
                    videoSide = AmbientEdge.TOP,
                    modifier = Modifier.fillMaxWidth().height(topInset)
                )
            }
            ui.Player(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(StreamPlayerUiState.DEFAULT_ASPECT_RATIO),
                fullscreen = false
            )
            if (channelPanel != null) {
                ChannelPanelReveal(ui = ui, reveal = reveal, glowing = glowHeight > 0.dp, panel = channelPanel)
            }
        }
        if (glowHeight > 0.dp) {
            AmbientGlow(
                ambience = ui.ambience,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(glowHeight)
                    .offset { IntOffset(0, glowHeight.roundToPx()) },
                alpha = { 1f - reveal.progress }
            )
        }
    }
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
    SideEffect(preferences.sideChatFraction) { fraction = preferences.sideChatFraction }
    var totalWidthPx by remember { mutableIntStateOf(0) }
    val surround = rememberLandscapeSurround(sideChat = mode == LandscapeChatMode.SIDE)
    if (!surround.isEmpty) AmbienceViewer(ui.ambience)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { totalWidthPx = it.width }
    ) {
        ui.Player(
            modifier = Modifier.weight(1f).fillMaxHeight().ambientSurround(ui.ambience, surround),
            fullscreen = true,
            overlay = if (mode == LandscapeChatMode.OVERLAY) {
                {
                    OverlayChatWindow(
                        bounds = preferences.overlayBounds,
                        look = OverlayChatLook.from(preferences),
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

@Composable
private fun rememberLandscapeSurround(sideChat: Boolean): AmbientSurroundInsets {
    val safe = WindowInsets.safeDrawing
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val left = safe.getLeft(density, direction).toFloat()
    val top = safe.getTop(density).toFloat()
    val right = if (sideChat) 0f else safe.getRight(density, direction).toFloat()
    return remember(left, top, right) { AmbientSurroundInsets(left = left, top = top, right = right, bottom = 0f) }
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
    val spill = remember { ChatSpill() }
    val reach = animateFloatAsState(
        targetValue = if (spill.active) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "sideChatSpill"
    )
    val spillShape = RoundedCornerShape(topStart = SpillCorner, bottomStart = SpillCorner)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .zIndex(1f)
            .layout { measurable, constraints ->
                val totalWidth = constraints.maxWidth + SideHandleWidth.roundToPx()
                val chatWidth = (totalWidth * fraction()).roundToInt().coerceIn(0, constraints.maxWidth)
                val playerWidth = (totalWidth - chatWidth - SideHandleWidth.roundToPx()).coerceAtLeast(0)
                val extra = (playerWidth * SPILL_FRACTION * reach.value).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(chatWidth + extra, constraints.maxHeight))
                layout(chatWidth, constraints.maxHeight) { placeable.place(-extra, 0) }
            }
            .graphicsLayer {
                val progress = reach.value
                shadowElevation = SpillElevation.toPx() * progress
                shape = spillShape
                clip = progress > 0f
            }
    ) {
        CompositionLocalProvider(
            LocalWindowSize provides ChatoneWindowSize.Compact,
            LocalChatSurfaceStyle provides SideChatStyle,
            LocalChatSpill provides spill
        ) {
            chat(Modifier.fillMaxSize())
        }
    }
}

private const val SPILL_FRACTION = 0.4f
private val SpillCorner = 18.dp
private val SpillElevation = 18.dp
