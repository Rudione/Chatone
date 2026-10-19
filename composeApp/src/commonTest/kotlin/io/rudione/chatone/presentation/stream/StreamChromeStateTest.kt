package io.rudione.chatone.presentation.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamChromeStateTest {

    @Test
    fun heldControlsCannotBeHiddenUntilEveryHoldIsReleased() {
        val chrome = StreamChromeState()
        chrome.hide()
        assertFalse(chrome.controlsVisible)

        chrome.hold()
        chrome.hold()
        assertTrue(chrome.controlsVisible)

        chrome.hide()
        chrome.toggle()
        chrome.release()
        chrome.hide()
        assertTrue(chrome.controlsVisible)

        chrome.release()
        assertFalse(chrome.held)
        chrome.hide()
        assertFalse(chrome.controlsVisible)
    }

    @Test
    fun releaseRestartsTheAutoHideTimerAndExtraReleasesAreIgnored() {
        val chrome = StreamChromeState()
        chrome.hold()
        val before = chrome.interactions

        chrome.release()
        chrome.release()

        assertEquals(before + 1, chrome.interactions)
        assertFalse(chrome.held)
    }
}
