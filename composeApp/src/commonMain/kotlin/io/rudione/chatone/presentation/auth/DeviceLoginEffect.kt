package io.rudione.chatone.presentation.auth

import io.rudione.chatone.base.UIEffect

sealed interface DeviceLoginEffect : UIEffect {
    data object SignedIn : DeviceLoginEffect
}
