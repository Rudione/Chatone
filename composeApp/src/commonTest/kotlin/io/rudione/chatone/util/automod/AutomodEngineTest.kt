package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutomodEngineTest {

    private val viewer = AutomodTarget(userId = "1", username = "viewer")

    @BeforeTest
    fun resetEngine() = AutomodEngine.invalidate()

    private fun word(pattern: String, configure: AutomodRule.() -> AutomodRule = { this }) =
        AutomodRule(id = "w-$pattern", scope = AutomodScope.GLOBAL, pattern = pattern).configure()

    private fun evaluate(
        text: String,
        vararg rules: AutomodRule,
        target: AutomodTarget = viewer,
        channel: String = "channel"
    ) = AutomodEngine.evaluate(text, target, channel, rules.toList())

    @Test
    fun substringMatchIsCaseInsensitiveByDefault() {
        assertEquals("spam", evaluate("SPAMMER here", word("spam"))?.matchedPattern)
    }

    @Test
    fun caseSensitiveRuleRespectsCase() {
        val strict = word("Spam") { copy(caseSensitive = true) }

        assertNull(evaluate("spam", strict))
        assertNotNull(evaluate("Spam", strict))
    }

    @Test
    fun wholeWordDoesNotMatchInsideOtherWords() {
        val whole = word("ass") { copy(wholeWord = true) }

        assertNull(evaluate("classic grass", whole))
        assertNotNull(evaluate("you ass!", whole))
    }

    @Test
    fun alternatesAreMatchedToo() {
        val rule = word("scam") { copy(alternates = listOf("sc4m", "skam")) }

        assertEquals("sc4m", evaluate("free sc4m", rule)?.matchedPattern)
    }

    @Test
    fun regexRulesUseThePattern() {
        val regex = word("""b[o0]t+s?""") { copy(isRegex = true) }

        assertNotNull(evaluate("cheap b0ttt here", regex))
        assertNull(evaluate("robust", word("""^b[o0]t$""") { copy(isRegex = true) }))
    }

    @Test
    fun brokenRegexNeverMatches() {
        assertNull(evaluate("anything", word("([unclosed") { copy(isRegex = true) }))
    }

    @Test
    fun ignoreLinksSkipsPatternsInsideUrls() {
        val rule = word("promo") { copy(ignoreLinks = true) }

        assertNull(evaluate("see https://site.com/promo", rule))
        assertNotNull(evaluate("promo code inside", rule))
    }

    @Test
    fun frequencyThresholdWaitsForRepeatedHits() {
        val rule = word("lol") { copy(frequencyThreshold = 3, frequencyWindowMs = 60_000L) }

        assertNull(evaluate("lol", rule))
        assertNull(evaluate("lol", rule))
        assertNotNull(evaluate("lol", rule))
    }

    @Test
    fun timeoutIsReportedInSeconds() {
        val rule = word("bad") { copy(action = AutomodAction.TIMEOUT, timeoutMs = 600_000L) }

        val verdict = assertNotNull(evaluate("bad", rule))

        assertEquals(AutomodAction.TIMEOUT, verdict.action)
        assertEquals(600, verdict.timeoutSeconds)
    }

    @Test
    fun exemptionsAndScopeAreHonoured() {
        val local = word("bad") { copy(scope = AutomodScope.LOCAL, channelLogin = "channel") }

        assertNull(evaluate("bad", word("bad"), target = viewer.copy(isMod = true)))
        assertNull(evaluate("bad", word("bad"), target = viewer.copy(isBroadcaster = true)))
        assertNull(evaluate("bad", local, channel = "other"))
        assertNotNull(evaluate("bad", local, channel = "CHANNEL"))
        assertNull(evaluate("bad", word("bad") { copy(enabled = false) }))
    }
}
