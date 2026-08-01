package io.rudione.chatone.domain.model

import androidx.compose.runtime.Immutable
import io.rudione.chatone.util.chat.MessageToken

@Immutable
sealed class DisplayMessage {
    abstract val id: String
    abstract val timestamp: Long
    abstract val channel: String

    data class PrivMsg(
        override val id: String,
        override val timestamp: Long,
        override val channel: String,
        val userId: String,
        val username: String,
        val displayName: String,
        val tokens: List<MessageToken>,
        val color: String?,
        val badges: List<Badge>,
        val isModerator: Boolean,
        val isSubscriber: Boolean,
        val isVip: Boolean,
        val isBroadcaster: Boolean,
        val isGrandMod: Boolean = false,
        val isMention: Boolean,
        val isAction: Boolean,
        val isFirstMessage: Boolean = false,
        val isHighlighted: Boolean = false,
        val customRewardId: String? = null,
        val rewardName: String? = null,
        val isDeleted: Boolean = false,
        val rawMessage: ChatMessage? = null,
        val sevenTvPaint: SevenTvCosmetics.Paint? = null,
        val sevenTvBadge: SevenTvCosmetics.Badge? = null,
        val highlightColor: Long? = null
    ) : DisplayMessage()

    data class SystemMsg(
        override val id: String,
        override val timestamp: Long,
        override val channel: String,
        val text: String,
        val type: SystemType = SystemType.INFO
    ) : DisplayMessage() {
        enum class SystemType { INFO, NOTICE, ROOM_STATE }
    }

    data class UserNoticeMsg(
        override val id: String,
        override val timestamp: Long,
        override val channel: String,
        val systemText: String,
        val innerMessage: PrivMsg? = null,
        val noticeType: String = "",
        val announceColor: String? = null
    ) : DisplayMessage()

    data class ModerationMsg(
        override val id: String,
        override val timestamp: Long,
        override val channel: String,
        val text: String,
        val action: ModerationAction,
        val targetUser: String? = null,
        val duration: Int? = null,
        val moderatorLogin: String? = null
    ) : DisplayMessage() {
        enum class ModerationAction {
            BAN, TIMEOUT, DELETE, CLEAR, UNBAN
        }
    }

    data class AutoModMsg(
        override val id: String,
        override val timestamp: Long,
        override val channel: String,
        val msgId: String,
        val userId: String,
        val username: String,
        val displayName: String,
        val text: String,
        val color: String? = null,
        val status: AutoModStatus = AutoModStatus.PENDING,
        val reasonCategory: String? = null,
        val reasonLevel: Int? = null
    ) : DisplayMessage() {
        enum class AutoModStatus { PENDING, ALLOWED, DENIED }
    }
}
