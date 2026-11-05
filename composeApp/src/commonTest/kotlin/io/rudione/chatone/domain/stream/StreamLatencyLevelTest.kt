package io.rudione.chatone.domain.stream

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamLatencyLevelTest {

    @Test
    fun lowUpToThreeSeconds() {
        assertEquals(StreamLatencyLevel.LOW, StreamLatencyLevel.of(0.8f))
        assertEquals(StreamLatencyLevel.LOW, StreamLatencyLevel.of(3f))
    }

    @Test
    fun mediumUpToEightSeconds() {
        assertEquals(StreamLatencyLevel.MEDIUM, StreamLatencyLevel.of(3.1f))
        assertEquals(StreamLatencyLevel.MEDIUM, StreamLatencyLevel.of(8f))
    }

    @Test
    fun highAboveEightSeconds() {
        assertEquals(StreamLatencyLevel.HIGH, StreamLatencyLevel.of(8.1f))
    }
}
