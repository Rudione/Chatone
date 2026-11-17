package io.rudione.chatone.presentation.main

import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.domain.model.TwitchAccount

sealed class MainEvent : UiEvent {
    object ToggleSidebar : MainEvent()
    object CloseSidebar : MainEvent()
    object OpenSidebar : MainEvent()
    object ToggleSidebarCollapsed : MainEvent()
    data class SetSidebarWidth(val widthDp: Float) : MainEvent()
    data class SetMiniRailCollapsed(val collapsed: Boolean) : MainEvent()
    data class SelectChannel(val login: String, val scrollToMessageId: String? = null) : MainEvent()
    data class ToggleLiveNotify(val login: String) : MainEvent()
    object ClearPendingScrollMessage : MainEvent()
    object SelectMentionsChannel : MainEvent()
    object CloseMentionsChannel : MainEvent()
    data class OpenMonitorTab(val login: String) : MainEvent()
    data class CloseMonitorTab(val login: String) : MainEvent()
    data class CloseChannel(val login: String) : MainEvent()
    data class ReorderChannels(val fromIndex: Int, val toIndex: Int) : MainEvent()
    data class DropChannelOnFolder(val channelLogin: String, val folderId: String) : MainEvent()
    data class AddChannel(
        val login: String,
        val profileImageUrl: String = "",
        val displayName: String = ""
    ) : MainEvent()

    data class AddChannels(val channels: List<AddChannel>) : MainEvent()

    data class ReorderUnfolderedChannels(val fromIndex: Int, val toIndex: Int) : MainEvent()
    data class ReorderFolderChannels(
        val folderId: String,
        val fromIndex: Int,
        val toIndex: Int
    ) : MainEvent()

    object ShowAddChannelDialog : MainEvent()
    object HideAddChannelDialog : MainEvent()
    data class UpdateAddChannelQuery(val query: String) : MainEvent()
    object SearchChannels : MainEvent()
    data class ShowCreateFolderDialog(val seedChannelLogin: String? = null) : MainEvent()
    object HideCreateFolderDialog : MainEvent()
    data class UpdateNewFolderName(val name: String) : MainEvent()
    data class UpdateNewFolderColor(val colorHex: String) : MainEvent()
    object CreateFolder : MainEvent()
    data class ShowEditFolderDialog(val folderId: String) : MainEvent()
    object HideEditFolderDialog : MainEvent()
    data class UpdateEditFolderName(val name: String) : MainEvent()
    data class UpdateEditFolderColor(val colorHex: String) : MainEvent()
    object SaveFolderEdit : MainEvent()
    data class ToggleFolder(val folderId: String) : MainEvent()
    data class DeleteFolder(val folderId: String) : MainEvent()
    data class MoveChannelToFolder(val channelLogin: String, val folderId: String?) : MainEvent()
    data class SelectAccount(val account: TwitchAccount) : MainEvent()
    object AddAccount : MainEvent()
    data class DeleteAccount(val userId: String) : MainEvent()
    object ReconnectChat : MainEvent()
    object ShowSettings : MainEvent()
    object HideSettings : MainEvent()
    data class IncrementMentionCount(val channelLogin: String) : MainEvent()
    data class ResetMentionCount(val channelLogin: String) : MainEvent()
    object NavigateToAuth : MainEvent()

    object ToggleWhisperPanel : MainEvent()
    object HideWhisperPanel : MainEvent()
    data class OpenWhisperWith(
        val userId: String,
        val username: String,
        val displayName: String,
        val avatarUrl: String = "",
        val color: String? = null
    ) : MainEvent()

    data class SendWhisper(val toUserId: String, val toUsername: String, val text: String) :
        MainEvent()

    data class ReceiveWhisper(
        val fromUserId: String,
        val fromUsername: String,
        val fromDisplayName: String,
        val fromColor: String?,
        val text: String
    ) : MainEvent()

    data class MarkWhisperRead(val userId: String) : MainEvent()
    data class MarkMentionRead(val messageId: String) : MainEvent()
    data class MarkChannelMentionsRead(val channelLogin: String) : MainEvent()
    object ToggleMentionsFeed : MainEvent()
    object HideMentionsFeed : MainEvent()
    object MarkAllMentionsRead : MainEvent()
    data class AddMentionEntry(val entry: MentionEntry) : MainEvent()

    object ShowChattersPanel : MainEvent()
    object HideChattersPanel : MainEvent()
    data class SetActiveChatChannelId(val channelId: String) : MainEvent()
    data class MuteMentionsUser(val userLogin: String) : MainEvent()
    data class UnmuteMentionsUser(val userLogin: String) : MainEvent()
    data class MuteMentionsChannel(val channelLogin: String) : MainEvent()
    data class UnmuteMentionsChannel(val channelLogin: String) : MainEvent()
    data class MuteMentionsUserInChannel(val userLogin: String, val channelLogin: String) :
        MainEvent()

    data class UnmuteMentionsUserInChannel(val userLogin: String, val channelLogin: String) :
        MainEvent()

    object ShowBrowse : MainEvent()
    object HideBrowse : MainEvent()
    object ReturnToBrowse : MainEvent()
    object ReturnToStream : MainEvent()
    data class OpenRecentChannel(
        val login: String,
        val displayName: String = "",
        val avatarUrl: String = "",
        val fromBrowse: Boolean = false
    ) : MainEvent()

    data class ForgetRecentChannel(val login: String) : MainEvent()
    data class KeepRecentChannel(val login: String) : MainEvent()
}
