package io.rudione.chatone.domain.model

import kotlinx.datetime.Clock

data class ChatPanel(
    val panelId: String,
    val channelLogin: String,
    val openedAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        const val MAX_PANELS = 7
        const val COMPACT_THRESHOLD = 4

        fun newId(channelLogin: String): String =
            "panel_${channelLogin.lowercase()}_${Clock.System.now().toEpochMilliseconds()}"
    }
}

enum class PanelLayoutMode {
    SINGLE,
    SPLIT,
    COMPACT
}

fun List<ChatPanel>.layoutMode(): PanelLayoutMode = when {
    size <= 1 -> PanelLayoutMode.SINGLE
    size < ChatPanel.COMPACT_THRESHOLD -> PanelLayoutMode.SPLIT
    else -> PanelLayoutMode.COMPACT
}
