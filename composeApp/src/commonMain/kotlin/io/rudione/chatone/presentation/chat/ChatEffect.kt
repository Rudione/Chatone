package io.rudione.chatone.presentation.chat

import io.rudione.chatone.presentation.text.UiText
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.domain.model.DisplayMessage

sealed class ChatEffect : UIEffect {
    data class ShowError(val message: UiText) : ChatEffect()
    object ScrollToBottom : ChatEffect()
    object HistoryMerged : ChatEffect()
    data class MentionDetected(
        val channelLogin: String,
        val message: DisplayMessage.PrivMsg? = null,
        val playSound: Boolean = false
    ) : ChatEffect()

    data class OpenUserProfile(
        val userId: String,
        val username: String,
        val displayName: String,
        val color: String?
    ) : ChatEffect()

    object FocusChatInput : ChatEffect()
}
