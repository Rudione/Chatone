package io.rudione.chatone.presentation.stream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.rounded.CommentsDisabled
import io.rudione.chatone.icons.material.rounded.Lock
import io.rudione.chatone.icons.material.rounded.LockOpen
import io.rudione.chatone.icons.material.rounded.VerticalSplit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.OverlayChatBounds
import io.rudione.chatone.presentation.chat.ChatSurfaceStyle
import io.rudione.chatone.presentation.chat.LocalChatSurfaceStyle
import io.rudione.chatone.presentation.components.ChatoneWindowSize
import io.rudione.chatone.presentation.components.LocalWindowSize
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.StreamPlayerStrings
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val WindowShape = RoundedCornerShape(16.dp)
private val WindowCorner = 16.dp
private val GripTouchSize = 32.dp
private val BarHeight = 30.dp
private const val IDLE_HIDE_DELAY_MS = 2_000L
private const val BORDER_ALPHA = 0.12f
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val ActiveChatStyle = ChatSurfaceStyle(transparentBackground = true, showHeader = false, showPinnedMessage = false)
private val IdleChatStyle = ActiveChatStyle.copy(showInput = false, showScrollbar = false)

private enum class ResizeCorner { BOTTOM_START, BOTTOM_END }

@Stable
private class OverlayChrome {
    var revealed by mutableStateOf(false)
    var pressed by mutableStateOf(false)
    var touches by mutableIntStateOf(0)

    fun press() {
        revealed = true
        pressed = true
        touches++
    }

    fun release() {
        pressed = false
    }
}

