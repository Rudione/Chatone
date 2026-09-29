package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import io.rudione.chatone.util.chat.MessageToken
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock

class ChatRuleEngineTest {

    private val viewer = AutomodTarget(userId = "1", username = "viewer")

    @BeforeTest
    fun resetEngine() = ChatRuleEngine.invalidate()

    private fun rule(type: ChatRuleType, configure: ChatRule.() -> ChatRule = { this }) =
        ChatRule(id = type.name, type = type).configure()

    private fun evaluate(
        text: String,
        vararg rules: ChatRule,
        target: AutomodTarget = viewer,
        channel: String = "channel",
        tokens: List<MessageToken> = emptyList(),
        accountCreatedAtMs: Long? = null
    ) = ChatRuleEngine.evaluate(
        text = text,
        tokens = tokens,
        target = target,
        currentChannelLogin = channel,
        rules = rules.toList(),
        accountCreatedAtMs = accountCreatedAtMs
    )

    @Test
    fun spamRateFlagsBurstOverLimit() {
        val spam = rule(ChatRuleType.SPAM_RATE) { copy(spamMaxMessages = 3, spamWindowSeconds = 30) }

        assertNull(evaluate("one", spam))
        assertNull(evaluate("two", spam))
        assertNotNull(evaluate("three", spam))
    }

    @Test
    fun spamRateCountsEachChatterSeparately() {
        val spam = rule(ChatRuleType.SPAM_RATE) { copy(spamMaxMessages = 2, spamWindowSeconds = 30) }

        assertNull(evaluate("hi", spam, target = viewer))
        assertNull(evaluate("hi", spam, target = viewer.copy(userId = "2")))
        assertNotNull(evaluate("again", spam, target = viewer))
    }

    @Test
    fun allCapsNeedsThresholdAndMinimumLength() {
        val caps = rule(ChatRuleType.ALL_CAPS) { copy(capsThresholdPercent = 70, capsMinLength = 8) }

        assertNull(evaluate("LOUD", caps))
        assertNull(evaluate("mostly quiet TEXT", caps))
        assertNotNull(evaluate("THIS IS SHOUTING", caps))
    }

    @Test
    fun allCapsIgnoresMessagesWithoutLetters() {
        val caps = rule(ChatRuleType.ALL_CAPS) { copy(capsMinLength = 2) }

        assertNull(evaluate("12345 !!! ???", caps))
    }

    @Test
    fun linksFlagPlainHttpAndBareDomainsByDefault() {
        val links = rule(ChatRuleType.LINKS)

        assertNotNull(evaluate("go to http://spam.example/free", links))
        assertNotNull(evaluate("visit www.spam.com now", links))
        assertNotNull(evaluate("cheap.ru/offer here", links))
    }

    @Test
    fun linksIgnoreHttpsWhenOnlyInsecureLinksAreTargeted() {
        val insecureOnly = rule(ChatRuleType.LINKS) { copy(linksRequireHttps = true) }
        val every = rule(ChatRuleType.LINKS) { copy(linksRequireHttps = false) }

        assertNull(evaluate("https://example.com/page", insecureOnly))
        assertNotNull(evaluate("https://example.com/page", every))
    }

    @Test
    fun linksAllowWhitelistedSitesAndSubdomains() {
        val links = rule(ChatRuleType.LINKS) {
            copy(linksRequireHttps = false, linksAllowedSites = listOf("youtube.com"))
        }

        assertNull(evaluate("https://www.youtube.com/watch?v=1", links))
        assertNull(evaluate("https://m.youtube.com/watch?v=1", links))
        assertNotNull(evaluate("https://notyoutube.com/x", links))
    }

    @Test
    fun linksAllowClipsFromAnyChannelByDefault() {
        val links = rule(ChatRuleType.LINKS) { copy(linksRequireHttps = false) }

        assertNull(evaluate("https://clips.twitch.tv/FunnyClip-abc", links))
        assertNull(evaluate("https://www.twitch.tv/other/clip/FunnyClip-abc", links))
    }

    @Test
    fun linksCanRestrictClipsToTheCurrentChannel() {
        val links = rule(ChatRuleType.LINKS) {
            copy(
                linksRequireHttps = false,
                linksClipsSameChannelOnly = true,
                linksClipsAllowedChannels = listOf("friend")
            )
        }

        assertNull(evaluate("https://www.twitch.tv/channel/clip/Mine-1", links))
        assertNull(evaluate("https://www.twitch.tv/friend/clip/Theirs-1", links))
        assertNotNull(evaluate("https://www.twitch.tv/stranger/clip/Other-1", links))
    }

    @Test
    fun linksCanBanClipsEntirely() {
        val links = rule(ChatRuleType.LINKS) { copy(linksRequireHttps = false, linksAllowClips = false) }

        assertNotNull(evaluate("https://clips.twitch.tv/FunnyClip-abc", links))
    }

