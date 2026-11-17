package io.rudione.chatone.presentation.settings

import io.rudione.chatone.base.UIEffect

sealed class SettingsEffect : UIEffect {
    object NavigateToAuth : SettingsEffect()
}
