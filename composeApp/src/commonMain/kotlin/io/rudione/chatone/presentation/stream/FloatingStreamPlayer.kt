package io.rudione.chatone.presentation.stream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.automirrored.rounded.VolumeOff
import io.rudione.chatone.icons.material.automirrored.rounded.VolumeUp
import io.rudione.chatone.icons.material.rounded.Close
import io.rudione.chatone.icons.material.rounded.Fullscreen
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Stable
class FloatingStreamPlayerHost internal constructor(
    internal val host: StreamPlayerHost,
    private val pictureInPicture: PictureInPictureController
) {
    private var requests by mutableIntStateOf(0)
    private var windowHeight by mutableStateOf(0.dp)

    internal var anchor by mutableStateOf(FloatingAnchor.BottomEnd)

    val isShowing: Boolean get() = requests > 0

    val bottomEndClearance: Dp
        get() = if (isShowing && anchor == FloatingAnchor.BottomEnd && windowHeight > 0.dp) {
            windowHeight + WindowMargin
        } else {
            0.dp
        }

    val isPictureInPicture: Boolean get() = pictureInPicture.isActive

    internal fun acquire() {
        requests++
    }

    internal fun release() {
        requests = (requests - 1).coerceAtLeast(0)
        if (requests == 0) windowHeight = 0.dp
    }

    internal fun reportWindowHeight(height: Dp) {
        windowHeight = height
    }
}

val LocalFloatingStreamPlayer = staticCompositionLocalOf<FloatingStreamPlayerHost?> { null }

internal enum class FloatingAnchor { TopStart, TopEnd, BottomStart, BottomEnd }

@Composable
fun FloatingStreamPlayer(onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val floating = LocalFloatingStreamPlayer.current ?: return
    val open by remember(floating) {
        floating.host.state.map { it.isOpen }.distinctUntilChanged()
    }.collectAsState(floating.host.state.value.isOpen)
    if (!open || floating.isPictureInPicture) return
    DisposableEffect(floating) {
        floating.acquire()
        onDispose { floating.release() }
    }
    FloatingPlayerWindow(floating = floating, onExpand = onExpand, modifier = modifier)
}

