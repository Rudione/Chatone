package io.rudione.chatone.presentation.components.expressive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawerSettleTest {
    private val fling = 1_000f

    @Test
    fun slowReleasePastHalfOpens() {
        assertTrue(DrawerSettle.shouldOpen(position = 0.6f, velocityPx = 120f, flingPx = fling))
    }

    @Test
    fun slowReleaseBeforeHalfCloses() {
        assertFalse(DrawerSettle.shouldOpen(position = 0.4f, velocityPx = 120f, flingPx = fling))
    }

    @Test
    fun flingWinsOverPosition() {
        assertTrue(DrawerSettle.shouldOpen(position = 0.1f, velocityPx = 1_500f, flingPx = fling))
        assertFalse(DrawerSettle.shouldOpen(position = 0.9f, velocityPx = -1_500f, flingPx = fling))
    }

    @Test
    fun dragFollowsFingerUntilFullyOpen() {
        assertEquals(0.35f, DrawerSettle.stretched(DrawerSettle.clampRaw(0.35f)))
        assertEquals(0f, DrawerSettle.clampRaw(-0.4f))
    }

    @Test
    fun overDragStretchesWithResistanceAndStops() {
        val stretched = DrawerSettle.stretched(DrawerSettle.clampRaw(5f))
        assertEquals(1f + ExpressiveDrawerDefaults.STRETCH_LIMIT, stretched, 0.0001f)
        assertTrue(DrawerSettle.stretched(1.1f) < 1.1f)
    }
}
