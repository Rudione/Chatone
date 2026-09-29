package io.rudione.chatone.presentation.chat.multichat

object ChatDragData {
    const val MIME_TYPE = "application/x-chatone-channel"
    const val PREFIX = "chatone-channel:"

    fun encode(channelLogin: String): String = "$PREFIX${channelLogin.lowercase()}"

    fun decode(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith(PREFIX) -> trimmed.removePrefix(PREFIX).takeIf { it.isNotBlank() }
            else -> trimmed.takeIf { it.matches(Regex("^[A-Za-z0-9_]{3,32}$")) }
        }
    }
}
