package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.auth.LoginPayloadCodec
import io.rudione.chatone.data.auth.LoginPayloadResult
import io.rudione.chatone.data.auth.LoginSiteResolver
import io.rudione.chatone.data.auth.WebLoginSession
import io.rudione.chatone.data.remote.TwitchFirstPartyClient
import io.rudione.chatone.domain.model.LoginCredentials
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.link.isSafeHttpUrl
import io.rudione.chatone.util.network.isRetryableFailure
import io.rudione.chatone.util.settings.AppConfig
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoginFailure {
    UnsafeLoginUrl,
    ClipboardEmpty,
    Malformed,
    DecryptionFailed,
    Incomplete,
    ForeignClientId,
    IdentityMismatch,
    TokenRejected,
    EncryptionUnsupported,
    RightsNotGranted,
    Network
}

sealed class WebLoginStage {
    data object Idle : WebLoginStage()
    data class Preparing(val attempt: Int = 1) : WebLoginStage()
    data object AwaitingPaste : WebLoginStage()
    data class Verifying(val attempt: Int = 1) : WebLoginStage()
    data class AwaitingRights(val account: TwitchAccount, val activationUrl: String? = null) : WebLoginStage()
    data class Success(val account: TwitchAccount) : WebLoginStage()
    data class Failure(val reason: LoginFailure, val detail: String? = null) : WebLoginStage()
}