@Composable
private fun FloatingPlayerWindow(
    floating: FloatingStreamPlayerHost,
    onExpand: () -> Unit,
    modifier: Modifier
) {
    val host = floating.host
    val state by host.state.collectAsState()
    val strings = LocalStrings.current.player
    val expand by rememberUpdatedState(onExpand)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(WindowMargin)
    ) {
        val ratio = state.videoAspectRatio.takeIf { it.isFinite() && it > 0f }
            ?.coerceIn(MIN_RATIO, MAX_RATIO) ?: StreamPlayerUiState.DEFAULT_ASPECT_RATIO
        val preferredWidth = (maxWidth * WIDTH_FRACTION).coerceIn(MinWindowWidth, MaxWindowWidth)
        val windowHeight = min(preferredWidth / ratio, maxHeight * MAX_HEIGHT_FRACTION)
        val windowWidth = min(windowHeight * ratio, maxWidth)
        val density = LocalDensity.current
        val areaWidth = constraints.maxWidth.toFloat()
        val areaHeight = constraints.maxHeight.toFloat()
        val widthPx = with(density) { windowWidth.toPx() }
        val heightPx = with(density) { windowHeight.toPx() }
        val travel = Offset((areaWidth - widthPx).coerceAtLeast(0f), (areaHeight - heightPx).coerceAtLeast(0f))
        val position = remember { Animatable(anchorPosition(floating.anchor, travel), Offset.VectorConverter) }
        val appear = remember { Animatable(0f) }
        var controlsVisible by remember { mutableStateOf(false) }
        var interactionTick by remember { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(travel) { position.snapTo(anchorPosition(floating.anchor, travel)) }
        LaunchedEffect(windowHeight) { floating.reportWindowHeight(windowHeight) }
        LaunchedEffect(Unit) {
            appear.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
        }
        LaunchedEffect(controlsVisible, interactionTick) {
            if (controlsVisible) {
                delay(CONTROLS_AUTO_HIDE_MS)
                controlsVisible = false
            }
        }

        Box(
            Modifier
                .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
                .size(windowWidth, windowHeight)
                .graphicsLayer {
                    val progress = appear.value
                    alpha = progress.coerceIn(0f, 1f)
                    scaleX = APPEAR_SCALE + (1f - APPEAR_SCALE) * progress
                    scaleY = scaleX
                }
                .shadow(WindowElevation, WindowShape)
                .clip(WindowShape)
                .background(Color.Black)
                .pointerInput(travel) {
                    val tracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDrag = { change, drag ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, position.value + change.position)
                            scope.launch { position.snapTo(clampToArea(position.value + drag, travel)) }
                        },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity()
                            val projected = position.value + Offset(velocity.x, velocity.y) * FLING_PROJECTION_SECONDS
                            val target = nearestAnchor(projected, travel)
                            floating.anchor = target
                            scope.launch {
                                position.animateTo(
                                    anchorPosition(target, travel),
                                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
                                    initialVelocity = Offset(velocity.x, velocity.y)
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch { position.animateTo(anchorPosition(floating.anchor, travel), spring()) }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            interactionTick++
                        },
                        onDoubleTap = { expand() }
                    )
                }
        ) {
            StreamPictureInPictureView(
                host = host,
                modifier = Modifier.fillMaxSize(),
                surfaceKind = VideoSurfaceKind.Composited
            )
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = SCRIM_ALPHA))) {
                    PlayerIconButton(
                        icon = if (state.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = if (state.isMuted) strings.unmute else strings.mute,
                        onClick = {
                            interactionTick++
                            host.onEvent(StreamPlayerEvent.ToggleMute)
                        },
                        size = SmallButton,
                        iconSize = SmallIcon,
                        active = state.isMuted,
                        modifier = Modifier.align(Alignment.TopStart).padding(ButtonInset)
                    )
                    PlayerIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = strings.close,
                        onClick = { host.onEvent(StreamPlayerEvent.Dismiss) },
                        size = SmallButton,
                        iconSize = SmallIcon,
                        modifier = Modifier.align(Alignment.TopEnd).padding(ButtonInset)
                    )
                    PlayerIconButton(
                        icon = Icons.Rounded.Fullscreen,
                        contentDescription = strings.expandPlayer,
                        onClick = { expand() },
                        size = LargeButton,
                        iconSize = LargeIcon,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

private fun anchorPosition(anchor: FloatingAnchor, travel: Offset): Offset = when (anchor) {
    FloatingAnchor.TopStart -> Offset.Zero
    FloatingAnchor.TopEnd -> Offset(travel.x, 0f)
    FloatingAnchor.BottomStart -> Offset(0f, travel.y)
    FloatingAnchor.BottomEnd -> travel
}

private fun nearestAnchor(position: Offset, travel: Offset): FloatingAnchor {
    val end = position.x > travel.x / 2f
    val bottom = position.y > travel.y / 2f
    return when {
        bottom && end -> FloatingAnchor.BottomEnd
        bottom -> FloatingAnchor.BottomStart
        end -> FloatingAnchor.TopEnd
        else -> FloatingAnchor.TopStart
    }
}

private fun clampToArea(position: Offset, travel: Offset): Offset =
    Offset(position.x.coerceIn(0f, travel.x), position.y.coerceIn(0f, travel.y))

private const val WIDTH_FRACTION = 0.5f
private const val MAX_HEIGHT_FRACTION = 0.42f
private const val MIN_RATIO = 0.5f
private const val MAX_RATIO = 2.4f
private const val APPEAR_SCALE = 0.86f
private const val SCRIM_ALPHA = 0.38f
private const val CONTROLS_AUTO_HIDE_MS = 3_000L
private const val FLING_PROJECTION_SECONDS = 0.18f
private val MinWindowWidth = 168.dp
private val MaxWindowWidth = 300.dp
private val WindowMargin = 12.dp
private val WindowElevation = 14.dp
private val WindowShape = RoundedCornerShape(16.dp)
private val SmallButton = 32.dp
private val SmallIcon = 17.dp
private val LargeButton = 44.dp
private val LargeIcon = 24.dp
private val ButtonInset = 6.dp
