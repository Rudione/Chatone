package io.rudione.chatone.presentation.components

import androidx.compose.ui.geometry.Offset
import io.rudione.chatone.presentation.components.SwipeIntent.Decision
import kotlin.test.Test
import kotlin.test.assertEquals

class SwipeIntentTest {

    private val slop = 8f
    private val right = SwipeDirection.Right.sign

    @Test
    fun waitsUntilTheFingerLeavesTheSlop() {
        assertEquals(Decision.Undecided, SwipeIntent.decide(Offset(5f, 3f), right, slop, heldTooLong = false))
    }

    @Test
    fun claimsAMostlyHorizontalSwipeAsSoonAsItPassesTheSlop() {
        assertEquals(Decision.Claim, SwipeIntent.decide(Offset(9f, 6f), right, slop, heldTooLong = false))
    }

    @Test
    fun leavesVerticalScrollsAndOppositeSwipesToOthers() {
        assertEquals(Decision.Reject, SwipeIntent.decide(Offset(4f, 9f), right, slop, heldTooLong = false))
        assertEquals(Decision.Reject, SwipeIntent.decide(Offset(-12f, 0f), right, slop, heldTooLong = false))
        assertEquals(Decision.Reject, SwipeIntent.decide(Offset(9f, 8f), right, slop, heldTooLong = false))
    }

    @Test
    fun ignoresMovesThatFollowALongPress() {
        assertEquals(Decision.Reject, SwipeIntent.decide(Offset(30f, 0f), right, slop, heldTooLong = true))
    }
}
