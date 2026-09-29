package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object ChannelPanelRequestBus {
    private val _openPointsBitsPanel = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openPointsBitsPanel: SharedFlow<String> = _openPointsBitsPanel

    fun requestOpenPointsBitsPanel(channelLogin: String) {
        _openPointsBitsPanel.tryEmit(channelLogin)
    }

    private val _toggleHiddenEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toggleHiddenEvents: SharedFlow<String> = _toggleHiddenEvents

    fun requestToggleHiddenEvents(channelLogin: String) {
        _toggleHiddenEvents.tryEmit(channelLogin)
    }
}
