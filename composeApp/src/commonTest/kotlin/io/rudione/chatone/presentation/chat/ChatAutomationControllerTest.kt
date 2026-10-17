package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.AutomationKind
import io.rudione.chatone.domain.model.ChatAutomation
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.presentation.settings.SettingsState
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatAutomationControllerTest {

    private data class Sent(val channel: String, val text: String, val replyTo: ChatMessage?)

    private val sends = mutableListOf<Sent>()
    private var sounds = 0
    private var seq = 0
    private var autos = listOf<ChatAutomation>()
    private val chat = ChatState(
        channelLogin = "streamer",
        channelId = "42",
        currentAccessToken = "token",
        currentUserId = "1",
        currentUserLogin = "me",
        currentDisplayName = "Me",
        isConnected = true
    )

    private fun TestScope.controller(gate: AutomationGate = AutomationGate()) = ChatAutomationController(
        scope = backgroundScope,
        getChatState = { chat },
        getSettings = { SettingsState(automations = autos) },
        sendAsUser = { channel, text, replyTo -> sends += Sent(channel, text, replyTo); true },
        playSound = { sounds++ },
        gate = gate,
        random = Random(7),
        now = { EPOCH + testScheduler.currentTime }
    )

    private fun msg(text: String, channel: String = "streamer", user: String = "viewer") = ChatMessage(
        id = "m${seq++}",
        channelId = "42",
        channelName = channel,
        userId = "u_$user",
        username = user,
        displayName = user,
        message = text,
        timestamp = 0L
    )

    private fun reply(keyword: String = "!состав", channel: String = "streamer", cooldown: Int = 60) =
        ChatAutomation(
            id = "reply",
            kind = AutomationKind.AUTO_REPLY,
            channelLogin = channel,
            keyword = keyword,
            message = "Состав: Ана, Топсон",
            cooldownSeconds = cooldown
        )

    private fun TestScope.settle(ms: Long = ChatAutomationController.MAX_REPLY_DELAY_MS + 1) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun replyGoesOutAsReplyFromUserAfterTypingPause() = runTest {
        autos = listOf(reply())
        val c = controller()
        val trigger = msg("!состав")

        c.onIncomingMessage(trigger, isOwnMessage = false)
        settle(ChatAutomationController.MIN_REPLY_DELAY_MS - 1)
        assertTrue(sends.isEmpty())

        settle()
        assertEquals(listOf(Sent("streamer", "Состав: Ана, Топсон", trigger)), sends)
    }

    @Test
    fun replyIgnoresLongerWordsOtherChannelsAndOwnMessages() = runTest {
        autos = listOf(reply())
        val c = controller()

        c.onIncomingMessage(msg("!составы"), isOwnMessage = false)
        c.onIncomingMessage(msg("!состав", channel = "other"), isOwnMessage = false)
        c.onIncomingMessage(msg("!состав", user = "me"), isOwnMessage = true)
        settle()
        assertTrue(sends.isEmpty())

        c.onIncomingMessage(msg("кто скажет !СОСТАВ?"), isOwnMessage = false)
        settle()
        assertEquals(1, sends.size)
    }

    @Test
    fun sameChannelOpenInTwoPanelsRepliesOnce() = runTest {
        autos = listOf(reply())
        val gate = AutomationGate()
        val first = controller(gate)
        val second = controller(gate)
        val trigger = msg("!состав")

        first.onIncomingMessage(trigger, isOwnMessage = false)
        second.onIncomingMessage(trigger, isOwnMessage = false)
        settle()

        assertEquals(1, sends.size)
    }

    @Test
    fun cooldownNeverDropsBelowTwitchDuplicateWindow() = runTest {
        autos = listOf(reply(cooldown = 5))
        val c = controller()

        c.onIncomingMessage(msg("!состав"), isOwnMessage = false)
        settle()
        advanceTimeBy(5_000)
        c.onIncomingMessage(msg("!состав"), isOwnMessage = false)
        settle()
        assertEquals(1, sends.size)

        advanceTimeBy(30_000)
        c.onIncomingMessage(msg("!состав"), isOwnMessage = false)
        settle()
        assertEquals(2, sends.size)
    }

    @Test
    fun channelTypedWithHashSpacesAndCapsStillMatches() = runTest {
        autos = listOf(reply(channel = " #Streamer "))
        val c = controller()

        c.onIncomingMessage(msg("!состав"), isOwnMessage = false)
        settle()

        assertEquals(1, sends.size)
    }

    @Test
    fun recentIdenticalManualMessageBlocksAutomation() = runTest {
        autos = listOf(reply())
        val gate = AutomationGate()
        val c = controller(gate)
        gate.recordSent("streamer", "Состав: Ана, Топсон 󠀀", EPOCH)

        c.onIncomingMessage(msg("!состав"), isOwnMessage = false)
        settle()

        assertTrue(sends.isEmpty())
    }

    @Test
    fun timerUsesAtLeastFiveMinutesNeedsChatActivityAndSendsPlainText() = runTest {
        autos = listOf(
            ChatAutomation(id = "timer", kind = AutomationKind.TIMED_MESSAGE, channelLogin = "streamer", message = "Подпишись", intervalMinutes = 1)
        )
        val c = controller()
        c.start()

        settle(ChatAutomationController.TIMER_TICK_MS)
        repeat(5) { c.onIncomingMessage(msg("hi $it"), isOwnMessage = false) }
        settle(4 * 60_000L)
        assertTrue(sends.isEmpty())

        settle(4 * 60_000L)
        assertEquals(listOf(Sent("streamer", "Подпишись", null)), sends)
    }

    @Test
    fun timerStaysQuietInDeadChat() = runTest {
        autos = listOf(
            ChatAutomation(id = "timer", kind = AutomationKind.TIMED_MESSAGE, channelLogin = "streamer", message = "Подпишись", intervalMinutes = 5)
        )
        val c = controller()
        c.start()

        settle(30 * 60_000L)

        assertTrue(sends.isEmpty())
    }

    @Test
    fun keywordSoundRespectsSharedCooldown() = runTest {
        autos = listOf(ChatAutomation(id = "snd", kind = AutomationKind.KEYWORD_SOUND, channelLogin = "*", keyword = "рудик", cooldownSeconds = 1))
        val gate = AutomationGate()
        val first = controller(gate)
        val second = controller(gate)
        val trigger = msg("привет рудик")

        first.onIncomingMessage(trigger, isOwnMessage = false)
        second.onIncomingMessage(trigger, isOwnMessage = false)

        assertEquals(1, sounds)
        assertTrue(sends.isEmpty())
    }

    @Test
    fun keywordMatchingIgnoresInvisibleCharsAndRespectsWordEdges() {
        assertTrue(ChatAutomationController.matchesKeyword("!состав󠀀", "!состав"))
        assertTrue(ChatAutomationController.matchesKeyword("эй @Me, привет", "@me"))
        assertFalse(ChatAutomationController.matchesKeyword("@me2 привет", "@me"))
        assertFalse(ChatAutomationController.matchesKeyword("приветствую", "привет"))
        assertEquals("a b", AutomationGate.normalizeOutgoing(" a​  b 󠀀"))
    }

    private companion object {
        const val EPOCH = 1_800_000_000_000L
    }
}