    @Test
    fun emoteSpamCountsTwitchAndThirdPartyEmotes() {
        val emotes = rule(ChatRuleType.EMOTE_SPAM) { copy(emoteMaxCount = 2) }
        val kappa = MessageToken.TwitchEmoteToken("25", "Kappa", "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/1.0")
        val space = MessageToken.Text(" ")

        assertNull(evaluate("Kappa Kappa", emotes, tokens = listOf(kappa, space, kappa)))
        assertNotNull(evaluate("Kappa Kappa Kappa", emotes, tokens = listOf(kappa, space, kappa, space, kappa)))
    }

    @Test
    fun newAccountNeedsKnownCreationDate() {
        val fresh = rule(ChatRuleType.NEW_ACCOUNT) { copy(newAccountAgeDays = 7) }
        val now = Clock.System.now().toEpochMilliseconds()

        assertNull(evaluate("hello", fresh))
        assertNotNull(evaluate("hello", fresh, accountCreatedAtMs = now - 2 * DAY_MS))
        assertNull(evaluate("hello", fresh, accountCreatedAtMs = now - 30 * DAY_MS))
    }

    @Test
    fun duplicateMessageIgnoresWhitespaceDifferences() {
        val duplicate = rule(ChatRuleType.DUPLICATE_MESSAGE) { copy(duplicateMinLength = 5) }

        assertNull(evaluate("buy my course", duplicate))
        assertNotNull(evaluate("buy   my course ", duplicate))
    }

    @Test
    fun duplicateMessageSkipsShortPhrases() {
        val duplicate = rule(ChatRuleType.DUPLICATE_MESSAGE) { copy(duplicateMinLength = 8) }

        assertNull(evaluate("gg", duplicate))
        assertNull(evaluate("gg", duplicate))
    }

    @Test
    fun consecutiveNumbersToleratesSingleSpaces() {
        val numbers = rule(ChatRuleType.CONSECUTIVE_NUMBERS) { copy(consecutiveNumbersThreshold = 8) }

        assertNull(evaluate("score 1234567", numbers))
        assertNotNull(evaluate("call 8 800 555 35 35", numbers))
        assertNotNull(evaluate("card 12345678", numbers))
    }

    @Test
    fun messageLengthFlagsOnlyLongerMessages() {
        val length = rule(ChatRuleType.MESSAGE_LENGTH) { copy(messageMaxLength = 350) }

        assertNull(evaluate("a".repeat(350), length))
        assertEquals(ChatRuleType.MESSAGE_LENGTH, evaluate("a".repeat(351), length)?.rule?.type)
    }

    @Test
    fun verdictCarriesActionAndTimeout() {
        val length = rule(ChatRuleType.MESSAGE_LENGTH) {
            copy(messageMaxLength = 5, action = ChatRuleAction.TIMEOUT, timeoutSeconds = 600)
        }

        val verdict = assertNotNull(evaluate("too long text", length))

        assertEquals(ChatRuleAction.TIMEOUT, verdict.action)
        assertEquals(600, verdict.timeoutSeconds)
    }

    @Test
    fun exemptionsFollowRuleSettings() {
        val length = rule(ChatRuleType.MESSAGE_LENGTH) {
            copy(messageMaxLength = 1, exemptMods = true, exemptVips = true, exemptSubs = true)
        }
        val strict = length.copy(exemptMods = false, exemptVips = false, exemptSubs = false)

        assertNull(evaluate("long", length, target = viewer.copy(isMod = true)))
        assertNull(evaluate("long", length, target = viewer.copy(isVip = true)))
        assertNull(evaluate("long", length, target = viewer.copy(isSubscriber = true)))
        assertNotNull(evaluate("long", strict, target = viewer.copy(isMod = true)))
    }

    @Test
    fun broadcasterIsNeverModerated() {
        val length = rule(ChatRuleType.MESSAGE_LENGTH) { copy(messageMaxLength = 1, exemptMods = false) }

        assertNull(evaluate("long", length, target = viewer.copy(isBroadcaster = true)))
    }

    @Test
    fun localRuleOnlyAppliesToItsChannel() {
        val local = rule(ChatRuleType.MESSAGE_LENGTH) {
            copy(messageMaxLength = 1, scope = AutomodScope.LOCAL, channelLogin = "Channel")
        }

        assertNotNull(evaluate("long", local, channel = "channel"))
        assertNull(evaluate("long", local, channel = "elsewhere"))
    }

    @Test
    fun disabledAndEventRulesAreSkipped() {
        val disabled = rule(ChatRuleType.MESSAGE_LENGTH) { copy(messageMaxLength = 1, enabled = false) }
        val event = rule(ChatRuleType.STREAM_ONLINE)

        assertNull(evaluate("long", disabled, event))
    }

    @Test
    fun firstMatchingRuleWins() {
        val caps = rule(ChatRuleType.ALL_CAPS) { copy(capsMinLength = 2) }
        val length = rule(ChatRuleType.MESSAGE_LENGTH) { copy(messageMaxLength = 2) }

        assertEquals(ChatRuleType.ALL_CAPS, evaluate("LOUD", caps, length)?.rule?.type)
        assertEquals(ChatRuleType.MESSAGE_LENGTH, evaluate("quiet", caps, length)?.rule?.type)
    }

    @Test
    fun blankMessagesAreIgnored() {
        assertNull(evaluate("   ", rule(ChatRuleType.MESSAGE_LENGTH) { copy(messageMaxLength = 1) }))
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
