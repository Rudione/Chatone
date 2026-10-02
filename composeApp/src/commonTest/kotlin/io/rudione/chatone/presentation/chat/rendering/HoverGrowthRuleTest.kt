package io.rudione.chatone.presentation.chat.rendering

import kotlin.test.Test
import kotlin.test.assertEquals

class HoverGrowthRuleTest {

    private fun edge(count: Int = 40, key: Any? = "m40", overflow: Float? = 0f, scrolling: Boolean = false) =
        BottomEdge(itemCount = count, lastKey = key, overflowPx = overflow, scrolling = scrolling)

    @Test
    fun aWrappedMessageAtTheBottomIsPulledBackAboveTheInput() {
        assertEquals(18f, growthToAbsorb(edge(overflow = 0f), edge(overflow = 18f), tolerancePx = 1f))
    }

    @Test
    fun aNewMessageWhilePausedIsLeftAlone() {
        assertEquals(0f, growthToAbsorb(edge(), edge(count = 41, key = "m41", overflow = 22f), tolerancePx = 1f))
    }

    @Test
    fun readingHistoryIsNeverMoved() {
        assertEquals(0f, growthToAbsorb(edge(overflow = 40f), edge(overflow = 58f), tolerancePx = 1f))
        assertEquals(0f, growthToAbsorb(edge(overflow = null), edge(overflow = 12f), tolerancePx = 1f))
    }

    @Test
    fun userScrollingWinsOverTheCorrection() {
        assertEquals(0f, growthToAbsorb(edge(), edge(overflow = 18f, scrolling = true), tolerancePx = 1f))
    }

    @Test
    fun shrinkingNeedsNoCorrection() {
        assertEquals(0f, growthToAbsorb(edge(overflow = 0f), edge(overflow = -18f), tolerancePx = 1f))
        assertEquals(0f, growthToAbsorb(null, edge(overflow = 18f), tolerancePx = 1f))
    }
}
