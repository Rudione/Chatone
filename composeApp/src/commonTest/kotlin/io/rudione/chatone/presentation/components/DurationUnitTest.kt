package io.rudione.chatone.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationUnitTest {

    @Test
    fun picksLargestUnitThatDividesExactly() {
        assertEquals(DurationUnit.WEEKS, DurationUnit.bestFor(1_209_600))
        assertEquals(DurationUnit.DAYS, DurationUnit.bestFor(3 * 86_400))
        assertEquals(DurationUnit.HOURS, DurationUnit.bestFor(7_200))
        assertEquals(DurationUnit.MINUTES, DurationUnit.bestFor(90 * 60))
        assertEquals(DurationUnit.SECONDS, DurationUnit.bestFor(90))
    }

    @Test
    fun staysWithinAllowedUnits() {
        val allowed = listOf(DurationUnit.SECONDS, DurationUnit.MINUTES, DurationUnit.HOURS)
        assertEquals(DurationUnit.HOURS, DurationUnit.bestFor(1_209_600, allowed))
        assertEquals(DurationUnit.MINUTES, DurationUnit.bestFor(30, listOf(DurationUnit.MINUTES, DurationUnit.HOURS)))
    }
}
