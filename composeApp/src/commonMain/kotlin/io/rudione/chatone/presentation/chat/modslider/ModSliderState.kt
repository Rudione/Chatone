package io.rudione.chatone.presentation.chat.modslider

import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.rudione.chatone.domain.moderation.ModSliderAction
import io.rudione.chatone.domain.moderation.ModSliderGeometry
import io.rudione.chatone.domain.moderation.ModSliderMetrics
import io.rudione.chatone.domain.moderation.ModSliderTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class ModSliderHost {
    var active: ModSliderState? by mutableStateOf(null)
        private set

    val isEngaged: Boolean get() = active != null

    private var hoveredBy: ModSliderState? by mutableStateOf(null)

    val cursor: PointerIcon? get() = if (active != null || hoveredBy != null) PointerIcon.Hand else null

    fun cancelActive() {
        active?.cancel()
    }

    internal fun onZoneEntered(state: ModSliderState) {
        hoveredBy = state
    }

    internal fun onZoneExited(state: ModSliderState) {
        if (hoveredBy === state) hoveredBy = null
    }

    internal fun onEngaged(state: ModSliderState) {
        val previous = active
        if (previous != null && previous !== state) previous.cancel()
        active = state
    }

    internal fun onReleased(state: ModSliderState) {
        if (active === state) active = null
    }
}

@Stable
class ModSliderState internal constructor(
    private val host: ModSliderHost,
    private val scope: CoroutineScope
) : RememberObserver {

    var offset by mutableFloatStateOf(0f)
        private set

    var engaged by mutableStateOf(false)
        private set

    var selected: ModSliderAction? by mutableStateOf(null)
        private set

    var raised by mutableStateOf(false)
        private set

    val lift = Animatable(0f)

    internal var rowWidth = 0f

    var geometry: ModSliderGeometry? = null
        private set

    var track: ModSliderTrack = ModSliderTrack.Empty
        private set

    private var raw = 0f
    private var index = ModSliderGeometry.NONE
    private var settleJob: Job? = null

    fun begin(track: ModSliderTrack, metrics: ModSliderMetrics, travelFraction: Float): Boolean {
        if (track.isEmpty) return false
        settleJob?.cancel()
        this.track = track
        geometry = ModSliderGeometry.resolve(
            metrics = metrics,
            travelFraction = travelFraction,
            availableWidth = rowWidth,
            weights = track.weights,
            hasBackward = track.backward != null
        )
        raw = offset
        index = ModSliderGeometry.NONE
        engaged = true
        raised = true
        host.onEngaged(this)
        scope.launch { lift.animateTo(1f, tween(LIFT_IN_MS)) }
        update()
        return true
    }

    fun dragBy(delta: Float) {
        if (!engaged) return
        raw += delta
        update()
    }

    fun release(): ModSliderAction? {
        if (!engaged) return null
        val action = selected
        settle(keepSelection = action != null)
        return action
    }

    fun cancel() {
        if (!engaged) return
        settle(keepSelection = false)
    }

    private fun update() {
        val g = geometry ?: return
        index = if (raw >= 0f) g.forwardIndex(raw, index) else ModSliderGeometry.NONE
        selected = when {
            raw < 0f -> track.backward.takeIf { g.backwardArmed(raw) }
            index >= 0 -> track.forward[index]
            else -> null
        }
        offset = g.displayOffset(raw)
    }

    private fun settle(keepSelection: Boolean) {
        engaged = false
        index = ModSliderGeometry.NONE
        if (!keepSelection) selected = null
        host.onReleased(this)
        settleJob?.cancel()
        settleJob = scope.launch {
            launch { lift.animateTo(0f, tween(LIFT_OUT_MS)) }
            animate(
                initialValue = offset,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = SETTLE_DAMPING, stiffness = Spring.StiffnessMediumLow)
            ) { value, _ -> offset = value }
            selected = null
            raised = false
        }
    }

    override fun onRemembered() = Unit

    internal fun zoneEntered() = host.onZoneEntered(this)

    internal fun zoneExited() = host.onZoneExited(this)

    override fun onForgotten() {
        cancel()
        host.onZoneExited(this)
    }

    override fun onAbandoned() {
        cancel()
        host.onZoneExited(this)
    }

    private companion object {
        const val LIFT_IN_MS = 140
        const val LIFT_OUT_MS = 220
        const val SETTLE_DAMPING = 0.72f
    }
}

@Composable
fun rememberModSliderState(host: ModSliderHost): ModSliderState {
    val scope = rememberCoroutineScope()
    return remember(host, scope) { ModSliderState(host, scope) }
}

@Composable
fun ModSliderGuard(host: ModSliderHost) {
    if (!host.isEngaged) return
    val windowInfo = LocalWindowInfo.current
    DisposableEffect(host) {
        val unregister = GlobalKeyDispatcher.registerShortcut { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                host.cancelActive()
                true
            } else false
        }
        onDispose { unregister() }
    }
    LaunchedEffect(host, windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.first { focused -> !focused }
        host.cancelActive()
    }
}
