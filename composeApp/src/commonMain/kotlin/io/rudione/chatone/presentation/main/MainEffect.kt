package io.rudione.chatone.presentation.main

import io.rudione.chatone.presentation.text.UiText
import io.rudione.chatone.base.UIEffect

sealed class MainEffect : UIEffect {
    object NavigateToAuth : MainEffect()
    data class ShowError(val message: UiText) : MainEffect()
    data class IncomingWhisper(val fromDisplayName: String, val text: String) : MainEffect()
    data class MentionToast(
        val channelLogin: String,
        val fromDisplayName: String,
        val text: String,
        val messageId: String
    ) : MainEffect()
}
