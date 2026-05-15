package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import io.rudione.chatone.domain.model.DisplayMessage


@Composable
fun rememberDedupedMessages(
    messages: List<DisplayMessage>,
    blockedUserIds: Set<String>,
    showBlockedMode: Int
): List<DisplayMessage> {
    return remember(messages, blockedUserIds, showBlockedMode) {
        val seen = HashSet<String>(messages.size)
        messages.filter { msg ->
            if (!seen.add(msg.id)) return@filter false
            if (msg is DisplayMessage.PrivMsg && msg.userId in blockedUserIds) {
                when (showBlockedMode) {
                    0 -> false
                    1, 2 -> true
                    else -> false
                }
            } else true
        }
    }
}


@Composable
fun rememberLastMessageId(messages: List<DisplayMessage>): String? {
    return remember(messages.lastOrNull()?.id) { messages.lastOrNull()?.id }
}
