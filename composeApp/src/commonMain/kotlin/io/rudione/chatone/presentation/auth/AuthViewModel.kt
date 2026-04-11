package io.rudione.chatone.presentation.auth

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.rudione.chatone.auth.PlatformAuthHandler
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.usecase.AuthenticateWithTokenUseCase
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import io.rudione.chatone.util.AppConfig
import io.rudione.chatone.util.Result
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = true,
    val isCheckingToken: Boolean = true,
    val error: String? = null
) : UiState

sealed class AuthEvent : UiEvent {
    data class OnTokenReceived(val token: String) : AuthEvent()
    object OnLoginClicked : AuthEvent()
    object OnGuestClicked : AuthEvent()
    object OnRetry : AuthEvent()
}

sealed class AuthEffect : UIEffect {
    data class NavigateToHome(val account: TwitchAccount?) : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
    data class OpenAuthUrl(val url: String) : AuthEffect()
}

class AuthViewModel(
    private val authenticateWithTokenUseCase: AuthenticateWithTokenUseCase,
    private val getFirstValidAccountUseCase: GetFirstValidAccountUseCase,
    private val platformAuthHandler: PlatformAuthHandler
) : BaseViewModel<AuthState, AuthEvent, AuthEffect>(AuthState()) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        subscribeToEvents()
        checkExistingToken()
    }

    private var authInProgress = false

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
            val authUrl = AppConfig.getAuthUrl(redirectUri)
            Napier.d("═══════════════════════════════════════", tag = TAG)
            Napier.d("OAuth URL: $authUrl", tag = TAG)
            Napier.d("Redirect URI: $redirectUri", tag = TAG)
            Napier.d("Client ID: ${AppConfig.TWITCH_CLIENT_ID}", tag = TAG)
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
                    update { it.copy(isLoading = false) }
                    sendEffect(AuthEffect.NavigateToHome(result.data))
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

    private fun handleGuestLogin() {
        viewModelScope.launch {
            sendEffect(AuthEffect.NavigateToHome(null))
        }
    }
}
