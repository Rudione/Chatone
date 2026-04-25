package io.rudione.chatone.presentation.main

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
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.remote.RecentMessagesClient
import io.rudione.chatone.data.repository.MentionRepository
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


data class ChannelFolder(
    val id: String,
    val name: String,
    val color: String = "#9146FF",
    val isExpanded: Boolean = true,
    val channels: List<ChannelTab> = emptyList()
)

data class ChannelTab(
    val login: String,
    val displayName: String,
    val profileImageUrl: String = "",
    val isLive: Boolean = false,
    val unreadCount: Int = 0
)

data class MainState(
    val accounts: List<TwitchAccount> = emptyList(),
    val selectedAccount: TwitchAccount? = null,
    val isGuest: Boolean = false,
    val folders: List<ChannelFolder> = emptyList(),
    val unfolderedChannels: List<ChannelTab> = emptyList(),
    val activeChannelLogin: String? = null,
    val openChannels: List<ChannelTab> = emptyList(),
    val sidebarExpanded: Boolean = false,
    val sidebarCollapsed: Boolean = false,
    val isAddChannelDialogVisible: Boolean = false,
    val addChannelQuery: String = "",
    val searchResults: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
    val isConnected: Boolean = false,
    val showSettings: Boolean = false,
    val isCreateFolderDialogVisible: Boolean = false,
    val newFolderName: String = "",

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
    val needsReauth: Boolean = false
) : UiState


sealed class MainEvent : UiEvent {
    object ToggleSidebar : MainEvent()
    object CloseSidebar : MainEvent()
    object ToggleSidebarCollapsed : MainEvent()
    data class SelectChannel(val login: String) : MainEvent()
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
    object ShowCreateFolderDialog : MainEvent()
    object HideCreateFolderDialog : MainEvent()
    data class UpdateNewFolderName(val name: String) : MainEvent()
    object CreateFolder : MainEvent()
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
}


