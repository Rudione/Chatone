package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HoldToConfirmButton(
    text: String,
    holdingText: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    holdMillis: Int = DEFAULT_HOLD_MILLIS,
    accent: Color = MaterialTheme.colorScheme.error,
    onAccent: Color = MaterialTheme.colorScheme.onError,
    height: Dp = 54.dp
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val confirm by rememberUpdatedState(onConfirmed)
    var holding by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    val squeeze by animateFloatAsState(
        targetValue = if (holding) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "holdSqueeze"
    )
    val label = if (holding) holdingText else text

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = squeeze
                scaleY = squeeze
            }
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.38f), CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = text
            }
            .pointerInput(holdMillis) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (confirmed) return@awaitEachGesture
                    holding = true
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    val fill = scope.launch {
                        val remaining = ((1f - progress.value) * holdMillis).roundToInt().coerceAtLeast(1)
                        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
                        confirmed = true
                        holding = false
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        confirm()
                    }
                    waitForUpOrCancellation()
                    if (!confirmed) {
                        holding = false
                        fill.cancel()
                        scope.launch { progress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        HoldLabel(label, icon, accent)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    clipRect(right = size.width * progress.value) { this@drawWithContent.drawContent() }
                }
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            HoldLabel(label, icon, onAccent)
        }
    }
}

@Composable
private fun HoldLabel(label: String, icon: ImageVector?, color: Color) {
    Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(LABEL_FADE_MS)) togetherWith fadeOut(tween(LABEL_FADE_MS)) },
            label = "holdLabel"
        ) { shown ->
            Text(
                shown,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val DEFAULT_HOLD_MILLIS = 2_000
private const val LABEL_FADE_MS = 140
