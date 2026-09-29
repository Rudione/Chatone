package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.model.ChatMessageScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatMessageScaleRepository(private val settings: Settings) {

    private val _scale = MutableStateFlow(
        ChatMessageScale.of(settings.getInt(KEY_PERCENT, ChatMessageScale.DEFAULT_PERCENT))
    )
    val scale: StateFlow<ChatMessageScale> = _scale.asStateFlow()

    fun set(scale: ChatMessageScale) {
        if (_scale.value == scale) return
        _scale.value = scale
        settings.putInt(KEY_PERCENT, scale.percent)
    }

    fun enlarge() = set(_scale.value.larger())

    fun shrink() = set(_scale.value.smaller())

    fun reset() = set(ChatMessageScale.Default)

    private companion object {
        const val KEY_PERCENT = "chat_message_scale_percent"
    }
}
