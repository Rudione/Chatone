package io.rudione.chatone.presentation.auth

import io.rudione.chatone.base.UiEvent

sealed interface DeviceLoginEvent : UiEvent {
    data object Start : DeviceLoginEvent
    data object StartRights : DeviceLoginEvent
    data object Retry : DeviceLoginEvent
    data object SkipRights : DeviceLoginEvent
    data object Cancel : DeviceLoginEvent
}
