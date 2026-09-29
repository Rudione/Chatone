package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.rudione.chatone.domain.model.ChatPanel

class PanelMentionTracker {
    private val unread = mutableMapOf<String, Int>()

    fun increment(panelId: String) {
        unread[panelId] = (unread[panelId] ?: 0) + 1
    }

    fun clear(panelId: String) {
        unread.remove(panelId)
    }

    fun count(panelId: String): Int = unread[panelId] ?: 0

    fun total(): Int = unread.values.sum()
}

@Composable
fun rememberPanelMentionTracker(): PanelMentionTracker = remember { PanelMentionTracker() }

@Composable
fun PanelMentionEffect(
    bus: PanelEventBus,
    tracker: PanelMentionTracker,
    panelManager: ChatPanelManager
) {
    LaunchedEffect(bus) {
        bus.events.collect { event ->
            when (event) {
                is PanelEventBus.Event.MentionAcrossPanels -> {
                    val targetPanel = panelManager.panels.value.firstOrNull {
                        it.channelLogin.equals(event.fromChannel, ignoreCase = true)
                    }
                    val activeId = panelManager.activePanelId.value
                    if (targetPanel != null && targetPanel.panelId != activeId) {
                        tracker.increment(targetPanel.panelId)
                    }
                }
                else -> {}
            }
        }
    }
}
