package io.rudione.chatone.presentation.main

import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.remote.emote.SevenTvEventApi
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.usecase.*
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.repository.ChannelFolderRepository
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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
    val isAddChannelDialogVisible: Boolean = false,
    val addChannelQuery: String = "",
    val searchResults: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
    val isConnected: Boolean = false,
    val showSettings: Boolean = false,
    val isCreateFolderDialogVisible: Boolean = false,
    val newFolderName: String = ""
) : UiState

sealed class MainEvent : UiEvent {
    object ToggleSidebar : MainEvent()
    object CloseSidebar : MainEvent()
    data class SelectChannel(val login: String) : MainEvent()
    data class CloseChannel(val login: String) : MainEvent()
    // В sealed class MainEvent добавь:
    data class ReorderChannels(val fromIndex: Int, val toIndex: Int) : MainEvent()
    data class DropChannelOnFolder(val channelLogin: String, val folderId: String) : MainEvent()
    data class AddChannel(val login: String, val profileImageUrl: String = "", val displayName: String = "") : MainEvent()
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
}

sealed class MainEffect : UIEffect {
    object NavigateToAuth : MainEffect()
    data class ShowError(val message: String) : MainEffect()
    data class ShowEmoteUpdate(val text: String) : MainEffect()
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
    private val apiClient: TwitchApiClient
) : BaseViewModel<MainState, MainEvent, MainEffect>(MainState()) {

    companion object {
        private const val TAG = "MainViewModel"
        private const val KEY_OPEN_CHANNELS = "open_channels"
        private const val KEY_ACTIVE_CHANNEL = "active_channel"
        private const val KEY_FOLDERS = "folders"
        private const val LIVE_POLL_INTERVAL_MS = 60_000L
    }

    private val settings = Settings()

    init {
        subscribeToEvents()
        loadAccounts()
        restoreSavedChannels()
        restoreFolders()
        observeEmoteUpdates()
        startLiveStatusPolling()
    }

    override suspend fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.ToggleSidebar -> update { it.copy(sidebarExpanded = !it.sidebarExpanded) }
            MainEvent.CloseSidebar -> update { it.copy(sidebarExpanded = false) }
            is MainEvent.SelectChannel -> selectChannel(event.login)
            is MainEvent.CloseChannel -> closeChannel(event.login)
            is MainEvent.AddChannel -> addChannel(event.login, event.profileImageUrl, event.displayName)
            MainEvent.ShowAddChannelDialog -> update { it.copy(isAddChannelDialogVisible = true, addChannelQuery = "", searchResults = emptyList()) }
            MainEvent.HideAddChannelDialog -> update { it.copy(isAddChannelDialogVisible = false) }
            is MainEvent.UpdateAddChannelQuery -> update { it.copy(addChannelQuery = event.query) }
            MainEvent.SearchChannels -> searchChannels()
            MainEvent.ShowCreateFolderDialog -> update { it.copy(isCreateFolderDialogVisible = true, newFolderName = "") }
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

            is MainEvent.DropChannelOnFolder -> {
                moveChannelToFolder(event.channelLogin, event.folderId)
            }
            is MainEvent.ToggleFolder -> toggleFolder(event.folderId)
            is MainEvent.DeleteFolder -> deleteFolder(event.folderId)
            is MainEvent.MoveChannelToFolder -> moveChannelToFolder(event.channelLogin, event.folderId)
            is MainEvent.SelectAccount -> selectAccount(event.account)
            MainEvent.AddAccount -> sendEffect(MainEffect.NavigateToAuth)
            is MainEvent.DeleteAccount -> deleteAccount(event.userId)
            MainEvent.ShowSettings -> update { it.copy(showSettings = true) }
            MainEvent.HideSettings -> update { it.copy(showSettings = false) }
            is MainEvent.IncrementMentionCount -> incrementMentionCount(event.channelLogin)
            is MainEvent.ResetMentionCount -> resetMentionCount(event.channelLogin)
            MainEvent.NavigateToAuth -> sendEffect(MainEffect.NavigateToAuth)
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().collectLatest { accounts ->
                update { state ->
                    val selected = state.selectedAccount ?: accounts.firstOrNull()
                    state.copy(accounts = accounts, selectedAccount = selected, isGuest = accounts.isEmpty())
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
        if (existing == null) { addChannel(normalized); return }
        update { state ->
            state.copy(
                activeChannelLogin = normalized,
                sidebarExpanded = false,
                openChannels = state.openChannels.map { if (it.login == normalized) it.copy(unreadCount = 0) else it },
                unfolderedChannels = state.unfolderedChannels.map { if (it.login == normalized) it.copy(unreadCount = 0) else it },
                folders = state.folders.map { folder ->
                    folder.copy(channels = folder.channels.map { if (it.login == normalized) it.copy(unreadCount = 0) else it })
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
            update { it.copy(activeChannelLogin = normalized, isAddChannelDialogVisible = false, sidebarExpanded = false) }
            return
        }
        val tab = ChannelTab(login = normalized, displayName = displayName.ifEmpty { normalized }, profileImageUrl = profileImageUrl)
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
                // ↓↓↓ ВАЖНО: сразу проверяем live статус и аватарки для нового канала
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
            val newActive = if (state.activeChannelLogin == login) newOpen.lastOrNull()?.login else state.activeChannelLogin
            state.copy(openChannels = newOpen, activeChannelLogin = newActive, unfolderedChannels = state.unfolderedChannels.filter { it.login != login })
        }
        saveChannelState()
        viewModelScope.launch {
            try { chatRepository.partChannel(login) } catch (e: Exception) { Napier.w("Failed to part $login: ${e.message}", tag = TAG) }
        }
    }

    private fun searchChannels() {
        val account = state.value.selectedAccount
        val query = state.value.addChannelQuery.trim()
        if (query.isBlank()) return
        if (account == null) { addChannel(query); return }
        viewModelScope.launch {
            update { it.copy(isSearching = true) }
            when (val result = searchChannelsUseCase(query, account.accessToken)) {
                is Result.Success -> update { it.copy(searchResults = result.data, isSearching = false) }
                is Result.Error -> { update { it.copy(isSearching = false) }; sendEffect(MainEffect.ShowError("Search failed")) }
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
                    val channelTabs = channelLogins.map { login -> ChannelTab(login = login, displayName = login) }
                    folder.copy(channels = channelTabs)
                }
                val folderedLogins = foldersWithChannels.flatMap { it.channels }.map { it.login }.toSet()
                update { state ->
                    state.copy(folders = foldersWithChannels, unfolderedChannels = state.unfolderedChannels.filter { it.login !in folderedLogins })
                }
            }
        } catch (e: Exception) { Napier.w("Failed to restore folders: ${e.message}", tag = TAG) }
    }

    private fun createFolder() {
        val name = state.value.newFolderName.trim()
        if (name.isEmpty()) return
        val folder = ChannelFolder(id = "folder_${Clock.System.now().toEpochMilliseconds()}", name = name, isExpanded = true)
        update { state -> state.copy(folders = state.folders + folder, isCreateFolderDialogVisible = false, newFolderName = "") }
        try { channelFolderRepository.insertFolder(folder, sortOrder = state.value.folders.size) } catch (e: Exception) { Napier.w("Failed to persist folder: ${e.message}", tag = TAG) }
    }

    private fun toggleFolder(folderId: String) {
        update { state ->
            state.copy(folders = state.folders.map { folder -> if (folder.id == folderId) folder.copy(isExpanded = !folder.isExpanded) else folder })
        }
        val expanded = state.value.folders.find { it.id == folderId }?.isExpanded ?: return
        try { channelFolderRepository.updateFolderExpanded(folderId, expanded) } catch (e: Exception) { Napier.w("Failed to persist folder toggle: ${e.message}", tag = TAG) }
    }

    private fun deleteFolder(folderId: String) {
        update { state ->
            val folder = state.folders.find { it.id == folderId }
            val channelsToMove = folder?.channels ?: emptyList()
            state.copy(folders = state.folders.filter { it.id != folderId }, unfolderedChannels = state.unfolderedChannels + channelsToMove)
        }
        try { channelFolderRepository.deleteFolder(folderId) } catch (e: Exception) { Napier.w("Failed to delete folder from DB: ${e.message}", tag = TAG) }
    }

    private fun moveChannelToFolder(channelLogin: String, folderId: String?) {
        val currentFolderId = state.value.folders.find { folder -> folder.channels.any { it.login == channelLogin } }?.id
        update { state ->
            val channel = state.openChannels.find { it.login == channelLogin } ?: state.unfolderedChannels.find { it.login == channelLogin } ?: state.folders.flatMap { it.channels }.find { it.login == channelLogin } ?: return@update state
            if (folderId == null) {
                val updatedFolders = state.folders.map { folder -> folder.copy(channels = folder.channels.filter { it.login != channelLogin }) }
                state.copy(folders = updatedFolders, unfolderedChannels = if (state.unfolderedChannels.none { it.login == channelLogin }) state.unfolderedChannels + channel else state.unfolderedChannels)
            } else {
                val updatedFolders = state.folders.map { folder ->
                    if (folder.id == folderId) { if (folder.channels.none { it.login == channelLogin }) folder.copy(channels = folder.channels + channel) else folder }
                    else { folder.copy(channels = folder.channels.filter { it.login != channelLogin }) }
                }
                state.copy(folders = updatedFolders, unfolderedChannels = state.unfolderedChannels.filter { it.login != channelLogin })
            }
        }
        try {
            if (currentFolderId != null) channelFolderRepository.removeChannelFromFolder(channelLogin, currentFolderId)
            if (folderId != null) channelFolderRepository.addChannelToFolder(channelLogin, folderId)
        } catch (e: Exception) { Napier.w("Failed to persist channel-folder mapping: ${e.message}", tag = TAG) }
    }

    private fun incrementMentionCount(channelLogin: String) {
        if (channelLogin == state.value.activeChannelLogin) return
        update { state ->
            state.copy(
                openChannels = state.openChannels.map { if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it },
                unfolderedChannels = state.unfolderedChannels.map { if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it },
                folders = state.folders.map { folder -> folder.copy(channels = folder.channels.map { if (it.login == channelLogin) it.copy(unreadCount = it.unreadCount + 1) else it }) }
            )
        }
    }

    private fun resetMentionCount(channelLogin: String) {
        update { state ->
            state.copy(
                openChannels = state.openChannels.map { if (it.login == channelLogin) it.copy(unreadCount = 0) else it },
                unfolderedChannels = state.unfolderedChannels.map { if (it.login == channelLogin) it.copy(unreadCount = 0) else it },
                folders = state.folders.map { folder -> folder.copy(channels = folder.channels.map { if (it.login == channelLogin) it.copy(unreadCount = 0) else it }) }
            )
        }
    }

    private fun selectAccount(account: TwitchAccount) { update { it.copy(selectedAccount = account, isGuest = false) }; connectToChat(account) }
    private fun deleteAccount(userId: String) { viewModelScope.launch { try { deleteAccountUseCase(userId) } catch (e: Exception) { sendEffect(MainEffect.ShowError("Failed to delete account")) } } }

    private fun restoreSavedChannels() {
        val saved = settings.getStringOrNull(KEY_OPEN_CHANNELS) ?: return
        val activeChannel = settings.getStringOrNull(KEY_ACTIVE_CHANNEL)
        val channelLogins = saved.split(",").filter { it.isNotBlank() }
        if (channelLogins.isEmpty()) return
        val tabs = channelLogins.map { login -> ChannelTab(login = login, displayName = login) }
        update { state -> state.copy(openChannels = tabs, unfolderedChannels = tabs, activeChannelLogin = activeChannel ?: tabs.firstOrNull()?.login) }
        viewModelScope.launch {
            var attempts = 0
            while (!state.value.isConnected && attempts < 20) { kotlinx.coroutines.delay(500); attempts++ }
            if (state.value.isConnected) {
                channelLogins.forEach { login ->
                    try { joinChannelUseCase(login); Napier.d("Restored channel: $login", tag = TAG) } catch (e: Exception) { Napier.w("Failed to restore channel $login: ${e.message}", tag = TAG) }
                }
                fetchAndUpdateProfileImages()
            }
        }
    }

    private fun saveChannelState() {
        val channels = state.value.openChannels.joinToString(",") { it.login }
        settings.putString(KEY_OPEN_CHANNELS, channels)
        state.value.activeChannelLogin?.let { settings.putString(KEY_ACTIVE_CHANNEL, it) } ?: settings.remove(KEY_ACTIVE_CHANNEL)
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
        viewModelScope.launch { while (true) { delay(LIVE_POLL_INTERVAL_MS); pollLiveStatus() } }
        viewModelScope.launch { delay(3000); pollLiveStatus() }
    }

    private suspend fun pollLiveStatus() {
        val account = state.value.selectedAccount ?: return
        val allLogins = getAllChannelLogins()
        if (allLogins.isEmpty()) return

        Napier.d("🔵 [PollLive] Requesting streams for: $allLogins", tag = TAG)

        try {
            val result = apiClient.getStreams(
                accessToken = account.accessToken,
                userLogins = allLogins.take(100),
                first = 100  // ← ВАЖНО: увеличь с 20 до 100!
            )
            when (result) {
                is Result.Success -> {
                    val liveLogins = result.data.data.map { it.userLogin.lowercase() }.toSet()
                    Napier.d("🟢 [PollLive] API returned ${result.data.data.size} live streams: $liveLogins", tag = TAG)
                    updateLiveStatus(liveLogins)
                    fetchAndUpdateProfileImages()
                }
                is Result.Error -> {
                    Napier.e("🔴 [PollLive] API error: ${result.exception?.message}", tag = TAG)
                }
                else -> {}
            }
        } catch (e: Exception) {
            Napier.e("🔴 [PollLive] Exception: ${e.message}", e, tag = TAG)
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
                folders = state.folders.map { folder -> folder.copy(channels = folder.channels.map { ch -> ch.copy(isLive = ch.login.lowercase() in liveLogins) }) }
            )
        }
    }

    private suspend fun fetchAndUpdateProfileImages() {
        val account = state.value.selectedAccount ?: return
        val channelsToUpdate = state.value.openChannels.filter { it.profileImageUrl.isEmpty() }
        if (channelsToUpdate.isEmpty()) return
        try {
            val result = apiClient.getUsers(accessToken = account.accessToken, logins = channelsToUpdate.map { it.login }.take(100))
            if (result is Result.Success) {
                val imageMap = result.data.data.associateBy({ it.login.lowercase() }, { it.profileImageUrl })
                update { state ->
                    state.copy(
                        openChannels = state.openChannels.map { ch -> imageMap[ch.login]?.let { url -> ch.copy(profileImageUrl = url) } ?: ch },
                        unfolderedChannels = state.unfolderedChannels.map { ch -> imageMap[ch.login]?.let { url -> ch.copy(profileImageUrl = url) } ?: ch },
                        folders = state.folders.map { folder -> folder.copy(channels = folder.channels.map { ch -> imageMap[ch.login]?.let { url -> ch.copy(profileImageUrl = url) } ?: ch }) }
                    )
                }
            }
        } catch (e: Exception) { Napier.w("Failed to fetch profile images: ${e.message}", tag = TAG) }
    }

    override fun onCleared() { viewModelScope.launch { sevenTvEventApi.disconnect() }; super.onCleared() }
}