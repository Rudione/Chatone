package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatCommandKind { TEXT, MACRO }

@Serializable
data class ChatCommand(
    val id: String,
    val trigger: String,
    val kind: ChatCommandKind = ChatCommandKind.TEXT,
    val replacement: String = "",
    val macroId: String? = null,
    val wholeMessageOnly: Boolean = false,
    val enabled: Boolean = true,
    val hotkey: String = "",
    val sendImmediately: Boolean = false,
)
