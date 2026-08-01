package io.rudione.chatone.util.system

import io.github.aakira.napier.Napier

private val TWITCH_LOGIN = Regex("^[A-Za-z0-9_]{1,25}$")
private val STREAM_QUALITY = Regex("^[A-Za-z0-9_,.\\-]{1,32}$")

actual fun openInStreamlink(channelLogin: String, quality: String) {
    if (!TWITCH_LOGIN.matches(channelLogin)) {
        Napier.w("Refusing to launch streamlink for invalid login: $channelLogin", tag = "Streamlink")
        return
    }
    val safeQuality = quality.takeIf { STREAM_QUALITY.matches(it) } ?: "best"

    val url = "https://twitch.tv/$channelLogin"
    try {
        ProcessBuilder("streamlink", url, safeQuality)
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
