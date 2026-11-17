package io.rudione.chatone.presentation.auth

import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.data.repository.DeviceLoginController
import io.rudione.chatone.data.repository.DeviceLoginStage
import kotlinx.coroutines.launch

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
