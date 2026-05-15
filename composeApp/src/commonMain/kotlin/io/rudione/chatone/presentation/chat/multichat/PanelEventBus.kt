package io.rudione.chatone.presentation.chat.multichat

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow


class PanelEventBus {
    sealed class Event {
        data class FocusPanel(val panelId: String) : Event()
        data class ScrollToBottom(val panelId: String) : Event()
        data class MentionAcrossPanels(val fromChannel: String, val text: String) : Event()
        data class PanelOpenedByDrag(val channelLogin: String) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun emit(event: Event) {
        _events.emit(event)
    }

    fun tryEmit(event: Event): Boolean = _events.tryEmit(event)
}
