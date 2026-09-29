package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import io.rudione.chatone.domain.model.MentionEntry

@Immutable
data class ChatSessionIdentity(
    val accessToken: String = "",
    val userId: String = "",
    val userLogin: String = "",
    val displayName: String = ""
)

@Immutable
data class ChatScreenOptions(
    val isWideScreen: Boolean = false,
    val renderBackground: Boolean = true,
    val isMultiChat: Boolean = false,
    val menuBadgeCount: Int = 0,
    val pendingScrollMessageId: String? = null
)

@Stable
class ChatScreenCallbacks(
    val onNavigateBack: () -> Unit = {},
    val onMentionDetected: (String) -> Unit = {},
    val onMentionReceived: (MentionEntry) -> Unit = {},
    val onOpenWhisper: OpenWhisperCallback = { _, _, _, _, _ -> },
    val onChannelIdResolved: (String) -> Unit = {},
    val onScrollToMessageHandled: () -> Unit = {}
)
