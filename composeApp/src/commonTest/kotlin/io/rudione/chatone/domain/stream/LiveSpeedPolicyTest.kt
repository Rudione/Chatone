package io.rudione.chatone.domain.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveSpeedPolicyTest {

    private val target = 1_600_000L

    private fun policy(minSpeed: Float = 0.95f, maxSpeed: Float = 1.1f) = LiveSpeedPolicy().apply {
        configure(
            targetUs = target,
            minTargetUs = 1_000_000L,
            maxTargetUs = 6_000_000L,
            minSpeed = minSpeed,
            maxSpeed = maxSpeed
        )
    }

    @Test
    fun targetDoesNotCreepWhenTheBufferIsJittery() {
        val policy = policy()
        var now = 0L
        repeat(600) { step ->
            val buffered = if (step % 3 == 0) 650_000L else 1_350_000L
            val offset = target + if (step % 2 == 0) 80_000L else -80_000L
            assertEquals(1f, policy.speed(offset, buffered, now))
            now += 1_000L
        }
        assertEquals(target, policy.targetUs)
    }

    @Test
    fun catchesUpWhenBehindWithAHealthyBuffer() {
        val speed = policy().speed(target + 1_000_000L, 1_200_000L, 0L)
        assertEquals(1.07f, speed, 0.001f)
    }

    @Test
    fun catchUpIsCappedByTheMaximumSpeed() {
        assertEquals(1.1f, policy().speed(target + 5_000_000L, 2_000_000L, 0L))
    }

    @Test
    fun doesNotSpeedUpOnAThinBuffer() {
        assertEquals(1f, policy().speed(target + 1_000_000L, 450_000L, 0L))
    }

    @Test
    fun slowsDownWhenTheBufferIsAboutToRunDry() {
        assertEquals(0.95f, policy().speed(target + 1_000_000L, 200_000L, 0L))
    }

    @Test
    fun slowsDownGentlyWhenAheadOfTheTarget() {
        val speed = policy().speed(target - 400_000L, 900_000L, 0L)
        assertEquals(0.972f, speed, 0.001f)
    }

    @Test
    fun speedIsRecalculatedOncePerInterval() {
        val policy = policy()
        assertEquals(1.07f, policy.speed(target + 1_000_000L, 1_200_000L, 0L), 0.001f)
        assertEquals(1.07f, policy.speed(target, 1_200_000L, 500L), 0.001f)
        assertEquals(1f, policy.speed(target, 1_200_000L, 1_000L))
    }

    @Test
    fun rebufferRaisesTheTargetAndItRecoversAfterAStablePeriod() {
        val policy = policy()
        policy.onRebuffer(0L)
        assertEquals(target + 300_000L, policy.targetUs)
        policy.speed(target, 1_000_000L, 10_000L)
        assertEquals(target + 300_000L, policy.targetUs)
        var now = 20_000L
        repeat(3) {
            policy.speed(target, 1_000_000L, now)
            now += 1_000L
        }
        assertEquals(target, policy.targetUs)
    }

    @Test
    fun rebufferGrowthIsBounded() {
        val policy = policy()
        repeat(40) { policy.onRebuffer(it * 100L) }
        assertEquals(target + 2_500_000L, policy.targetUs)
    }

    @Test
    fun repeatedConfigurationKeepsTheRaisedTarget() {
        val policy = policy()
        policy.onRebuffer(0L)
        policy.configure(target, 1_000_000L, 6_000_000L, 0.95f, 1.1f)
        assertEquals(target + 300_000L, policy.targetUs)
    }

    @Test
    fun fixedSpeedDisablesTheControl() {
        val policy = policy(minSpeed = 1f, maxSpeed = 1f)
        assertEquals(LiveSpeedPolicy.UNSET, policy.targetUs)
        assertEquals(1f, policy.speed(target + 3_000_000L, 2_000_000L, 0L))
    }

    @Test
    fun overrideReplacesTheConfiguredTarget() {
        val policy = policy()
        policy.override(4_000_000L)
        assertEquals(4_000_000L, policy.targetUs)
        policy.override(LiveSpeedPolicy.UNSET)
        assertEquals(target, policy.targetUs)
    }

    @Test
    fun targetBelowTheMinimumIsRaised() {
        val policy = LiveSpeedPolicy()
        policy.configure(500_000L, 1_000_000L, 6_000_000L, 0.95f, 1.1f)
        assertEquals(1_000_000L, policy.targetUs)
    }

    @Test
    fun guardJumpsOnlyAfterAPersistentLag() {
        val guard = LiveEdgeGuard()
        val lagging = target + 3_500_000L
        assertFalse(guard.shouldJump(lagging, target, 0L))
        assertFalse(guard.shouldJump(lagging, target, 2_000L))
        assertTrue(guard.shouldJump(lagging, target, 3_000L))
    }

    @Test
    fun guardForgetsAShortLag() {
        val guard = LiveEdgeGuard()
        val lagging = target + 3_500_000L
        assertFalse(guard.shouldJump(lagging, target, 0L))
        assertFalse(guard.shouldJump(target, target, 1_000L))
        assertFalse(guard.shouldJump(lagging, target, 2_000L))
        assertFalse(guard.shouldJump(lagging, target, 4_000L))
        assertTrue(guard.shouldJump(lagging, target, 5_000L))
    }

    @Test
    fun guardWaitsForTheCooldownBetweenJumps() {
        val guard = LiveEdgeGuard()
        val lagging = target + 3_500_000L
        guard.shouldJump(lagging, target, 0L)
        assertTrue(guard.shouldJump(lagging, target, 3_000L))
        guard.shouldJump(lagging, target, 4_000L)
        assertFalse(guard.shouldJump(lagging, target, 10_000L))
        assertTrue(guard.shouldJump(lagging, target, 33_000L))
    }
}
