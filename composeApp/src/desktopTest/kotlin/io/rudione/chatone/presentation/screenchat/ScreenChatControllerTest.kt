package io.rudione.chatone.presentation.screenchat

import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenChatControllerTest {

    private val node = Preferences.userRoot().node("chatone-test-screen-chat-${System.nanoTime()}")
    private val settings = PreferencesSettings(node)

    @AfterTest
    fun removeNode() {
        node.removeNode()
    }

    @Test
    fun togglingOpensAndClosesTheWindow() {
        val controller = ScreenChatController(settings, isSupported = true)
        controller.toggle("#Streamer")
        assertTrue(controller.isOpen("streamer"))
        assertEquals(listOf("streamer"), controller.open.value)
        controller.toggle("streamer")
        assertFalse(controller.isOpen("streamer"))
    }

    @Test
    fun openWindowsSurviveARestart() {
        ScreenChatController(settings, isSupported = true).apply {
            open("first")
            open("second")
        }
        assertEquals(listOf("first", "second"), ScreenChatController(settings, isSupported = true).open.value)
    }

    @Test
    fun closingChannelsClosesTheirWindowsOnly() {
        val controller = ScreenChatController(settings, isSupported = true)
        controller.open("first")
        controller.open("second")
        controller.closeAll(listOf("FIRST", "unknown"))
        assertEquals(listOf("second"), controller.open.value)
    }

    @Test
    fun foreignInputNeverBecomesAWindowOrAKey() {
        val controller = ScreenChatController(settings, isSupported = true)
        controller.open("../../etc")
        controller.open("a b")
        controller.open("")
        settings.putString("screen_chat_open", "ok_login,..,bad name")
        assertEquals(emptyList(), controller.open.value)
        assertEquals(listOf("ok_login"), ScreenChatController(settings, isSupported = true).open.value)
    }

    @Test
    fun phonesNeverOpenScreenChat() {
        val controller = ScreenChatController(settings, isSupported = false)
        controller.open("streamer")
        assertFalse(controller.isOpen("streamer"))
    }
}
