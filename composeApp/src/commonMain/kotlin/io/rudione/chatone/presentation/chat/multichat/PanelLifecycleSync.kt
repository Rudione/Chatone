package io.rudione.chatone.presentation.chat.multichat

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.domain.model.ChatPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PanelLifecycleSync(
    private val ircClient: TwitchIrcClient,
    private val scope: CoroutineScope
) {
    companion object { private const val TAG = "PanelLifecycleSync" }

    private var observerJob: Job? = null
    private val joinedByPanels = mutableSetOf<String>()

    fun attach(panelManager: ChatPanelManager) {
        observerJob?.cancel()
        observerJob = scope.launch {
            panelManager.panels.collect { panels ->
                syncJoins(panels)
            }
        }
    }

    fun detach() {
        observerJob?.cancel()
        observerJob = null
        scope.launch {
            joinedByPanels.toList().forEach { ch ->
                try { ircClient.partChannel(ch) } catch (e: Exception) {
                    Napier.w("part failed: ${e.message}", tag = TAG)
                }
            }
            joinedByPanels.clear()
        }
    }

    private suspend fun syncJoins(panels: List<ChatPanel>) {
        val desired = panels.map { it.channelLogin.lowercase() }.toSet()
        val toJoin = desired - joinedByPanels
        val toPart = joinedByPanels - desired
        toJoin.forEach {
            try { ircClient.joinChannel(it); joinedByPanels.add(it) }
            catch (e: Exception) { Napier.w("join $it failed: ${e.message}", tag = TAG) }
        }
        toPart.forEach {
            try { ircClient.partChannel(it); joinedByPanels.remove(it) }
            catch (e: Exception) { Napier.w("part $it failed: ${e.message}", tag = TAG) }
        }
    }
}
