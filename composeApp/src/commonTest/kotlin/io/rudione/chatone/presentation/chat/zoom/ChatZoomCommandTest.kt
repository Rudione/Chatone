package io.rudione.chatone.presentation.chat.zoom

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatZoomCommandTest {

    @Test
    fun plusAndEqualsEnlarge() {
        listOf(Key.Equals, Key.Plus, Key.NumPadAdd).forEach { key ->
            assertEquals(ChatZoomCommand.ENLARGE, chatZoomCommandOf(key, primaryModifier = true, altPressed = false))
        }
    }

    @Test
    fun minusShrinks() {
        listOf(Key.Minus, Key.NumPadSubtract).forEach { key ->
            assertEquals(ChatZoomCommand.SHRINK, chatZoomCommandOf(key, primaryModifier = true, altPressed = false))
        }
    }

    @Test
    fun zeroResets() {
        assertEquals(ChatZoomCommand.RESET, chatZoomCommandOf(Key.Zero, primaryModifier = true, altPressed = false))
    }

    @Test
    fun plainTypingIsNeverAZoomShortcut() {
        assertNull(chatZoomCommandOf(Key.Minus, primaryModifier = false, altPressed = false))
        assertNull(chatZoomCommandOf(Key.Equals, primaryModifier = true, altPressed = true))
        assertNull(chatZoomCommandOf(Key.A, primaryModifier = true, altPressed = false))
    }
}
