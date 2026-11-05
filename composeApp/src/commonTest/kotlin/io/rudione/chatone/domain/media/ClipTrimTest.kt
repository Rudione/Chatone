package io.rudione.chatone.domain.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClipTrimTest {

    @Test
    fun defaultIsTheLastThirtySeconds() {
        assertEquals(60f..90f, ClipTrim.defaultRange(90f))
        assertEquals(0f..20f, ClipTrim.defaultRange(20f))
    }

    @Test
    fun handlesKeepTheLengthBetweenFiveAndSixtySeconds() {
        assertEquals(10f..70f, ClipTrim.clamp(0f, 70f, 90f, movingStart = true))
        assertEquals(65f..70f, ClipTrim.clamp(68f, 70f, 90f, movingStart = true))
        assertEquals(10f..70f, ClipTrim.clamp(10f, 85f, 90f, movingStart = false))
        assertEquals(10f..15f, ClipTrim.clamp(10f, 11f, 90f, movingStart = false))
    }

    @Test
    fun shiftingStaysInsideTheWindow() {
        assertEquals(60f..90f, ClipTrim.shift(50f..80f, 40f, 90f))
        assertEquals(0f..30f, ClipTrim.shift(10f..40f, -40f, 90f))
    }

    @Test
    fun movingAGripGoesThroughTheSameLimits() {
        assertEquals(40f..70f, ClipTrim.move(ClipTrimGrip.START, 30f..70f, 10f, 90f))
        assertEquals(30f..80f, ClipTrim.move(ClipTrimGrip.END, 30f..70f, 10f, 90f))
        assertEquals(40f..80f, ClipTrim.move(ClipTrimGrip.BODY, 30f..70f, 10f, 90f))
    }

    @Test
    fun presetKeepsTheEndWhileItFits() {
        assertEquals(55f..70f, ClipTrim.withLength(40f..70f, 15f, 90f))
        assertEquals(0f..60f, ClipTrim.withLength(0f..20f, 60f, 90f))
    }

    @Test
    fun rangeIsRefittedWhenTheMediaIsShorter() {
        assertEquals(1f..31f, ClipTrim.fit(30f..60f, 31f))
        assertEquals(0f..20f, ClipTrim.fit(30f..60f, 20f))
    }

    @Test
    fun playheadNeverLeavesTheSelection() {
        assertEquals(30f, ClipTrim.playhead(0f, 30f..60f))
        assertEquals(60f, ClipTrim.playhead(75f, 30f..60f))
    }

    @Test
    fun loopRestartsOnlyOutsideTheSelection() {
        assertTrue(ClipTrim.leavesRange(60f, 30f..60f))
        assertTrue(ClipTrim.leavesRange(0f, 30f..60f))
        assertFalse(ClipTrim.leavesRange(29.8f, 30f..60f))
        assertFalse(ClipTrim.leavesRange(45f, 30f..60f))
    }

    @Test
    fun windowFollowsTheRealMediaLength() {
        assertTrue(ClipTrim.needsWindowUpdate(60f, 31f))
        assertFalse(ClipTrim.needsWindowUpdate(60f, 60.2f))
        assertFalse(ClipTrim.needsWindowUpdate(60f, 0f))
    }
}
