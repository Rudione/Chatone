package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import io.rudione.chatone.presentation.system.IosSystemBars

@Composable
actual fun SystemBarsAppearance(lightBackground: Boolean, lightStatusBar: Boolean) {
    SideEffect { IosSystemBars.applyStatusBar(lightStatusBar) }
}
