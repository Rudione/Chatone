package io.rudione.chatone.domain.mention

import io.rudione.chatone.domain.model.HighlightRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MentionMatcherTest {

    private fun match(
        text: String,
        rules: List<HighlightRule> = emptyList(),
        replyParent: String? = null
    ) = MentionMatcher.match(
        text = text,
        login = "rudione",
        displayName = "Rudione",
        replyParentLogin = replyParent,
        replyParentDisplayName = null,
        rules = rules
    )

    @Test
    fun ownLoginWithOrWithoutAtIsAMention() {
        assertEquals(MentionMatcher.Match.OwnName, match("@rudione привет"))
        assertEquals(MentionMatcher.Match.OwnName, match("RUDIONE, глянь"))
    }

    @Test
    fun loginInsideAnotherWordIsNotAMention() {
        assertNull(match("rudioneeee"))
        assertNull(match("xrudione"))
        assertNull(match("rudione_fan"))
    }

    @Test
    fun replyToOwnMessageIsAMention() {
        assertEquals(MentionMatcher.Match.OwnName, match("согласен", replyParent = "Rudione"))
    }

    @Test
    fun customRuleMatchesWholeWordsOnly() {
        val rule = HighlightRule(id = "custom", pattern = "chatone")
        assertIs<MentionMatcher.Match.Rule>(match("люблю chatone", listOf(rule)))
        assertNull(match("chatoneapp", listOf(rule)))
        assertIs<MentionMatcher.Match.Rule>(match("chatoneapp", listOf(rule.copy(matchSubstring = true))))
    }

    @Test
    fun disabledAndNonTextRulesNeverMatch() {
        val disabled = HighlightRule(id = "custom", pattern = "hello", enabled = false)
        val whispers = HighlightRule(id = "whispers", pattern = "hello")
        assertNull(match("hello", listOf(disabled, whispers)))
    }

    @Test
    fun disabledUsernameRuleDisablesOwnNameAndRepliesButKeepsCustomRules() {
        val rules = listOf(HighlightRule.USERNAME_RULE.copy(enabled = false), HighlightRule(id = "custom", pattern = "chatone"))
        assertNull(match("@rudione", rules))
        assertNull(match("ответ", rules, replyParent = "rudione"))
        assertIs<MentionMatcher.Match.Rule>(match("@rudione chatone", rules))
    }

    @Test
    fun atMentionDoesNotMatchAPrefixOfAnotherLogin() {
        assertNull(match("@rudioneeee"))
        assertNull(match("@rudione_fan"))
        assertEquals(MentionMatcher.Match.OwnName, match("(@Rudione)!"))
    }

    @Test
    fun localizedDisplayNameAndReplyDisplayNameAreRecognized() {
        fun localized(text: String, parent: String? = null) = MentionMatcher.match(
            text, "rudione", "Денис", null, parent, emptyList()
        )
        assertEquals(MentionMatcher.Match.OwnName, localized("@Денис привет"))
        assertEquals(MentionMatcher.Match.OwnName, localized("ответ", "Денис"))
        assertNull(localized("Денисов"))
    }

    @Test
    fun customRulesRespectCaseSensitivityAndIgnoreInvalidRegex() {
        val rule = HighlightRule(id = "custom", pattern = "Chatone", caseSensitive = true)
        assertNull(match("chatone", listOf(rule)))
        assertIs<MentionMatcher.Match.Rule>(match("Chatone", listOf(rule)))
        assertNull(match("text", listOf(rule.copy(pattern = "[", isRegex = true))))
    }

    @Test
    fun withoutAnIdentityNothingMatches() {
        assertNull(
            MentionMatcher.match(
                text = "hello",
                login = "",
                displayName = "",
                replyParentLogin = null,
                replyParentDisplayName = null,
                rules = listOf(HighlightRule(id = "custom", pattern = "hello"))
            )
        )
    }
}
