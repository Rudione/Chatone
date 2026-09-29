package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRuleType {
    SPAM_RATE,
    ALL_CAPS,
    LINKS,
    EMOTE_SPAM,
    NEW_ACCOUNT,
    DUPLICATE_MESSAGE,
    CONSECUTIVE_NUMBERS,
    STREAM_ONLINE,
    STREAM_OFFLINE,
    FIRST_MESSAGE_GREETING,
    RAID_WELCOME,
    MESSAGE_LENGTH
}

@Serializable
enum class ChatRuleAction { DELETE, TIMEOUT, BAN, SEND_MESSAGE }

val ChatRuleType.isEventTrigger: Boolean
    get() = this == ChatRuleType.STREAM_ONLINE ||
            this == ChatRuleType.STREAM_OFFLINE ||
            this == ChatRuleType.FIRST_MESSAGE_GREETING ||
            this == ChatRuleType.RAID_WELCOME

val ChatRuleType.supportsReply: Boolean
    get() = !isEventTrigger

@Serializable
data class ChatRule(
    val id: String,
    val type: ChatRuleType,
    val scope: AutomodScope = AutomodScope.GLOBAL,
    val channelLogin: String? = null,
    val action: ChatRuleAction = ChatRuleAction.DELETE,
    val timeoutSeconds: Int = 60,
    val enabled: Boolean = true,

    val spamMaxMessages: Int = 5,
    val spamWindowSeconds: Int = 10,

    val capsThresholdPercent: Int = 70,
    val capsMinLength: Int = 8,

    val linksAllowClips: Boolean = true,
    val linksClipsSameChannelOnly: Boolean = false,
    val linksClipsAllowedChannels: List<String> = emptyList(),
    val linksAllowedSites: List<String> = emptyList(),
    val linksRequireHttps: Boolean = true,

    val emoteMaxCount: Int = 8,

    val consecutiveNumbersThreshold: Int = DEFAULT_CONSECUTIVE_NUMBERS_THRESHOLD,

    val newAccountAgeDays: Int = 7,

    val duplicateMinLength: Int = 8,

    val messageMaxLength: Int = DEFAULT_MESSAGE_MAX_LENGTH,

    val exemptMods: Boolean = true,
    val exemptVips: Boolean = true,
    val exemptSubs: Boolean = false,

    val eventMessage: String = "",
    val eventRepeat: Int = 1,
    val eventDelaySeconds: Int = 0,

    val replyEnabled: Boolean = false,
    val replyTemplate: String = "",
    val replyCooldownSeconds: Int = DEFAULT_REPLY_COOLDOWN_SECONDS,

    val createdAt: Long = 0L
) {
    companion object {
        const val DEFAULT_CONSECUTIVE_NUMBERS_THRESHOLD = 8
        const val DEFAULT_MESSAGE_MAX_LENGTH = 300
        const val MAX_MESSAGE_LENGTH = 500
        const val DEFAULT_REPLY_COOLDOWN_SECONDS = 15
        const val MAX_REPLY_COOLDOWN_SECONDS = 3_600
    }

    val displayLabel: String get() = when (type) {
        ChatRuleType.SPAM_RATE -> "Spam: ≤$spamMaxMessages msgs/$spamWindowSeconds s"
        ChatRuleType.ALL_CAPS -> "All caps: ≥$capsThresholdPercent%"
        ChatRuleType.LINKS -> "Links"
        ChatRuleType.EMOTE_SPAM -> "Emote spam: ≤$emoteMaxCount"
        ChatRuleType.NEW_ACCOUNT -> "New account: <$newAccountAgeDays d"
        ChatRuleType.DUPLICATE_MESSAGE -> "Duplicate messages"
        ChatRuleType.CONSECUTIVE_NUMBERS -> "Consecutive numbers: ≥$consecutiveNumbersThreshold"
        ChatRuleType.STREAM_ONLINE -> "On stream online: send ×$eventRepeat"
        ChatRuleType.STREAM_OFFLINE -> "On stream offline: send ×$eventRepeat"
        ChatRuleType.FIRST_MESSAGE_GREETING -> "Greet first-time chatters"
        ChatRuleType.RAID_WELCOME -> "Welcome raids"
        ChatRuleType.MESSAGE_LENGTH -> "Long messages: >$messageMaxLength chars"
    }
    val scopeLabel: String get() = when (scope) {
        AutomodScope.GLOBAL -> "GLOBAL"
        AutomodScope.LOCAL -> "#${channelLogin.orEmpty()}"
    }
}
