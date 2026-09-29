package io.rudione.chatone.util.automod

import kotlin.time.Clock

class ChatRuleReplyThrottle(
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val lastReplyAt = mutableMapOf<String, Long>()

    fun tryAcquire(channelLogin: String, ruleId: String, cooldownSeconds: Int): Boolean {
        val key = "${channelLogin.lowercase()}|$ruleId"
        val now = nowMs()
        val last = lastReplyAt[key]
        if (last != null && now - last < cooldownSeconds.coerceAtLeast(0) * 1_000L) return false
        lastReplyAt[key] = now
        return true
    }

    fun reset() {
        lastReplyAt.clear()
    }
}

object ChatRuleReplyFormatter {

    private const val USER_PLACEHOLDER = "{user}"
    private const val CHANNEL_PLACEHOLDER = "{channel}"
    private const val TWITCH_MESSAGE_LIMIT = 500
    private val WHITESPACE = Regex("\\s+")

    fun format(template: String, userMention: String, channelLogin: String): String =
        template
            .replace(USER_PLACEHOLDER, userMention, ignoreCase = true)
            .replace(CHANNEL_PLACEHOLDER, channelLogin, ignoreCase = true)
            .replace(WHITESPACE, " ")
            .trim()
            .take(TWITCH_MESSAGE_LIMIT)

    fun mentionFor(displayName: String, login: String): String = when {
        login.isBlank() -> displayName
        displayName.equals(login, ignoreCase = true) -> displayName
        else -> login
    }
}
