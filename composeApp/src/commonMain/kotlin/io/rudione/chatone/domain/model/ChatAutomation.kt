package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AutomationKind {
    TIMED_MESSAGE,

    AUTO_REPLY,

    KEYWORD_SOUND
}

@Serializable
data class ChatAutomation(
    val id: String,
    val enabled: Boolean = true,
    val kind: AutomationKind = AutomationKind.TIMED_MESSAGE,
    val channelLogin: String = "",
    val message: String = "",
    val keyword: String = "",
    val intervalMinutes: Int = 15,
    val cooldownSeconds: Int = 60,
    val onlyWhenMentioned: Boolean = false
) {
    val effectiveIntervalMinutes: Int get() = intervalMinutes.coerceAtLeast(MIN_TIMER_MINUTES)

    val effectiveCooldownSeconds: Int
        get() = cooldownSeconds.coerceAtLeast(
            if (kind == AutomationKind.KEYWORD_SOUND) MIN_SOUND_COOLDOWN_SECONDS else MIN_REPLY_COOLDOWN_SECONDS
        )

    fun appliesTo(channel: String): Boolean {
        val target = normalizeAutomationChannel(channelLogin)
        if (target.isEmpty()) return kind != AutomationKind.TIMED_MESSAGE
        return target == ALL_CHANNELS || target == normalizeAutomationChannel(channel)
    }

    companion object {
        const val ALL_CHANNELS = "*"
        const val MIN_TIMER_MINUTES = 5
        const val MAX_TIMER_MINUTES = 720
        const val MIN_REPLY_COOLDOWN_SECONDS = 30
        const val MIN_SOUND_COOLDOWN_SECONDS = 5
        const val MAX_COOLDOWN_SECONDS = 3600
    }
}

fun normalizeAutomationChannel(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed == ChatAutomation.ALL_CHANNELS) return trimmed
    return trimmed
        .substringAfter("://")
        .removePrefix("www.")
        .removePrefix("m.")
        .removePrefix("twitch.tv/")
        .substringBefore('/')
        .substringBefore('?')
        .removePrefix("#")
        .removePrefix("@")
        .trim()
        .lowercase()
}
