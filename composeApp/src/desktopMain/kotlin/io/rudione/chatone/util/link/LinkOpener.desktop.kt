package io.rudione.chatone.util.link

import io.rudione.chatone.presentation.settings.SettingsState
import java.awt.Desktop
import java.io.File
import java.net.URI

actual fun openUrl(url: String, mode: SettingsState.LinkOpenMode) {
    if (!isSafeHttpUrl(url)) return

    if (mode == SettingsState.LinkOpenMode.INCOGNITO) {
        val os = System.getProperty("os.name", "").lowercase()
        val launched = when {
            os.contains("win") -> windowsIncognitoCandidates(url).any { tryLaunch(*it) }
            os.contains("mac") -> tryLaunch("open", "-a", "Google Chrome", "--args", "--incognito", "--", url)
                    || tryLaunch("open", "-a", "Firefox", "--args", "--private-window", url)
                    || tryLaunch("open", "-a", "Microsoft Edge", "--args", "--inprivate", "--", url)
            else -> tryLaunch("google-chrome", "--incognito", "--", url)
                    || tryLaunch("google-chrome-stable", "--incognito", "--", url)
                    || tryLaunch("chromium-browser", "--incognito", "--", url)
                    || tryLaunch("firefox", "--private-window", url)
                    || tryLaunch("microsoft-edge", "--inprivate", "--", url)
        }
        if (!launched) {
            openDefault(url)
        }
    } else {
        openDefault(url)
    }
}

private fun windowsIncognitoCandidates(url: String): List<Array<String>> {
    val programFiles = System.getenv("ProgramFiles").orEmpty()
    val programFilesX86 = System.getenv("ProgramFiles(x86)").orEmpty()
    val localAppData = System.getenv("LocalAppData").orEmpty()
    fun paths(vararg p: String) = p.filter { it.isNotBlank() && File(it).canExecute() }

    val chrome = paths(
        "$programFiles\\Google\\Chrome\\Application\\chrome.exe",
        "$programFilesX86\\Google\\Chrome\\Application\\chrome.exe",
        "$localAppData\\Google\\Chrome\\Application\\chrome.exe"
    )
    val firefox = paths(
        "$programFiles\\Mozilla Firefox\\firefox.exe",
        "$programFilesX86\\Mozilla Firefox\\firefox.exe"
    )
    val edge = paths(
        "$programFiles\\Microsoft\\Edge\\Application\\msedge.exe",
        "$programFilesX86\\Microsoft\\Edge\\Application\\msedge.exe"
    )
    return buildList {
        chrome.forEach { add(arrayOf(it, "--incognito", "--", url)) }
        firefox.forEach { add(arrayOf(it, "--private-window", url)) }
        edge.forEach { add(arrayOf(it, "--inprivate", "--", url)) }
    }
}

private fun openDefault(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            val os = System.getProperty("os.name", "").lowercase()
            when {
                os.contains("win") -> Unit
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
