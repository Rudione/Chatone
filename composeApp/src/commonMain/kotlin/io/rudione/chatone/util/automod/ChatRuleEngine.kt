package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import io.rudione.chatone.domain.model.isEventTrigger
import io.rudione.chatone.util.chat.MessageToken
import kotlin.time.Clock

object ChatRuleEngine {

    private val spamBuckets = mutableMapOf<String, ArrayDeque<Long>>()
    private val lastMessage = mutableMapOf<String, String>()

    data class Verdict(
        val rule: ChatRule,
        val action: ChatRuleAction,
        val timeoutSeconds: Int,
        val reason: String
    )

    fun evaluate(
        text: String,
        tokens: List<MessageToken>,
        target: AutomodTarget,
        currentChannelLogin: String,
        rules: List<ChatRule>,
        accountCreatedAtMs: Long? = null
    ): Verdict? {
        if (text.isBlank() || rules.isEmpty()) return null
        if (target.isBroadcaster) return null

        for (rule in rules) {
            if (!rule.enabled) continue
            if (rule.type.isEventTrigger) continue
            if (rule.scope == AutomodScope.LOCAL &&
                !rule.channelLogin.equals(currentChannelLogin, ignoreCase = true)) continue
            if (rule.exemptMods && target.isMod) continue
            if (rule.exemptVips && target.isVip) continue
            if (rule.exemptSubs && target.isSubscriber) continue

            val reason = check(rule, text, tokens, target.userId, currentChannelLogin, accountCreatedAtMs) ?: continue
            return Verdict(rule, rule.action, rule.timeoutSeconds, reason)
        }
        return null
    }

    private fun check(
        rule: ChatRule,
        text: String,
        tokens: List<MessageToken>,
        userId: String,
        currentChannelLogin: String,
        accountCreatedAtMs: Long?
    ): String? = when (rule.type) {
        ChatRuleType.SPAM_RATE       -> checkSpam(rule, userId)
        ChatRuleType.ALL_CAPS        -> checkCaps(rule, text)
        ChatRuleType.LINKS           -> checkLinks(rule, text, currentChannelLogin)
        ChatRuleType.EMOTE_SPAM      -> checkEmotes(rule, tokens)
        ChatRuleType.NEW_ACCOUNT     -> checkNewAccount(rule, accountCreatedAtMs)
        ChatRuleType.DUPLICATE_MESSAGE -> checkDuplicate(rule, text, userId)
        ChatRuleType.CONSECUTIVE_NUMBERS -> checkConsecutiveNumbers(rule, text)
        ChatRuleType.STREAM_ONLINE,
        ChatRuleType.STREAM_OFFLINE,
        ChatRuleType.FIRST_MESSAGE_GREETING,
        ChatRuleType.RAID_WELCOME -> null
    }

    private fun checkSpam(rule: ChatRule, userId: String): String? {
        val now = Clock.System.now().toEpochMilliseconds()
        val windowMs = rule.spamWindowSeconds * 1000L
        val bucket = spamBuckets.getOrPut(userId) { ArrayDeque() }
        bucket.addLast(now)
        while (bucket.isNotEmpty() && now - bucket.first() > windowMs) bucket.removeFirst()
        return if (bucket.size >= rule.spamMaxMessages)
            "spam: ${bucket.size} msgs in ${rule.spamWindowSeconds}s" else null
    }

    private fun checkCaps(rule: ChatRule, text: String): String? {
        if (text.length < rule.capsMinLength) return null
        val letters = text.count { it.isLetter() }
        if (letters == 0) return null
        val pct = text.count { it.isUpperCase() } * 100 / letters
        return if (pct >= rule.capsThresholdPercent) "all-caps $pct%" else null
    }

