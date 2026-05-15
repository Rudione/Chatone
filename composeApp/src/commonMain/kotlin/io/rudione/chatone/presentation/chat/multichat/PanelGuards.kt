package io.rudione.chatone.presentation.chat.multichat

import io.rudione.chatone.domain.model.ChatPanel


object PanelGuards {
    fun canOpen(panelManager: ChatPanelManager, channelLogin: String): Boolean {
        if (channelLogin.isBlank()) return false
        if (panelManager.count() >= ChatPanel.MAX_PANELS) return false
        if (panelManager.isOpen(channelLogin)) return false
        return true
    }

    fun sanitizeChannel(raw: String): String? {
        val s = raw.trim().lowercase().removePrefix("#").removePrefix("@")
        return s.takeIf { it.matches(Regex("^[a-z0-9_]{3,32}$")) }
    }
}
