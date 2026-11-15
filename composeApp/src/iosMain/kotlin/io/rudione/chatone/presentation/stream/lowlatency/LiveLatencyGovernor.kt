package io.rudione.chatone.presentation.stream.lowlatency

import io.rudione.chatone.domain.stream.LiveEdgeGuard
import io.rudione.chatone.domain.stream.LiveSpeedPolicy

internal class LiveLatencyGovernor(
    private val policy: LiveSpeedPolicy = LiveSpeedPolicy(),
    private val edgeGuard: LiveEdgeGuard = LiveEdgeGuard(landingBufferUs = Long.MIN_VALUE)
) {
    class Profile(val targetMs: Long, val minMs: Long, val maxMs: Long)

    val targetMs: Long
        get() = policy.targetUs.let { if (it == LiveSpeedPolicy.UNSET) Long.MAX_VALUE else it / MICROS_PER_MILLI }

    fun configure(profile: Profile) {
        policy.configure(
            targetUs = profile.targetMs * MICROS_PER_MILLI,
            minTargetUs = profile.minMs * MICROS_PER_MILLI,
            maxTargetUs = profile.maxMs * MICROS_PER_MILLI,
            minSpeed = MIN_RATE,
            maxSpeed = MAX_RATE
        )
        edgeGuard.reset()
    }

    fun onRebuffer(nowMs: Long) = policy.onRebuffer(nowMs)

    fun onPlaybackInterrupted() = edgeGuard.reset()

    fun rate(latencyMs: Long, bufferedMs: Long, nowMs: Long): Float =
        policy.speed(latencyMs * MICROS_PER_MILLI, bufferedMs * MICROS_PER_MILLI, nowMs)

    fun shouldRejoin(latencyMs: Long, nowMs: Long): Boolean =
        edgeGuard.shouldJump(latencyMs * MICROS_PER_MILLI, policy.targetUs, 0L, nowMs)

    companion object {
        val LOW_LATENCY = Profile(targetMs = 1_600L, minMs = 1_000L, maxMs = 6_000L)
        val NORMAL = Profile(targetMs = 5_000L, minMs = 3_000L, maxMs = 12_000L)
        private const val MICROS_PER_MILLI = 1_000L
        private const val MIN_RATE = 0.95f
        private const val MAX_RATE = 1.1f
    }
}
