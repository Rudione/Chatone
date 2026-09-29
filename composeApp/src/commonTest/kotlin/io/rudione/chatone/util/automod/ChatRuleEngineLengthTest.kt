package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChatRuleEngineLengthTest {

    private val viewer = AutomodTarget(
        userId = "1",
        username = "viewer",
        isMod = false,
        isSubscriber = false,
        isVip = false,
        isBroadcaster = false
    )

    private fun evaluate(text: String, maxLength: Int) = ChatRuleEngine.evaluate(
        text = text,
        tokens = emptyList(),
        target = viewer,
        currentChannelLogin = "channel",
        rules = listOf(ChatRule(id = "length", type = ChatRuleType.MESSAGE_LENGTH, messageMaxLength = maxLength))
    )

    @Test
    fun messageWithinLimitPasses() {
        assertNull(evaluate("a".repeat(10), maxLength = 10))
    }

    @Test
    fun messageOverLimitIsFlagged() {
        val verdict = assertNotNull(evaluate("a".repeat(11), maxLength = 10))
        assertEquals(ChatRuleType.MESSAGE_LENGTH, verdict.rule.type)
    }

    @Test
    fun surrogatePairsCountAsOneCharacter() {
        assertNull(evaluate("😀".repeat(10), maxLength = 10))
    }

    @Test
    fun defaultLimitIsThreeHundredCharacters() {
        assertEquals(300, ChatRule(id = "rule", type = ChatRuleType.MESSAGE_LENGTH).messageMaxLength)
    }
}
