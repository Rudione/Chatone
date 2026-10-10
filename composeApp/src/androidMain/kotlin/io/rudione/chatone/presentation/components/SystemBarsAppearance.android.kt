package io.rudione.chatone.presentation.components

import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import io.rudione.chatone.presentation.stream.findActivity

@Composable
actual fun SystemBarsAppearance(lightBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.parent as? DialogWindowProvider)?.window ?: view.context.findActivity()?.window ?: return
    SideEffect { window.applyTransparentBars(lightBackground) }
}

@Suppress("DEPRECATION")
private fun Window.applyTransparentBars(lightBackground: Boolean) {
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isStatusBarContrastEnforced = false
        isNavigationBarContrastEnforced = false
    }
    WindowCompat.getInsetsController(this, decorView).apply {
        isAppearanceLightStatusBars = lightBackground
        isAppearanceLightNavigationBars = lightBackground
    }
}
