package io.rudione.chatone.domain.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveQualityPolicyTest {

    private val bitrates = intArrayOf(6_800_000, 3_400_000, 1_400_000, 230_000)
    private val allowAll: (Int) -> Boolean = { true }

    private fun policy(capacity: Long = 5_000_000L) = AdaptiveQualityPolicy().apply { start(0L, capacity) }

    private fun burst(bps: Long) = TransferSample(totalBytes = 800_000L, burstBps = bps, steadyBps = 0L)

    private fun steady(bps: Long) = TransferSample(totalBytes = 800_000L, burstBps = 0L, steadyBps = bps)

    @Test
    fun startsWithTheBestQualityTheHintAllows() {
        assertEquals(1, policy().select(bitrates, -1, allowAll, 0L))
    }

    @Test
    fun aFastBurstLiftsQualityRightAway() {
        val policy = policy()
        policy.select(bitrates, -1, allowAll, 0L)
        policy.onTransfer(burst(40_000_000L), loadingBitrate = 3_400_000L, nowMs = 1_000L)
        assertEquals(0, policy.select(bitrates, 1, allowAll, 2_000L))
    }

    @Test
    fun streamPacedDownloadsDoNotLowerQuality() {
        val policy = policy()
        var now = 0L
        var current = policy.select(bitrates, -1, allowAll, now)
        repeat(60) {
            now += 2_000L
            policy.onTransfer(TransferSample(850_000L, 3_500_000L, 3_350_000L), 3_400_000L, now)
            current = policy.select(bitrates, current, allowAll, now)
        }
        assertTrue(current <= 1)
    }

    @Test
    fun slowDownloadsStepDown() {
        val policy = policy()
        policy.select(bitrates, -1, allowAll, 0L)
        policy.onTransfer(steady(1_600_000L), loadingBitrate = 3_400_000L, nowMs = 20_000L)
        assertEquals(2, policy.select(bitrates, 1, allowAll, 20_100L))
    }

    @Test
    fun rebufferStepsDownAndBlocksTheFailedLevel() {
        val policy = policy(capacity = 40_000_000L)
        policy.select(bitrates, -1, allowAll, 0L)
        policy.onRebuffer(loadingBitrate = 6_800_000L, nowMs = 30_000L)
        assertEquals(1, policy.select(bitrates, 0, allowAll, 30_100L))
        policy.onTransfer(burst(40_000_000L), 3_400_000L, 31_000L)
        assertEquals(1, policy.select(bitrates, 1, allowAll, 40_000L))
        assertEquals(0, policy.select(bitrates, 1, allowAll, 46_000L))
    }

    @Test
    fun repeatedFailuresWaitLonger() {
        val policy = policy(capacity = 40_000_000L)
        policy.select(bitrates, -1, allowAll, 0L)
        policy.onRebuffer(6_800_000L, 30_000L)
        policy.select(bitrates, 0, allowAll, 30_100L)
        policy.onTransfer(burst(40_000_000L), 3_400_000L, 46_000L)
        assertEquals(0, policy.select(bitrates, 1, allowAll, 46_000L))
        policy.onRebuffer(6_800_000L, 50_000L)
        assertEquals(1, policy.select(bitrates, 0, allowAll, 50_100L))
        policy.onTransfer(burst(40_000_000L), 3_400_000L, 66_000L)
        assertEquals(1, policy.select(bitrates, 1, allowAll, 66_000L))
        assertEquals(0, policy.select(bitrates, 1, allowAll, 81_000L))
    }

    @Test
    fun slightlySlowDownloadsStillStepDown() {
        val policy = policy(capacity = 9_000_000L)
        policy.select(bitrates, -1, allowAll, 0L)
        policy.onTransfer(steady(5_700_000L), loadingBitrate = 6_800_000L, nowMs = 20_000L)
        assertEquals(1, policy.select(bitrates, 0, allowAll, 20_100L))
    }

    @Test
    fun aFailedLevelIsNotRetriedRightAway() {
        val policy = policy()
        policy.select(bitrates, -1, allowAll, 0L)
        policy.onTransfer(steady(1_600_000L), 3_400_000L, 20_000L)
        assertEquals(2, policy.select(bitrates, 1, allowAll, 20_100L))
        policy.onTransfer(burst(5_000_000L), 1_400_000L, 21_000L)
        assertEquals(2, policy.select(bitrates, 2, allowAll, 22_000L))
    }

    @Test
    fun probesUpwardWhenNothingCanBeMeasured() {
        val policy = policy(capacity = 300_000L)
        var current = policy.select(bitrates, -1, allowAll, 0L)
        assertEquals(3, current)
        var now = 0L
        repeat(40) {
            now += 1_000L
            policy.onTransfer(TransferSample(60_000L, 0L, 230_000L), 230_000L, now)
            current = policy.select(bitrates, current, allowAll, now)
        }
        assertTrue(current < 3)
    }

    @Test
    fun excludedTracksAreSkipped() {
        val policy = policy(capacity = 40_000_000L)
        assertEquals(1, policy.select(bitrates, -1, { it != 0 }, 0L))
    }

    @Test
    fun samplerFindsTheBacklogBurst() {
        val sampler = TransferSampler()
        var now = 0L
        sampler.onBytes(now, 16_384)
        repeat(20) {
            now += 1_000_000L
            sampler.onBytes(now, 16_384)
        }
        repeat(30) {
            now += 33_000_000L
            sampler.onBytes(now, 16_384)
        }
        val sample = sampler.finish()!!
        assertTrue(sample.burstBps > 100_000_000L)
        assertTrue(sample.steadyBps in 3_000_000L..10_000_000L)
    }

    @Test
    fun samplerIgnoresPlaylists() {
        val sampler = TransferSampler()
        sampler.onBytes(0L, 4_000)
        sampler.onBytes(2_000_000L, 2_000)
        assertNull(sampler.finish())
    }

    @Test
    fun samplerIgnoresTinyBufferDrains() {
        val sampler = TransferSampler()
        var now = 0L
        sampler.onBytes(now, 16_384)
        repeat(4) {
            now += 100_000L
            sampler.onBytes(now, 16_384)
        }
        repeat(20) {
            now += 40_000_000L
            sampler.onBytes(now, 8_192)
        }
        assertEquals(0L, sampler.finish()!!.burstBps)
    }
}
