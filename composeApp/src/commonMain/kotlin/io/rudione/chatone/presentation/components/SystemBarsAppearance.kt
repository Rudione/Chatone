package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
expect fun SystemBarsAppearance(lightBackground: Boolean, lightStatusBar: Boolean = lightBackground)

@Stable
class StatusBarOverrides {
    private var darkContentHolders by mutableIntStateOf(0)

    val darkContentOnTop: Boolean get() = darkContentHolders > 0

    internal fun acquireDarkContent() {
        darkContentHolders++
    }

    internal fun releaseDarkContent() {
        darkContentHolders = (darkContentHolders - 1).coerceAtLeast(0)
    }
}

val LocalStatusBarOverrides = staticCompositionLocalOf { StatusBarOverrides() }

@Composable
fun AppSystemBars(lightBackground: Boolean) {
    val overrides = LocalStatusBarOverrides.current
    SystemBarsAppearance(
        lightBackground = lightBackground,
        lightStatusBar = lightBackground && !overrides.darkContentOnTop
    )
}

@Composable
fun StatusBarOverDarkContent() {
    val overrides = LocalStatusBarOverrides.current
    DisposableEffect(overrides) {
        overrides.acquireDarkContent()
        onDispose { overrides.releaseDarkContent() }
    }
}
