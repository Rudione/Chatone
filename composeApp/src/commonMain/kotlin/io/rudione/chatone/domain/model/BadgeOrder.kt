package io.rudione.chatone.domain.model

internal fun Badge.displayPriority(): Int = when (setId.ifBlank { id }.lowercase()) {
    "broadcaster" -> 0
    "lead_moderator", "grand_moderator", "chat_manager", "broadcaster_mode", "super_moderator" -> 1
    "moderator" -> if ((version.toIntOrNull() ?: 1) >= 2) 1 else 2
    "vip" -> 3
    "artist" -> 4
    "staff" -> 5
    "admin" -> 6
    "global_mod" -> 7
    "subscriber", "founder" -> 20
    "shared-chat", "shared_chat" -> 30
    "bot" -> 40
    else -> 10
}
