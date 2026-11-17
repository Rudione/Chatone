package io.rudione.chatone.presentation.auth

import androidx.compose.runtime.Immutable
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.repository.DeviceActivation
import io.rudione.chatone.data.repository.DeviceLoginFailure

enum class DeviceLoginStep { ACCOUNT, RIGHTS }

@Immutable
data class DeviceLoginState(
    val step: DeviceLoginStep = DeviceLoginStep.ACCOUNT,
    val activation: DeviceActivation? = null,
    val loading: Boolean = true,
    val failure: DeviceLoginFailure? = null
) : UiState
