package io.rudione.chatone.presentation.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileInsightFormattingTest {

    @Test
    fun ageUsesTheLargestWholeUnit() {
        val minute = 60_000L
        assertEquals(Age(5, AgeUnit.MINUTES), ageOf(0L, 5 * minute))
        assertEquals(Age(3, AgeUnit.HOURS), ageOf(0L, 3 * 60 * minute))
        assertEquals(Age(2, AgeUnit.DAYS), ageOf(0L, 2 * 24 * 60 * minute))
        assertEquals(Age(1, AgeUnit.YEARS), ageOf(0L, 400L * 24 * 60 * minute))
    }
}
