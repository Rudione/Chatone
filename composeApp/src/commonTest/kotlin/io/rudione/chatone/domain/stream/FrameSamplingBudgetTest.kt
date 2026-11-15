package io.rudione.chatone.domain.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrameSamplingBudgetTest {

    @Test
    fun cheapSamplingKeepsTheRequestedInterval() {
        val budget = FrameSamplingBudget()
        repeat(10) { budget.onSampled(costMs = 4f) }
        assertEquals(1_000L, budget.intervalMs(minIntervalMs = 1_000))
    }

    @Test
    fun expensiveSamplingIsSpacedOutToStayWithinItsShare() {
        val budget = FrameSamplingBudget(share = 0.04f)
        repeat(20) { budget.onSampled(costMs = 100f) }
        assertEquals(2_500L, budget.intervalMs(minIntervalMs = 1_000))
    }

    @Test
    fun neverWaitsLongerThanTheCap() {
        val budget = FrameSamplingBudget(share = 0.04f, maxIntervalMs = 8_000)
        repeat(20) { budget.onSampled(costMs = 600f) }
        assertEquals(8_000L, budget.intervalMs(minIntervalMs = 1_000))
    }

    @Test
    fun oneSlowSampleOnlyNudgesTheInterval() {
        val budget = FrameSamplingBudget(share = 0.04f)
        repeat(20) { budget.onSampled(costMs = 40f) }
        val before = budget.intervalMs(minIntervalMs = 500)
        budget.onSampled(costMs = 400f)
        val after = budget.intervalMs(minIntervalMs = 500)
        assertTrue(after > before && after < 5_000, "before $before after $after")
    }

    @Test
    fun startsFromTheRequestedIntervalAfterAReset() {
        val budget = FrameSamplingBudget()
        repeat(5) { budget.onSampled(costMs = 300f) }
        budget.reset()
        assertEquals(1_000L, budget.intervalMs(minIntervalMs = 1_000))
    }

    @Test
    fun aRequestedIntervalAboveTheCapIsRespected() {
        val budget = FrameSamplingBudget(maxIntervalMs = 8_000)
        budget.onSampled(costMs = 10f)
        assertEquals(12_000L, budget.intervalMs(minIntervalMs = 12_000))
    }
}
