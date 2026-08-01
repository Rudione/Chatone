package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object ChannelPanelRequestBus {
    private val _openPointsBitsPanel = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openPointsBitsPanel: SharedFlow<String> = _openPointsBitsPanel

    fun requestOpenPointsBitsPanel(channelLogin: String) {
        _openPointsBitsPanel.tryEmit(channelLogin)
    }

    private val _toggleHidePin = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toggleHidePin: SharedFlow<String> = _toggleHidePin

    fun requestToggleHidePin(channelLogin: String) {
        _toggleHidePin.tryEmit(channelLogin)
    }
}
