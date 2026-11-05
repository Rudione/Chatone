package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.DeviceCodeInfo
import io.rudione.chatone.data.remote.DeviceCodeResult
import io.rudione.chatone.data.remote.DevicePollResult
import io.rudione.chatone.data.remote.TwitchDeviceAuthClient
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import io.rudione.chatone.util.link.isSafeHttpUrl
import io.rudione.chatone.util.network.isRetryableMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class DeviceActivation(val userCode: String, val activationUrl: String, val expiresAtMs: Long)

enum class DeviceLoginFailure { Unavailable, Expired, Rejected, Network }

sealed interface DeviceLoginStage {
    data object Idle : DeviceLoginStage
    data object Requesting : DeviceLoginStage
    data class AwaitingAccount(val activation: DeviceActivation) : DeviceLoginStage
    data object Verifying : DeviceLoginStage
    data class AwaitingRights(val account: TwitchAccount, val activation: DeviceActivation?) : DeviceLoginStage
    data class Done(val account: TwitchAccount) : DeviceLoginStage
    data class Failed(val reason: DeviceLoginFailure) : DeviceLoginStage
}

class DeviceLoginController(
    private val deviceAuthClient: TwitchDeviceAuthClient,
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val rights: FirstPartyDeviceAuthController,
    private val scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val _stage = MutableStateFlow<DeviceLoginStage>(DeviceLoginStage.Idle)
    val stage: StateFlow<DeviceLoginStage> = _stage.asStateFlow()

    private var job: Job? = null

    fun start() {
        job?.cancel()
        rights.cancel()
        job = scope.launch { runAccountStep() }
    }

    fun cancel() {
        job?.cancel()
        rights.cancel()
        _stage.value = DeviceLoginStage.Idle
    }

    fun startRights() {
        job?.cancel()
        job = scope.launch {
            val account = activeAccount() ?: return@launch fail(DeviceLoginFailure.Unavailable)
            runRightsStep(account)
        }
    }

    private suspend fun activeAccount(): TwitchAccount? {
        val activeId = accountManager.activeAccountId.value
        return activeId.takeIf { it.isNotBlank() }?.let { authRepository.getAccountById(it) }
            ?: authRepository.getFirstValidAccount()
    }

    fun skipRights() {
        val pending = _stage.value as? DeviceLoginStage.AwaitingRights ?: return
        job?.cancel()
        rights.cancel()
        _stage.value = DeviceLoginStage.Done(pending.account)
    }

    fun retryRights() {
        val pending = _stage.value as? DeviceLoginStage.AwaitingRights ?: return
        job?.cancel()
        job = scope.launch { runRightsStep(pending.account) }
    }

    private suspend fun runAccountStep() {
        while (true) {
            _stage.value = DeviceLoginStage.Requesting
            val info = requestCode() ?: return fail(DeviceLoginFailure.Unavailable)
            val activationUrl = info.activationUri.takeIf(::isSafeHttpUrl)
                ?: return fail(DeviceLoginFailure.Unavailable)
            _stage.value = DeviceLoginStage.AwaitingAccount(
                DeviceActivation(info.userCode, activationUrl, now() + info.expiresInSeconds * MS_PER_SECOND)
            )
            when (val result = awaitApproval(info)) {
                is DevicePollResult.Success -> return finishAccountStep(result)
                DevicePollResult.ExpiredOrDenied -> continue
                is DevicePollResult.Error -> return fail(DeviceLoginFailure.Network)
                else -> return fail(DeviceLoginFailure.Rejected)
            }
        }
    }

    private suspend fun requestCode(): DeviceCodeInfo? {
        repeat(REQUEST_ATTEMPTS) { attempt ->
            when (val result = deviceAuthClient.requestAccountCode()) {
                is DeviceCodeResult.Issued -> return result.info
                is DeviceCodeResult.Failed -> if (!result.retryable) return null
            }
            delay(REQUEST_RETRY_MS * (attempt + 1))
        }
        return null
    }

    private suspend fun awaitApproval(info: DeviceCodeInfo): DevicePollResult {
        val deadline = now() + info.expiresInSeconds * MS_PER_SECOND
        val baseInterval = info.intervalSeconds.coerceAtLeast(1) * MS_PER_SECOND
        var interval = baseInterval
        var offlineStreak = 0
        while (now() < deadline) {
            delay(interval)
            when (val result = deviceAuthClient.pollToken(info)) {
                is DevicePollResult.Success, DevicePollResult.ExpiredOrDenied -> return result
                DevicePollResult.Pending -> {
                    offlineStreak = 0
                    interval = baseInterval
                }

                DevicePollResult.SlowDown -> interval += SLOW_DOWN_STEP_MS
                is DevicePollResult.Error -> {
                    offlineStreak++
                    if (!isRetryableMessage(result.message) || offlineStreak >= MAX_OFFLINE_POLLS) return result
                    Napier.d("Device login poll retry $offlineStreak", tag = TAG)
                    interval = (interval * 2).coerceAtMost(MAX_POLL_INTERVAL_MS)
                }
            }
        }
        return DevicePollResult.ExpiredOrDenied
    }

    private suspend fun finishAccountStep(result: DevicePollResult.Success) {
        _stage.value = DeviceLoginStage.Verifying
        when (val auth = authRepository.authenticateWithToken(result.token, result.refreshToken)) {
            is Result.Success -> {
                accountManager.setActiveAccount(auth.data.userId)
                runRightsStep(auth.data)
            }

            else -> fail(DeviceLoginFailure.Rejected)
        }
    }

    private suspend fun runRightsStep(account: TwitchAccount) {
        _stage.value = DeviceLoginStage.AwaitingRights(account, activation = null)
        val started = rights.prepare(account.userId) as? DeviceAuthStart.Started
        val waiting = started?.waiting
        val url = waiting?.verificationUri?.takeIf(::isSafeHttpUrl)
        if (waiting == null || url == null) {
            _stage.value = DeviceLoginStage.Done(account)
            return
        }
        _stage.value = DeviceLoginStage.AwaitingRights(
            account,
            DeviceActivation(waiting.userCode, url, now() + RIGHTS_WINDOW_MS)
        )
        val outcome = rights.state.first { it !is DeviceAuthState.WaitingForApproval && it !is DeviceAuthState.Validating }
        if (outcome is DeviceAuthState.Error) Napier.w("Extended rights not confirmed: ${outcome.message}", tag = TAG)
        _stage.value = DeviceLoginStage.Done(account)
    }

    private fun fail(reason: DeviceLoginFailure) {
        _stage.value = DeviceLoginStage.Failed(reason)
    }

    private companion object {
        const val TAG = "DeviceLogin"
        const val MS_PER_SECOND = 1000L
        const val REQUEST_ATTEMPTS = 3
        const val REQUEST_RETRY_MS = 1_500L
        const val SLOW_DOWN_STEP_MS = 2_000L
        const val MAX_OFFLINE_POLLS = 6
        const val MAX_POLL_INTERVAL_MS = 20_000L
        const val RIGHTS_WINDOW_MS = 30 * 60 * 1000L
    }
}
