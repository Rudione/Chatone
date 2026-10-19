package io.rudione.chatone.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class AiChatScannerTest {

    @Test
    fun replyNumbersBecomeZeroBasedIndicesWithinTheBatch() {
        assertEquals(setOf(0, 2), AiChatScanner.parseIndices("[1, 3]", size = 3))
        assertEquals(setOf(1), AiChatScanner.parseIndices("Messages: [2, 7, 0]", size = 4))
        assertEquals(emptySet(), AiChatScanner.parseIndices("[]", size = 5))
    }
}
