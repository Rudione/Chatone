package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

internal object PaintAnimationClock {

    private const val FRAME_MS = 40L
    private const val REPEAT_CYCLE_MS = 4_000L
    private const val SWAY_HALF_MS = 2_800L
    private const val SWAY_AMPLITUDE = 0.09f

    private val origin = TimeSource.Monotonic.markNow()
    private val time = mutableLongStateOf(0L)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var holders = 0
    private var ticker: Job? = null

    val nowMs: State<Long> get() = time

    fun retain() {
        holders++
        if (ticker?.isActive == true) return
        time.longValue = elapsedMs()
        ticker = scope.launch {
            while (isActive) {
                delay(FRAME_MS)
                time.longValue = elapsedMs()
            }
        }
    }

    fun release() {
        if (--holders > 0) return
        holders = 0
        ticker?.cancel()
        ticker = null
    }

    fun phaseAt(timeMs: Long, repeat: Boolean): Float {
        if (repeat) return (timeMs % REPEAT_CYCLE_MS).toFloat() / REPEAT_CYCLE_MS
        val cycle = timeMs % (SWAY_HALF_MS * 2)
        val forward = cycle < SWAY_HALF_MS
        val progress = (if (forward) cycle else cycle - SWAY_HALF_MS).toFloat() / SWAY_HALF_MS
        val eased = FastOutSlowInEasing.transform(if (forward) progress else 1f - progress)
        return -SWAY_AMPLITUDE + 2 * SWAY_AMPLITUDE * eased
    }

    private fun elapsedMs(): Long = origin.elapsedNow().inWholeMilliseconds
}

private class PaintClockHandle : RememberObserver {
    override fun onRemembered() = PaintAnimationClock.retain()
    override fun onForgotten() = PaintAnimationClock.release()
    override fun onAbandoned() = Unit
}

@Composable
internal fun rememberSharedPaintPhase(repeat: Boolean): State<Float> {
    remember { PaintClockHandle() }
    return remember(repeat) {
        derivedStateOf { PaintAnimationClock.phaseAt(PaintAnimationClock.nowMs.value, repeat) }
    }
}
