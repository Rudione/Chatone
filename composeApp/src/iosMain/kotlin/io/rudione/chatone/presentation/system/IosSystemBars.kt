package io.rudione.chatone.presentation.system

import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

object IosSystemBars {
    private var immersiveHolders = 0

    var onImmersiveChanged: ((Boolean) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(immersiveHolders > 0)
        }

    internal fun applyStatusBar(light: Boolean) {
        val style = if (light) UIUserInterfaceStyle.UIUserInterfaceStyleLight else UIUserInterfaceStyle.UIUserInterfaceStyleDark
        appWindows().forEach { it.overrideUserInterfaceStyle = style }
    }

    internal fun acquireImmersive() {
        if (immersiveHolders++ == 0) onImmersiveChanged?.invoke(true)
    }

    internal fun releaseImmersive() {
        if (immersiveHolders == 0) return
        if (--immersiveHolders == 0) onImmersiveChanged?.invoke(false)
    }

    private fun appWindows(): List<UIWindow> =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
}
