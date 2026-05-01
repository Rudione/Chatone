package io.rudione.chatone.util

import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import kotlinx.datetime.Clock

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
            if (rule.scope == AutomodScope.LOCAL &&
                !rule.channelLogin.equals(currentChannelLogin, ignoreCase = true)) continue
            if (rule.exemptMods && target.isMod) continue
            if (rule.exemptVips && target.isVip) continue
            if (rule.exemptSubs && target.isSubscriber) continue

            val reason = check(rule, text, tokens, target.userId, accountCreatedAtMs) ?: continue
            return Verdict(rule, rule.action, rule.timeoutSeconds, reason)
        }
        return null
    }

    private fun check(
        rule: ChatRule,
        text: String,
        tokens: List<MessageToken>,
        userId: String,
        accountCreatedAtMs: Long?
    ): String? = when (rule.type) {
        ChatRuleType.SPAM_RATE       -> checkSpam(rule, userId)
        ChatRuleType.ALL_CAPS        -> checkCaps(rule, text)
        ChatRuleType.LINKS           -> checkLinks(rule, text)
        ChatRuleType.EMOTE_SPAM      -> checkEmotes(rule, tokens)
        ChatRuleType.NEW_ACCOUNT     -> checkNewAccount(rule, accountCreatedAtMs)
        ChatRuleType.DUPLICATE_MESSAGE -> checkDuplicate(rule, text, userId)
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

    private val LINK_RE = Regex(
        """(?:https?://|www\.)[^\s<>]+|(?:[a-zA-Z0-9](?:[a-zA-Z0-9\-]*[a-zA-Z0-9])?\.)+(?:com|net|org|io|tv|gg|me|ru|de|fr|uk|co|app|dev|live|stream|xyz)/\S+""",
        RegexOption.IGNORE_CASE
    )
    private val CLIP_RE = Regex("""clips\.twitch\.tv""", RegexOption.IGNORE_CASE)

    private fun checkLinks(rule: ChatRule, text: String): String? {
        val match = LINK_RE.find(text) ?: return null
        if (rule.linksAllowClips && CLIP_RE.containsMatchIn(match.value)) return null
        return "link: ${match.value.take(40)}"
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

    private fun checkDuplicate(rule: ChatRule, text: String, userId: String): String? {
        if (text.length < rule.duplicateMinLength) return null
        val prev = lastMessage[userId]
        lastMessage[userId] = text
        return if (prev == text) "duplicate message" else null
    }

    fun invalidate() {
        spamBuckets.clear()
        lastMessage.clear()
    }
}
