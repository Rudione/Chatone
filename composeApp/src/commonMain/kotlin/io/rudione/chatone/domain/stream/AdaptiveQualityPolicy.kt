package io.rudione.chatone.domain.stream

import kotlin.math.max
import kotlin.math.min

class TransferSample(val totalBytes: Long, val burstBps: Long, val steadyBps: Long)

class TransferSampler(
    private val burstGapNs: Long = 25_000_000L,
    private val minRunBytes: Long = 48_000L,
    private val minRunNs: Long = 10_000_000L,
    private val minTransferBytes: Long = 32_000L,
    private val minSteadyNs: Long = 150_000_000L
) {
    private var firstNs = UNSET
    private var lastNs = UNSET
    private var totalBytes = 0L
    private var runStartNs = UNSET
    private var runBytes = 0L
    private var bestRunBps = 0L

    fun onBytes(nowNs: Long, bytes: Int) {
        if (bytes <= 0) return
        totalBytes += bytes
        if (firstNs == UNSET) {
            firstNs = nowNs
            runStartNs = nowNs
            lastNs = nowNs
            return
        }
        if (nowNs - lastNs > burstGapNs) {
            closeRun()
            runStartNs = nowNs
            runBytes = 0L
        } else {
            runBytes += bytes
        }
        lastNs = nowNs
    }

    fun finish(): TransferSample? {
        closeRun()
        if (totalBytes < minTransferBytes || firstNs == UNSET) return null
        val span = lastNs - firstNs
        val steady = if (span >= minSteadyNs) bitsPerSecond(totalBytes, span) else 0L
        return TransferSample(totalBytes, bestRunBps, steady)
    }

    private fun closeRun() {
        val span = lastNs - runStartNs
        if (runStartNs != UNSET && runBytes >= minRunBytes && span >= minRunNs) {
            bestRunBps = max(bestRunBps, bitsPerSecond(runBytes, span))
        }
    }

    private fun bitsPerSecond(bytes: Long, spanNs: Long): Long = bytes * 8L * 1_000_000_000L / spanNs

    private companion object {
        const val UNSET = Long.MIN_VALUE
    }
}

class AdaptiveQualityPolicy(private val tuning: Tuning = Tuning()) {

    data class Tuning(
        val safety: Float = 0.8f,
        val burstFactor: Float = 1.4f,
        val slowFactor: Float = 0.85f,
        val riseWeight: Float = 0.5f,
        val fallWeight: Float = 0.6f,
        val rebufferCut: Float = 0.8f,
        val startupMs: Long = 8_000L,
        val upgradeHoldMs: Long = 8_000L,
        val probeIntervalMs: Long = 15_000L,
        val blockMs: Long = 15_000L,
        val maxBlockMs: Long = 120_000L,
        val calmResetMs: Long = 120_000L,
        val maxCapacityBps: Long = 400_000_000L
    )

    var capacityBps: Long = 0L
        private set

    private var measured = false
    private var sessionStartMs = 0L
    private var lastSwitchMs = NEVER
    private var healthySinceMs = 0L
    private var probing = false
    private var blockedBitrate = Long.MAX_VALUE
    private var blockedUntilMs = NEVER
    private var blockMs = tuning.blockMs

    fun start(nowMs: Long, initialCapacityBps: Long) {
        sessionStartMs = nowMs
        healthySinceMs = nowMs
        capacityBps = initialCapacityBps.coerceIn(1L, tuning.maxCapacityBps)
        measured = false
        probing = false
        lastSwitchMs = NEVER
    }

    fun onTransfer(sample: TransferSample, loadingBitrate: Long, nowMs: Long) {
        val evidence = max(sample.burstBps, sample.steadyBps)
        when {
            loadingBitrate <= 0L || evidence >= loadingBitrate * tuning.burstFactor -> {
                if (evidence > 0L) raiseTo(evidence)
            }
            sample.steadyBps in 1L until (loadingBitrate * tuning.slowFactor).toLong() -> {
                capacityBps = min(capacityBps, blend(capacityBps, sample.steadyBps, tuning.fallWeight))
                measured = true
                trouble(loadingBitrate, nowMs)
            }
        }
    }

    fun onRebuffer(loadingBitrate: Long, nowMs: Long) {
        if (loadingBitrate > 0L) capacityBps = min(capacityBps, (loadingBitrate * tuning.rebufferCut).toLong())
        trouble(loadingBitrate, nowMs)
    }

    fun select(bitrates: IntArray, current: Int, isAllowed: (Int) -> Boolean, nowMs: Long): Int {
        if (bitrates.isEmpty()) return 0
        if (nowMs - healthySinceMs >= tuning.calmResetMs) blockMs = tuning.blockMs
        val usable = { index: Int -> isAllowed(index) && !isBlocked(bitrates[index], nowMs) }
        maybeProbe(bitrates, current, usable, nowMs)
        val target = idealIndex(bitrates, usable, isAllowed)
        val inStartup = nowMs - sessionStartMs < tuning.startupMs
        if (current !in bitrates.indices || !isAllowed(current) || inStartup) return switchTo(current, target, nowMs)
        val upgradeHeld = target < current && !probing && nowMs - lastSwitchMs < tuning.upgradeHoldMs
        return if (upgradeHeld) current else switchTo(current, target, nowMs)
    }

    private fun maybeProbe(bitrates: IntArray, current: Int, usable: (Int) -> Boolean, nowMs: Long) {
        if (probing && nowMs - lastSwitchMs >= tuning.upgradeHoldMs) probing = false
        if (probing || current !in bitrates.indices || nowMs - healthySinceMs < tuning.probeIntervalMs) return
        val next = (current - 1 downTo 0).firstOrNull(usable) ?: return
        val needed = (bitrates[next] / tuning.safety).toLong() + 1L
        if (capacityBps >= needed) return
        capacityBps = needed
        probing = true
        healthySinceMs = nowMs
    }

    private fun switchTo(current: Int, target: Int, nowMs: Long): Int {
        if (target != current) {
            lastSwitchMs = nowMs
            healthySinceMs = nowMs
            if (target > current) probing = false
        }
        return target
    }

    private fun idealIndex(bitrates: IntArray, usable: (Int) -> Boolean, isAllowed: (Int) -> Boolean): Int {
        val budget = capacityBps * tuning.safety
        var lowestUsable = -1
        for (i in bitrates.indices) {
            if (!usable(i)) continue
            if (bitrates[i] <= budget) return i
            lowestUsable = i
        }
        if (lowestUsable >= 0) return lowestUsable
        return bitrates.indices.lastOrNull(isAllowed) ?: bitrates.lastIndex
    }

    private fun isBlocked(bitrate: Int, nowMs: Long): Boolean = nowMs < blockedUntilMs && bitrate >= blockedBitrate

    private fun trouble(loadingBitrate: Long, nowMs: Long) {
        healthySinceMs = nowMs
        probing = false
        if (loadingBitrate <= 0L) return
        blockedBitrate = loadingBitrate
        blockedUntilMs = nowMs + blockMs
        blockMs = min(blockMs * 2, tuning.maxBlockMs)
    }

    private fun raiseTo(evidenceBps: Long) {
        val sample = min(evidenceBps, tuning.maxCapacityBps)
        capacityBps = if (measured) max(capacityBps, blend(capacityBps, sample, tuning.riseWeight)) else max(capacityBps, sample)
        measured = true
    }

    private fun blend(current: Long, sample: Long, weight: Float): Long =
        (current + (sample - current) * weight).toLong()

    private companion object {
        const val NEVER = Long.MIN_VALUE / 4
    }
}
