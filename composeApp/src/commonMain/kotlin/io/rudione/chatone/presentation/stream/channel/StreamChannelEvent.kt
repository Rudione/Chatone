package io.rudione.chatone.presentation.stream.channel

import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.domain.channel.ChannelSnapshot

sealed interface StreamChannelEvent : UiEvent {
    data class Bind(val login: String, val hint: ChannelSnapshot? = null) : StreamChannelEvent
    data object ToggleFollow : StreamChannelEvent
    data object DismissFailure : StreamChannelEvent
}
