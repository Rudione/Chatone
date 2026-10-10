package io.rudione.chatone.presentation.stream.ambient

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.stream.VideoFrameSink
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

@Stable
class StreamAmbience internal constructor() : VideoFrameSink {

    override val sampleWidth: Int = SAMPLE_WIDTH
    override val sampleHeight: Int = SAMPLE_HEIGHT
    override val sampleIntervalMs: Long = SAMPLE_INTERVAL_MS

    private val sample = FloatArray(AmbientColors.SIZE)
    private val target = FloatArray(AmbientColors.SIZE)
    private val current = FloatArray(AmbientColors.SIZE)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var hasColors = false
    private var intensity = 0f
    private var intensityTarget = 0f
    private var version by mutableIntStateOf(0)
    private var glowConsumers by mutableIntStateOf(0)

    internal val glowAttached: Boolean get() = glowConsumers > 0

    override fun accept(argb: IntArray) {
        AmbientColors.analyze(argb, sampleWidth, sampleHeight, sample)
        if (!hasColors || intensity < VISIBLE_INTENSITY) {
            sample.copyInto(target)
            sample.copyInto(current)
            hasColors = true
            version++
        } else {
            var change = 0f
            for (i in target.indices) {
                val next = target[i] + (sample[i] - target[i]) * SAMPLE_BLEND
                target[i] = next
                change = max(change, abs(next - current[i]))
            }
            if (change < SIGNIFICANT_CHANGE && intensityTarget == 1f) return
        }
        intensityTarget = 1f
        wake.trySend(Unit)
    }

    fun fadeOut() {
        if (intensityTarget == 0f) return
        intensityTarget = 0f
        wake.trySend(Unit)
    }

    internal suspend fun animate() {
        for (signal in wake) {
            var previousFrame = Long.MIN_VALUE
            do {
                val frame = withFrameMillis { it }
                val elapsed = if (previousFrame == Long.MIN_VALUE) STEP_MS else (frame - previousFrame).coerceIn(1L, MAX_STEP_MS)
                previousFrame = frame
                val moving = step(elapsed)
                version++
                if (moving) delay(STEP_MS)
            } while (moving)
        }
    }

    internal fun attachGlow() {
        glowConsumers++
    }

    internal fun detachGlow() {
        glowConsumers = (glowConsumers - 1).coerceAtLeast(0)
    }

    internal fun visibleIntensity(): Float {
        version
        return if (hasColors && intensity >= VISIBLE_INTENSITY) intensity else 0f
    }

    internal fun barColors(edge: AmbientEdge, alpha: Float): List<Color> =
        List(AmbientColors.STOPS) { stop -> AmbientColors.barColor(current, AmbientColors.index(edge, stop), alpha) }

    internal fun glowColors(edge: AmbientEdge, alpha: Float, darkBackground: Boolean): List<Color> =
        List(AmbientColors.STOPS) { stop ->
            AmbientColors.glowColor(current, AmbientColors.index(edge, stop), alpha, darkBackground)
        }

    private fun step(elapsedMs: Long): Boolean {
        val colorFactor = 1f - exp(-elapsedMs / COLOR_TIME_CONSTANT_MS)
        var remaining = 0f
        for (i in current.indices) {
            val delta = target[i] - current[i]
            current[i] += delta * colorFactor
            remaining = max(remaining, abs(target[i] - current[i]))
        }
        if (remaining < SETTLED) target.copyInto(current)
        val intensityFactor = 1f - exp(-elapsedMs / INTENSITY_TIME_CONSTANT_MS)
        intensity += (intensityTarget - intensity) * intensityFactor
        val intensitySettled = abs(intensityTarget - intensity) < SETTLED
        if (intensitySettled) intensity = intensityTarget
        return remaining >= SETTLED || !intensitySettled
    }

    private companion object {
        const val SAMPLE_WIDTH = 64
        const val SAMPLE_HEIGHT = 36
        const val SAMPLE_INTERVAL_MS = 1_000L
        const val SAMPLE_BLEND = 0.6f
        const val SIGNIFICANT_CHANGE = 0.004f
        const val SETTLED = 0.0015f
        const val VISIBLE_INTENSITY = 0.01f
        const val COLOR_TIME_CONSTANT_MS = 900f
        const val INTENSITY_TIME_CONSTANT_MS = 350f
        const val STEP_MS = 33L
        const val MAX_STEP_MS = 100L
    }
}
