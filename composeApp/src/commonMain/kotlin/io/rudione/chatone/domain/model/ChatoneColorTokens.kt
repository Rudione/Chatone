package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatoneColorTokens(
    val mentionBg: Long = 0x269B6DFF,
    val mentionAccent: Long = 0xFFFF6B6B,
    val highlightBg: Long = 0xFFFFD700,
    val firstMessageColor: Long = 0xFF7B2FBE,
    val searchMatchBg: Long = 0xFF4FC3F7,
    val ownMessageBg: Long = 0x00000000,

    val modTimeout: Long = 0xFFFBBF24,
    val modBan: Long = 0xFFF87171,
    val modDelete: Long = 0xFFFF8C42,
    val modUnban: Long = 0xFF34D399,

    val live: Long = 0xFFEB0400,
    val connected: Long = 0xFF34D399,

    val automodSpam: Long = 0xFF82AEDA,
    val automodCaps: Long = 0xFFD9B57F,
    val automodLinks: Long = 0xFF8FC992,
    val automodEmotes: Long = 0xFFB39DDB,
    val automodNewAccount: Long = 0xFFE0A284,
    val automodDuplicate: Long = 0xFFA8B2C0,
    val automodNumbers: Long = 0xFFD79BBE,
    val automodStreamOnline: Long = 0xFF7FC9C0,
    val automodStreamOffline: Long = 0xFFD98F8F,
    val automodFirstGreeting: Long = 0xFFD7C97F,
    val automodRaid: Long = 0xFF9FA8DA,
) {
    fun automodColorFor(type: ChatRuleType): Long = when (type) {
        ChatRuleType.SPAM_RATE -> automodSpam
        ChatRuleType.ALL_CAPS -> automodCaps
        ChatRuleType.LINKS -> automodLinks
        ChatRuleType.EMOTE_SPAM -> automodEmotes
        ChatRuleType.NEW_ACCOUNT -> automodNewAccount
        ChatRuleType.DUPLICATE_MESSAGE -> automodDuplicate
        ChatRuleType.CONSECUTIVE_NUMBERS -> automodNumbers
        ChatRuleType.STREAM_ONLINE -> automodStreamOnline
        ChatRuleType.STREAM_OFFLINE -> automodStreamOffline
        ChatRuleType.FIRST_MESSAGE_GREETING -> automodFirstGreeting
        ChatRuleType.RAID_WELCOME -> automodRaid
    }

    fun modActionColor(action: AutomodAction): Long = when (action) {
        AutomodAction.DELETE -> modDelete
        AutomodAction.TIMEOUT -> modTimeout
        AutomodAction.BAN -> modBan
    }
}
