package io.rudione.chatone.presentation.home

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.usecase.ConnectChatUseCase
import io.rudione.chatone.domain.usecase.DeleteAccountUseCase
import io.rudione.chatone.domain.usecase.GetAccountsUseCase
import io.rudione.chatone.domain.usecase.GetChannelInfoUseCase
import io.rudione.chatone.domain.usecase.JoinChannelUseCase
import io.rudione.chatone.domain.usecase.SearchChannelsUseCase
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.util.Result
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeState(
    val accounts: List<TwitchAccount> = emptyList(),
    val selectedAccount: TwitchAccount? = null,
    val isLoading: Boolean = false,
    val isGuest: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Channel> = emptyList(),
    val isSearching: Boolean = false,
    val joinedChannels: List<String> = emptyList()
) : UiState

sealed class HomeEvent : UiEvent {
    data class OnAccountSelected(val account: TwitchAccount) : HomeEvent()
    data class OnDeleteAccount(val userId: String) : HomeEvent()
    data class OnSearchQueryChanged(val query: String) : HomeEvent()
    object OnSearchChannels : HomeEvent()
    data class OnJoinChannel(val channelLogin: String) : HomeEvent()
    data class OnOpenChannel(val channelLogin: String) : HomeEvent()
    object OnAddAccount : HomeEvent()
}

sealed class HomeEffect : UIEffect {
    object NavigateToAuth : HomeEffect()
    data class NavigateToChat(val channelLogin: String) : HomeEffect()
    data class ShowError(val message: String) : HomeEffect()
}

class HomeViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val searchChannelsUseCase: SearchChannelsUseCase,
    private val getChannelInfoUseCase: GetChannelInfoUseCase,
    private val joinChannelUseCase: JoinChannelUseCase,
    private val connectChatUseCase: ConnectChatUseCase,
    private val chatRepository: ChatRepository
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(HomeState()) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    init {
        subscribeToEvents()
        loadAccounts()
    }

    override suspend fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnAccountSelected -> selectAccount(event.account)
            is HomeEvent.OnDeleteAccount -> deleteAccount(event.userId)
            is HomeEvent.OnSearchQueryChanged -> updateSearchQuery(event.query)
            HomeEvent.OnSearchChannels -> searchChannels()
            is HomeEvent.OnJoinChannel -> joinChannel(event.channelLogin)
            is HomeEvent.OnOpenChannel -> openChannel(event.channelLogin)
            HomeEvent.OnAddAccount -> sendEffect(HomeEffect.NavigateToAuth)
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            update { it.copy(isLoading = true) }
            getAccountsUseCase().collectLatest { accounts ->
                update { state ->
                    val selected = state.selectedAccount ?: accounts.firstOrNull()
                    state.copy(
                        accounts = accounts,
                        selectedAccount = selected,
                        isLoading = false,
                        isGuest = accounts.isEmpty()
                    )
                }

                val account = state.value.selectedAccount
                if (account != null) {
                    connectToChat(account)
                } else {
                    // Guest mode - anonymous connection
                    connectAnonymous()
                }
            }
        }
    }

    private fun selectAccount(account: TwitchAccount) {
        update { it.copy(selectedAccount = account, isGuest = false) }
        connectToChat(account)
    }

    private fun connectToChat(account: TwitchAccount) {
        viewModelScope.launch {
            try {
                connectChatUseCase(account)
                Napier.d("Connected to chat with account: ${account.login}", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to connect to chat: ${e.message}", e, tag = TAG)
                sendEffect(HomeEffect.ShowError("Failed to connect to chat: ${e.message}"))
            }
        }
    }

    private fun connectAnonymous() {
        viewModelScope.launch {
            try {
                chatRepository.connectAnonymous()
                Napier.d("Connected anonymously", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to connect anonymously: ${e.message}", e, tag = TAG)
            }
        }
    }

    private fun deleteAccount(userId: String) {
        viewModelScope.launch {
            try {
                deleteAccountUseCase(userId)
                Napier.d("Account deleted: $userId", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to delete account: ${e.message}", e, tag = TAG)
                sendEffect(HomeEffect.ShowError("Failed to delete account: ${e.message}"))
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        update { it.copy(searchQuery = query) }
    }

    private fun searchChannels() {
        val account = state.value.selectedAccount
        val query = state.value.searchQuery
        if (query.isBlank()) return

        if (account == null) {
            sendEffect(HomeEffect.ShowError("Login to search channels"))
            return
        }

        viewModelScope.launch {
            update { it.copy(isSearching = true) }

            when (val result = searchChannelsUseCase(query, account.accessToken)) {
                is Result.Success -> {
                    update { it.copy(searchResults = result.data, isSearching = false) }
                }
                is Result.Error -> {
                    Napier.e("Search failed: ${result.exception.message}", result.exception, tag = TAG)
                    update { it.copy(isSearching = false) }
                    sendEffect(HomeEffect.ShowError("Search failed: ${result.exception.message}"))
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun joinChannel(channelLogin: String) {
        viewModelScope.launch {
            try {
                joinChannelUseCase(channelLogin)
                update { state ->
                    state.copy(joinedChannels = state.joinedChannels + channelLogin)
                }
                Napier.d("Joined channel: $channelLogin", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to join channel: ${e.message}", e, tag = TAG)
                sendEffect(HomeEffect.ShowError("Failed to join channel: ${e.message}"))
            }
        }
    }

    private fun openChannel(channelLogin: String) {
        viewModelScope.launch {
            if (!state.value.joinedChannels.contains(channelLogin)) {
                joinChannel(channelLogin)
            }
            sendEffect(HomeEffect.NavigateToChat(channelLogin))
        }
    }
}
