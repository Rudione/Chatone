package io.rudione.chatone.presentation.stream

import io.rudione.chatone.domain.stream.LiveEdgeGuard
import io.rudione.chatone.domain.stream.LiveSpeedPolicy

internal class IosLiveRateControl(
    private val policy: LiveSpeedPolicy = LiveSpeedPolicy(),
    private val lagGuard: LiveEdgeGuard = LiveEdgeGuard(
        excessUs = RELOAD_EXCESS_US,
        persistenceMs = RELOAD_PERSISTENCE_MS,
        cooldownMs = RELOAD_COOLDOWN_MS,
        landingBufferUs = Long.MIN_VALUE
    )
) {
    private var targetUs = LiveSpeedPolicy.UNSET

    fun enable(targetLatencyMs: Long) {
        targetUs = targetLatencyMs * MICROS_PER_MILLI
        policy.configure(
            targetUs = targetUs,
            minTargetUs = MIN_TARGET_US,
            maxTargetUs = MAX_TARGET_US,
            minSpeed = MIN_RATE,
            maxSpeed = MAX_RATE
        )
    }

    fun disable() {
        targetUs = LiveSpeedPolicy.UNSET
        policy.configure(LiveSpeedPolicy.UNSET, LiveSpeedPolicy.UNSET, LiveSpeedPolicy.UNSET, 1f, 1f)
    }

    fun onRebuffer(nowMs: Long) = policy.onRebuffer(nowMs)

    fun rate(latencyMs: Long, bufferedMs: Long, nowMs: Long): Float =
        policy.speed(latencyMs * MICROS_PER_MILLI, bufferedMs * MICROS_PER_MILLI, nowMs)

    fun shouldReload(latencyMs: Long, nowMs: Long): Boolean =
        lagGuard.shouldJump(latencyMs * MICROS_PER_MILLI, targetUs, 0L, nowMs)

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
        const val MIN_TARGET_US = 4_500_000L
        const val MAX_TARGET_US = 9_000_000L
        const val RELOAD_EXCESS_US = 3_000_000L
        const val RELOAD_PERSISTENCE_MS = 4_000L
        const val RELOAD_COOLDOWN_MS = 30_000L
        const val MIN_RATE = 0.97f
        const val MAX_RATE = 1f
    }
}
