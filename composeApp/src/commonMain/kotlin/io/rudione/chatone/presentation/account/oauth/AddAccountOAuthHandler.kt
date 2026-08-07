package io.rudione.chatone.presentation.account.oauth

import io.rudione.chatone.data.auth.PlatformAuthHandler
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.account.AccountManager
import io.rudione.chatone.util.settings.AppConfig
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AddAccountOAuthHandler(
    private val platformAuth: PlatformAuthHandler,
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {
    sealed class State {
        object Idle : State()
        object Launching : State()
        object AwaitingCallback : State()
        data class Success(val account: TwitchAccount) : State()
        data class Failure(val reason: String) : State()
    }

    fun buildAuthUrl(): String {
        val clientId = platformAuth.getClientId()
        val redirectUri = platformAuth.getRedirectUri()
        val state = platformAuth.newAuthSession()
        return AppConfig.getAuthUrl(clientId, redirectUri, state)
    }

    fun launchBrowserAuth(onResult: (State) -> Unit) {
        val url = buildAuthUrl()
        try {
            platformAuth.startAuth(url)
        } catch (e: Exception) {
            onResult(State.Failure(e.message ?: "Failed to launch browser"))
            return
        }
        onResult(State.AwaitingCallback)
        scope.launch {
            try {
                val token = platformAuth.awaitToken()
                if (token.isNullOrBlank()) {
                    onResult(State.Failure("No token received"))
                    return@launch
                }
                completeWithToken(token, onResult)
            } catch (e: Exception) {
                onResult(State.Failure(e.message ?: "Browser auth failed"))
            } finally {
                runCatching { platformAuth.cleanup() }
            }
        }
    }

    fun completeWithToken(token: String, onResult: (State) -> Unit) {
        if (token.isBlank()) {
            onResult(State.Failure("Empty token"))
            return
        }
        scope.launch {
            try {
                when (val res = authRepository.authenticateWithToken(token)) {
                    is Result.Success -> {

                        onResult(State.Success(res.data))
                    }

                    is Result.Error -> onResult(State.Failure(res.exception.message ?: "Auth failed"))
                    Result.Loading -> {}
                }
            } catch (e: Exception) {
                onResult(State.Failure(e.message ?: "Unknown error"))
            }
        }
    }
}
