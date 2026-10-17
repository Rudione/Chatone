package io.rudione.chatone.presentation.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeDismissDecisionTest {

    private fun dismiss(travel: Float, velocity: Float = 0f, width: Float = 400f) =
        SwipeDismissDecision.shouldDismiss(travel, width, velocity, flingVelocity = 2_000f, flingMinTravel = 100f)

    @Test
    fun longDragDismisses() {
        assertTrue(dismiss(travel = 160f))
    }

    @Test
    fun shortSlowDragSettlesBack() {
        assertFalse(dismiss(travel = 80f, velocity = 500f))
    }

    @Test
    fun quickFlingDismissesOnlyAfterRealTravel() {
        assertTrue(dismiss(travel = 120f, velocity = 2_500f))
        assertFalse(dismiss(travel = 40f, velocity = 4_000f))
    }

    @Test
    fun unmeasuredPanelNeverDismisses() {
        assertFalse(dismiss(travel = 500f, velocity = 5_000f, width = 0f))
    }
}
