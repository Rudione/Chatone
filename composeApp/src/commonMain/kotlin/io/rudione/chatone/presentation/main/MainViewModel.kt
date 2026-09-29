package io.rudione.chatone.presentation.main

import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.remote.emote.SevenTvEventApi
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.model.WhisperConversation
import io.rudione.chatone.domain.model.WhisperMessage
import io.rudione.chatone.domain.usecase.*
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.repository.ChannelFolderRepository
import io.rudione.chatone.presentation.main.components.sidebar.FolderColors
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.remote.RecentMessagesClient
import io.rudione.chatone.data.remote.RecentMessagesResult
import io.rudione.chatone.data.repository.MentionRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.SidebarLayoutRepository
import io.rudione.chatone.data.repository.LiveAlertsRepository
import io.rudione.chatone.data.repository.RecentChannelsRepository
import io.rudione.chatone.domain.model.RecentChannel
import io.rudione.chatone.data.repository.ThirdPartyBadgeRepository
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.util.system.dismissSystemNotifications
import io.rudione.chatone.domain.model.SidebarLayout
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.util.system.AppRestarter
import io.rudione.chatone.util.automod.RegexCache
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.settings.AppConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class MainState(
    val accounts: List<TwitchAccount> = emptyList(),
    val selectedAccount: TwitchAccount? = null,
    val isGuest: Boolean = false,
    val folders: List<ChannelFolder> = emptyList(),
    val unfolderedChannels: List<ChannelTab> = emptyList(),
    val activeChannelLogin: String? = null,
    val pendingScrollMessageId: String? = null,
    val mentionsChannelActive: Boolean = false,
    val openChannels: List<ChannelTab> = emptyList(),
    val monitorTabs: List<String> = emptyList(),
    val sidebarExpanded: Boolean = false,
    val sidebarCollapsed: Boolean = false,
    val sidebarWidthDp: Float = SidebarLayout.DEFAULT_WIDTH_DP,
    val miniRailCollapsed: Boolean = false,
    val isAddChannelDialogVisible: Boolean = false,
    val addChannelQuery: String = "",
    val searchResults: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
    val isConnected: Boolean = false,
    val showSettings: Boolean = false,
    val isCreateFolderDialogVisible: Boolean = false,
    val newFolderName: String = "",
    val newFolderColor: String = FolderColors.DEFAULT_HEX,
    val newFolderSeedChannel: String? = null,
    val editingFolderId: String? = null,
    val editFolderName: String = "",
    val editFolderColor: String = FolderColors.DEFAULT_HEX,

    val whisperConversations: List<WhisperConversation> = emptyList(),
    val activeWhisperUserId: String? = null,
    val showWhisperPanel: Boolean = false,
    val totalUnreadWhispers: Int = 0,

    val mentions: List<MentionEntry> = emptyList(),
    val showMentionsFeed: Boolean = false,
    val unreadMentionsCount: Int = 0,

    val showChattersPanel: Boolean = false,
    val activeChatChannelId: String = "",
    val channelIdMap: Map<String, String> = emptyMap(),
    val moderatedChannelIds: Set<String> = emptySet(),
    val liveNotifyChannels: Set<String> = emptySet(),
    val needsReauth: Boolean = false,
    val recentChannels: List<ChannelTab> = emptyList(),
    val showBrowse: Boolean = false
) : UiState

sealed class MainEvent : UiEvent {
    object ToggleSidebar : MainEvent()
    object CloseSidebar : MainEvent()
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
    data class MuteMentionsUserInChannel(val userLogin: String, val channelLogin: String) : MainEvent()
    data class UnmuteMentionsUserInChannel(val userLogin: String, val channelLogin: String) : MainEvent()
    object ShowBrowse : MainEvent()
    object HideBrowse : MainEvent()
    data class OpenRecentChannel(
        val login: String,
        val displayName: String = "",
        val avatarUrl: String = ""
    ) : MainEvent()
    data class ForgetRecentChannel(val login: String) : MainEvent()
    data class KeepRecentChannel(val login: String) : MainEvent()
}

sealed class MainEffect : UIEffect {
    object NavigateToAuth : MainEffect()
    data class ShowError(val message: String) : MainEffect()
    data class IncomingWhisper(val fromDisplayName: String, val text: String) : MainEffect()
    data class MentionToast(
        val channelLogin: String,
        val fromDisplayName: String,
        val text: String
    ) : MainEffect()
}

class MainViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val searchChannelsUseCase: SearchChannelsUseCase,
    private val connectChatUseCase: ConnectChatUseCase,
    private val joinChannelUseCase: JoinChannelUseCase,
    private val chatRepository: ChatRepository,
    private val emoteRepository: EmoteRepository,
    private val sevenTvEventApi: SevenTvEventApi,
    private val channelFolderRepository: ChannelFolderRepository,
    private val apiClient: TwitchApiClient,
    private val mentionRepository: MentionRepository,
    private val recentMessagesClient: RecentMessagesClient,
    private val thirdPartyBadgeRepository: ThirdPartyBadgeRepository,
    private val sidebarLayoutRepository: SidebarLayoutRepository,
    private val liveAlertsRepository: LiveAlertsRepository,
    private val recentChannelsRepository: RecentChannelsRepository
) : BaseViewModel<MainState, MainEvent, MainEffect>(MainState()) {

    val mentionMuteRepository = MentionMuteRepository()

    companion object {
        private const val TAG = "MainViewModel"
        private const val KEY_OPEN_CHANNELS = "open_channels"
        private const val KEY_ACTIVE_CHANNEL = "active_channel"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_UNFOLDERED_ORDER = "unfoldered_order_v2"
        private const val KEY_WARNED_SCOPES = "warned_missing_scopes"
        private const val LIVE_POLL_INTERVAL_MS = 60_000L
    }

    private val settings = Settings()

    private fun uiStrings() =
        AppStrings.forLocale(
            settings.getString("language", "en")
        )

    init {
        restoreSidebarLayout()
        subscribeToEvents()
        loadAccounts()
        restoreRecentChannels()
        restoreSavedChannels()
        restoreActiveRecentChannel()
        restoreFolders()
        observeWhispers()
        observeLiveMentions()
        observeLiveAlertChannels()
        startLiveStatusPolling()
        loadPersistedMentions()
    }

    override suspend fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.ToggleSidebar -> update { it.copy(sidebarExpanded = !it.sidebarExpanded) }
            MainEvent.CloseSidebar -> update { it.copy(sidebarExpanded = false) }
            MainEvent.ToggleSidebarCollapsed -> {
                val collapsed = !state.value.sidebarCollapsed
                update { it.copy(sidebarCollapsed = collapsed) }
                sidebarLayoutRepository.saveCollapsed(collapsed)
            }
            is MainEvent.SetSidebarWidth -> {
                update { it.copy(sidebarWidthDp = event.widthDp) }
                sidebarLayoutRepository.saveWidth(event.widthDp)
            }
            is MainEvent.SetMiniRailCollapsed -> {
                update { it.copy(miniRailCollapsed = event.collapsed) }
                sidebarLayoutRepository.saveMiniRailCollapsed(event.collapsed)
            }
            is MainEvent.SelectChannel -> selectChannel(event.login, event.scrollToMessageId)
            MainEvent.ShowBrowse -> update { it.copy(showBrowse = true, sidebarExpanded = false) }
            MainEvent.HideBrowse -> update { it.copy(showBrowse = false) }
            is MainEvent.OpenRecentChannel -> openRecentChannel(event.login, event.displayName, event.avatarUrl)
            is MainEvent.ForgetRecentChannel -> forgetRecentChannel(event.login)
            is MainEvent.KeepRecentChannel -> keepRecentChannel(event.login)
            is MainEvent.ToggleLiveNotify -> liveAlertsRepository.toggle(event.login)
            MainEvent.SelectMentionsChannel -> update {
                it.copy(
                    mentionsChannelActive = true,
                    sidebarExpanded = false,
                    showMentionsFeed = false
                )
            }
            MainEvent.CloseMentionsChannel -> update { it.copy(mentionsChannelActive = false) }
            MainEvent.ClearPendingScrollMessage -> update { it.copy(pendingScrollMessageId = null) }
            is MainEvent.OpenMonitorTab -> update {
                it.copy(
                    monitorTabs = (it.monitorTabs + event.login).distinct(),
                    activeChannelLogin = event.login,
                    mentionsChannelActive = false,
                    showMentionsFeed = false,
                    sidebarExpanded = false
                )
            }
            is MainEvent.CloseMonitorTab -> update {
                val remaining = it.monitorTabs - event.login
                val nextActive = if (it.activeChannelLogin == event.login)
                    (remaining.firstOrNull() ?: it.openChannels.firstOrNull()?.login)
                else it.activeChannelLogin
                it.copy(monitorTabs = remaining, activeChannelLogin = nextActive)
            }
            is MainEvent.CloseChannel -> closeChannel(event.login)
            is MainEvent.AddChannel -> addChannel(
                event.login,
                event.profileImageUrl,
                event.displayName
            )

            is MainEvent.MarkMentionRead -> {
                update { state ->
                    val alreadyRead =
                        state.mentions.find { it.messageId == event.messageId }?.isRead == true
                    if (!alreadyRead) {
                        state.copy(
                            mentions = state.mentions.map { m ->
                                if (m.messageId == event.messageId) m.copy(isRead = true) else m
                            },
                            unreadMentionsCount = (state.unreadMentionsCount - 1).coerceAtLeast(0)
                        )
                    } else state
                }
                viewModelScope.launch { mentionRepository.markAsRead(event.messageId) }
            }

            MainEvent.ShowAddChannelDialog -> update {
                it.copy(
                    isAddChannelDialogVisible = true,
                    addChannelQuery = "",
                    searchResults = emptyList()
                )
            }

            MainEvent.HideAddChannelDialog -> update { it.copy(isAddChannelDialogVisible = false) }
            is MainEvent.UpdateAddChannelQuery -> update { it.copy(addChannelQuery = event.query) }
            MainEvent.SearchChannels -> searchChannels()
            is MainEvent.ShowCreateFolderDialog -> update {
                it.copy(
                    isCreateFolderDialogVisible = true,
                    newFolderName = "",
                    newFolderColor = FolderColors.DEFAULT_HEX,
                    newFolderSeedChannel = event.seedChannelLogin
                )
            }

            MainEvent.HideCreateFolderDialog -> update {
                it.copy(isCreateFolderDialogVisible = false, newFolderSeedChannel = null)
            }
            is MainEvent.UpdateNewFolderName -> update { it.copy(newFolderName = event.name) }
            is MainEvent.UpdateNewFolderColor -> update { it.copy(newFolderColor = event.colorHex) }
            MainEvent.CreateFolder -> createFolder()
            is MainEvent.ShowEditFolderDialog -> update { state ->
                val folder = state.folders.find { it.id == event.folderId }
                if (folder == null) state else state.copy(
                    editingFolderId = folder.id,
                    editFolderName = folder.name,
                    editFolderColor = FolderColors.normalize(folder.color)
                )
            }

            MainEvent.HideEditFolderDialog -> update { it.copy(editingFolderId = null) }
            is MainEvent.UpdateEditFolderName -> update { it.copy(editFolderName = event.name) }
            is MainEvent.UpdateEditFolderColor -> update { it.copy(editFolderColor = event.colorHex) }
            MainEvent.SaveFolderEdit -> saveFolderEdit()
            is MainEvent.ReorderChannels -> {
                update { state ->
                    val channels = state.openChannels.toMutableList()
                    if (event.fromIndex in channels.indices && event.toIndex in channels.indices) {
                        val item = channels.removeAt(event.fromIndex)
                        channels.add(event.toIndex, item)
                        state.copy(openChannels = channels)
                    } else state
                }
                saveChannelState()
            }

            is MainEvent.ReorderUnfolderedChannels -> {
                update { state ->
                    val list = state.unfolderedChannels.toMutableList()
                    if (event.fromIndex in list.indices && event.toIndex in list.indices) {
                        val item = list.removeAt(event.fromIndex)
                        list.add(event.toIndex, item)
                        state.copy(unfolderedChannels = list)
                    } else state
                }
                saveChannelState()
            }

            is MainEvent.ReorderFolderChannels -> {
                update { state ->
                    val folders = state.folders.map { folder ->
                        if (folder.id == event.folderId) {
                            val list = folder.channels.toMutableList()
                            if (event.fromIndex in list.indices && event.toIndex in list.indices) {
                                val item = list.removeAt(event.fromIndex)
                                list.add(event.toIndex, item)
                            }
                            folder.copy(channels = list)
                        } else folder
                    }
                    state.copy(folders = folders)
                }
                saveChannelState()
            }

            is MainEvent.DropChannelOnFolder -> moveChannelToFolder(
                event.channelLogin,
                event.folderId
            )

            is MainEvent.ToggleFolder -> toggleFolder(event.folderId)
            is MainEvent.DeleteFolder -> deleteFolder(event.folderId)
            is MainEvent.MoveChannelToFolder -> moveChannelToFolder(
                event.channelLogin,
                event.folderId
            )

            is MainEvent.SelectAccount -> selectAccount(event.account)
            MainEvent.AddAccount -> sendEffect(MainEffect.NavigateToAuth)
            is MainEvent.DeleteAccount -> deleteAccount(event.userId)
            MainEvent.ShowSettings -> update { it.copy(showSettings = true) }
            MainEvent.HideSettings -> update { it.copy(showSettings = false) }

            is MainEvent.IncrementMentionCount -> {
                incrementMentionCount(event.channelLogin)

                update { it.copy(unreadMentionsCount = it.unreadMentionsCount + 1) }
            }

            is MainEvent.ResetMentionCount -> resetMentionCount(event.channelLogin)
            MainEvent.NavigateToAuth -> {
                AppRestarter.restart(delayMs = 300L)
            }

            MainEvent.ToggleWhisperPanel -> update {
                it.copy(
                    showWhisperPanel = !it.showWhisperPanel,
                    showMentionsFeed = false
                )
            }

            MainEvent.HideWhisperPanel -> update { it.copy(showWhisperPanel = false) }

            is MainEvent.OpenWhisperWith -> {

                if (event.userId.isEmpty()) {
                    update { it.copy(activeWhisperUserId = null, showWhisperPanel = false) }
                    return
                }

                update { state ->
                    val existing = state.whisperConversations.find { it.userId == event.userId }
                    val conversations = if (existing == null) {
                        state.whisperConversations + WhisperConversation(
                            userId = event.userId,
                            username = event.username,
                            displayName = event.displayName,
                            avatarUrl = event.avatarUrl,
                            color = event.color
                        )
                    } else {
                        state.whisperConversations.map {
                            if (it.userId == event.userId) it.copy(
                                unreadCount = 0
                            ) else it
                        }
                    }
                    state.copy(
                        whisperConversations = conversations,
                        activeWhisperUserId = event.userId,
                        showWhisperPanel = true,
                        showMentionsFeed = false,
                        totalUnreadWhispers = conversations.sumOf { it.unreadCount }
                    )
                }
            }

            is MainEvent.SendWhisper -> sendWhisperMessage(
                event.toUserId,
                event.toUsername,
                event.text
            )

            is MainEvent.ReceiveWhisper -> receiveWhisper(
                event.fromUserId,
                event.fromUsername,
                event.fromDisplayName,
                event.fromColor,
                event.text
            )

            is MainEvent.MarkWhisperRead -> markWhisperRead(event.userId)

            MainEvent.ToggleMentionsFeed -> update {
                it.copy(
                    showMentionsFeed = !it.showMentionsFeed,
                    showWhisperPanel = false
                )
            }

            MainEvent.HideMentionsFeed -> update { it.copy(showMentionsFeed = false) }
            is MainEvent.MarkChannelMentionsRead -> markChannelMentionsRead(event.channelLogin)
            MainEvent.MarkAllMentionsRead -> {
                update {
                    it.copy(
                        mentions = it.mentions.map { m -> m.copy(isRead = true) },
                        unreadMentionsCount = 0
                    )
                }
                viewModelScope.launch { mentionRepository.markAllAsRead() }
            }

            is MainEvent.AddMentionEntry -> {
                val alreadyExists =
                    state.value.mentions.any { it.messageId == event.entry.messageId }
                if (!alreadyExists) {
                    val isMuted = mentionMuteRepository.isMuted(
                        userLogin = event.entry.fromUsername,
                        channelLogin = event.entry.channelLogin
                    )
                    update {
                        it.copy(
                            mentions = (listOf(event.entry) + it.mentions).take(200),
                            unreadMentionsCount = if (isMuted) it.unreadMentionsCount else it.unreadMentionsCount + 1
                        )
                    }
                    viewModelScope.launch { mentionRepository.saveMention(event.entry) }
                }
            }

            is MainEvent.MuteMentionsUser -> {
                mentionMuteRepository.muteUser(event.userLogin)
            }
            is MainEvent.UnmuteMentionsUser -> {
                mentionMuteRepository.unmuteUser(event.userLogin)
            }
            is MainEvent.MuteMentionsChannel -> {
                mentionMuteRepository.muteChannel(event.channelLogin)
                refreshChannelMuteFlags()
            }
            is MainEvent.UnmuteMentionsChannel -> {
                mentionMuteRepository.unmuteChannel(event.channelLogin)
                refreshChannelMuteFlags()
            }
            is MainEvent.MuteMentionsUserInChannel -> {
                mentionMuteRepository.muteUserInChannel(event.userLogin, event.channelLogin)
            }
            is MainEvent.UnmuteMentionsUserInChannel -> {
                mentionMuteRepository.unmuteUserInChannel(event.userLogin, event.channelLogin)
            }

            MainEvent.ShowChattersPanel -> update { it.copy(showChattersPanel = true) }
            MainEvent.HideChattersPanel -> update { it.copy(showChattersPanel = false) }
            is MainEvent.SetActiveChatChannelId -> update { state ->
                val login = state.activeChannelLogin?.lowercase() ?: ""
                val newMap = if (login.isNotEmpty() && event.channelId.isNotEmpty())
                    state.channelIdMap + (login to event.channelId)
                else state.channelIdMap
                state.copy(activeChatChannelId = event.channelId, channelIdMap = newMap)
            }
        }
    }

    private fun observeWhispers() {
        viewModelScope.launch {
            chatRepository.events.collect { event ->
                if (event is IrcEvent.Whisper) {
                    receiveWhisper(
                        fromUserId = event.userId,
                        fromUsername = event.fromUser,
                        fromDisplayName = event.displayName,
                        fromColor = event.color,
                        text = event.message
                    )
                }
            }
        }
    }

    private fun openWhisperWith(
        userId: String,
        username: String,
        displayName: String,
        avatarUrl: String,
        color: String?
    ) {
        update { state ->
            val existing = state.whisperConversations.find { it.userId == userId }
            val conversations = if (existing == null) {
                state.whisperConversations + WhisperConversation(
                    userId = userId, username = username, displayName = displayName,
                    avatarUrl = avatarUrl, color = color
                )
            } else {
                state.whisperConversations.map { if (it.userId == userId) it.copy(unreadCount = 0) else it }
            }
            state.copy(
                whisperConversations = conversations,
                activeWhisperUserId = userId,
                showWhisperPanel = true,
                totalUnreadWhispers = conversations.sumOf { it.unreadCount }
            )
        }
    }

    private fun sendWhisperMessage(toUserId: String, toUsername: String, text: String) {
        val account = state.value.selectedAccount ?: return
        val msg = WhisperMessage(
            fromUserId = account.userId,
            fromUsername = account.login,
            fromDisplayName = account.displayName,
            text = text,
            isOwn = true
        )
        update { state ->
            val conversations = state.whisperConversations.toMutableList()
            val idx = conversations.indexOfFirst { it.userId == toUserId }
            if (idx >= 0) {
                conversations[idx] =
                    conversations[idx].copy(messages = conversations[idx].messages + msg)
            } else {
                conversations.add(
                    WhisperConversation(
                        userId = toUserId,
                        username = toUsername,
                        displayName = toUsername,
                        messages = listOf(msg)
                    )
                )
            }
            state.copy(whisperConversations = conversations)
        }
        viewModelScope.launch {
            try {
                apiClient.sendWhisper(
                    accessToken = account.accessToken,
                    fromUserId = account.userId,
                    toUserId = toUserId,
                    message = text
                )
            } catch (e: Exception) {
                Napier.e("Whisper send failed: ${e.message}", tag = TAG)
                sendEffect(MainEffect.ShowError("Failed to send whisper"))
            }
        }
    }

    private fun receiveWhisper(
        fromUserId: String,
        fromUsername: String,
        fromDisplayName: String,
        fromColor: String?,
        text: String
    ) {
        val msg = WhisperMessage(
            fromUserId = fromUserId,
            fromUsername = fromUsername,
            fromDisplayName = fromDisplayName,
            fromColor = fromColor,
            text = text,
            isOwn = false
        )
        val isActiveConvo =
            state.value.activeWhisperUserId == fromUserId && state.value.showWhisperPanel
        update { state ->
            val conversations = state.whisperConversations.toMutableList()
            val idx = conversations.indexOfFirst { it.userId == fromUserId }
            if (idx >= 0) {
                val conv = conversations[idx]
                conversations[idx] = conv.copy(
                    messages = conv.messages + msg,
                    unreadCount = if (isActiveConvo) 0 else conv.unreadCount + 1
                )
            } else {
                conversations.add(
                    0, WhisperConversation(
                        userId = fromUserId, username = fromUsername, displayName = fromDisplayName,
                        color = fromColor, messages = listOf(msg),
                        unreadCount = if (isActiveConvo) 0 else 1
                    )
                )
            }
            state.copy(
                whisperConversations = conversations,
                totalUnreadWhispers = conversations.sumOf { it.unreadCount }
            )
        }
        if (!isActiveConvo) {
            viewModelScope.launch { sendEffect(MainEffect.IncomingWhisper(fromDisplayName, text)) }
        }
    }

    private fun markWhisperRead(userId: String) {
        update { state ->
            val conversations = state.whisperConversations.map {
                if (it.userId == userId) it.copy(unreadCount = 0) else it
            }
            state.copy(
                whisperConversations = conversations,
                totalUnreadWhispers = conversations.sumOf { it.unreadCount }
            )
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().collectLatest { accounts ->
                val previousId = state.value.selectedAccount?.userId
                val selected = accounts.firstOrNull { it.userId == previousId } ?: accounts.firstOrNull()
                val needsReauth = selected != null && !selected.scopes.contains("user:write:chat")
                update { state ->
                    state.copy(
                        accounts = accounts,
                        selectedAccount = selected,
                        isGuest = accounts.isEmpty(),
                        needsReauth = needsReauth
                    )
                }
                val account = state.value.selectedAccount
                if (account != null) connectToChat(account) else connectAnonymous()
            }
        }
    }

    private fun connectToChat(account: TwitchAccount) {
        viewModelScope.launch {
            try {
                connectChatUseCase(account)
                update { it.copy(isConnected = true) }
                launch { emoteRepository.loadGlobalEmotes() }
                launch { thirdPartyBadgeRepository.loadAll() }
                launch { sevenTvEventApi.connect() }
                launch {
                    try {
                        val res = apiClient.getModeratedChannels(account.accessToken, account.userId)
                        if (res is Result.Success) {
                            update { it.copy(moderatedChannelIds = res.data) }
                        }
                    } catch (_: Exception) {}
                }
                launch { checkTokenScopes(account) }
                Napier.d("Connected as ${account.login}", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to connect: ${e.message}", e, tag = TAG)
                sendEffect(MainEffect.ShowError("Failed to connect: ${e.message}"))
            }
        }
    }

    private suspend fun checkTokenScopes(account: TwitchAccount) {
        try {
            val r = apiClient.validateToken(account.accessToken)
            if (r !is Result.Success) return
            val granted = r.data.scopes.toSet()
            val missing = AppConfig.REQUIRED_SCOPES.filter { it !in granted }
            if (missing.isNotEmpty()) {
                Napier.w("Token missing ${missing.size} scopes: $missing", tag = TAG)
                val fingerprint = missing.sorted().joinToString(",")
                if (settings.getStringOrNull(KEY_WARNED_SCOPES) != fingerprint) {
                    settings.putString(KEY_WARNED_SCOPES, fingerprint)
                    sendEffect(
                        MainEffect.ShowError(
                            uiStrings().tokenStaleWarning.replace("{0}", missing.size.toString())
                        )
                    )
                }
            } else {
                settings.remove(KEY_WARNED_SCOPES)
            }
        } catch (e: Exception) {
            Napier.w("Scope check failed: ${e.message}", tag = TAG)
        }
    }

    private fun connectAnonymous() {
        viewModelScope.launch {
            try {
                chatRepository.connectAnonymous()
                update { it.copy(isConnected = true) }
                launch { emoteRepository.loadGlobalEmotes() }
                launch { thirdPartyBadgeRepository.loadAll() }
                Napier.d("Connected anonymously", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to connect anonymously: ${e.message}", e, tag = TAG)
            }
        }
    }

    private fun restoreSidebarLayout() {
        val layout = sidebarLayoutRepository.load()
        update {
            it.copy(
                sidebarCollapsed = layout.collapsed,
                sidebarWidthDp = layout.widthDp,
                miniRailCollapsed = layout.miniRailCollapsed
            )
        }
    }

    private fun markChannelMentionsRead(channelLogin: String) {
        val login = channelLogin.lowercase()
        val toMark = state.value.mentions.filter {
            !it.isRead && it.channelLogin.equals(login, ignoreCase = true)
        }
        if (toMark.isEmpty()) return
        update { state ->
            state.copy(
                mentions = state.mentions.map { m ->
                    if (!m.isRead && m.channelLogin.equals(login, ignoreCase = true)) m.copy(isRead = true) else m
                },
                unreadMentionsCount = (state.unreadMentionsCount - toMark.size).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            toMark.forEach { mentionRepository.markAsRead(it.messageId) }
        }
    }

    private fun selectChannel(login: String, scrollToMessageId: String? = null) {
        if (login.startsWith("/")) {
            update {
                it.copy(
                    monitorTabs = (it.monitorTabs + login).distinct(),
                    activeChannelLogin = login,
                    mentionsChannelActive = false,
                    showMentionsFeed = false,
                    sidebarExpanded = false
                )
            }
            return
        }
        val normalized = login.lowercase().removePrefix("#")
        dismissSystemNotifications(normalized)
        val existing = state.value.openChannels.find { it.login == normalized }
        if (existing == null) {
            val recent = state.value.recentChannels.find { it.login == normalized }
            if (recent != null) openRecentChannel(recent.login, recent.displayName, recent.profileImageUrl)
            else addChannel(normalized)
            return
        }
        update { state ->
            val restoredChannelId = state.channelIdMap[normalized] ?: ""
            state.copy(
                activeChannelLogin = normalized,
                activeChatChannelId = restoredChannelId,
                sidebarExpanded = false,
                showMentionsFeed = false,
                mentionsChannelActive = false,
                pendingScrollMessageId = scrollToMessageId,
                openChannels = state.openChannels.map {
                    if (it.login == normalized) it.copy(unreadCount = 0) else it
                },
                unfolderedChannels = state.unfolderedChannels.map {
                    if (it.login == normalized) it.copy(unreadCount = 0) else it
                },
                folders = state.folders.map { folder ->
                    folder.copy(channels = folder.channels.map {
                        if (it.login == normalized) it.copy(unreadCount = 0) else it
                    })
                }
            )
        }
        markChannelMentionsRead(normalized)
        saveChannelState()
    }

    private fun addChannel(login: String, profileImageUrl: String = "", displayName: String = "") {
        val normalized = login.lowercase().removePrefix("#").trim()
        if (normalized.isEmpty()) return
        val alreadyOpen = state.value.openChannels.find { it.login == normalized }
        if (alreadyOpen != null) {
            update {
                it.copy(
                    activeChannelLogin = normalized,
                    isAddChannelDialogVisible = false,
                    sidebarExpanded = false
                )
            }
            return
        }
        val tab = ChannelTab(
            login = normalized,
            displayName = displayName.ifEmpty { normalized },
            profileImageUrl = profileImageUrl,
            notificationsMuted = mentionMuteRepository.isChannelMuted(normalized)
        )
        update { state ->
            state.copy(
                openChannels = state.openChannels + tab,
                activeChannelLogin = normalized,
                isAddChannelDialogVisible = false,
                sidebarExpanded = false,
                unfolderedChannels = state.unfolderedChannels + tab
            )
        }
        saveChannelState()
        viewModelScope.launch {
            try {
                joinChannelUseCase(normalized)
                pollLiveStatus()
                Napier.d("Joined channel: $normalized", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to join $normalized: ${e.message}", e, tag = TAG)
                sendEffect(MainEffect.ShowError("Failed to join $normalized"))
            }
        }
    }

    private fun closeChannel(login: String) {
        if (state.value.openChannels.none { it.login == login } &&
            state.value.recentChannels.any { it.login == login }
        ) {
            forgetRecentChannel(login)
            return
        }
        update { state ->
            val newOpen = state.openChannels.filter { it.login != login }
            val newActive =
                if (state.activeChannelLogin == login) newOpen.lastOrNull()?.login else state.activeChannelLogin
            state.copy(
                openChannels = newOpen,
                activeChannelLogin = newActive,
                unfolderedChannels = state.unfolderedChannels.filter { it.login != login }
            )
        }
        saveChannelState()
        viewModelScope.launch {
            try {
                chatRepository.partChannel(login)
            } catch (e: Exception) {
                Napier.w("Failed to part $login: ${e.message}", tag = TAG)
            }
        }
    }

    private fun RecentChannel.toTab(previous: ChannelTab?): ChannelTab = ChannelTab(
        login = login,
        displayName = previous?.displayName?.takeUnless { it.equals(login, ignoreCase = true) } ?: displayName,
        profileImageUrl = previous?.profileImageUrl?.ifEmpty { null } ?: avatarUrl,
        isLive = previous?.isLive == true,
        notificationsMuted = mentionMuteRepository.isChannelMuted(login)
    )

    private fun List<RecentChannel>.toTabs(current: List<ChannelTab>): List<ChannelTab> =
        map { recent -> recent.toTab(current.firstOrNull { it.login == recent.login }) }

    private fun restoreRecentChannels() {
        val recents = recentChannelsRepository.load()
        if (recents.isNotEmpty()) update { it.copy(recentChannels = recents.toTabs(emptyList())) }
    }

    private fun restoreActiveRecentChannel() {
        val active = settings.getStringOrNull(KEY_ACTIVE_CHANNEL) ?: return
        val current = state.value
        if (current.openChannels.any { it.login == active }) return
        if (current.recentChannels.none { it.login == active }) return
        update { it.copy(activeChannelLogin = active) }
        viewModelScope.launch {
            var attempts = 0
            while (!state.value.isConnected && attempts < 20) {
                delay(500); attempts++
            }
            if (state.value.isConnected) {
                runCatching { joinChannelUseCase(active) }
                    .onFailure { Napier.w("Failed to restore recent channel $active: ${it.message}", tag = TAG) }
            }
        }
    }

    private fun openRecentChannel(login: String, displayName: String, avatarUrl: String) {
        val normalized = login.lowercase().removePrefix("#").trim()
        if (normalized.isEmpty()) return
        if (state.value.openChannels.any { it.login == normalized }) {
            update { it.copy(showBrowse = false) }
            selectChannel(normalized)
            return
        }
        val previous = state.value.recentChannels
        val recents = recentChannelsRepository.remember(normalized, displayName, avatarUrl)
        if (recents.none { it.login == normalized }) return
        dismissSystemNotifications(normalized)
        update { state ->
            state.copy(
                recentChannels = recents.toTabs(state.recentChannels),
                activeChannelLogin = normalized,
                activeChatChannelId = state.channelIdMap[normalized] ?: "",
                sidebarExpanded = false,
                showBrowse = false,
                showMentionsFeed = false,
                mentionsChannelActive = false
            )
        }
        saveChannelState()
        partEvictedRecents(previous, recents)
        viewModelScope.launch {
            try {
                joinChannelUseCase(normalized)
                pollLiveStatus()
            } catch (e: Exception) {
                Napier.e("Failed to join recent $normalized: ${e.message}", e, tag = TAG)
                sendEffect(MainEffect.ShowError("Failed to join $normalized"))
            }
        }
    }

    private fun partEvictedRecents(previous: List<ChannelTab>, current: List<RecentChannel>) {
        val kept = current.map { it.login }.toSet()
        val state = state.value
        previous.map { it.login }
            .filter { it !in kept && it != state.activeChannelLogin && state.openChannels.none { open -> open.login == it } }
            .forEach { login -> viewModelScope.launch { runCatching { chatRepository.partChannel(login) } } }
    }

    private fun forgetRecentChannel(login: String) {
        val recents = recentChannelsRepository.forget(login)
        val isOpen = state.value.openChannels.any { it.login == login }
        update { state ->
            val nextActive = if (state.activeChannelLogin == login && !isOpen) {
                state.openChannels.lastOrNull()?.login
            } else {
                state.activeChannelLogin
            }
            state.copy(recentChannels = recents.toTabs(state.recentChannels), activeChannelLogin = nextActive)
        }
        saveChannelState()
        if (!isOpen) viewModelScope.launch { runCatching { chatRepository.partChannel(login) } }
    }

    private fun promoteRecentChannel(login: String): Boolean {
        val tab = state.value.recentChannels.find { it.login == login } ?: return false
        if (state.value.openChannels.any { it.login == login }) return false
        val recents = recentChannelsRepository.forget(login)
        update { state ->
            state.copy(
                recentChannels = recents.toTabs(state.recentChannels),
                openChannels = state.openChannels + tab,
                unfolderedChannels = state.unfolderedChannels + tab
            )
        }
        saveChannelState()
        return true
    }

    private fun keepRecentChannel(login: String) {
        if (promoteRecentChannel(login)) selectChannel(login)
    }

    private fun searchChannels() {
        val account = state.value.selectedAccount
        val query = state.value.addChannelQuery.trim()
        if (query.isBlank()) return
        if (account == null) {
            addChannel(query); return
        }
        viewModelScope.launch {
            update { it.copy(isSearching = true) }
            when (val result = searchChannelsUseCase(query, account.accessToken)) {
                is Result.Success -> update {
                    it.copy(
                        searchResults = result.data,
                        isSearching = false
                    )
                }

                is Result.Error -> {
                    update { it.copy(isSearching = false) }; sendEffect(MainEffect.ShowError("Search failed"))
                }

                is Result.Loading -> {}
            }
        }
    }

    private fun restoreFolders() {
        try {
            val folders = channelFolderRepository.getAllFolders()
            if (folders.isNotEmpty()) {
                val foldersWithChannels = folders.map { folder ->
                    val channelLogins = channelFolderRepository.getChannelLoginsInFolder(folder.id)
                    val channelTabs = channelLogins.map { login ->
                        ChannelTab(
                            login = login,
                            displayName = login,
                            notificationsMuted = mentionMuteRepository.isChannelMuted(login)
                        )
                    }
                    folder.copy(channels = channelTabs)
                }
                val folderedLogins =
                    foldersWithChannels.flatMap { it.channels }.map { it.login }.toSet()
                update { state ->
                    state.copy(
                        folders = foldersWithChannels,
                        unfolderedChannels = state.unfolderedChannels.filter { it.login !in folderedLogins }
                    )
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to restore folders: ${e.message}", tag = TAG)
        }
    }

    private fun createFolder() {
        val name = state.value.newFolderName.trim()
        if (name.isEmpty()) return
        val seedChannel = state.value.newFolderSeedChannel
        val folder = ChannelFolder(
            id = "folder_${Clock.System.now().toEpochMilliseconds()}",
            name = name,
            color = state.value.newFolderColor,
            isExpanded = true
        )
        update { state ->
            state.copy(
                folders = state.folders + folder,
                isCreateFolderDialogVisible = false,
                newFolderName = "",
                newFolderColor = FolderColors.DEFAULT_HEX,
                newFolderSeedChannel = null
            )
        }
        try {
            channelFolderRepository.insertFolder(folder, sortOrder = state.value.folders.size)
        } catch (e: Exception) {
            Napier.w("Failed to persist folder: ${e.message}", tag = TAG)
            return
        }
        seedChannel?.let { moveChannelToFolder(it, folder.id) }
    }

    private fun saveFolderEdit() {
        val folderId = state.value.editingFolderId ?: return
        val name = state.value.editFolderName.trim()
        if (name.isEmpty()) return
        val color = state.value.editFolderColor
        update { state ->
            state.copy(
                folders = state.folders.map {
                    if (it.id == folderId) it.copy(name = name, color = color) else it
                },
                editingFolderId = null
            )
        }
        try {
            channelFolderRepository.updateFolderName(folderId, name)
            channelFolderRepository.updateFolderColor(folderId, color)
        } catch (e: Exception) {
            Napier.w("Failed to persist folder edit: ${e.message}", tag = TAG)
        }
    }

    private fun toggleFolder(folderId: String) {
        update { state ->
            state.copy(folders = state.folders.map {
                if (it.id == folderId) it.copy(isExpanded = !it.isExpanded) else it
            })
        }
        val expanded = state.value.folders.find { it.id == folderId }?.isExpanded ?: return
        try {
            channelFolderRepository.updateFolderExpanded(folderId, expanded)
        } catch (e: Exception) {
            Napier.w("Failed to persist folder toggle: ${e.message}", tag = TAG)
        }
    }

    private fun deleteFolder(folderId: String) {
        update { state ->
            val folder = state.folders.find { it.id == folderId }
            val channelsToMove = folder?.channels ?: emptyList()
            state.copy(
                folders = state.folders.filter { it.id != folderId },
                unfolderedChannels = state.unfolderedChannels + channelsToMove
            )
        }
        try {
            channelFolderRepository.deleteFolder(folderId)
        } catch (e: Exception) {
            Napier.w("Failed to delete folder from DB: ${e.message}", tag = TAG)
        }
    }

    private fun moveChannelToFolder(channelLogin: String, folderId: String?) {
        promoteRecentChannel(channelLogin)
        val currentFolderId =
            state.value.folders.find { folder -> folder.channels.any { it.login == channelLogin } }?.id
        update { state ->
            val channel = state.openChannels.find { it.login == channelLogin }
                ?: state.unfolderedChannels.find { it.login == channelLogin }
                ?: state.folders.flatMap { it.channels }.find { it.login == channelLogin }
                ?: return@update state
            if (folderId == null) {
                val updatedFolders =
                    state.folders.map { folder -> folder.copy(channels = folder.channels.filter { it.login != channelLogin }) }
                state.copy(
                    folders = updatedFolders,
                    unfolderedChannels = if (state.unfolderedChannels.none { it.login == channelLogin }) state.unfolderedChannels + channel else state.unfolderedChannels
                )
            } else {
                val updatedFolders = state.folders.map { folder ->
                    if (folder.id == folderId) {
                        if (folder.channels.none { it.login == channelLogin }) folder.copy(channels = folder.channels + channel) else folder
                    } else {
                        folder.copy(channels = folder.channels.filter { it.login != channelLogin })
                    }
                }
                state.copy(
                    folders = updatedFolders,
                    unfolderedChannels = state.unfolderedChannels.filter { it.login != channelLogin }
                )
            }
        }
        try {
            if (currentFolderId != null) channelFolderRepository.removeChannelFromFolder(
                channelLogin,
                currentFolderId
            )
            if (folderId != null) channelFolderRepository.addChannelToFolder(channelLogin, folderId)
        } catch (e: Exception) {
            Napier.w("Failed to persist channel-folder mapping: ${e.message}", tag = TAG)
        }
    }

    private fun incrementMentionCount(channelLogin: String) {
        if (channelLogin == state.value.activeChannelLogin) return
        update { state ->
            state.copy(
                openChannels = state.openChannels.map {
                    if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it
                },
                unfolderedChannels = state.unfolderedChannels.map {
                    if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it
                },
                folders = state.folders.map { folder ->
                    folder.copy(channels = folder.channels.map {
                        if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it
                    })
                }
            )
        }
    }

    private fun resetMentionCount(channelLogin: String) {
        update { state ->
            state.copy(
                openChannels = state.openChannels.map {
                    if (it.login == channelLogin) it.copy(unreadCount = 0) else it
                },
                unfolderedChannels = state.unfolderedChannels.map {
                    if (it.login == channelLogin) it.copy(unreadCount = 0) else it
                },
                folders = state.folders.map { folder ->
                    folder.copy(channels = folder.channels.map {
                        if (it.login == channelLogin) it.copy(unreadCount = 0) else it
                    })
                }
            )
        }
    }

    private fun selectAccount(account: TwitchAccount) {
        update { it.copy(selectedAccount = account, isGuest = false) }; connectToChat(account)
    }

    private fun deleteAccount(userId: String) {
        viewModelScope.launch {
            try {
                deleteAccountUseCase(userId)

                val remaining = state.value.accounts.filter { it.userId != userId }
                if (remaining.isEmpty()) {
                    update { it.copy(
                        selectedAccount = null,
                        isGuest = true,
                        needsReauth = false,
                        openChannels = emptyList(),
                        activeChannelLogin = ""
                    )}
                    sendEffect(MainEffect.NavigateToAuth)
                }
            } catch (e: Exception) {
                sendEffect(MainEffect.ShowError("Failed to delete account"))
            }
        }
    }

    private fun restoreSavedChannels() {
        val saved = settings.getStringOrNull(KEY_OPEN_CHANNELS) ?: return
        val activeChannel = settings.getStringOrNull(KEY_ACTIVE_CHANNEL)
        val channelLogins = saved.split(",").filter { it.isNotBlank() }
        if (channelLogins.isEmpty()) return
        val openTabs = channelLogins.map { login ->
            ChannelTab(
                login = login,
                displayName = login,
                notificationsMuted = mentionMuteRepository.isChannelMuted(login)
            )
        }
        val openSet = channelLogins.toSet()

        val savedUnfoldered = settings.getStringOrNull(KEY_UNFOLDERED_ORDER)
        val unfolderedTabs = if (savedUnfoldered != null) {
            val orderedLogins = savedUnfoldered.split(",").filter { it.isNotBlank() && it in openSet }
            val missing = channelLogins.filter { it !in orderedLogins.toSet() }
            (orderedLogins + missing).map { login -> ChannelTab(login = login, displayName = login) }
        } else {
            openTabs
        }

        update { state ->
            state.copy(
                openChannels = openTabs,
                unfolderedChannels = unfolderedTabs,
                activeChannelLogin = activeChannel ?: openTabs.firstOrNull()?.login
            )
        }
        viewModelScope.launch {
            var attempts = 0
            while (!state.value.isConnected && attempts < 20) {
                delay(500); attempts++
            }
            if (state.value.isConnected) {
                channelLogins.forEach { login ->
                    try {
                        joinChannelUseCase(login)
                    } catch (e: Exception) {
                        Napier.w("Failed to restore channel $login: ${e.message}", tag = TAG)
                    }
                }
                fetchAndUpdateProfileImages()

                delay(1500)
                scanAllChannelsForMentions(channelLogins)
            }
        }
    }

    private fun saveChannelState() {
        val channels = state.value.openChannels.joinToString(",") { it.login }
        settings.putString(KEY_OPEN_CHANNELS, channels)
        state.value.activeChannelLogin?.let { settings.putString(KEY_ACTIVE_CHANNEL, it) }
            ?: settings.remove(KEY_ACTIVE_CHANNEL)
        val unfolderedOrder = state.value.unfolderedChannels.joinToString(",") { it.login }
        settings.putString(KEY_UNFOLDERED_ORDER, unfolderedOrder)
    }

    private fun startLiveStatusPolling() {
        viewModelScope.launch {
            while (true) {
                delay(LIVE_POLL_INTERVAL_MS); pollLiveStatus()
            }
        }
        viewModelScope.launch { delay(3000); pollLiveStatus() }
    }

    private suspend fun pollLiveStatus() {
        val account = state.value.selectedAccount ?: return
        val allLogins = getAllChannelLogins()
        if (allLogins.isEmpty()) return
        try {
            val result = apiClient.getStreams(
                accessToken = account.accessToken,
                userLogins = allLogins.take(100),
                first = 100
            )
            when (result) {
                is Result.Success -> {
                    updateLiveStatus(result.data.data.map { it.userLogin.lowercase() }.toSet())
                    fetchAndUpdateProfileImages()
                }

                else -> {}
            }
        } catch (e: Exception) {
            Napier.e("PollLive exception: ${e.message}", e, tag = TAG)
        }
    }

    private fun getAllChannelLogins(): List<String> {
        val s = state.value
        val logins = mutableSetOf<String>()
        s.openChannels.forEach { logins.add(it.login.lowercase()) }
        s.unfolderedChannels.forEach { logins.add(it.login.lowercase()) }
        s.folders.forEach { folder -> folder.channels.forEach { logins.add(it.login.lowercase()) } }
        s.recentChannels.forEach { logins.add(it.login.lowercase()) }
        return logins.toList()
    }

    private fun observeLiveAlertChannels() {
        viewModelScope.launch {
            liveAlertsRepository.channels.collect { channels ->
                update { it.copy(liveNotifyChannels = channels) }
            }
        }
    }

    private fun updateLiveStatus(liveLogins: Set<String>) {
        update { state ->
            state.copy(
                openChannels = state.openChannels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) },
                unfolderedChannels = state.unfolderedChannels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) },
                folders = state.folders.map { folder ->
                    folder.copy(channels = folder.channels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) })
                },
                recentChannels = state.recentChannels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) }
            )
        }
    }

    private suspend fun fetchAndUpdateProfileImages() {
        val account = state.value.selectedAccount ?: return
        val channelsToUpdate = getAllChannelLogins().filter { login ->
            val tab = state.value.openChannels.firstOrNull { it.login.lowercase() == login }
                ?: state.value.unfolderedChannels.firstOrNull { it.login.lowercase() == login }
                ?: state.value.folders.firstNotNullOfOrNull { f -> f.channels.firstOrNull { it.login.lowercase() == login } }
                ?: state.value.recentChannels.firstOrNull { it.login.lowercase() == login }
            tab == null || tab.profileImageUrl.isEmpty() || tab.displayName.equals(tab.login, ignoreCase = true)
        }
        if (channelsToUpdate.isEmpty()) return
        try {
            val result = apiClient.getUsers(
                accessToken = account.accessToken,
                logins = channelsToUpdate.take(100)
            )
            if (result is Result.Success) {
                val imageMap =
                    result.data.data.associateBy({ it.login.lowercase() }, { it.profileImageUrl })
                val nameMap =
                    result.data.data.associateBy({ it.login.lowercase() }, { it.displayName })
                fun enrich(ch: ChannelTab): ChannelTab {
                    val key = ch.login.lowercase()
                    val url = imageMap[key]
                    val name = nameMap[key]?.takeIf { it.isNotBlank() }
                    return ch.copy(
                        profileImageUrl = if (ch.profileImageUrl.isEmpty() && url != null) url else ch.profileImageUrl,
                        displayName = if (name != null && ch.displayName.equals(ch.login, ignoreCase = true)) name else ch.displayName
                    )
                }
                update { state ->
                    state.copy(
                        openChannels = state.openChannels.map(::enrich),
                        unfolderedChannels = state.unfolderedChannels.map(::enrich),
                        folders = state.folders.map { folder ->
                            folder.copy(channels = folder.channels.map(::enrich))
                        },
                        recentChannels = state.recentChannels.map(::enrich)
                    )
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to fetch profile images: ${e.message}", tag = TAG)
        }
    }

    private fun formatWhisperTime(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    }

    private fun formatMentionTime(ts: Long): String {
        val dt = Instant.fromEpochMilliseconds(ts).toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    }

    private fun parseHexColor(hex: String?): Color? {
        if (hex == null || !hex.startsWith("#")) return null
        return try {
            val v = hex.substring(1).toLong(16)
            Color(
                red = ((v shr 16) and 0xFF) / 255f,
                green = ((v shr 8) and 0xFF) / 255f,
                blue = (v and 0xFF) / 255f
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadPersistedMentions() {
        viewModelScope.launch {
            mentionRepository.pruneOld()
            val persisted = mentionRepository.loadMentions()
            if (persisted.isNotEmpty()) {
                update { st ->
                    val existingIds = st.mentions.mapTo(HashSet()) { it.messageId }
                    val merged = (st.mentions + persisted.filter { it.messageId !in existingIds })
                        .sortedByDescending { it.timestamp }
                        .take(200)
                    st.copy(
                        mentions = merged,
                        unreadMentionsCount = merged.count { !it.isRead }
                    )
                }
            }
        }
    }

    private fun observeLiveMentions() {
        viewModelScope.launch {
            chatRepository.messages.collect { message ->
                val account = state.value.selectedAccount ?: return@collect
                if (message.userId == account.userId) return@collect

                val settings = SettingsViewModel.loadInitialState()
                val userLogin = account.login.lowercase()
                val matched = checkMentionInText(
                    message.message, userLogin,
                    settings.highlightRules.filter { it.enabled }
                )
                if (!matched) return@collect

                val channelLogin = message.channelName.lowercase()
                val activeChannel = state.value.activeChannelLogin?.lowercase()
                val isActiveChannel = channelLogin == activeChannel

                val entry = MentionEntry(
                    messageId = message.id,
                    channelLogin = channelLogin,
                    fromUsername = message.username,
                    fromDisplayName = message.displayName,
                    fromColor = message.color,
                    text = message.message,
                    timestamp = message.timestamp,
                    isRead = isActiveChannel
                )

                if (state.value.mentions.any { it.messageId == entry.messageId }) return@collect

                val isMuted = mentionMuteRepository.isMuted(
                    userLogin = entry.fromUsername,
                    channelLogin = entry.channelLogin
                )

                update { st ->
                    st.copy(
                        mentions = (listOf(entry) + st.mentions).take(200),
                        unreadMentionsCount = if (isMuted || isActiveChannel) st.unreadMentionsCount else st.unreadMentionsCount + 1,

                        openChannels = if (isActiveChannel) st.openChannels else st.openChannels.map {
                            if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it
                        },
                        unfolderedChannels = if (isActiveChannel) st.unfolderedChannels else st.unfolderedChannels.map {
                            if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it
                        },
                        folders = if (isActiveChannel) st.folders else st.folders.map { folder ->
                            folder.copy(channels = folder.channels.map {
                                if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it
                            })
                        }
                    )
                }

                viewModelScope.launch { mentionRepository.saveMention(entry) }

                if (!isMuted) {
                    sendEffect(
                        MainEffect.MentionToast(
                            channelLogin = channelLogin,
                            fromDisplayName = message.displayName,
                            text = message.message.take(100)
                        )
                    )
                }
            }
        }
    }

    private fun scanAllChannelsForMentions(channelLogins: List<String>) {
        val account = state.value.selectedAccount ?: return
        val activeChannel = state.value.activeChannelLogin?.lowercase()
        val settings = SettingsViewModel.loadInitialState()
        val userLogin = account.login.lowercase()
        val userId = account.userId

        channelLogins.forEach { login ->
            val normalized = login.lowercase()
            if (normalized == activeChannel) return@forEach

            viewModelScope.launch {
                try {
                    val result = recentMessagesClient.getRecentMessages(normalized, limit = 100)
                    val messages = (result as? RecentMessagesResult.Success)
                        ?.messages ?: return@launch
                    messages.forEach { msg ->
                        if (msg.userId == userId) return@forEach

                        if (state.value.mentions.any { it.messageId == msg.id }) return@forEach

                        if (checkMentionInText(
                                msg.message,
                                userLogin,
                                settings.highlightRules.filter { it.enabled })
                        ) {
                            val entry = MentionEntry(
                                messageId = msg.id,
                                channelLogin = normalized,
                                fromUsername = msg.username,
                                fromDisplayName = msg.displayName,
                                fromColor = msg.color,
                                text = msg.message,
                                timestamp = msg.timestamp,
                                isRead = false
                            )
                            update { st ->
                                st.copy(
                                    mentions = (st.mentions + entry)
                                        .sortedByDescending { it.timestamp }
                                        .take(200),
                                    unreadMentionsCount = st.unreadMentionsCount + 1,
                                    openChannels = st.openChannels.map {
                                        if (it.login == normalized) it.copy(unreadCount = it.unreadCount + 1) else it
                                    },
                                    unfolderedChannels = st.unfolderedChannels.map {
                                        if (it.login == normalized) it.copy(unreadCount = it.unreadCount + 1) else it
                                    },
                                    folders = st.folders.map { folder ->
                                        folder.copy(channels = folder.channels.map {
                                            if (it.login == normalized) it.copy(unreadCount = it.unreadCount + 1) else it
                                        })
                                    }
                                )
                            }
                            mentionRepository.saveMention(entry)
                        }
                    }
                } catch (e: Exception) {
                    Napier.w("Mention scan failed for $normalized: ${e.message}", tag = TAG)
                }
            }
        }
    }

    private fun checkMentionInText(
        text: String,
        userLogin: String,
        rules: List<HighlightRule>
    ): Boolean {
        if (userLogin.isNotEmpty() && text.contains("@$userLogin", ignoreCase = true)) return true
        for (rule in rules) {
            val pattern = when (rule.id) {
                "username" -> userLogin
                "whispers", "subscriptions", "first_message" -> continue
                else -> rule.pattern
            }
            if (pattern.isEmpty()) continue
            val matches = if (rule.isRegex) {
                RegexCache.regex(pattern, ignoreCase = !rule.caseSensitive)
                    ?.containsMatchIn(text) ?: false
            } else if (rule.matchSubstring) {
                text.contains(pattern, ignoreCase = !rule.caseSensitive)
            } else {
                RegexCache.wholeWord(pattern, ignoreCase = !rule.caseSensitive)
                    ?.containsMatchIn(text)
                    ?: text.equals(pattern, ignoreCase = !rule.caseSensitive)
            }
            if (matches) return true
        }
        return false
    }

    override fun onCleared() {
        viewModelScope.launch { sevenTvEventApi.disconnect() }
        super.onCleared()
    }
    private fun refreshChannelMuteFlags() {
        update { state ->
            state.copy(
                openChannels = state.openChannels.map {
                    it.copy(notificationsMuted = mentionMuteRepository.isChannelMuted(it.login))
                },
                unfolderedChannels = state.unfolderedChannels.map {
                    it.copy(notificationsMuted = mentionMuteRepository.isChannelMuted(it.login))
                },
                folders = state.folders.map { folder ->
                    folder.copy(
                        channels = folder.channels.map {
                            it.copy(notificationsMuted = mentionMuteRepository.isChannelMuted(it.login))
                        }
                    )
                }
            )
        }
    }

}
