package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.SystemBackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private const val DISMISS_FRACTION = 0.3f
private const val DISMISS_VELOCITY = 1_400f
private const val SCRIM_ALPHA = 0.5f
private val WHEEL_QUIET = 350.milliseconds

@Stable
class ExpressiveSheetState internal constructor(
    private val scope: CoroutineScope,
    private val onDismissed: () -> Unit
) {
    internal val hidden = Animatable(1f)
    internal var height by mutableIntStateOf(0)
    private var closing by mutableStateOf(false)

    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch {
            hidden.animateTo(1f, tween(200, easing = FastOutLinearInEasing))
            then()
            onDismissed()
        }
    }

    internal fun open() {
        scope.launch { hidden.animateTo(0f, settleSpec()) }
    }

    private var position = 1f
    private var dragging = false

    internal fun dragBy(deltaPx: Float): Float {
        if (closing || height <= 0) return 0f
        if (!dragging) {
            position = hidden.value
            dragging = true
        }
        val before = position
        position = (before + deltaPx / height).coerceIn(0f, 1f)
        val target = position
        scope.launch { hidden.snapTo(target) }
        return (target - before) * height
    }

    internal fun settle(velocity: Float) {
        val travelled = if (dragging) position else hidden.value
        dragging = false
        if (closing) return
        if (velocity > DISMISS_VELOCITY || travelled > DISMISS_FRACTION) close() else open()
    }

    internal val isOffset: Boolean get() = (if (dragging) position else hidden.value) > 0f

    private var lastWheel: TimeMark? = null

    internal fun noteWheel() {
        lastWheel = TimeSource.Monotonic.markNow()
    }

    internal val wheelActive: Boolean get() = lastWheel?.let { it.elapsedNow() < WHEEL_QUIET } == true

    private fun settleSpec() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

@Composable
fun ExpressiveSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(ExpressiveSheetState) -> Unit
) {
    SwipeDismissSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = 48.dp)
            .widthIn(max = 640.dp)
            .fillMaxWidth(),
        surface = Modifier
            .shadow(16.dp, SheetShape)
            .clip(SheetShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) { state ->
        Column(
            Modifier.heightIn(min = 0.dp).windowInsetsPadding(WindowInsets.navigationBars)
        ) { content(state) }
    }
}

@Composable
fun SwipeDismissSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    surface: Modifier = Modifier,
    scrimAlpha: Float = SCRIM_ALPHA,
    handleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    content: @Composable ColumnScope.(ExpressiveSheetState) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismissRequest)
    val state = remember(scope) { ExpressiveSheetState(scope) { dismiss() } }
    LaunchedEffect(state) { state.open() }
    SystemBackHandler { state.close() }
    val connection = remember(state) { SheetNestedScroll(state) }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - state.hidden.value }
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { state.close() }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(modifier)
                .onSizeChanged { state.height = it.height }
                .graphicsLayer { translationY = state.hidden.value * state.height }
                .then(surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .pointerInput(state) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Scroll) state.noteWheel()
                        }
                    }
                }
                .nestedScroll(connection)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> state.dragBy(delta) },
                    onDragStopped = { velocity -> state.settle(velocity) }
                )
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
            content(state)
        }
    }
}

private class SheetNestedScroll(private val state: ExpressiveSheetState) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0f && state.isOffset && source == NestedScrollSource.UserInput && !state.wheelActive) {
            Offset(0f, state.dragBy(available.y))
        } else {
            Offset.Zero
        }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset =
        if (available.y > 0f && source == NestedScrollSource.UserInput && !state.wheelActive) {
            Offset(0f, state.dragBy(available.y))
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!state.isOffset) return Velocity.Zero
        state.settle(available.y)
        return available
    }
}
