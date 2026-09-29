package io.rudione.chatone.presentation.chat.rendering

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaintAnimationClockTest {

    @Test
    fun repeatingPaintsSweepOnceEveryFourSeconds() {
        assertEquals(0f, PaintAnimationClock.phaseAt(0, repeat = true))
        assertEquals(0.25f, PaintAnimationClock.phaseAt(1_000, repeat = true))
        assertEquals(0f, PaintAnimationClock.phaseAt(4_000, repeat = true))
    }

    @Test
    fun swayingPaintsGoBackAndForthWithoutJumps() {
        assertEquals(-0.09f, PaintAnimationClock.phaseAt(0, repeat = false), 1e-4f)
        assertEquals(0.09f, PaintAnimationClock.phaseAt(2_800, repeat = false), 1e-4f)
        assertEquals(-0.09f, PaintAnimationClock.phaseAt(5_600, repeat = false), 1e-4f)
        var previous = PaintAnimationClock.phaseAt(0, repeat = false)
        (40L..11_200L step 40).forEach { time ->
            val phase = PaintAnimationClock.phaseAt(time, repeat = false)
            assertTrue(phase in -0.0901f..0.0901f)
            assertTrue(abs(phase - previous) < 0.01f, "jump at $time")
            previous = phase
        }
    }
}
