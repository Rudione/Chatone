package io.rudione.chatone.domain.moderation

import kotlin.math.max
import kotlin.math.min

sealed interface ModSliderAction {
    data object Delete : ModSliderAction
    data class Timeout(val seconds: Int) : ModSliderAction
    data object Ban : ModSliderAction
    data object Unban : ModSliderAction
}

object ModSliderDurations {
    const val MINUTE = 60
    const val HOUR = 3_600
    const val DAY = 86_400
    const val MAX_SECONDS = 14 * DAY

    val TWITCH_SCALE: List<Int> = buildList {
        for (second in 1..59) add(second)
        for (minute in 1..59) add(minute * MINUTE)
        for (hour in 1..23) add(hour * HOUR)
        for (day in 1..14) add(day * DAY)
    }

    fun format(seconds: Int): String = when {
        seconds >= DAY && seconds % DAY == 0 -> "${seconds / DAY}d"
        seconds >= HOUR && seconds % HOUR == 0 -> "${seconds / HOUR}h"
        seconds >= MINUTE && seconds % MINUTE == 0 -> "${seconds / MINUTE}m"
        else -> "${seconds}s"
    }

    fun unitOf(seconds: Int): Int = when {
        seconds >= DAY -> DAY
        seconds >= HOUR -> HOUR
        seconds >= MINUTE -> MINUTE
        else -> 1
    }
}

class ModSliderTrack private constructor(
    val forward: List<ModSliderAction>,
    val backward: ModSliderAction?,
    val weights: FloatArray,
    val milestones: List<Int>
) {
    val isEmpty: Boolean get() = forward.isEmpty() && backward == null

    override fun equals(other: Any?): Boolean =
        other is ModSliderTrack && forward == other.forward && backward == other.backward

    override fun hashCode(): Int = 31 * forward.hashCode() + (backward?.hashCode() ?: 0)

    companion object {
        val Empty = ModSliderTrack(emptyList(), null, FloatArray(0), emptyList())

        fun build(
            deleteAtStart: Boolean,
            durations: List<Int>,
            banAtEnd: Boolean,
            canDelete: Boolean,
            canPunish: Boolean
        ): ModSliderTrack {
            val timeouts = if (canPunish) durations else emptyList()
            val grouped = timeouts.size > MAX_EVEN_STOPS
            val unitCounts = timeouts.groupingBy(ModSliderDurations::unitOf).eachCount()
            val edgeWeight = if (grouped) GROUPED_EDGE_WEIGHT else 1f
            val forward = mutableListOf<ModSliderAction>()
            val weights = mutableListOf<Float>()
            val milestones = mutableListOf<Int>()
            if (deleteAtStart && canDelete) {
                milestones += forward.size
                forward += ModSliderAction.Delete
                weights += edgeWeight
            }
            var previousUnit = -1
            timeouts.forEach { seconds ->
                val unit = ModSliderDurations.unitOf(seconds)
                if (!grouped || unit != previousUnit) milestones += forward.size
                previousUnit = unit
                forward += ModSliderAction.Timeout(seconds)
                weights += if (grouped) 1f / (unitCounts[unit] ?: 1) else 1f
            }
            if (canPunish && banAtEnd) {
                milestones += forward.size
                forward += ModSliderAction.Ban
                weights += edgeWeight
            }
            return ModSliderTrack(
                forward = forward,
                backward = if (canPunish) ModSliderAction.Unban else null,
                weights = weights.toFloatArray(),
                milestones = milestones
            )
        }

        private const val MAX_EVEN_STOPS = 16
        private const val GROUPED_EDGE_WEIGHT = 0.35f
    }
}

data class ModSliderMetrics(
    val deadZone: Float,
    val minTravel: Float,
    val backwardReach: Float,
    val hysteresis: Float,
    val overshoot: Float
)

class ModSliderGeometry(
    val metrics: ModSliderMetrics,
    val travel: Float,
    weights: FloatArray,
    val hasBackward: Boolean
) {
    private val starts: FloatArray
    private val ends: FloatArray

    init {
        val total = weights.sum().takeIf { it > 0f } ?: 1f
        val span = (travel - metrics.deadZone).coerceAtLeast(0f)
        starts = FloatArray(weights.size)
        ends = FloatArray(weights.size)
        var cursor = metrics.deadZone
        weights.forEachIndexed { index, weight ->
            starts[index] = cursor
            cursor += span * weight / total
            ends[index] = cursor
        }
    }

    val stops: Int get() = starts.size

    private val backwardLimit: Float
        get() = if (hasBackward) metrics.backwardReach else metrics.deadZone

    fun threshold(index: Int): Float = starts[index]

    fun forwardIndex(offset: Float, previous: Int): Int {
        if (stops == 0) return NONE
        if (previous in 0 until stops) {
            val slack = min(metrics.hysteresis, (ends[previous] - starts[previous]) * HYSTERESIS_SHARE)
            val low = starts[previous] - slack
            val high = if (previous == stops - 1) Float.POSITIVE_INFINITY else ends[previous] + slack
            if (offset in low..high) return previous
        }
        if (offset < metrics.deadZone) return NONE
        var lo = 0
        var hi = stops - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (starts[mid] <= offset) lo = mid else hi = mid - 1
        }
        return lo
    }

    fun backwardArmed(offset: Float): Boolean = hasBackward && offset <= -metrics.backwardReach

    fun displayOffset(raw: Float): Float = when {
        raw > travel -> travel + damp(raw - travel)
        raw < -backwardLimit -> -backwardLimit - damp(-backwardLimit - raw)
        else -> raw
    }

    fun progress(display: Float): Float =
        if (display >= 0f) (display / travel).coerceIn(0f, 1f) else (-display / backwardLimit).coerceIn(0f, 1f)

    private fun damp(excess: Float): Float {
        val limit = metrics.overshoot
        if (limit <= 0f) return 0f
        return limit * (1f - 1f / (excess / limit + 1f))
    }

    companion object {
        const val NONE = -1
        const val MIN_FRACTION = 0.3f
        const val MAX_FRACTION = 0.95f
        private const val HYSTERESIS_SHARE = 0.4f

        fun resolve(
            metrics: ModSliderMetrics,
            travelFraction: Float,
            availableWidth: Float,
            weights: FloatArray,
            hasBackward: Boolean
        ): ModSliderGeometry {
            val fraction = travelFraction.coerceIn(MIN_FRACTION, MAX_FRACTION)
            val floor = min(metrics.minTravel, availableWidth * MAX_FRACTION)
            val travel = max(availableWidth * fraction, floor).coerceAtLeast(metrics.deadZone * 2f)
            return ModSliderGeometry(metrics, travel, weights, hasBackward)
        }
    }
}
