package io.rudione.chatone.presentation.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Stable
internal class ProfileTabSwipe(private val scope: CoroutineScope) {
    val offset = Animatable(0f)
    var width = 1f

    fun drag(delta: Float, canGoBack: Boolean, canGoForward: Boolean) {
        val next = offset.value + delta
        val blocked = (next > 0f && !canGoBack) || (next < 0f && !canGoForward)
        scope.launch { offset.snapTo(if (blocked) offset.value + delta * EDGE_RESISTANCE else next) }
    }

    fun release(velocity: Float, onSwitch: (direction: Int) -> Boolean) {
        val travelled = offset.value
        val direction = when {
            abs(velocity) > FLING_VELOCITY -> -sign(velocity).toInt()
            abs(travelled) > width * COMMIT_FRACTION -> -sign(travelled).toInt()
            else -> 0
        }
        scope.launch {
            if (direction != 0 && onSwitch(direction)) {
                offset.animateTo(-direction * width * EXIT_FRACTION, tween(EXIT_MS))
                offset.snapTo(direction * width * ENTER_FRACTION)
            }
            offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    fun contentAlpha(): Float = 1f - (abs(offset.value) / width).coerceIn(0f, 1f) * FADE

    private companion object {
        const val EDGE_RESISTANCE = 0.25f
        const val FLING_VELOCITY = 900f
        const val COMMIT_FRACTION = 0.22f
        const val EXIT_FRACTION = 0.45f
        const val ENTER_FRACTION = 0.3f
        const val EXIT_MS = 120
        const val FADE = 0.7f
    }
}

internal val LocalProfileTabSwipe = staticCompositionLocalOf<ProfileTabSwipe?> { null }

@Composable
internal fun rememberProfileTabSwipe(): ProfileTabSwipe {
    val scope = rememberCoroutineScope()
    return remember(scope) { ProfileTabSwipe(scope) }
}

internal fun Modifier.profileTabSwipe(
    swipe: ProfileTabSwipe,
    current: ProfileTab,
    onSelect: (ProfileTab) -> Unit
): Modifier = composed {
    val tab by rememberUpdatedState(current)
    val select by rememberUpdatedState(onSelect)
    onSizeChanged { swipe.width = it.width.coerceAtLeast(1).toFloat() }
        .pointerInput(swipe) {
            val tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = { tracker.resetTracking() },
                onDragEnd = {
                    swipe.release(tracker.calculateVelocity().x) { direction ->
                        val target = ProfileTab.entries.getOrNull(tab.ordinal + direction) ?: return@release false
                        select(target)
                        true
                    }
                },
                onDragCancel = { swipe.release(0f) { false } },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    tracker.addPosition(change.uptimeMillis, change.position)
                    swipe.drag(
                        amount,
                        canGoBack = tab.ordinal > 0,
                        canGoForward = tab.ordinal < ProfileTab.entries.lastIndex
                    )
                }
            )
        }
}

internal fun Modifier.followsProfileTabSwipe(swipe: ProfileTabSwipe?): Modifier =
    if (swipe == null) this else graphicsLayer {
        translationX = swipe.offset.value
        alpha = swipe.contentAlpha()
    }
