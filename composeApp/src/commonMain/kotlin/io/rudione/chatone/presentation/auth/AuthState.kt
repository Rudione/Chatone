package io.rudione.chatone.presentation.auth

import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.LoginFailure
import io.rudione.chatone.data.repository.RightsHandoff

data class AuthState(
    val isCheckingToken: Boolean = true,
    val isPreparing: Boolean = false,
    val isVerifying: Boolean = false,
    val prepareAttempt: Int = 1,
    val verifyAttempt: Int = 1,
    val awaitingPaste: Boolean = false,
    val awaitingRights: Boolean = false,
    val activationUrl: String? = null,
    val rightsHandoff: RightsHandoff = RightsHandoff.Manual,
    val automaticReturn: Boolean = false,
    val loginUrl: String? = null,
    val deviceState: DeviceAuthState = DeviceAuthState.Idle,
    val failure: LoginFailure? = null,
    val failureDetail: String? = null
) : UiState
