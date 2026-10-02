package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.rudione.chatone.icons.lucide.CircleQuestionMark
import io.rudione.chatone.icons.lucide.Lucide
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Composable
fun HelpHint(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var pinned by remember { mutableStateOf(false) }
    var hoverMuted by remember { mutableStateOf(false) }
    val closing = remember { ClosingTap() }
    LaunchedEffect(hovered) { if (!hovered) hoverMuted = false }
    val open = pinned || (hovered && !hoverMuted)
    val tint = if (open) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .hoverable(interaction)
            .pointerInput(closing) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    closing.pressed()
                }
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                val wasOpen = pinned || closing.consume()
                pinned = !wasOpen
                hoverMuted = wasOpen
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Lucide.CircleQuestionMark, contentDescription = title, tint = tint, modifier = Modifier.size(16.dp))
        if (open) {
            val inset = with(LocalDensity.current) { HINT_INSET.roundToPx() }
            val position = remember(inset) { HintPosition(inset) }
            Popup(
                popupPositionProvider = position,
                onDismissRequest = {
                    if (pinned) {
                        pinned = false
                        closing.closed()
                    }
                },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
            ) {
                HintCard(title, text)
            }
        }
    }
}

@Composable
private fun HintCard(title: String, text: String) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
    Column(
        modifier = Modifier
            .padding(HINT_INSET)
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                val scale = 0.92f + 0.08f * appear.value
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(1f, 0f)
            }
            .widthIn(max = HINT_WIDTH)
            .expressiveGlass(RoundedCornerShape(18.dp), elevation = 12.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private class ClosingTap {
    private var closedAt: TimeMark? = null
    private var pressedAt: TimeMark? = null

    fun closed() {
        closedAt = TimeSource.Monotonic.markNow()
    }

    fun pressed() {
        pressedAt = TimeSource.Monotonic.markNow()
    }

    fun consume(): Boolean {
        val closed = closedAt
        val pressed = pressedAt
        closedAt = null
        pressedAt = null
        if (closed == null || pressed == null) return false
        return (closed.elapsedNow() - pressed.elapsedNow()).absoluteValue < SAME_TAP_WINDOW
    }
}

private class HintPosition(private val insetPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.right + insetPx - popupContentSize.width).coerceIn(0, maxX)
        val fitsBelow = anchorBounds.bottom + popupContentSize.height <= windowSize.height
        val y = if (fitsBelow) anchorBounds.bottom
        else (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

private val HINT_WIDTH = 260.dp
private val HINT_INSET = 6.dp
private val SAME_TAP_WINDOW = 150.milliseconds
