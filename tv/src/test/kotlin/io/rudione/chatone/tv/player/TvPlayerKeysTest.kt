package io.rudione.chatone.tv.player

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TvPlayerKeysTest {

    @Test
    fun directionsOpenTheSidePanels() {
        assertEquals(TvPlayerCommand.OPEN_CHANNELS, idleCommand(Key.DirectionLeft))
        assertEquals(TvPlayerCommand.FOCUS_CHAT, idleCommand(Key.DirectionRight))
        assertEquals(TvPlayerCommand.SHOW_CONTROLS, idleCommand(Key.DirectionCenter))
        assertEquals(TvPlayerCommand.SHOW_CONTROLS, idleCommand(Key.DirectionDown))
    }

    @Test
    fun mediaKeysWorkFromAnyPanel() {
        assertEquals(TvPlayerCommand.TOGGLE_PLAYBACK, mediaCommand(Key.MediaPlayPause))
        assertEquals(TvPlayerCommand.NEXT_CHANNEL, mediaCommand(Key.ChannelDown))
        assertEquals(TvPlayerCommand.PREVIOUS_CHANNEL, mediaCommand(Key.ChannelUp))
        assertNull(mediaCommand(Key.DirectionLeft))
    }

    @Test
    fun channelSwitchWrapsAround() {
        val channels = listOf("a", "b", "c")
        assertEquals("a", channels.neighbour(2, 1))
        assertEquals("c", channels.neighbour(0, -1))
        assertEquals("b", channels.neighbour(-1, 1))
        assertNull(emptyList<String>().neighbour(0, 1))
    }
}
