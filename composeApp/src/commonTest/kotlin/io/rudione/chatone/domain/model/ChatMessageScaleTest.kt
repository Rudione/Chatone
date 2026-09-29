package io.rudione.chatone.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatMessageScaleTest {

    @Test
    fun defaultIsOneHundredPercent() {
        assertEquals(100, ChatMessageScale.Default.percent)
        assertEquals(1f, ChatMessageScale.Default.factor)
        assertTrue(ChatMessageScale.Default.isDefault)
    }

    @Test
    fun eachStepIsTenPercent() {
        assertEquals(110, ChatMessageScale.Default.larger().percent)
        assertEquals(90, ChatMessageScale.Default.smaller().percent)
    }

    @Test
    fun scaleIsClampedToHalfAndDouble() {
        var scale = ChatMessageScale.Default
        repeat(30) { scale = scale.larger() }
        assertEquals(200, scale.percent)
        repeat(30) { scale = scale.smaller() }
        assertEquals(50, scale.percent)
    }

    @Test
    fun arbitraryValuesSnapToTheNearestStep() {
        assertEquals(120, ChatMessageScale.of(117).percent)
        assertEquals(110, ChatMessageScale.of(113.9f).percent)
        assertEquals(50, ChatMessageScale.of(-40).percent)
        assertEquals(200, ChatMessageScale.of(9_000).percent)
        assertEquals(100, ChatMessageScale.of(Float.NaN).percent)
    }
}
