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
