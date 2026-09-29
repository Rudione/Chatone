package io.rudione.chatone.presentation.chat.rendering

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatFollowRuleTest {

    @Test
    fun keepsFollowingWhileTheNewestMessageIsWithinTheThreshold() {
        assertFalse(shouldPauseFollowing(overflowPx = 0f, itemCount = 50, thresholdPx = 12f))
        assertFalse(shouldPauseFollowing(overflowPx = 12f, itemCount = 50, thresholdPx = 12f))
        assertFalse(shouldPauseFollowing(overflowPx = -30f, itemCount = 50, thresholdPx = 12f))
    }

    @Test
    fun pausesOnceTheUserPullsTheNewestMessageBelowTheThreshold() {
        assertTrue(shouldPauseFollowing(overflowPx = 12.5f, itemCount = 50, thresholdPx = 12f))
        assertTrue(shouldPauseFollowing(overflowPx = 24f, itemCount = 50, thresholdPx = 12f))
    }

    @Test
    fun pausesWhenTheNewestMessageIsOffScreen() {
        assertTrue(shouldPauseFollowing(overflowPx = null, itemCount = 50, thresholdPx = 12f))
    }

    @Test
    fun anEmptyChatNeverPauses() {
        assertFalse(shouldPauseFollowing(overflowPx = null, itemCount = 0, thresholdPx = 12f))
    }
}
