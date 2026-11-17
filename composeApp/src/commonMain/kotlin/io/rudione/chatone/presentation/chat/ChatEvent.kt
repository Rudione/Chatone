package io.rudione.chatone.presentation.chat

import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.data.remote.GqlChannelPointReward
import io.rudione.chatone.domain.moderation.ChannelRole
import io.rudione.chatone.domain.moderation.RoleTarget
import io.rudione.chatone.domain.moderation.UserRoles
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.domain.model.GifSearchItem
import io.rudione.chatone.domain.model.Macro as ChatMacro

sealed class ChatEvent : UiEvent {
    data class OnInit(
        val channelLogin: String,
        val accessToken: String = "",
        val userId: String = "",
        val userLogin: String = "",
        val userDisplayName: String = ""
    ) : ChatEvent()

    data class OnMessageInputChanged(val input: String) : ChatEvent()
    object OnSendMessage : ChatEvent()

    object OnSendMessageKeepText : ChatEvent()

    object OnHistoryUp : ChatEvent()
    object OnHistoryDown : ChatEvent()
    object OnReconnect : ChatEvent()
    object OnToggleModMode : ChatEvent()
    data class OnModModeDefaultChanged(val enabled: Boolean) : ChatEvent()
    data class OnTimeoutUser(val userId: String, val duration: Int) : ChatEvent()
    data class OnBanUser(val userId: String, val reason: String? = null) : ChatEvent()
    data class OnUnbanUser(val userId: String, val login: String = "") : ChatEvent()
    data class OnNoticeUserVisible(val userId: String) : ChatEvent()
    data class OnDeleteMessage(val messageId: String) : ChatEvent()
    data class OnWhisper(val username: String) : ChatEvent()
    data class OnInsertMention(val displayName: String) : ChatEvent()
    data class OnInsertMentions(val names: List<String>) : ChatEvent()
    data class OnSetUserRole(
        val target: RoleTarget,
        val current: UserRoles,
        val role: ChannelRole,
        val granted: Boolean
    ) : ChatEvent()

    data class OnSelectEmoteCompletion(val emote: GenericEmote) : ChatEvent()
    object OnDismissCompletions : ChatEvent()
    data class OnSelectMentionCompletion(val username: String) : ChatEvent()
    object OnDismissMentionCompletions : ChatEvent()

    data class OnUpdateChatSettings(val settings: Map<String, Any>) : ChatEvent()
    object OnClearChat : ChatEvent()

    data class OnReplyToMessage(val message: DisplayMessage.PrivMsg) : ChatEvent()
    data class OnSendThreadReply(val target: DisplayMessage.PrivMsg, val text: String) : ChatEvent()
    object OnCancelReply : ChatEvent()

    data class OnPinMessage(val messageId: String) : ChatEvent()
    object OnUnpinMessage : ChatEvent()

    object OnOpenPollCreation : ChatEvent()
    object OnClosePollCreation : ChatEvent()
    data class OnCreatePoll(val title: String, val choices: List<String>, val durationSeconds: Int) : ChatEvent()
    object OnOpenPredictionCreation : ChatEvent()
    object OnClosePredictionCreation : ChatEvent()
    data class OnCreatePrediction(val title: String, val outcomes: List<String>, val windowSeconds: Int) : ChatEvent()
    data class OnVotePoll(val pollId: String, val choiceId: String) : ChatEvent()
    data class OnPlacePrediction(val eventId: String, val outcomeId: String, val points: Int) : ChatEvent()
    data class OnResolvePrediction(val eventId: String, val outcomeId: String) : ChatEvent()
    data class OnLockPrediction(val eventId: String) : ChatEvent()
    data class OnCancelPrediction(val eventId: String) : ChatEvent()

    object OnOpenPointsBitsPanel : ChatEvent()
    object OnPointsBalanceWanted : ChatEvent()
    object OnClosePointsBitsPanel : ChatEvent()
    object OnClaimPoints : ChatEvent()
    object OnToggleHiddenEvents : ChatEvent()
    data class OnHideEventBanner(val key: String) : ChatEvent()
    data class OnShowEventBanner(val key: String) : ChatEvent()
    data class OnToggleEventCollapsed(val key: String) : ChatEvent()
    data class OnRedeemReward(
        val reward: GqlChannelPointReward,
        val textInput: String = ""
    ) : ChatEvent()

    data class OnSendAnnouncement(val message: String, val color: String = "primary") : ChatEvent()
    data class OnStartRaid(val targetLogin: String) : ChatEvent()
    object OnCancelRaid : ChatEvent()
    object OnRaidNow : ChatEvent()
    data class OnSendShoutout(val targetUserId: String) : ChatEvent()
    data class OnSendMessageText(val text: String) : ChatEvent()
    data class OnExecuteMacro(val macro: ChatMacro) : ChatEvent()
    data class OnAllowAutoModMessage(val msgId: String) : ChatEvent()
    data class OnDenyAutoModMessage(val msgId: String) : ChatEvent()
    object OnToggleEmotePicker : ChatEvent()
    data class OnSendGif(val gif: GifSearchItem) : ChatEvent()
    object OnClearGifSendError : ChatEvent()
    object OnRefreshGifAccess : ChatEvent()
    object OnRefreshChannel : ChatEvent()
    data class OnBlockUser(val targetUserId: String, val targetLogin: String) : ChatEvent()
    data class OnUnblockUser(val targetUserId: String, val targetLogin: String) : ChatEvent()
    data class OnSetShowBlockedMode(val mode: Int) : ChatEvent()

    data class OnFilesDropped(val paths: List<String>) : ChatEvent()
    object OnConfirmPendingUpload : ChatEvent()
    object OnCancelPendingUpload : ChatEvent()
    object OnClearUploadedLink : ChatEvent()

    data class OnScrollbackPinned(val pinned: Boolean) : ChatEvent()
}
