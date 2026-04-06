package io.rudione.chatone.domain.model

import kotlinx.datetime.Clock

/** Одно ЛС-сообщение */
data class WhisperMessage(
    val id: String = "whisper_${Clock.System.now().toEpochMilliseconds()}",
    val fromUserId: String,
    val fromUsername: String,
    val fromDisplayName: String,
    val fromColor: String? = null,
    val fromAvatarUrl: String = "",
    val text: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isOwn: Boolean = false          // true = мы отправили
)

/** Диалог с конкретным юзером */
data class WhisperConversation(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val color: String? = null,
    val messages: List<WhisperMessage> = emptyList(),
    val unreadCount: Int = 0
) {
    val lastMessage: WhisperMessage? get() = messages.lastOrNull()
}

/** Упоминание: сообщение в котором нас тегнули */
data class MentionEntry(
    val messageId: String,
    val channelLogin: String,
    val fromUsername: String,
    val fromDisplayName: String,
    val fromColor: String? = null,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean = false
)