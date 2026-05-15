package io.rudione.chatone.presentation.chat.multichat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow


class PanelMessageDispatcher {
    data class TargetedMessage(
        val panelId: String,
        val channelLogin: String,
        val text: String
    )

    private val _outgoing = MutableSharedFlow<TargetedMessage>(extraBufferCapacity = 64)
    val outgoing: SharedFlow<TargetedMessage> = _outgoing.asSharedFlow()

    suspend fun send(panelId: String, channelLogin: String, text: String) {
        _outgoing.emit(TargetedMessage(panelId, channelLogin, text))
    }

    fun trySend(panelId: String, channelLogin: String, text: String): Boolean =
        _outgoing.tryEmit(TargetedMessage(panelId, channelLogin, text))
}
