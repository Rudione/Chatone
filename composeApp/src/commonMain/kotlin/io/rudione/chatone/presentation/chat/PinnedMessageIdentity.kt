package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.DisplayMessage

internal data class PinnedMessageIdentity(val name: String?, val badges: List<Badge>, val user: ChatNoticeUser?)

internal fun pinnedMessageIdentity(
    name: String,
    login: String,
    userId: String,
    reportedBadges: List<Badge>,
    channelLogin: String,
    messages: List<DisplayMessage>,
    color: String? = null
): PinnedMessageIdentity {
    val known = messages.filterIsInstance<DisplayMessage.PrivMsg>().lastOrNull {
        (userId.isNotBlank() && it.userId == userId) ||
            (login.isNotBlank() && it.username.equals(login, ignoreCase = true))
    }
    val badges = reportedBadges.ifEmpty { known?.badges.orEmpty() }.toMutableList()
    if (login.isNotBlank() && login.equals(channelLogin, ignoreCase = true) && badges.none { it.id == "broadcaster" }) {
        badges.add(0, Badge("broadcaster", "1", ""))
    }
    val displayName = name.ifBlank { known?.displayName.orEmpty() }.ifBlank { login }.takeIf { it.isNotBlank() }
    val user = displayName?.let { ChatNoticeUser(
        userId.ifBlank { known?.userId.orEmpty() },
        login.ifBlank { known?.username.orEmpty() },
        it,
        color ?: known?.color
    ) }
    return PinnedMessageIdentity(displayName, badges, user)
}
