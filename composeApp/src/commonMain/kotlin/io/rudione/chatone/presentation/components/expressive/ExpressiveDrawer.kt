package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.SwipeDirection
import io.rudione.chatone.presentation.components.SwipeDragHandler
import io.rudione.chatone.presentation.components.SystemPredictiveBackHandler
import io.rudione.chatone.presentation.components.directionalDrag
import io.rudione.chatone.presentation.components.directionalSwipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

object ExpressiveDrawerDefaults {
    val FallbackWidth: Dp = 264.dp
    val FlingVelocity: Dp = 400.dp
    const val SCRIM_ALPHA = 0.45f
    const val OPEN_FRACTION = 0.5f
    const val STRETCH_LIMIT = 0.04f
    const val STRETCH_RESISTANCE = 0.18f
    const val BACK_TRAVEL = 0.22f
    fun settleSpec() = spring<Float>(dampingRatio = 0.8f, stiffness = 380f)
}

@Stable
class ExpressiveDrawerState internal constructor(private val scope: CoroutineScope) : SwipeDragHandler {
    private val position = Animatable(0f)
    private var dragging by mutableStateOf(false)
    private var target by mutableStateOf(false)
    private var dragPosition = 0f
    private var requested = false
    internal var measuredWidthPx = 0f
    internal var density: Density = Density(1f)
    internal var onOpenChange: (Boolean) -> Unit = {}

    private val widthPx: Float
        get() = measuredWidthPx.takeIf { it > 0f } ?: with(density) { ExpressiveDrawerDefaults.FallbackWidth.toPx() }

    val progress: Float get() = position.value

    internal val isComposed by derivedStateOf { dragging || target || position.value > 0f }

    internal fun sync(open: Boolean) {
        requested = open
        if (dragging || open == target) return
        animateTo(open, 0f)
    }

    fun open() = settle(true, 0f)

    fun close() = settle(false, 0f)

    override fun onSwipeDrag(deltaPx: Float) {
        if (!dragging) {
            dragging = true
            dragPosition = position.value
        }
        dragPosition = DrawerSettle.clampRaw(dragPosition + deltaPx / widthPx)
        val shown = DrawerSettle.stretched(dragPosition)
        scope.launch { position.snapTo(shown) }
    }

    override fun onSwipeRelease(velocityPx: Float) {
        if (!dragging) return
        dragging = false
        val flingPx = with(density) { ExpressiveDrawerDefaults.FlingVelocity.toPx() }
        settle(DrawerSettle.shouldOpen(position.value, velocityPx, flingPx), velocityPx / widthPx)
    }

    internal fun onBackProgress(fraction: Float) {
        dragging = true
        scope.launch { position.snapTo(1f - fraction * ExpressiveDrawerDefaults.BACK_TRAVEL) }
    }

    internal fun onBackCancelled() {
        dragging = false
        settle(true, 0f)
    }

    internal fun onBackCommitted() {
        dragging = false
        settle(false, 0f)
    }

    private fun settle(open: Boolean, velocity: Float) {
        animateTo(open, velocity)
        if (open != requested) {
            requested = open
            onOpenChange(open)
        }
    }

    private fun animateTo(open: Boolean, velocity: Float) {
        target = open
        scope.launch {
            position.animateTo(if (open) 1f else 0f, ExpressiveDrawerDefaults.settleSpec(), initialVelocity = velocity)
        }
    }
}

internal object DrawerSettle {
    fun shouldOpen(position: Float, velocityPx: Float, flingPx: Float): Boolean =
        if (abs(velocityPx) >= flingPx) velocityPx > 0f else position >= ExpressiveDrawerDefaults.OPEN_FRACTION

    fun clampRaw(raw: Float): Float =
        raw.coerceIn(0f, 1f + ExpressiveDrawerDefaults.STRETCH_LIMIT / ExpressiveDrawerDefaults.STRETCH_RESISTANCE)

    fun stretched(raw: Float): Float =
        if (raw <= 1f) raw else 1f + (raw - 1f) * ExpressiveDrawerDefaults.STRETCH_RESISTANCE
}

val LocalModalDrawer = staticCompositionLocalOf<ExpressiveDrawerState?> { null }

@Composable
fun rememberExpressiveDrawerState(): ExpressiveDrawerState {
    val scope = rememberCoroutineScope()
    return remember(scope) { ExpressiveDrawerState(scope) }
}

fun Modifier.openDrawerSwipe(drawer: ExpressiveDrawerState?, onOpen: () -> Unit): Modifier =
    if (drawer != null) directionalDrag(SwipeDirection.Right, drawer) else directionalSwipe(SwipeDirection.Right, onOpen)

@Composable
fun BoxScope.ExpressiveModalDrawer(
    state: ExpressiveDrawerState,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    scrimColor: Color = Color.Black,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    SideEffect {
        state.density = density
        state.onOpenChange = onOpenChange
    }
    LaunchedEffect(state, open) { state.sync(open) }
    SystemPredictiveBackHandler(
        enabled = open,
        onProgress = state::onBackProgress,
        onCancel = state::onBackCancelled,
        onBack = state::onBackCommitted
    )
    if (!state.isComposed) return
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = state.progress.coerceIn(0f, 1f) * ExpressiveDrawerDefaults.SCRIM_ALPHA }
            .background(scrimColor)
            .directionalDrag(SwipeDirection.Left, state)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.close() }
    )
    Box(
        Modifier
            .align(Alignment.TopStart)
            .onSizeChanged { if (it.width > 0) state.measuredWidthPx = it.width.toFloat() }
            .graphicsLayer {
                val p = state.progress
                translationX = -(1f - p.coerceAtMost(1f)) * size.width
                scaleX = 1f + (p - 1f).coerceAtLeast(0f)
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .directionalDrag(SwipeDirection.Left, state)
    ) {
        content()
    }
}
