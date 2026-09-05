package io.rudione.chatone.presentation.auth

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.LoginFailure
import io.rudione.chatone.data.repository.WebLoginController
import io.rudione.chatone.data.repository.WebLoginStage
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.usecase.GetFirstValidAccountUseCase
import kotlinx.coroutines.launch

data class AuthState(
    val isCheckingToken: Boolean = true,
    val isPreparing: Boolean = false,
    val isVerifying: Boolean = false,
    val prepareAttempt: Int = 1,
    val verifyAttempt: Int = 1,
    val awaitingPaste: Boolean = false,
    val awaitingRights: Boolean = false,
    val loginUrl: String? = null,
    val deviceState: DeviceAuthState = DeviceAuthState.Idle,
    val failure: LoginFailure? = null,
    val failureDetail: String? = null
) : UiState

sealed class AuthEvent : UiEvent {
    data object OnStartLogin : AuthEvent()
    data object OnReopenBrowser : AuthEvent()
    data class OnPastePayload(val payload: String, val auto: Boolean = false) : AuthEvent()
    data object OnRetryPayload : AuthEvent()
    data object OnGuestClicked : AuthEvent()
    data object OnCancel : AuthEvent()
    data object OnDismissFailure : AuthEvent()
    data object OnContinueWithoutRights : AuthEvent()
}

sealed class AuthEffect : UIEffect {
    data class NavigateToHome(val account: TwitchAccount?) : AuthEffect()
    data class OpenAuthUrl(val url: String) : AuthEffect()
}

class AuthViewModel(
    private val getFirstValidAccountUseCase: GetFirstValidAccountUseCase,
    private val webLoginController: WebLoginController
) : BaseViewModel<AuthState, AuthEvent, AuthEffect>(AuthState()) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        subscribeToEvents()
        observeLogin()
        checkExistingToken()
    }

    override suspend fun onEvent(event: AuthEvent) {
        when (event) {
            AuthEvent.OnStartLogin -> startLogin()
            AuthEvent.OnReopenBrowser -> state.value.loginUrl?.let {
                sendEffect(AuthEffect.OpenAuthUrl(it))
            }
            is AuthEvent.OnPastePayload -> webLoginController.submit(event.payload, event.auto)
            AuthEvent.OnRetryPayload -> webLoginController.retryPendingPayload()
            AuthEvent.OnGuestClicked -> sendEffect(AuthEffect.NavigateToHome(null))
            AuthEvent.OnCancel -> webLoginController.reset()
            AuthEvent.OnDismissFailure -> webLoginController.dismissFailure()
            AuthEvent.OnContinueWithoutRights -> webLoginController.continueWithoutRights()
        }
    }

    private fun startLogin() {
        webLoginController.begin { url -> sendEffect(AuthEffect.OpenAuthUrl(url)) }
    }

    private fun observeLogin() {
        viewModelScope.launch {
            webLoginController.stage.collect { stage ->
                update {
                    it.copy(
                        isPreparing = stage is WebLoginStage.Preparing,
                        isVerifying = stage is WebLoginStage.Verifying,
                        prepareAttempt = (stage as? WebLoginStage.Preparing)?.attempt ?: 1,
                        verifyAttempt = (stage as? WebLoginStage.Verifying)?.attempt ?: 1,
                        awaitingPaste = stage is WebLoginStage.AwaitingPaste ||
                                stage is WebLoginStage.Verifying ||
                                stage is WebLoginStage.Failure,
                        awaitingRights = stage is WebLoginStage.AwaitingRights,
                        failure = (stage as? WebLoginStage.Failure)?.reason,
                        failureDetail = (stage as? WebLoginStage.Failure)?.detail
                    )
                }
                if (stage is WebLoginStage.Success) {
                    Napier.d("Login complete for ${stage.account.login}", tag = TAG)
                    sendEffect(AuthEffect.NavigateToHome(stage.account))
                }
            }
        }
        viewModelScope.launch {
            webLoginController.loginUrl.collect { url -> update { it.copy(loginUrl = url) } }
        }
        viewModelScope.launch {
            webLoginController.deviceAuthState.collect { device ->
                update { it.copy(deviceState = device) }
            }
        }
    }

    private fun checkExistingToken() {
        viewModelScope.launch {
            val account = try {
                getFirstValidAccountUseCase()
            } catch (e: Exception) {
                Napier.e("Error checking token: ${e.message}", e, tag = TAG)
                null
            }
            update { it.copy(isCheckingToken = false) }
            if (account != null) {
                Napier.d("Found valid existing account: ${account.login}", tag = TAG)
                sendEffectWaitSubscriber(AuthEffect.NavigateToHome(account))
            }
        }
    }
}
