package io.rudione.chatone.presentation.browse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrowseFormattingTest {

    private fun ru(value: Int) = BrowseFormatting.render(BrowseFormatting.compactCount(value), ",", " тыс.", " млн")
    private fun en(value: Int) = BrowseFormatting.render(BrowseFormatting.compactCount(value), ".", "K", "M")

    @Test
    fun compactCountsUseTheLocaleSeparatorAndSuffix() {
        assertEquals("950", ru(950))
        assertEquals("1,2 тыс.", ru(1_234))
        assertEquals("126 тыс.", ru(126_193))
        assertEquals("1,5 млн", ru(1_512_000))
        assertEquals("1.2K", en(1_234))
        assertEquals("2K", en(2_000))
    }

    @Test
    fun compactCountsPluralizeAsMany() {
        assertEquals(21, compactQuantity(21))
        assertEquals(1_000, compactQuantity(126_193))
    }

    @Test
    fun uptimeSplitsHoursAndMinutes() {
        assertEquals(Uptime(2, 5), BrowseFormatting.uptime(startedAtMs = 0L, nowMs = 125 * 60_000L))
        assertEquals(Uptime(0, 0), BrowseFormatting.uptime(startedAtMs = 1_000L, nowMs = 1_000L))
        assertNull(BrowseFormatting.uptime(startedAtMs = null, nowMs = 1_000L))
        assertNull(BrowseFormatting.uptime(startedAtMs = 5_000_000L, nowMs = 1_000L))
    }
}
