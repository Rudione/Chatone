package io.rudione.chatone.util

import io.rudione.chatone.presentation.settings.SettingsState
import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String, mode: SettingsState.LinkOpenMode) {
    if (mode == SettingsState.LinkOpenMode.INCOGNITO) {
        val os = System.getProperty("os.name", "").lowercase()
        val launched = when {
            os.contains("win") -> tryLaunch("cmd", "/c", "start", "chrome", "--incognito", url)
                    || tryLaunch("cmd", "/c", "start", "firefox", "--private-window", url)
                    || tryLaunch("cmd", "/c", "start", "msedge", "--inprivate", url)
            os.contains("mac") -> tryLaunch("open", "-a", "Google Chrome", "--args", "--incognito", url)
                    || tryLaunch("open", "-a", "Firefox", "--args", "--private-window", url)
                    || tryLaunch("open", "-a", "Microsoft Edge", "--args", "--inprivate", url)
            else -> tryLaunch("google-chrome", "--incognito", url)
                    || tryLaunch("google-chrome-stable", "--incognito", url)
                    || tryLaunch("chromium-browser", "--incognito", url)
                    || tryLaunch("firefox", "--private-window", url)
                    || tryLaunch("microsoft-edge", "--inprivate", url)
        }
        if (!launched) {
            openDefault(url)
        }
    } else {
        openDefault(url)
    }
}

private fun openDefault(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            val os = System.getProperty("os.name", "").lowercase()
            when {
                os.contains("win") -> Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
                os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", url))
                else -> Runtime.getRuntime().exec(arrayOf("xdg-open", url))
            }
        }
    } catch (_: Exception) {}
}

private fun tryLaunch(vararg cmd: String): Boolean {
    return try {
        Runtime.getRuntime().exec(cmd)
        true
    } catch (_: Exception) {
        false
    }
}
