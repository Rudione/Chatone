package io.rudione.chatone.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModActionButtonDurationTest {

    @Test
    fun wholeWeeksAreShownAsWeeks() {
        assertEquals("1w", ModActionButton.formatDuration(604_800))
        assertEquals("2w", ModActionButton.formatDuration(ModActionButton.MAX_TIMEOUT_SECONDS))
        assertEquals("2" to "w", ModActionButton.splitDuration(1_209_600))
    }

    @Test
    fun otherDurationsKeepTheirUnits() {
        assertEquals("1s", ModActionButton.formatDuration(1))
        assertEquals("5m", ModActionButton.formatDuration(300))
        assertEquals("1h", ModActionButton.formatDuration(3_600))
        assertEquals("3d", ModActionButton.formatDuration(259_200))
        assertEquals("10d", ModActionButton.formatDuration(864_000))
    }

    @Test
    fun nonPositiveDurationsHaveNoLabel() {
        assertEquals("", ModActionButton.formatDuration(0))
        assertNull(ModActionButton.splitDuration(-1))
    }
}
