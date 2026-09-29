package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalAutomodDeciderTest {

    private val viewer = AutomodTarget(userId = "7", username = "viewer")
    private val lengthRule = ChatRule(id = "len", type = ChatRuleType.MESSAGE_LENGTH, messageMaxLength = 10)
    private val wordRule = AutomodRule(id = "w", scope = AutomodScope.GLOBAL, pattern = "scam", action = AutomodAction.BAN)

    @BeforeTest
    fun reset() {
        AutomodEngine.invalidate()
        ChatRuleEngine.invalidate()
        ChatRuleClaims.clear()
    }

    private fun decide(text: String, words: List<AutomodRule> = emptyList(), chat: List<ChatRule> = emptyList()) =
        LocalAutomodDecider.decide(text, emptyList(), viewer, "channel", words, chat)

    @Test
    fun wordRulesTakePriorityOverChatRules() {
        val decision = decide("this scam message is long", listOf(wordRule), listOf(lengthRule))

        assertIs<LocalAutomodDecision.Word>(decision)
        assertEquals(LocalModAction.BAN, decision.modAction)
    }

    @Test
    fun chatRulesApplyWhenNoWordMatches() {
        val decision = decide("a perfectly normal but long message", listOf(wordRule), listOf(lengthRule))

        assertIs<LocalAutomodDecision.Chat>(decision)
        assertEquals(LocalModAction.DELETE, decision.modAction)
    }

    @Test
    fun cleanMessageHasNoDecision() {
        assertNull(decide("short", listOf(wordRule), listOf(lengthRule)))
    }

    @Test
    fun sendMessageActionOnlyReplies() {
        val replyOnly = lengthRule.copy(action = ChatRuleAction.SEND_MESSAGE, replyEnabled = true)

        assertEquals(LocalModAction.NONE, decide("way too long text", chat = listOf(replyOnly))?.modAction)
    }

    @Test
    fun timeoutDurationIsCarriedThrough() {
        val timeout = lengthRule.copy(action = ChatRuleAction.TIMEOUT, timeoutSeconds = 90)

        val decision = decide("way too long text", chat = listOf(timeout))

        assertEquals(LocalModAction.TIMEOUT, decision?.modAction)
        assertEquals(90, decision?.timeoutSeconds)
    }

    @Test
    fun noticeNamesTheRuleOutcome() {
        val chat = decide("way too long text", chat = listOf(lengthRule))!!
        val word = decide("scam", listOf(wordRule))!!

        assertEquals("ChatRule [GLOBAL] deleted @Viewer: message too long: 17 chars", LocalAutomodDecider.notice(chat, "Viewer"))
        assertEquals("Automod [GLOBAL] banned @Viewer: matched \"scam\"", LocalAutomodDecider.notice(word, "Viewer"))
    }

    @Test
    fun messageIsClaimedOnlyOnce() {
        assertTrue(ChatRuleClaims.claim("msg-1"))
        assertFalse(ChatRuleClaims.claim("msg-1"))
        assertTrue(ChatRuleClaims.claim("msg-2"))
        assertTrue(ChatRuleClaims.claim(""))
    }

    @Test
    fun displayedChannelsAreReferenceCounted() {
        DisplayedChatChannels.acquire("#Streamer")
        DisplayedChatChannels.acquire("streamer")
        DisplayedChatChannels.release("streamer")

        assertTrue(DisplayedChatChannels.isDisplayed("STREAMER"))

        DisplayedChatChannels.release("streamer")

        assertFalse(DisplayedChatChannels.isDisplayed("streamer"))
    }
}
