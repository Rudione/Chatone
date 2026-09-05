package io.rudione.chatone.util.link

import io.rudione.chatone.presentation.settings.SettingsState

expect fun openUrl(url: String, mode: SettingsState.LinkOpenMode)

fun openChatUrl(url: String, mode: SettingsState.LinkOpenMode) {
    val host = httpUrlHost(url) ?: return
    if (!OutboundUrlPolicy.isPublicHost(host)) return
    openUrl(url, mode)
}