@Composable
internal fun OverlayChatWindow(
    bounds: OverlayChatBounds,
    opacity: Float,
    messageOpacity: Float,
    locked: Boolean,
    onBoundsCommitted: (OverlayChatBounds) -> Unit,
    onLockedChange: (Boolean) -> Unit,
    onModeChange: (LandscapeChatMode) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    var local by remember { mutableStateOf(bounds.sanitized()) }
    LaunchedEffect(bounds) { local = bounds.sanitized() }
    val commit by rememberUpdatedState(onBoundsCommitted)
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val widthPx by rememberUpdatedState(containerSize.width.coerceAtLeast(1))
    val heightPx by rememberUpdatedState(containerSize.height.coerceAtLeast(1))
    val density = LocalDensity.current
    val gripPx = with(density) { GripTouchSize.roundToPx() }
    val strings = LocalStrings.current.player
    val chrome = remember { OverlayChrome() }
    var inputFocused by remember { mutableStateOf(false) }
    val ime = WindowInsets.ime
    val imeOpen by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    val typing = inputFocused && imeOpen

    LaunchedEffect(chrome.revealed, chrome.pressed, chrome.touches, typing) {
        if (chrome.revealed && !chrome.pressed && !typing) {
            delay(IDLE_HIDE_DELAY_MS)
            chrome.revealed = false
        }
    }

    val revealed = chrome.revealed
    val chatStyle = remember(revealed, messageOpacity) {
        (if (revealed) ActiveChatStyle else IdleChatStyle).copy(messageAlpha = messageOpacity)
    }
    val backgroundAlpha = animateFloatAsState(
        targetValue = if (revealed) opacity else 0f,
        animationSpec = if (revealed) tween(200, easing = EmphasizedDecelerate) else tween(350, easing = EmphasizedAccelerate),
        label = "overlayBackground"
    )
    val borderAlpha = animateFloatAsState(
        targetValue = if (revealed) BORDER_ALPHA else 0f,
        animationSpec = tween(250),
        label = "overlayBorder"
    )

    Box(Modifier.fillMaxSize().onSizeChanged { containerSize = it }) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val window = local
                    val w = (window.width * constraints.maxWidth).roundToInt()
                    val h = (window.height * constraints.maxHeight).roundToInt()
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) {
                        placeable.place(
                            (window.x * constraints.maxWidth).roundToInt(),
                            (window.y * constraints.maxHeight).roundToInt()
                        )
                    }
                }
                .clip(WindowShape)
                .drawWithContent {
                    drawRect(Color.Black.copy(alpha = backgroundAlpha.value))
                    drawContent()
                    val stroke = 1.dp.toPx()
                    val radius = WindowCorner.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = borderAlpha.value),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(stroke)
                    )
                }
                .onFocusChanged { inputFocused = it.hasFocus }
                .trackTouches(chrome)
                .swallowTaps()
        ) {
            AnimatedVisibility(
                visible = revealed,
                enter = expandVertically(
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Top
                ) + fadeIn(tween(160, easing = EmphasizedDecelerate)),
                exit = shrinkVertically(tween(220, easing = EmphasizedAccelerate), shrinkTowards = Alignment.Top) +
                        fadeOut(tween(140, easing = EmphasizedAccelerate))
            ) {
                val dragModifier = if (locked) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { commit(local) },
                            onDragCancel = { commit(local) },
                            onDrag = { change, drag ->
                                change.consume()
                                local = local.copy(
                                    x = local.x + drag.x / widthPx,
                                    y = local.y + drag.y / heightPx
                                ).sanitized()
                            }
                        )
                    }
                }
                OverlayWindowBar(
                    locked = locked,
                    opacity = opacity,
                    strings = strings,
                    onLockedChange = onLockedChange,
                    onModeChange = onModeChange,
                    modifier = dragModifier
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(
                    LocalChatSurfaceStyle provides chatStyle,
                    LocalWindowSize provides ChatoneWindowSize.Compact
                ) {
                    content(Modifier.fillMaxSize())
                }
            }
        }

        if (revealed && !locked) {
            ResizeCorner.entries.forEach { corner ->
                ResizeGrip(
                    corner = corner,
                    modifier = Modifier
                        .offset {
                            val window = local
                            val left = (window.x * widthPx).roundToInt()
                            val right = ((window.x + window.width) * widthPx).roundToInt()
                            val bottom = ((window.y + window.height) * heightPx).roundToInt()
                            val x = if (corner == ResizeCorner.BOTTOM_START) left - gripPx / 3 else right - gripPx * 2 / 3
                            val y = bottom - gripPx * 2 / 3
                            IntOffset(
                                x.coerceIn(0, (widthPx - gripPx).coerceAtLeast(0)),
                                y.coerceIn(0, (heightPx - gripPx).coerceAtLeast(0))
                            )
                        }
                        .trackTouches(chrome)
                        .pointerInput(corner) {
                            detectDragGestures(
                                onDragEnd = { commit(local) },
                                onDragCancel = { commit(local) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    local = resized(local, corner, drag.x / widthPx, drag.y / heightPx)
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun OverlayWindowBar(
    locked: Boolean,
    opacity: Float,
    strings: StreamPlayerStrings,
    onLockedChange: (Boolean) -> Unit,
    onModeChange: (LandscapeChatMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .background(Color.Black.copy(alpha = (opacity + 0.15f).coerceAtMost(0.95f)))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WindowBarButton(
            icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            description = if (locked) strings.overlayUnlock else strings.overlayLock
        ) { onLockedChange(!locked) }
        WindowBarButton(Icons.Rounded.VerticalSplit, strings.chatSide) { onModeChange(LandscapeChatMode.SIDE) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (!locked) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }
        }
        WindowBarButton(Icons.Rounded.CommentsDisabled, strings.chatHidden) { onModeChange(LandscapeChatMode.HIDDEN) }
    }
}

private fun Modifier.trackTouches(chrome: OverlayChrome): Modifier = pointerInput(chrome) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val wasIdle = !chrome.revealed
        chrome.press()
        try {
            var dragged = false
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes
                    .firstOrNull { it.id == down.id } ?: break
                if (!dragged && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    dragged = true
                }
                if (!change.pressed) {
                    if (wasIdle && !dragged) change.consume()
                    break
                }
            }
        } finally {
            chrome.release()
        }
    }
}

private fun Modifier.swallowTaps(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        waitForUpOrCancellation()?.consume()
    }
}

private fun resized(bounds: OverlayChatBounds, corner: ResizeCorner, dx: Float, dy: Float): OverlayChatBounds {
    val height = (bounds.height + dy).coerceIn(OverlayChatBounds.MIN_HEIGHT, 1f - bounds.y)
    return when (corner) {
        ResizeCorner.BOTTOM_END -> bounds.copy(
            width = (bounds.width + dx).coerceIn(OverlayChatBounds.MIN_WIDTH, 1f - bounds.x),
            height = height
        )
        ResizeCorner.BOTTOM_START -> {
            val right = bounds.x + bounds.width
            val x = (bounds.x + dx).coerceIn(0f, right - OverlayChatBounds.MIN_WIDTH)
            bounds.copy(x = x, width = right - x, height = height)
        }
    }.sanitized()
}

@Composable
private fun WindowBarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ResizeGrip(corner: ResizeCorner, modifier: Modifier) {
    Box(modifier = modifier.size(GripTouchSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(14.dp)) {
            val stroke = 2.5.dp.toPx()
            val color = Color.White.copy(alpha = 0.85f)
            val w = size.width
            val h = size.height
            if (corner == ResizeCorner.BOTTOM_END) {
                drawLine(color, Offset(w, 0f), Offset(w, h), stroke, StrokeCap.Round)
                drawLine(color, Offset(0f, h), Offset(w, h), stroke, StrokeCap.Round)
            } else {
                drawLine(color, Offset(0f, 0f), Offset(0f, h), stroke, StrokeCap.Round)
                drawLine(color, Offset(0f, h), Offset(w, h), stroke, StrokeCap.Round)
            }
        }
    }
}
