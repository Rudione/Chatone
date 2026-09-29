package io.rudione.chatone.presentation.chat.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatInputGlassTest {

    @Test
    fun staysSolidWhileTheLastMessageRestsAboveTheInput() {
        assertEquals(0f, glassProgressFor(overflowPx = 0f, thresholdPx = 120, itemCount = 10))
        assertEquals(0f, glassProgressFor(overflowPx = -40f, thresholdPx = 120, itemCount = 10))
    }

    @Test
    fun becomesFullGlassOnceContentCoversTheInput() {
        assertEquals(1f, glassProgressFor(overflowPx = 120f, thresholdPx = 120, itemCount = 10))
        assertEquals(1f, glassProgressFor(overflowPx = 900f, thresholdPx = 120, itemCount = 10))
        assertEquals(1f, glassProgressFor(overflowPx = null, thresholdPx = 120, itemCount = 10))
    }

    @Test
    fun easesInAlongASmoothstep() {
        assertEquals(0.5f, glassProgressFor(overflowPx = 60f, thresholdPx = 120, itemCount = 10), 1e-4f)
        val early = glassProgressFor(overflowPx = 30f, thresholdPx = 120, itemCount = 10)
        assertTrue(early < 0.25f)
    }

    @Test
    fun emptyChatOrMissingOverlayNeverTurnsGlassy() {
        assertEquals(0f, glassProgressFor(overflowPx = null, thresholdPx = 120, itemCount = 0))
        assertEquals(0f, glassProgressFor(overflowPx = 50f, thresholdPx = 0, itemCount = 10))
    }
}
