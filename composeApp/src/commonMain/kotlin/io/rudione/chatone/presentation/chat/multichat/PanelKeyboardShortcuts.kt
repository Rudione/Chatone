package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

object PanelKeyboardShortcuts {

    fun closeActive(panelManager: ChatPanelManager): Boolean {
        val activeId = panelManager.activePanelId.value ?: return false
        panelManager.closePanel(activeId)
        return true
    }

    fun cycleNext(panelManager: ChatPanelManager): Boolean {
        val panels = panelManager.panels.value
        if (panels.size < 2) return false
        val activeId = panelManager.activePanelId.value
        val idx = panels.indexOfFirst { it.panelId == activeId }
        val next = panels[(idx + 1) % panels.size]
        panelManager.setActive(next.panelId)
        return true
    }

    fun cyclePrev(panelManager: ChatPanelManager): Boolean {
        val panels = panelManager.panels.value
        if (panels.size < 2) return false
        val activeId = panelManager.activePanelId.value
        val idx = panels.indexOfFirst { it.panelId == activeId }
        val prev = panels[((idx - 1) + panels.size) % panels.size]
        panelManager.setActive(prev.panelId)
        return true
    }
}

@Composable
fun rememberActivePanelChannel(panelManager: ChatPanelManager): String? {
    val panels by panelManager.panels.collectAsState()
    val activeId by panelManager.activePanelId.collectAsState()
    return panels.firstOrNull { it.panelId == activeId }?.channelLogin
}
