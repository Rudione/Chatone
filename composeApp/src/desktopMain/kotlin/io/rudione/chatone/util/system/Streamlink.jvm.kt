package io.rudione.chatone.util.system

import io.github.aakira.napier.Napier
import java.io.File

private val TWITCH_LOGIN = Regex("^[A-Za-z0-9_]{1,25}$")
private val STREAM_QUALITY = Regex("^[A-Za-z0-9_,.\\-]{1,32}$")

private fun streamlinkCommand(): String? {
    val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
    if (!isWindows) return "streamlink"
    val pathDirs = System.getenv("PATH").orEmpty().split(';')
    for (dir in pathDirs) {
        val trimmed = dir.trim().removeSurrounding("\"")
        if (trimmed.isEmpty()) continue
        val candidate = File(trimmed, "streamlink.exe")
        if (candidate.isAbsolute && candidate.isFile) return candidate.absolutePath
    }
    return null
}

actual fun openInStreamlink(channelLogin: String, quality: String) {
    if (!TWITCH_LOGIN.matches(channelLogin)) {
        Napier.w("Refusing to launch streamlink for invalid login: $channelLogin", tag = "Streamlink")
        return
    }
    val safeQuality = quality.takeIf { STREAM_QUALITY.matches(it) } ?: "best"

    val url = "https://twitch.tv/$channelLogin"
    try {
        val command = streamlinkCommand()
            ?: throw IllegalStateException("streamlink not found on PATH")
        ProcessBuilder(command, url, safeQuality)
            .redirectErrorStream(true)
            .start()
        notifySystem("Chatone", "Открываю $channelLogin в плеере ($safeQuality)…")
    } catch (e: Exception) {

        Napier.w("streamlink launch failed: ${e.message}", tag = "Streamlink")
        notifySystem("Chatone", "streamlink не найден — открываю в браузере")
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        } catch (_: Exception) {
        }
    }
}
