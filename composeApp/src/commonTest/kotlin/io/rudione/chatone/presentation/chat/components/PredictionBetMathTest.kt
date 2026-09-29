package io.rudione.chatone.presentation.chat.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PredictionBetMathTest {

    @Test
    fun chipAmountRoundTripsToItsOwnPercent() {
        val balance = 128_450L
        listOf(10, 25, 50, 75).forEach { p ->
            assertEquals(p, betPercent(betChipAmount(balance, p), balance))
        }
    }

    @Test
    fun percentRoundsToNearestInsteadOfTruncating() {
        assertEquals(25, betPercent(32_112L, 128_450L))
        assertEquals(1, betPercent(5L, 1_000L))
        assertEquals(0, betPercent(4L, 1_000L))
    }

    @Test
    fun emptyBankOrOverflowStaysInRange() {
        assertEquals(0, betPercent(500L, 0L))
        assertEquals(100, betPercent(9_999L, 1_000L))
        assertEquals(0L, betChipAmount(0L, 25))
    }
}
