package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.DeviceCodeInfo
import io.rudione.chatone.data.remote.DeviceCodeResult
import io.rudione.chatone.data.remote.DevicePollResult
import io.rudione.chatone.data.remote.TwitchDeviceAuthClient
import io.rudione.chatone.data.remote.TwitchFirstPartyClient
import io.rudione.chatone.util.network.isRetryableMessage
import io.rudione.chatone.util.system.appForeground
import io.rudione.chatone.util.system.notifySystem
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

sealed class DeviceAuthState {
    data object Idle : DeviceAuthState()
    data class WaitingForApproval(val userCode: String, val verificationUri: String) :
        DeviceAuthState()

    data object ConfirmingOnLoginPage : DeviceAuthState()
    data object Validating : DeviceAuthState()
    data class Success(val displayName: String, val userId: String) : DeviceAuthState()
    data class Error(val message: String) : DeviceAuthState()
}

sealed interface DeviceAuthStart {
    data class Started(val waiting: DeviceAuthState.WaitingForApproval) : DeviceAuthStart
    data class Unavailable(val retryable: Boolean) : DeviceAuthStart
}

class FirstPartyDeviceAuthController(
    private val deviceAuthClient: TwitchDeviceAuthClient,
    private val moderationAuthStore: ModerationAuthStore,
    private val scope: CoroutineScope,
    private val foreground: StateFlow<Boolean> = appForeground
) {
    private val _state = MutableStateFlow<DeviceAuthState>(DeviceAuthState.Idle)
    val state: StateFlow<DeviceAuthState> = _state.asStateFlow()

    private var pollJob: Job? = null

    suspend fun prepare(): DeviceAuthStart {
        pollJob?.cancel()
        val info = when (val result = deviceAuthClient.requestDeviceCode()) {
            is DeviceCodeResult.Issued -> result.info
            is DeviceCodeResult.Failed -> {
                _state.value = DeviceAuthState.Error(
                    if (result.retryable) "Could not reach Twitch to start authorization"
                    else "Twitch declined the authorization request"
                )
                return DeviceAuthStart.Unavailable(result.retryable)
            }
        }

        val waiting = DeviceAuthState.WaitingForApproval(info.userCode, info.activationUri)
        _state.value = waiting
        startPolling(info, expectedUserId = "", pollImmediately = false)
        return DeviceAuthStart.Started(waiting)
    }

    fun adopt(
        deviceCode: String,
        client: TwitchFirstPartyClient,
        expectedUserId: String,
        quiet: Boolean = false
    ) {
        if (deviceCode.isBlank() || expectedUserId.isBlank()) return
        pollJob?.cancel()
        _state.value = if (quiet) DeviceAuthState.Idle else DeviceAuthState.ConfirmingOnLoginPage
        startPolling(
            info = DeviceCodeInfo(
                deviceCode = deviceCode,
                userCode = "",
                verificationUri = "",
                expiresInSeconds = ADOPTED_EXPIRES_SECONDS,
                intervalSeconds = ADOPTED_INTERVAL_SECONDS,
                client = client
            ),
            expectedUserId = expectedUserId,
            pollImmediately = true,
            quiet = quiet
        )
    }

    private fun startPolling(
        info: DeviceCodeInfo,
        expectedUserId: String,
        pollImmediately: Boolean,
        quiet: Boolean = false
    ) {
        pollJob = scope.launch {
            val deadline = Clock.System.now().toEpochMilliseconds() + info.expiresInSeconds * 1000L
            val baseIntervalMs = info.intervalSeconds.coerceAtLeast(1) * 1000L
            var intervalMs = baseIntervalMs
            var waitMs = if (pollImmediately) 0L else baseIntervalMs
            var offlineStreak = 0

            while (Clock.System.now().toEpochMilliseconds() < deadline) {
                waitBeforePoll(waitMs)
                val startedInBackground = !foreground.value
                when (val result = deviceAuthClient.pollToken(info)) {
                    is DevicePollResult.Success -> {
                        if (!quiet) _state.value = DeviceAuthState.Validating
                        val binding = bindWithRetry(result.token, expectedUserId)
                        _state.value = when {
                            binding is ModerationAuthStore.Binding.Bound -> {
                                if (!quiet) notifyAuthorized()
                                DeviceAuthState.Success(
                                    displayName = binding.identity.displayName.ifBlank { binding.identity.login },
                                    userId = binding.identity.userId
                                )
                            }

                            quiet -> DeviceAuthState.Idle
                            binding == ModerationAuthStore.Binding.WrongAccount ->
                                DeviceAuthState.Error("Twitch confirmed a different account than the one you logged in with")

                            else -> DeviceAuthState.Error("Twitch rejected the token")
                        }
                        return@launch
                    }

                    DevicePollResult.Pending -> {
                        offlineStreak = 0
                        intervalMs = baseIntervalMs
                    }

                    DevicePollResult.SlowDown -> intervalMs += 2000L
                    DevicePollResult.ExpiredOrDenied -> {
                        _state.value = if (quiet) DeviceAuthState.Idle
                        else DeviceAuthState.Error("Authorization expired or was denied")
                        return@launch
                    }

                    is DevicePollResult.Error -> {
                        val backgrounded = startedInBackground || !foreground.value
                        if (backgrounded && isRetryableMessage(result.message)) {
                            intervalMs = baseIntervalMs
                            waitMs = intervalMs
                            continue
                        }
                        offlineStreak++
                        if (!isRetryableMessage(result.message) || offlineStreak >= MAX_OFFLINE_POLLS) {
                            _state.value =
                                if (quiet) DeviceAuthState.Idle else DeviceAuthState.Error(result.message)
                            return@launch
                        }
                        Napier.d(
                            "Device poll retry $offlineStreak after ${result.message}",
                            tag = TAG
                        )
                        intervalMs = (intervalMs * 2).coerceAtMost(MAX_POLL_INTERVAL_MS)
                    }
                }
                waitMs = intervalMs
            }
            _state.value =
                if (quiet) DeviceAuthState.Idle else DeviceAuthState.Error("Authorization timed out")
        }
    }

    private suspend fun waitBeforePoll(waitMs: Long) {
        if (waitMs <= 0L) return
        if (foreground.value) {
            delay(waitMs.milliseconds)
        } else {
            withTimeoutOrNull(waitMs.milliseconds) { foreground.first { it } }
        }
    }

    fun start() {
        scope.launch { prepare() }
    }

    fun cancel() {
        pollJob?.cancel()
        pollJob = null
        _state.value = DeviceAuthState.Idle
    }

    private suspend fun bindWithRetry(
        token: String,
        expectedUserId: String
    ): ModerationAuthStore.Binding {
        var attempt = 1
        while (true) {
            val binding = runCatching { moderationAuthStore.bind(token, userId = expectedUserId) }
                .onFailure { Napier.w("Rights validation failed: ${it.message}", tag = TAG) }
                .getOrDefault(ModerationAuthStore.Binding.Unverified)
            if (binding != ModerationAuthStore.Binding.Unverified || attempt >= VALIDATE_ATTEMPTS) return binding
            attempt++
            delay(VALIDATE_RETRY_DELAY_MS * (attempt - 1))
        }
    }

    private fun notifyAuthorized() {
        if (foreground.value) return
        runCatching {
            notifySystem(
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
        const val ADOPTED_EXPIRES_SECONDS = 1800
        const val ADOPTED_INTERVAL_SECONDS = 5
    }
}
