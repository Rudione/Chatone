package io.rudione.chatone.presentation.chat

import io.rudione.chatone.presentation.text.UiText
import io.rudione.chatone.presentation.chat.points.PointsClaim
import io.rudione.chatone.presentation.chat.points.PointsGain
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.remote.GqlChannelPointReward
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.domain.model.ChatViewerRelationship
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.moderation.UserRoles
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.data.remote.dto.ChannelData
import io.rudione.chatone.data.remote.dto.PollData
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.domain.model.GifAccess

data class PollHistoryEntry(val title: String, val choices: List<String>, val durationSeconds: Int)

data class PredictionHistoryEntry(val title: String, val outcomes: List<String>, val windowSeconds: Int)

data class ChatState(
    val channelLogin: String = "",
    val channelDisplayName: String = "",
    val liveStream: ChannelData? = null,
    val channelId: String = "",
    val messages: List<DisplayMessage> = emptyList(),

    val messagesSeq: Long = 0L,

    val messagesStartOrdinal: Long = 0L,
    val messageInput: String = "",
    val lastMessageSentAtMs: Long = 0L,
    val isConnected: Boolean = false,
    val connectionState: TwitchIrcClient.ConnectionState = TwitchIrcClient.ConnectionState.Disconnected,
    val isLoading: Boolean = false,
    val isMod: Boolean = false,
    val isBroadcaster: Boolean = false,
    val isGrandMod: Boolean = false,
    val modModeEnabled: Boolean = false,
    val roomState: RoomState = RoomState(),
    val roomStateReceived: Boolean = false,
    val viewerBadges: List<Badge>? = null,
    val viewerRelationship: ChatViewerRelationship? = null,
    val currentUserId: String = "",
    val currentUserLogin: String = "",
    val currentDisplayName: String = "",
    val currentUserColor: String = "",
    val currentAccessToken: String = "",
    val isEmotePickerVisible: Boolean = false,
    val emoteCompletions: List<GenericEmote> = emptyList(),
    val showEmoteCompletions: Boolean = false,

    val mentionCompletions: List<String> = emptyList(),
    val showMentionCompletions: Boolean = false,
    val mentionCount: Int = 0,

    val replyingTo: DisplayMessage.PrivMsg? = null,

    val pinnedMessage: DisplayMessage.PrivMsg? = null,
    val pinId: String? = null,
    val pinEndsAtMs: Long? = null,
    val pinnedByName: String? = null,
    val pinnedByBadges: List<Badge> = emptyList(),
    val pinnedByUser: ChatNoticeUser? = null,
    val pinLocallyHidden: Boolean = false,
    val hiddenEventIds: Set<String> = emptySet(),
    val collapsedEventIds: Set<String> = emptySet(),
    val showPollCreation: Boolean = false,
    val showPredictionCreation: Boolean = false,
    val predictionActionPending: Boolean = false,
    val recentPolls: List<PollHistoryEntry> = emptyList(),
    val recentPredictions: List<PredictionHistoryEntry> = emptyList(),
    val showPointsBitsPanel: Boolean = false,
    val pointsBitsLoading: Boolean = false,
    val pointsBalance: Long = 0L,
    val pointsBalanceAtMs: Long = 0L,
    val channelRewards: List<GqlChannelPointReward> = emptyList(),
    val pointsIconUrl: String? = null,
    val pointsBitsError: UiText? = null,
    val pointsClaim: PointsClaim? = null,
    val pointsClaiming: Boolean = false,
    val pointsGain: PointsGain? = null,

    val sentMessageHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,

    val pendingRaidTarget: String? = null,
    val pendingRaidTargetId: String? = null,
    val pendingRaidStartedAt: Long = 0L,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val inputGlowIntensity: Float = 0f,
    val inputGlowTriggerTs: Long = 0L,
    val activePollId: String? = null,
    val activePredictionId: String? = null,
    val activePredictionOutcomes: List<Pair<String, String>> = emptyList(),
    val livePoll: PollData? = null,
    val livePrediction: PredictionData? = null,
    val blockedUserIds: Set<String> = emptySet(),
    val showBlockedMode: Int = 0,
    val twitchSubscriberEmotes: List<GenericEmote> = emptyList(),
    val ownSubTier: Int = 0,
    val gifSendError: UiText? = null,
    val gifAccess: GifAccess = GifAccess.Loading,
    val twitchChannelEmotes: List<GenericEmote> = emptyList(),
    val twitchGlobalEmotes: List<GenericEmote> = emptyList(),
    val uploadProgress: Float? = null,
    val uploadedLink: String? = null,
    val uploadedLinkExpiresAtMs: Long? = null,
    val uploadError: String? = null,
    val pendingUploadPath: String? = null,
    val pendingUploadHost: String? = null,
    val pendingUploadHostIsNew: Boolean = false,
    val warmup: ChatWarmup = ChatWarmup(),
    val roleOverrides: Map<String, UserRoles> = emptyMap(),
    val bannedUserIds: Set<String> = emptySet()
) : UiState {
    val hasPrivilegedChatAccess: Boolean
        get() = isMod || isBroadcaster || isGrandMod ||
            (roleOverrides[currentUserId]?.vip ?: viewerBadges.orEmpty().any { it.id == "vip" })
    val canModerate: Boolean get() = isMod || isBroadcaster
    val canManageRoles: Boolean get() = isBroadcaster || isGrandMod

    fun rolesOf(message: DisplayMessage.PrivMsg): UserRoles =
        roleOverrides[message.userId] ?: UserRoles(moderator = message.isModerator, vip = message.isVip)
}

data class RoomState(
    val emoteOnly: Boolean = false,
    val followersOnly: Int = -1,
    val slowMode: Int = 0,
    val subsOnly: Boolean = false,
    val r9k: Boolean = false
)
