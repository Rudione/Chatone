package io.rudione.chatone.domain.stream

class FrameSamplingBudget(
    private val share: Float = DEFAULT_SHARE,
    private val maxIntervalMs: Long = DEFAULT_MAX_INTERVAL_MS
) {
    private var typicalCostMs = 0f

    fun onSampled(costMs: Float) {
        val cost = costMs.coerceAtLeast(0f)
        typicalCostMs = if (typicalCostMs == 0f) cost else typicalCostMs + (cost - typicalCostMs) * SMOOTHING
    }

    fun intervalMs(minIntervalMs: Long): Long =
        (typicalCostMs / share).toLong().coerceIn(minIntervalMs, maxOf(minIntervalMs, maxIntervalMs))

    fun reset() {
        typicalCostMs = 0f
    }

    private companion object {
        const val DEFAULT_SHARE = 0.04f
        const val DEFAULT_MAX_INTERVAL_MS = 8_000L
        const val SMOOTHING = 0.3f
    }
}
