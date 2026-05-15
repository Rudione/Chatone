package io.rudione.chatone.presentation.chat.multichat

import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.model.ChatPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatPanelManager {
    companion object {
        private const val TAG = "ChatPanelManager"
    }

    private val _panels = MutableStateFlow<List<ChatPanel>>(emptyList())
    val panels: StateFlow<List<ChatPanel>> = _panels.asStateFlow()

    private val _activePanelId = MutableStateFlow<String?>(null)
    val activePanelId: StateFlow<String?> = _activePanelId.asStateFlow()

    fun openPanel(channelLogin: String): String? {
        val current = _panels.value
        val normalized = channelLogin.lowercase().removePrefix("#")
        if (normalized.isBlank()) return null
        if (current.size >= ChatPanel.MAX_PANELS) {
            Napier.w("Max panels (${ChatPanel.MAX_PANELS}) reached", tag = TAG)
            return null
        }
        if (current.any { it.channelLogin.equals(normalized, ignoreCase = true) }) {
            val existing = current.first { it.channelLogin.equals(normalized, ignoreCase = true) }
            _activePanelId.value = existing.panelId
            return existing.panelId
        }
        val panel = ChatPanel(
            panelId = ChatPanel.newId(normalized),
            channelLogin = normalized
        )
        _panels.value = current + panel
        if (_activePanelId.value == null) _activePanelId.value = panel.panelId
        Napier.d("Opened panel ${panel.panelId} for $normalized", tag = TAG)
        return panel.panelId
    }

    fun closePanel(panelId: String) {
        val current = _panels.value
        val remaining = current.filterNot { it.panelId == panelId }
        if (remaining.size == current.size) return
        _panels.value = remaining
        if (_activePanelId.value == panelId) {
            _activePanelId.value = remaining.firstOrNull()?.panelId
        }
        Napier.d("Closed panel $panelId", tag = TAG)
    }

    fun closeAll() {
        _panels.value = emptyList()
        _activePanelId.value = null
    }

    fun setActive(panelId: String) {
        if (_panels.value.any { it.panelId == panelId }) {
            _activePanelId.value = panelId
        }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        val current = _panels.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _panels.value = current.toList()
    }

    fun canOpenMore(): Boolean = _panels.value.size < ChatPanel.MAX_PANELS

    fun isOpen(channelLogin: String): Boolean {
        val n = channelLogin.lowercase()
        return _panels.value.any { it.channelLogin.equals(n, ignoreCase = true) }
    }

    fun count(): Int = _panels.value.size
}
