package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.DevicePollResult
import io.rudione.chatone.data.remote.TwitchDeviceAuthClient
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DeviceAuthState {
    object Idle : DeviceAuthState()
    data class WaitingForApproval(val userCode: String, val verificationUri: String) : DeviceAuthState()
    object Validating : DeviceAuthState()
    data class Success(val displayName: String) : DeviceAuthState()
    data class Error(val message: String) : DeviceAuthState()
}

class FirstPartyDeviceAuthController(
    private val deviceAuthClient: TwitchDeviceAuthClient,
    private val moderationAuthStore: ModerationAuthStore,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<DeviceAuthState>(DeviceAuthState.Idle)
    val state: StateFlow<DeviceAuthState> = _state

    private var pollJob: Job? = null

    fun start() {
        pollJob?.cancel()
        pollJob = scope.launch {
            val info = deviceAuthClient.requestDeviceCode()
            if (info == null) {
                _state.value = DeviceAuthState.Error("Failed to start Twitch authorization")
                return@launch
            }
            _state.value = DeviceAuthState.WaitingForApproval(info.userCode, info.verificationUri)

            val deadline = Clock.System.now().toEpochMilliseconds() + info.expiresInSeconds * 1000L
            var intervalMs = info.intervalSeconds.coerceAtLeast(1) * 1000L

            while (Clock.System.now().toEpochMilliseconds() < deadline) {
                delay(intervalMs)
                when (val result = deviceAuthClient.pollToken(info.deviceCode)) {
                    is DevicePollResult.Success -> {
                        _state.value = DeviceAuthState.Validating
                        val identity = moderationAuthStore.setAndValidate(result.token)
                        _state.value = if (identity != null) {
                            DeviceAuthState.Success(identity.displayName.ifBlank { identity.login })
                        } else {
                            DeviceAuthState.Error("Twitch rejected the token")
                        }
                        return@launch
                    }

                    DevicePollResult.Pending -> {}
                    DevicePollResult.SlowDown -> intervalMs += 2000L
                    DevicePollResult.ExpiredOrDenied -> {
                        _state.value = DeviceAuthState.Error("Authorization expired or was denied")
                        return@launch
                    }

                    is DevicePollResult.Error -> {
                        _state.value = DeviceAuthState.Error(result.message)
                        return@launch
                    }
                }
            }
            _state.value = DeviceAuthState.Error("Authorization timed out")
        }
    }

    fun cancel() {
        pollJob?.cancel()
        pollJob = null
        _state.value = DeviceAuthState.Idle
    }
}