class WebLoginController(
    private val authRepository: AuthRepository,
    private val deviceAuthController: FirstPartyDeviceAuthController,
    private val moderationAuthStore: ModerationAuthStore,
    private val siteResolver: LoginSiteResolver,
    private val scope: CoroutineScope
) {
    private val session = WebLoginSession()

    private val _stage = MutableStateFlow<WebLoginStage>(WebLoginStage.Idle)
    val stage: StateFlow<WebLoginStage> = _stage.asStateFlow()

    private val _loginUrl = MutableStateFlow<String?>(null)
    val loginUrl: StateFlow<String?> = _loginUrl.asStateFlow()

    fun mirrorLoginUrl(): String? = _loginUrl.value?.let(siteResolver::alternateOf)

    val deviceAuthState = deviceAuthController.state

    private var prepareJob: Job? = null
    private var submitJob: Job? = null
    private var rightsJob: Job? = null

    private var acceptedPayload: String? = null
    private var pendingPayload: String? = null

    private val pendingAccount: TwitchAccount?
        get() = when (val current = _stage.value) {
            is WebLoginStage.AwaitingRights -> current.account
            is WebLoginStage.Failure -> lastAccount
            else -> null
        }

    private var lastAccount: TwitchAccount? = null

    private var rightsPendingInBackground = false

    fun begin(onUrlReady: (String) -> Unit) {
        val previous = prepareJob
        prepareJob = scope.launch {
            previous?.cancel()
            acceptedPayload = null
            pendingPayload = null
            _stage.value = WebLoginStage.Preparing()

            val url = session.begin(loginUrl = siteResolver.resolve())
            if (!isSafeHttpUrl(url)) {
                Napier.e("Refusing to open unsafe login URL", tag = TAG)
                _stage.value = WebLoginStage.Failure(LoginFailure.UnsafeLoginUrl)
                return@launch
            }

            _loginUrl.value = url
            _stage.value = WebLoginStage.AwaitingPaste
            onUrlReady(url)
        }
    }

    private suspend fun requestDeviceCode(): DeviceAuthState.WaitingForApproval? {
        var attempt = 1
        while (true) {
            when (val start = deviceAuthController.prepare()) {
                is DeviceAuthStart.Started -> return start.waiting
                is DeviceAuthStart.Unavailable -> if (!start.retryable || attempt >= DEVICE_ATTEMPTS) break
            }
            delay(DEVICE_RETRY_DELAY_MS * attempt)
            attempt++
        }
        Napier.w("Continuing login without extended rights: device code unavailable", tag = TAG)
        return null
    }

    fun submit(payload: String, auto: Boolean = false) {
        val trimmed = payload.trim()
        if (auto) {
            if (!LoginPayloadCodec.canAutoSubmit(trimmed)) return
            if (trimmed == acceptedPayload) return
            if (_stage.value is WebLoginStage.Verifying) return
            acceptedPayload = trimmed
        }

        val previous = submitJob
        submitJob = scope.launch {
            previous?.cancel()
            pendingPayload = trimmed
            _stage.value = WebLoginStage.Verifying()

            val credentials = when (val decoded = session.decode(trimmed)) {
                is LoginPayloadResult.Success -> decoded.credentials
                LoginPayloadResult.Empty -> return@launch fail(LoginFailure.ClipboardEmpty)
                LoginPayloadResult.Malformed -> return@launch fail(LoginFailure.Malformed)
                LoginPayloadResult.DecryptionFailed -> return@launch fail(LoginFailure.DecryptionFailed)
                LoginPayloadResult.Incomplete -> return@launch fail(LoginFailure.Incomplete)
                LoginPayloadResult.EncryptionUnsupported ->
                    return@launch fail(LoginFailure.EncryptionUnsupported)
            }

            if (credentials.clientId.isNotEmpty() && credentials.clientId != AppConfig.TWITCH_CLIENT_ID) {
                Napier.w("Pasted credentials belong to another application", tag = TAG)
                return@launch fail(LoginFailure.ForeignClientId)
            }

            when (val result = authenticate(credentials.oauthToken)) {
                is Result.Success -> {
                    if (credentials.userId.isNotEmpty() && result.data.userId != credentials.userId) {
                        Napier.e("Token identity does not match the pasted user id", tag = TAG)
                        authRepository.deleteAccount(result.data.userId, revoke = false)
                        return@launch fail(LoginFailure.IdentityMismatch)
                    }
                    pendingPayload = null
                    acceptedPayload = null
                    session.end()
                    _loginUrl.value = null
                    finishOrWaitForRights(result.data, credentials)
                }

                is Result.Error -> {
                    if (isRetryableFailure(result.exception)) {
                        Napier.w("Login blocked by connectivity: ${result.exception.message}", tag = TAG)
                        fail(LoginFailure.Network, result.exception.message)
                    } else {
                        fail(LoginFailure.TokenRejected, result.exception.message)
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    fun retryPendingPayload() {
        val payload = pendingPayload ?: return
        submit(payload)
    }

    private suspend fun authenticate(token: String): Result<TwitchAccount> {
        var attempt = 1
        while (true) {
            val result = authRepository.authenticateWithToken(token)
            if (result !is Result.Error) return result
            if (attempt >= VERIFY_ATTEMPTS || !isRetryableFailure(result.exception)) return result
            Napier.d("Retrying token verification after ${result.exception.message}", tag = TAG)
            attempt++
            _stage.value = WebLoginStage.Verifying(attempt)
            delay(VERIFY_RETRY_DELAY_MS * (1L shl (attempt - 2)))
        }
    }

    private suspend fun finishOrWaitForRights(account: TwitchAccount, credentials: LoginCredentials) {
        lastAccount = account
        val siteDevice = siteDeviceClient(credentials)
        if (moderationAuthStore.hasTokenFor(account.userId)) {
            if (siteDevice != null) {
                deviceAuthController.adopt(credentials.deviceCode, siteDevice, account.userId, quiet = true)
                rightsPendingInBackground = true
            }
            _stage.value = WebLoginStage.Success(account)
            return
        }
        if (!isDesktopPlatform) {
            awaitOwnDeviceApproval(account)
            return
        }
        if (siteDevice != null) {
            deviceAuthController.adopt(credentials.deviceCode, siteDevice, account.userId)
            _stage.value = WebLoginStage.AwaitingRights(account)
            watchRights(account)
            return
        }
        awaitOwnDeviceApproval(account)
    }

    private fun siteDeviceClient(credentials: LoginCredentials): TwitchFirstPartyClient? {
        if (credentials.deviceCode.isEmpty()) return null
        return TwitchFirstPartyClient.fromClientId(credentials.deviceClientId)
    }

    private suspend fun awaitOwnDeviceApproval(account: TwitchAccount) {
        _stage.value = WebLoginStage.AwaitingRights(account)
        val waiting = requestDeviceCode()
        val activation = waiting?.verificationUri?.takeIf(::isSafeHttpUrl)
        if (activation == null) {
            val detail = (deviceAuthController.state.value as? DeviceAuthState.Error)?.message
            _stage.value = WebLoginStage.Failure(LoginFailure.RightsNotGranted, detail)
            return
        }
        _stage.value = WebLoginStage.AwaitingRights(account, activation)
        watchRights(account)
    }

    fun activationUrl(): String? = (_stage.value as? WebLoginStage.AwaitingRights)?.activationUrl

    fun retryRights() {
        val account = pendingAccount ?: return
        val previous = prepareJob
        prepareJob = scope.launch {
            previous?.cancel()
            rightsJob?.cancel()
            awaitOwnDeviceApproval(account)
        }
    }

    private fun watchRights(account: TwitchAccount) {
        rightsJob?.cancel()
        rightsJob = scope.launch {
            deviceAuthController.state.collect { device ->
                when (device) {
                    is DeviceAuthState.Success -> {
                        _stage.value = WebLoginStage.Success(account)
                        return@collect
                    }

                    is DeviceAuthState.Error -> {
                        _stage.value = WebLoginStage.Failure(LoginFailure.RightsNotGranted, device.message)
                        return@collect
                    }

                    DeviceAuthState.Idle -> {
                        _stage.value = WebLoginStage.Failure(LoginFailure.RightsNotGranted)
                        return@collect
                    }

                    else -> Unit
                }
            }
        }
    }

    fun continueWithoutRights() {
        val pending = pendingAccount ?: return
        rightsJob?.cancel()
        rightsPendingInBackground = deviceAuthController.state.value.let {
            it is DeviceAuthState.WaitingForApproval || it == DeviceAuthState.ConfirmingOnLoginPage
        }
        _stage.value = WebLoginStage.Success(pending)
    }

    fun reset() {
        prepareJob?.cancel()
        submitJob?.cancel()
        rightsJob?.cancel()
        lastAccount = null
        acceptedPayload = null
        pendingPayload = null
        session.end()
        if (!rightsPendingInBackground) deviceAuthController.cancel()
        rightsPendingInBackground = false
        _loginUrl.value = null
        _stage.value = WebLoginStage.Idle
    }

    fun dismissFailure() {
        if (_stage.value is WebLoginStage.Failure) {
            _stage.value = if (_loginUrl.value != null) WebLoginStage.AwaitingPaste else WebLoginStage.Idle
        }
    }

    private fun fail(reason: LoginFailure, detail: String? = null) {
        if (reason != LoginFailure.Network) acceptedPayload = null
        _stage.value = WebLoginStage.Failure(reason, detail)
    }

    companion object {
        private const val TAG = "WebLogin"
        const val DEVICE_ATTEMPTS = 3
        private const val DEVICE_RETRY_DELAY_MS = 1200L
        const val VERIFY_ATTEMPTS = 5
        private const val VERIFY_RETRY_DELAY_MS = 1000L
    }
}