    internal val LINK_RE = Regex(
        """(?:https?://|www\.)[^\s<>]+|(?:[a-zA-Z0-9](?:[a-zA-Z0-9\-]*[a-zA-Z0-9])?\.)+(?:com|net|org|io|tv|gg|me|ru|de|fr|uk|co|app|dev|live|stream|xyz)/\S+""",
        RegexOption.IGNORE_CASE
    )
    private val CLIP_LEGACY_RE = Regex("""clips\.twitch\.tv/(?:embed\?clip=)?([A-Za-z0-9_\-]+)""", RegexOption.IGNORE_CASE)
    private val CLIP_EMBEDDED_RE = Regex("""(?:www\.)?twitch\.tv/([A-Za-z0-9_]+)/clip/[A-Za-z0-9_\-]+""", RegexOption.IGNORE_CASE)
    private val HTTPS_RE = Regex("""^https://""", RegexOption.IGNORE_CASE)

    internal fun extractClipChannel(url: String): String? {
        CLIP_EMBEDDED_RE.find(url)?.let { return it.groupValues[1].lowercase() }
        if (CLIP_LEGACY_RE.containsMatchIn(url)) return ""
        return null
    }

    private fun checkLinks(rule: ChatRule, text: String, currentChannelLogin: String): String? {
        val match = LINK_RE.find(text) ?: return null
        val url = match.value

        if (rule.linksRequireHttps && url.startsWith("https://", ignoreCase = true)) {
            return null
        }

        if (rule.linksAllowClips) {
            val clipChannel = extractClipChannel(url)
            if (clipChannel != null) {
                if (!rule.linksClipsSameChannelOnly) return null
                val current = currentChannelLogin.lowercase().removePrefix("#")
                val exceptions = rule.linksClipsAllowedChannels.map { it.lowercase().removePrefix("#") }
                if (clipChannel.isEmpty() || clipChannel == current || clipChannel in exceptions) {
                    return null
                }
                return "off-channel clip: $clipChannel"
            }
        }

        if (rule.linksAllowedSites.isNotEmpty()) {
            val host = extractHost(url).lowercase()
            if (rule.linksAllowedSites.any { site ->
                    val s = site.trim().lowercase()
                    host == s || host.endsWith(".$s")
                }) return null
        }

        return "link: ${url.take(40)}"
    }

    private fun extractHost(url: String): String {
        val withoutScheme = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
        return withoutScheme.substringBefore("/").substringBefore("?").substringBefore("#")
    }

    private fun checkEmotes(rule: ChatRule, tokens: List<MessageToken>): String? {
        val count = tokens.count {
            it is MessageToken.ThirdPartyEmoteToken || it is MessageToken.TwitchEmoteToken
        }
        return if (count > rule.emoteMaxCount) "emote spam: $count emotes" else null
    }

    private fun checkNewAccount(rule: ChatRule, createdAtMs: Long?): String? {
        createdAtMs ?: return null
        val ageDays = (Clock.System.now().toEpochMilliseconds() - createdAtMs) / 86_400_000L
        return if (ageDays < rule.newAccountAgeDays) "new account: ${ageDays}d old" else null
    }

    private val WHITESPACE_RE = Regex("\\s+")

    private fun checkDuplicate(rule: ChatRule, text: String, userId: String): String? {
        if (text.length < rule.duplicateMinLength) return null
        val normalized = text.trim().replace(WHITESPACE_RE, " ")
        val prev = lastMessage[userId]
        lastMessage[userId] = normalized
        return if (prev == normalized) "duplicate message" else null
    }

    private fun checkConsecutiveNumbers(rule: ChatRule, text: String): String? {
        val threshold = rule.consecutiveNumbersThreshold.coerceAtLeast(2)
        val pattern = Regex("""(?:\d[ ]?){${threshold},}""")
        val match = pattern.find(text) ?: return null
        val digitCount = match.value.count { it.isDigit() }
        return if (digitCount >= threshold) "consecutive numbers: $digitCount digits" else null
    }

    fun invalidate() {
        spamBuckets.clear()
        lastMessage.clear()
    }
}
