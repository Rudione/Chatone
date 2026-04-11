package io.rudione.chatone.domain.model

/**
 * Sealed class representing all IRC events the client can emit.
 */
sealed class IrcEvent {
    data class PrivMsg(val message: ChatMessage) : IrcEvent()

    data class Notice(
        val channel: String,
        val message: String,
        val msgId: String? = null
    ) : IrcEvent()

    data class UserNotice(
        val channel: String,
        val systemMsg: String,
        val message: ChatMessage? = null,
        val msgId: String? = null,
        val tags: Map<String, String> = emptyMap()
    ) : IrcEvent()

    data class ClearChat(
        val channel: String,
        val targetUser: String? = null,
        val banDuration: Int? = null,
        val tags: Map<String, String> = emptyMap()
    ) : IrcEvent()

    data class ClearMsg(
        val channel: String,
        val targetMessageId: String,
        val message: String,
        val login: String
    ) : IrcEvent()

    data class RoomState(
        val channel: String,
        val emoteOnly: Boolean? = null,
        val followersOnly: Int? = null,
        val slowMode: Int? = null,
        val subsOnly: Boolean? = null,
        val r9k: Boolean? = null,
        val roomId: String? = null
    ) : IrcEvent()

    data class UserState(
        val channel: String,
        val badgeInfo: String,
        val badges: String,
        val color: String?,
        val displayName: String,
        val emoteSets: List<String>,
        val isMod: Boolean
    ) : IrcEvent()

    data class GlobalUserState(
        val badgeInfo: String,
        val badges: String,
        val color: String?,
        val displayName: String,
        val emoteSets: List<String>,
        val userId: String
    ) : IrcEvent()

    data class Whisper(
        val fromUser: String,
        val displayName: String,
        val message: String,
        val color: String?,
        val badges: String,
        val userId: String
    ) : IrcEvent()

    /** AutoMod held message — only moderators/broadcasters receive this */
    data class AutoModHeld(
        val channel: String,
        val msgId: String,
        val userId: String,
        val username: String,
        val displayName: String,
        val message: String,
        val color: String? = null
    ) : IrcEvent()

    data class Connected(val channel: String) : IrcEvent()
    data class Reconnect(val reason: String = "Server requested reconnect") : IrcEvent()
    data class Error(val message: String) : IrcEvent()
}