sealed class MainEffect : UIEffect {
    object NavigateToAuth : MainEffect()
    data class ShowError(val message: String) : MainEffect()
    data class ShowEmoteUpdate(val text: String) : MainEffect()
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
    private val recentMessagesClient: RecentMessagesClient
) : BaseViewModel<MainState, MainEvent, MainEffect>(MainState()) {

    companion object {
        private const val TAG = "MainViewModel"
        private const val KEY_OPEN_CHANNELS = "open_channels"
        private const val KEY_ACTIVE_CHANNEL = "active_channel"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_UNFOLDERED_ORDER = "unfoldered_order_v2"
        private const val LIVE_POLL_INTERVAL_MS = 60_000L
    }

    private val settings = Settings()

    init {
        subscribeToEvents()
        loadAccounts()
        restoreSavedChannels()
        restoreFolders()
        observeEmoteUpdates()
        observeWhispers()
        observeLiveMentions()
        startLiveStatusPolling()
        loadPersistedMentions()
    }

    override suspend fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.ToggleSidebar -> update { it.copy(sidebarExpanded = !it.sidebarExpanded) }
            MainEvent.CloseSidebar -> update { it.copy(sidebarExpanded = false) }
            MainEvent.ToggleSidebarCollapsed -> update { it.copy(sidebarCollapsed = !it.sidebarCollapsed) }
            is MainEvent.SelectChannel -> selectChannel(event.login)
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
            MainEvent.ShowCreateFolderDialog -> update {
                it.copy(
                    isCreateFolderDialogVisible = true,
                    newFolderName = ""
                )
            }

            MainEvent.HideCreateFolderDialog -> update { it.copy(isCreateFolderDialogVisible = false) }
            is MainEvent.UpdateNewFolderName -> update { it.copy(newFolderName = event.name) }
            MainEvent.CreateFolder -> createFolder()
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
            MainEvent.NavigateToAuth -> sendEffect(MainEffect.NavigateToAuth)


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
            is MainEvent.MarkChannelMentionsRead -> {
                val login = event.channelLogin.lowercase()
                val toMark = state.value.mentions.filter {
                    !it.isRead && it.channelLogin.equals(login, ignoreCase = true)
                }
                if (toMark.isNotEmpty()) {
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
            }
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
                    update {
                        it.copy(
                            mentions = (listOf(event.entry) + it.mentions).take(200),
                            unreadMentionsCount = it.unreadMentionsCount + 1
                        )
                    }
                    viewModelScope.launch { mentionRepository.saveMention(event.entry) }
                }
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
                if (event is io.rudione.chatone.domain.model.IrcEvent.Whisper) {
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
                val selected = state.value.selectedAccount ?: accounts.firstOrNull()
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
                launch { sevenTvEventApi.connect() }
                Napier.d("Connected as ${account.login}", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to connect: ${e.message}", e, tag = TAG)
                sendEffect(MainEffect.ShowError("Failed to connect: ${e.message}"))
            }
        }
    }

    private fun connectAnonymous() {
        viewModelScope.launch {
            try {
                chatRepository.connectAnonymous()
                update { it.copy(isConnected = true) }
                launch { emoteRepository.loadGlobalEmotes() }
                Napier.d("Connected anonymously", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to connect anonymously: ${e.message}", e, tag = TAG)
            }
        }
    }

    private fun selectChannel(login: String) {
        val normalized = login.lowercase().removePrefix("#")
        val existing = state.value.openChannels.find { it.login == normalized }
        if (existing == null) {
            addChannel(normalized); return
        }
        update { state ->
            val restoredChannelId = state.channelIdMap[normalized] ?: ""
            state.copy(
                activeChannelLogin = normalized,
                activeChatChannelId = restoredChannelId,
                sidebarExpanded = false,
                showMentionsFeed = false,
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
            profileImageUrl = profileImageUrl
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
                            displayName = login
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
        val folder = ChannelFolder(
            id = "folder_${Clock.System.now().toEpochMilliseconds()}",
            name = name,
            isExpanded = true
        )
        update { state ->
            state.copy(
                folders = state.folders + folder,
                isCreateFolderDialogVisible = false,
                newFolderName = ""
            )
        }
        try {
            channelFolderRepository.insertFolder(folder, sortOrder = state.value.folders.size)
        } catch (e: Exception) {
            Napier.w("Failed to persist folder: ${e.message}", tag = TAG)
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
        val openTabs = channelLogins.map { login -> ChannelTab(login = login, displayName = login) }
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

    private fun observeEmoteUpdates() {
        viewModelScope.launch {
            sevenTvEventApi.emoteSetUpdates.collect { event ->
                val text = when (event) {
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteAdded -> "${event.actorName} added emote ${event.emoteName}"
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRemoved -> "${event.actorName} removed emote ${event.emoteName}"
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRenamed -> "${event.actorName} renamed ${event.oldName} to ${event.newName}"
                }
                sendEffect(MainEffect.ShowEmoteUpdate(text))
            }
        }
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
        return logins.toList()
    }

    private fun updateLiveStatus(liveLogins: Set<String>) {
        update { state ->
            state.copy(
                openChannels = state.openChannels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) },
                unfolderedChannels = state.unfolderedChannels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) },
                folders = state.folders.map { folder ->
                    folder.copy(channels = folder.channels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) })
                }
            )
        }
    }

    private suspend fun fetchAndUpdateProfileImages() {
        val account = state.value.selectedAccount ?: return
        val channelsToUpdate = state.value.openChannels.filter { it.profileImageUrl.isEmpty() }
        if (channelsToUpdate.isEmpty()) return
        try {
            val result = apiClient.getUsers(
                accessToken = account.accessToken,
                logins = channelsToUpdate.map { it.login }.take(100)
            )
            if (result is Result.Success) {
                val imageMap =
                    result.data.data.associateBy({ it.login.lowercase() }, { it.profileImageUrl })
                update { state ->
                    state.copy(
                        openChannels = state.openChannels.map { ch ->
                            imageMap[ch.login]?.let { url ->
                                ch.copy(
                                    profileImageUrl = url
                                )
                            } ?: ch
                        },
                        unfolderedChannels = state.unfolderedChannels.map { ch ->
                            imageMap[ch.login]?.let { url ->
                                ch.copy(
                                    profileImageUrl = url
                                )
                            } ?: ch
                        },
                        folders = state.folders.map { folder ->
                            folder.copy(channels = folder.channels.map { ch ->
                                imageMap[ch.login]?.let { url ->
                                    ch.copy(
                                        profileImageUrl = url
                                    )
                                } ?: ch
                            })
                        }
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
                val entry = MentionEntry(
                    messageId = message.id,
                    channelLogin = channelLogin,
                    fromUsername = message.username,
                    fromDisplayName = message.displayName,
                    fromColor = message.color,
                    text = message.message,
                    timestamp = message.timestamp,
                    isRead = false
                )


                if (state.value.mentions.any { it.messageId == entry.messageId }) return@collect

                val activeChannel = state.value.activeChannelLogin?.lowercase()
                val isActiveChannel = channelLogin == activeChannel

                update { st ->
                    st.copy(
                        mentions = (listOf(entry) + st.mentions).take(200),
                        unreadMentionsCount = st.unreadMentionsCount + 1,

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
                    val messages = recentMessagesClient.getRecentMessages(normalized, limit = 100)
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
        rules: List<io.rudione.chatone.domain.model.HighlightRule>
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
                try {
                    val opts =
                        if (rule.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                    Regex(pattern, opts).containsMatchIn(text)
                } catch (_: Exception) {
                    false
                }
            } else {
                text.contains(pattern, ignoreCase = !rule.caseSensitive)
            }
            if (matches) return true
        }
        return false
    }

    override fun onCleared() {
        viewModelScope.launch { sevenTvEventApi.disconnect() }
        super.onCleared()
    }
}