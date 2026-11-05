package io.rudione.chatone.presentation.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.repository.DeviceActivation
import io.rudione.chatone.data.repository.DeviceLoginController
import io.rudione.chatone.data.repository.DeviceLoginFailure
import io.rudione.chatone.data.repository.DeviceLoginStage
import kotlinx.coroutines.launch

enum class DeviceLoginStep { ACCOUNT, RIGHTS }

@Immutable
data class DeviceLoginState(
    val step: DeviceLoginStep = DeviceLoginStep.ACCOUNT,
    val activation: DeviceActivation? = null,
    val loading: Boolean = true,
    val failure: DeviceLoginFailure? = null
) : UiState

sealed interface DeviceLoginEvent : UiEvent {
    data object Start : DeviceLoginEvent
    data object StartRights : DeviceLoginEvent
    data object Retry : DeviceLoginEvent
    data object SkipRights : DeviceLoginEvent
    data object Cancel : DeviceLoginEvent
}

sealed interface DeviceLoginEffect : UIEffect {
    data object SignedIn : DeviceLoginEffect
}

class DeviceLoginViewModel(
    private val controller: DeviceLoginController
) : BaseViewModel<DeviceLoginState, DeviceLoginEvent, DeviceLoginEffect>(DeviceLoginState()) {

    init {
        subscribeToEvents()
        viewModelScope.launch { controller.stage.collect(::render) }
    }

    override suspend fun onEvent(event: DeviceLoginEvent) {
        when (event) {
            DeviceLoginEvent.Start -> controller.start()
            DeviceLoginEvent.StartRights -> controller.startRights()
            DeviceLoginEvent.Retry -> if (state.value.step == DeviceLoginStep.RIGHTS) controller.retryRights() else controller.start()
            DeviceLoginEvent.SkipRights -> controller.skipRights()
            DeviceLoginEvent.Cancel -> controller.cancel()
        }
    }

    private fun render(stage: DeviceLoginStage) {
        when (stage) {
            DeviceLoginStage.Idle, DeviceLoginStage.Requesting, DeviceLoginStage.Verifying ->
                update { it.copy(loading = true, activation = null, failure = null) }

            is DeviceLoginStage.AwaitingAccount ->
                update { DeviceLoginState(DeviceLoginStep.ACCOUNT, stage.activation, loading = false) }

            is DeviceLoginStage.AwaitingRights ->
                update { DeviceLoginState(DeviceLoginStep.RIGHTS, stage.activation, loading = stage.activation == null) }

            is DeviceLoginStage.Failed ->
                update { it.copy(loading = false, activation = null, failure = stage.reason) }

            is DeviceLoginStage.Done -> sendEffect(DeviceLoginEffect.SignedIn)
        }
    }
}
