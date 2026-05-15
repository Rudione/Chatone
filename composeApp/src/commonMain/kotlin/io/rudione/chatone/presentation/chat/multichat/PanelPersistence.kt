package io.rudione.chatone.presentation.chat.multichat

import com.russhwolf.settings.Settings


class PanelPersistence(private val settings: Settings) {
    companion object {
        private const val KEY_PANELS = "multichat_open_panels"
        private const val SEP = ","
    }

    fun save(panelManager: ChatPanelManager) {
        val list = panelManager.panels.value.joinToString(SEP) { it.channelLogin }
        if (list.isEmpty()) settings.remove(KEY_PANELS)
        else settings.putString(KEY_PANELS, list)
    }

    fun load(panelManager: ChatPanelManager) {
        val raw = settings.getStringOrNull(KEY_PANELS) ?: return
        raw.split(SEP).filter { it.isNotBlank() }.forEach { ch ->
            panelManager.openPanel(ch.trim())
        }
    }

    fun clear() {
        settings.remove(KEY_PANELS)
    }
}
