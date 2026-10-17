package io.rudione.chatone.presentation.screenchat

import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.util.chat.MessageToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ScreenChatMessagesTest {

    private fun message(
        id: String,
        replyLogin: String? = null,
        replyName: String? = null,
        replyBody: String? = null
    ): DisplayMessage.PrivMsg {
        val raw = ChatMessage(
            id = id,
            channelId = "1",
            channelName = "streamer",
            userId = "2",
            username = "viewer",
            displayName = "Viewer",
            message = "hello",
            timestamp = 0L,
            replyParentUserLogin = replyLogin,
            replyParentDisplayName = replyName,
            replyParentMsgBody = replyBody
        )
        return DisplayMessage.PrivMsg(
            id = id,
            timestamp = 0L,
            channel = "streamer",
            userId = "2",
            username = "viewer",
            displayName = "Viewer",
            tokens = listOf(MessageToken.Text("hello")),
            color = null,
            badges = emptyList(),
            isModerator = false,
            isSubscriber = false,
            isVip = false,
            isBroadcaster = false,
            isMention = false,
            isAction = false,
            rawMessage = raw
        )
    }

    private val timeout = DisplayMessage.ModerationMsg(
        id = "mod",
        timestamp = 0L,
        channel = "streamer",
        text = "viewer was timed out",
        action = DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT
    )

    @Test
    fun aReplyStartsWithTheNicknameInsteadOfTheLineAbove() {
        val inline = message("1", replyLogin = "n1gel1337", replyName = "N1gel1337", replyBody = "first").withInlineReply()
        assertEquals(
            listOf(MessageToken.Mention("@N1gel1337"), MessageToken.Text(" "), MessageToken.Text("hello")),
            inline.tokens
        )
        assertNull(inline.rawMessage?.replyParentDisplayName)
        assertNull(inline.rawMessage?.replyParentMsgBody)
    }

    @Test
    fun aLocalizedDisplayNameFallsBackToTheLogin() {
        val inline = message("1", replyLogin = "tanaka", replyName = "田中", replyBody = "hi").withInlineReply()
        assertEquals(MessageToken.Mention("@tanaka"), inline.tokens.first())
    }

    @Test
    fun aPlainMessageStaysTheSameInstance() {
        val plain = message("1")
        assertSame(plain, plain.withInlineReply())
    }

    @Test
    fun moderatorActionsCanBeHidden() {
        val messages = listOf(message("1"), timeout, message("2"))
        assertEquals(listOf("1", "mod", "2"), screenChatItems(messages, showModeration = true).map { it.id })
        assertEquals(listOf("1", "2"), screenChatItems(messages, showModeration = false).map { it.id })
    }

    @Test
    fun theBackdropIsGoneUntilItIsTurnedOn() {
        assertEquals(0f, ScreenChatSettings().backdropAlpha)
        assertEquals(0.35f, ScreenChatSettings(backdrop = true).backdropAlpha)
        assertEquals(ScreenChatSettings.MAX_FONT_SIZE, ScreenChatSettings(fontSize = 99).sanitized().fontSize)
    }
}
