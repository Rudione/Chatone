package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.DevicePollResult
import io.rudione.chatone.data.remote.TwitchDeviceAuthClient
import io.rudione.chatone.util.network.isRetryableMessage
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DeviceAuthState {
    data object Idle : DeviceAuthState()
    data class WaitingForApproval(val userCode: String, val verificationUri: String) : DeviceAuthState()
    data object Validating : DeviceAuthState()
    data class Success(val displayName: String, val userId: String) : DeviceAuthState()
    data class Error(val message: String) : DeviceAuthState()
}

class FirstPartyDeviceAuthController(
    private val deviceAuthClient: TwitchDeviceAuthClient,
    private val moderationAuthStore: ModerationAuthStore,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<DeviceAuthState>(DeviceAuthState.Idle)
    val state: StateFlow<DeviceAuthState> = _state.asStateFlow()

    private var pollJob: Job? = null

    suspend fun prepare(): DeviceAuthState.WaitingForApproval? {
        pollJob?.cancel()
        val info = deviceAuthClient.requestDeviceCode()
        if (info == null) {
            _state.value = DeviceAuthState.Error("Could not reach Twitch to start authorization")
            return null
        }

        val waiting = DeviceAuthState.WaitingForApproval(info.userCode, info.verificationUri)
        _state.value = waiting

        pollJob = scope.launch {
            val deadline = Clock.System.now().toEpochMilliseconds() + info.expiresInSeconds * 1000L
            val baseIntervalMs = info.intervalSeconds.coerceAtLeast(1) * 1000L
            var intervalMs = baseIntervalMs
            var offlineStreak = 0

            while (Clock.System.now().toEpochMilliseconds() < deadline) {
                delay(intervalMs)
                when (val result = deviceAuthClient.pollToken(info.deviceCode)) {
                    is DevicePollResult.Success -> {
                        _state.value = DeviceAuthState.Validating
                        val identity = validateWithRetry(result.token)
                        _state.value = if (identity != null) {
                            notifyAuthorized()
                            DeviceAuthState.Success(
                                displayName = identity.displayName.ifBlank { identity.login },
                                userId = identity.userId
                            )
                        } else {
                            DeviceAuthState.Error("Twitch rejected the token")
                        }
                        return@launch
                    }

                    DevicePollResult.Pending -> {
                        offlineStreak = 0
                        intervalMs = baseIntervalMs
                    }

                    DevicePollResult.SlowDown -> intervalMs += 2000L
                    DevicePollResult.ExpiredOrDenied -> {
                        _state.value = DeviceAuthState.Error("Authorization expired or was denied")
                        return@launch
                    }

                    is DevicePollResult.Error -> {
                        offlineStreak++
                        if (!isRetryableMessage(result.message) || offlineStreak >= MAX_OFFLINE_POLLS) {
                            _state.value = DeviceAuthState.Error(result.message)
                            return@launch
                        }
                        Napier.d("Device poll retry $offlineStreak after ${result.message}", tag = TAG)
                        intervalMs = (intervalMs * 2).coerceAtMost(MAX_POLL_INTERVAL_MS)
                    }
                }
            }
            _state.value = DeviceAuthState.Error("Authorization timed out")
        }

        return waiting
    }

    fun start() {
        scope.launch { prepare() }
    }

    fun cancel() {
        pollJob?.cancel()
        pollJob = null
        _state.value = DeviceAuthState.Idle
    }

    private suspend fun validateWithRetry(token: String): ModerationAuthStore.Identity? {
        var attempt = 1
        while (true) {
            val identity = runCatching { moderationAuthStore.setAndValidate(token, userId = "") }
                .onFailure { Napier.w("Rights validation failed: ${it.message}", tag = TAG) }
                .getOrNull()
            if (identity != null || attempt >= VALIDATE_ATTEMPTS) return identity
            attempt++
            delay(VALIDATE_RETRY_DELAY_MS * (attempt - 1))
        }
    }

    private fun notifyAuthorized() {
        if (io.rudione.chatone.util.system.isAppInForeground()) return
        runCatching {
            io.rudione.chatone.util.system.notifySystem(
                "Chatone",
                "Twitch authorization confirmed — tap to return"
            )
        }
    }

    private companion object {
        const val TAG = "DeviceAuth"
        const val MAX_OFFLINE_POLLS = 6
        const val MAX_POLL_INTERVAL_MS = 20_000L
        const val VALIDATE_ATTEMPTS = 4
        const val VALIDATE_RETRY_DELAY_MS = 1500L
    }
}
