package io.rudione.chatone.util

import io.github.aakira.napier.Napier

actual fun openInStreamlink(channelLogin: String, quality: String) {
    val url = "https://twitch.tv/$channelLogin"
    try {
        ProcessBuilder("streamlink", url, quality)
            .redirectErrorStream(true)
            .start()
        showSystemNotification("Chatone", "Открываю $channelLogin в плеере ($quality)…")
    } catch (e: Exception) {
        // streamlink not installed / not on PATH — at least open the stream in the browser
        Napier.w("streamlink launch failed: ${e.message}", tag = "Streamlink")
        showSystemNotification("Chatone", "streamlink не найден — открываю в браузере")
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        } catch (_: Exception) {
        }
    }
}
