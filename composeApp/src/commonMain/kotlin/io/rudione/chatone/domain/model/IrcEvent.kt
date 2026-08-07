package io.rudione.chatone.domain.model

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

    data class AutoModHeld(
        val channel: String,
        val msgId: String,
        val userId: String,
        val username: String,
        val displayName: String,
        val message: String,
        val color: String? = null,
        val reasonCategory: String? = null,
        val reasonLevel: Int? = null,
        val flaggedFragments: List<String> = emptyList()
    ) : IrcEvent()

    data class AutoModResolved(
        val channel: String,
        val msgId: String,
        val resolvedBy: String,
        val action: String
    ) : IrcEvent()

    data class Connected(val channel: String) : IrcEvent()

    data class UserJoin(val channel: String, val username: String) : IrcEvent()

    data class UserPart(val channel: String, val username: String) : IrcEvent()

    data class Reconnect(val reason: String = "Server requested reconnect") : IrcEvent()
    data class Error(val message: String) : IrcEvent()

    data class ModeratorAction(
        val channel: String,
        val action: String,
        val moderator: String,
        val moderatorUserId: String? = null,
        val target: String? = null,
        val targetUserId: String? = null,
        val duration: Int? = null,
        val reason: String? = null,
        val targetMessageId: String? = null
    ) : IrcEvent() {
        companion object {
            const val ACTION_BAN = "ban"
            const val ACTION_TIMEOUT = "timeout"
            const val ACTION_UNBAN = "unban"
            const val ACTION_UNTIMEOUT = "untimeout"
            const val ACTION_DELETE = "delete"
            const val ACTION_CLEAR = "clear"
            const val ACTION_MOD = "mod"
            const val ACTION_UNMOD = "unmod"
            const val ACTION_VIP = "vip"
            const val ACTION_UNVIP = "unvip"
        }
    }

    data class PinnedChatUpdated(
        val channel: String,
        val pin: PinnedChatPayload? = null,
        val isUnpin: Boolean = false,
        val unpinId: String? = null,
        val unpinnedBy: String? = null
    ) : IrcEvent()

    data class PointsClaimAvailable(val channelId: String, val claimId: String) : IrcEvent()

    data class PollUpdated(
        val channelId: String,
        val poll: io.rudione.chatone.data.remote.dto.PollData?
    ) : IrcEvent()

    data class PredictionUpdated(
        val channelId: String,
        val prediction: io.rudione.chatone.data.remote.dto.PredictionData?
    ) : IrcEvent()

    data class PinnedChatPayload(
        val pinId: String,
        val messageId: String,
        val text: String,
        val senderId: String,
        val senderName: String,
        val senderLogin: String,
        val senderColor: String?,
        val endsAtEpochMs: Long?,
        val pinnerName: String,
        val pinnerUserId: String = "",
        val pinnerLogin: String = ""
    )
}
