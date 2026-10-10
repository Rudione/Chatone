package io.rudione.chatone.domain.stream

import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.min

class LiveSpeedPolicy(private val tuning: Tuning = Tuning()) {

    data class Tuning(
        val updateIntervalMs: Long = 1_000L,
        val deadbandUs: Long = 120_000L,
        val gainPerSecond: Float = 0.07f,
        val criticalBufferUs: Long = 300_000L,
        val catchUpBufferUs: Long = 600_000L,
        val rebufferStepUs: Long = 300_000L,
        val maxExtraUs: Long = 2_500_000L,
        val recoveryDelayMs: Long = 20_000L,
        val recoveryStepUs: Long = 100_000L
    )

    @Volatile
    var targetUs: Long = UNSET
        private set

    private var configuredUs = UNSET
    private var overrideUs = UNSET
    private var minTargetUs = UNSET
    private var maxTargetUs = UNSET
    private var minSpeed = 1f
    private var maxSpeed = 1f
    private var speed = 1f
    private var lastUpdateMs = UNSET
    private var lastRebufferMs = UNSET

    fun configure(targetUs: Long, minTargetUs: Long, maxTargetUs: Long, minSpeed: Float, maxSpeed: Float) {
        this.minTargetUs = minTargetUs
        this.maxTargetUs = maxTargetUs
        this.minSpeed = minSpeed.coerceAtMost(1f)
        this.maxSpeed = maxSpeed.coerceAtLeast(1f)
        val configured = if (this.minSpeed == 1f && this.maxSpeed == 1f) UNSET else targetUs
        if (configured == configuredUs) return
        configuredUs = configured
        resetTarget()
    }

    fun override(liveOffsetUs: Long) {
        if (liveOffsetUs == overrideUs) return
        overrideUs = liveOffsetUs
        resetTarget()
    }

    fun onRebuffer(nowMs: Long) {
        val ideal = idealUs()
        if (ideal == UNSET || targetUs == UNSET) return
        targetUs = boundedByMax(min(targetUs + tuning.rebufferStepUs, ideal + tuning.maxExtraUs))
        lastRebufferMs = nowMs
        lastUpdateMs = UNSET
    }

    fun speed(liveOffsetUs: Long, bufferedUs: Long, nowMs: Long): Float {
        if (targetUs == UNSET || liveOffsetUs == UNSET) return 1f
        if (lastUpdateMs != UNSET && nowMs - lastUpdateMs < tuning.updateIntervalMs) return speed
        lastUpdateMs = nowMs
        recover(nowMs)
        val errorUs = liveOffsetUs - targetUs
        speed = when {
            bufferedUs < tuning.criticalBufferUs -> minSpeed
            abs(errorUs) <= tuning.deadbandUs -> 1f
            errorUs > 0 && bufferedUs < tuning.catchUpBufferUs -> 1f
            else -> (1f + tuning.gainPerSecond * errorUs / MICROS_PER_SECOND).coerceIn(minSpeed, maxSpeed)
        }
        return speed
    }

    private fun recover(nowMs: Long) {
        val ideal = idealUs()
        if (ideal == UNSET || targetUs <= ideal) return
        if (lastRebufferMs != UNSET && nowMs - lastRebufferMs < tuning.recoveryDelayMs) return
        targetUs = (targetUs - tuning.recoveryStepUs).coerceAtLeast(ideal)
    }

    private fun idealUs(): Long {
        if (configuredUs == UNSET) return UNSET
        if (overrideUs != UNSET) return overrideUs
        var ideal = configuredUs
        if (minTargetUs != UNSET && ideal < minTargetUs) ideal = minTargetUs
        return boundedByMax(ideal)
    }

    private fun boundedByMax(valueUs: Long): Long =
        if (maxTargetUs != UNSET && valueUs > maxTargetUs) maxTargetUs else valueUs

    private fun resetTarget() {
        targetUs = idealUs()
        lastUpdateMs = UNSET
        lastRebufferMs = UNSET
        speed = 1f
    }

    companion object {
        const val UNSET = Long.MIN_VALUE
        private const val MICROS_PER_SECOND = 1_000_000f
    }
}

class LiveEdgeGuard(
    private val excessUs: Long = 3_000_000L,
    private val persistenceMs: Long = 3_000L,
    private val cooldownMs: Long = 30_000L
) {
    private var excessSinceMs = LiveSpeedPolicy.UNSET
    private var lastJumpMs = LiveSpeedPolicy.UNSET

    fun shouldJump(liveOffsetUs: Long, targetUs: Long, nowMs: Long): Boolean {
        val unset = LiveSpeedPolicy.UNSET
        if (liveOffsetUs == unset || targetUs == unset || liveOffsetUs - targetUs <= excessUs) {
            excessSinceMs = unset
            return false
        }
        if (excessSinceMs == unset) excessSinceMs = nowMs
        val persisted = nowMs - excessSinceMs >= persistenceMs
        val cooledDown = lastJumpMs == unset || nowMs - lastJumpMs >= cooldownMs
        if (!persisted || !cooledDown) return false
        lastJumpMs = nowMs
        excessSinceMs = unset
        return true
    }

    fun reset() {
        excessSinceMs = LiveSpeedPolicy.UNSET
        lastJumpMs = LiveSpeedPolicy.UNSET
    }
}
