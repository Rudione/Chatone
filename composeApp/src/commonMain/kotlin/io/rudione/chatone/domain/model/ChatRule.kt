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
    RAID_WELCOME
}

@Serializable
enum class ChatRuleAction { DELETE, TIMEOUT, BAN, SEND_MESSAGE }

val ChatRuleType.isEventTrigger: Boolean
    get() = this == ChatRuleType.STREAM_ONLINE ||
            this == ChatRuleType.STREAM_OFFLINE ||
            this == ChatRuleType.FIRST_MESSAGE_GREETING ||
            this == ChatRuleType.RAID_WELCOME

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

    val consecutiveNumbersThreshold: Int = 8,

    val newAccountAgeDays: Int = 7,

    val duplicateMinLength: Int = 8,

    val exemptMods: Boolean = true,
    val exemptVips: Boolean = true,
    val exemptSubs: Boolean = false,

    val eventMessage: String = "",
    val eventRepeat: Int = 1,
    val eventDelaySeconds: Int = 0,

    val createdAt: Long = 0L
) {
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
    }
    val scopeLabel: String get() = when (scope) {
        AutomodScope.GLOBAL -> "GLOBAL"
        AutomodScope.LOCAL -> "#${channelLogin.orEmpty()}"
    }
}
