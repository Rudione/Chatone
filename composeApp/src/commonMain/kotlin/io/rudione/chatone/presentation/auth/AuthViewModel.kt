package io.rudione.chatone.presentation.auth

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.auth.PlatformAuthHandler
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.FirstPartyDeviceAuthController
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.usecase.AuthenticateWithTokenUseCase
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.util.settings.AppConfig
import io.rudione.chatone.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = true,
    val isCheckingToken: Boolean = true,
    val error: String? = null,
    val awaitingFirstParty: Boolean = false
) : UiState

sealed class AuthEvent : UiEvent {
    data class OnTokenReceived(val token: String) : AuthEvent()
    object OnLoginClicked : AuthEvent()
    object OnGuestClicked : AuthEvent()
    object OnRetry : AuthEvent()
    object OnSkipFirstParty : AuthEvent()
}

sealed class AuthEffect : UIEffect {
    data class NavigateToHome(val account: TwitchAccount?) : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
    data class OpenAuthUrl(val url: String) : AuthEffect()
}

class AuthViewModel(
    private val authenticateWithTokenUseCase: AuthenticateWithTokenUseCase,
    private val getFirstValidAccountUseCase: GetFirstValidAccountUseCase,
    private val platformAuthHandler: PlatformAuthHandler,
    private val firstPartyDeviceAuthController: FirstPartyDeviceAuthController,
    private val moderationAuthStore: ModerationAuthStore
) : BaseViewModel<AuthState, AuthEvent, AuthEffect>(AuthState()) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        subscribeToEvents()
        checkExistingToken()
    }

    private var authInProgress = false
    private var pendingAccount: TwitchAccount? = null
    private var firstPartyWatchJob: Job? = null

    override suspend fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.OnTokenReceived -> handleToken(event.token)
            AuthEvent.OnLoginClicked -> {
                if (!authInProgress) startOAuth()
            }
            AuthEvent.OnGuestClicked -> handleGuestLogin()
            AuthEvent.OnRetry -> {
                update { it.copy(error = null) }
                if (!authInProgress) startOAuth()
            }
            AuthEvent.OnSkipFirstParty -> finishFirstPartyStep()
        }
    }

    private fun checkExistingToken() {
        viewModelScope.launch {
            try {
                val account = getFirstValidAccountUseCase()
                if (account != null) {
                    Napier.d("Found valid existing account: ${account.login}", tag = TAG)
                    update { it.copy(isLoading = false, isCheckingToken = false) }
                    sendEffectWaitSubscriber(AuthEffect.NavigateToHome(account))
                } else {
                    Napier.d("No valid account found, showing login", tag = TAG)
                    update { it.copy(isLoading = false, isCheckingToken = false) }
                }
            } catch (e: Exception) {
                Napier.e("Error checking token: ${e.message}", e, tag = TAG)
                update { it.copy(isLoading = false, isCheckingToken = false) }
            }
        }
    }

    private fun startOAuth() {
        viewModelScope.launch {
            authInProgress = true
            update { it.copy(isLoading = true, error = null) }

            val redirectUri = platformAuthHandler.getRedirectUri()
            val clientId = platformAuthHandler.getClientId()
            val state = platformAuthHandler.newAuthSession()
            val authUrl = AppConfig.getAuthUrl(clientId, redirectUri, state)
            Napier.d("═══════════════════════════════════════", tag = TAG)
            Napier.d("OAuth URL: $authUrl", tag = TAG)
            Napier.d("Redirect URI: $redirectUri", tag = TAG)
            Napier.d("Client ID: $clientId", tag = TAG)
            Napier.d("IMPORTANT: Twitch app redirect URL MUST be exactly: $redirectUri", tag = TAG)
            Napier.d("═══════════════════════════════════════", tag = TAG)

            try {

                platformAuthHandler.startAuth(authUrl)

                val token = platformAuthHandler.awaitToken()
                if (token != null) {
                    Napier.d("Token received, authenticating...", tag = TAG)
                    handleToken(token)
                } else {
                    Napier.e("No token received from OAuth flow", tag = TAG)
                    update {
                        it.copy(
                            isLoading = false,
                            error = "Authentication failed.\n\nMake sure your Twitch app (dev.twitch.tv/console) has this exact OAuth Redirect URL:\n$redirectUri"
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e("OAuth flow error: ${e.message}", e, tag = TAG)
                update { it.copy(isLoading = false, error = "OAuth error: ${e.message}") }
            } finally {
                platformAuthHandler.cleanup()
                authInProgress = false
            }
        }
    }

    private fun handleToken(token: String) {
        viewModelScope.launch {
            update { it.copy(isLoading = true, error = null) }

            when (val result = authenticateWithTokenUseCase(token)) {
                is Result.Success -> {
                    Napier.d("Authentication successful", tag = TAG)
                    pendingAccount = result.data
                    update { it.copy(isLoading = false) }
                    if (moderationAuthStore.identity.value != null) {
                        sendEffect(AuthEffect.NavigateToHome(result.data))
                    } else {
                        startFirstPartyStep()
                    }
                }
                is Result.Error -> {
                    val errorMessage = result.exception.message ?: "Authentication failed"
                    Napier.e("Authentication failed: $errorMessage", result.exception, tag = TAG)
                    update { it.copy(isLoading = false, error = errorMessage) }
                    sendEffect(AuthEffect.ShowError(errorMessage))
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun startFirstPartyStep() {
        update { it.copy(awaitingFirstParty = true) }
        firstPartyDeviceAuthController.start()
        firstPartyWatchJob?.cancel()
        firstPartyWatchJob = viewModelScope.launch {
            var lastState: DeviceAuthState? = null
            firstPartyDeviceAuthController.state.collect { deviceState ->
                if (deviceState is DeviceAuthState.Success && lastState !is DeviceAuthState.Success) {
                    delay(900)
                    finishFirstPartyStep()
                }
                lastState = deviceState
            }
        }
    }

    private fun finishFirstPartyStep() {
        firstPartyWatchJob?.cancel()
        firstPartyWatchJob = null
        firstPartyDeviceAuthController.cancel()
        update { it.copy(awaitingFirstParty = false) }
        sendEffect(AuthEffect.NavigateToHome(pendingAccount))
    }

    private fun handleGuestLogin() {
        viewModelScope.launch {
            sendEffect(AuthEffect.NavigateToHome(null))
        }
    }
}
