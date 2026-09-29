package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.rudione.chatone.domain.model.DisplayMessage

@Stable
internal class ProfilePopupStack {

    @Stable
    class Slot internal constructor(val key: Int, message: DisplayMessage.PrivMsg) {
        var message by mutableStateOf(message)
            internal set
        var pinned by mutableStateOf(false)
            internal set
    }

    val slots = mutableStateListOf<Slot>()
    private var nextKey = 0

    fun open(message: DisplayMessage.PrivMsg) {
        slots.firstOrNull { it.message.isSameUser(message) }?.let { existing ->
            if (message.userId.isNotEmpty() || existing.message.userId.isEmpty()) existing.message = message
            return
        }
        val transient = slots.lastOrNull { !it.pinned }
        if (transient != null) transient.message = message
        else slots += Slot(nextKey++, message)
    }

    fun openLogin(login: String, channel: String, nowMs: Long) {
        val clean = login.removePrefix("@").trim()
        if (clean.isEmpty()) return
        open(
            DisplayMessage.PrivMsg(
                id = "profile_${clean.lowercase()}",
                timestamp = nowMs,
                channel = channel,
                userId = "",
                username = clean.lowercase(),
                displayName = clean,
                tokens = emptyList(),
                color = null,
                badges = emptyList(),
                isModerator = false,
                isSubscriber = false,
                isVip = false,
                isBroadcaster = false,
                isMention = false,
                isAction = false
            )
        )
    }

    fun setPinned(slot: Slot, pinned: Boolean) {
        slot.pinned = pinned
        if (!pinned) slots.removeAll { it !== slot && !it.pinned }
    }

    fun close(slot: Slot) {
        slots.remove(slot)
    }
}

private fun DisplayMessage.PrivMsg.isSameUser(other: DisplayMessage.PrivMsg): Boolean = when {
    userId.isNotEmpty() && other.userId.isNotEmpty() -> userId == other.userId
    else -> username.isNotBlank() && username.equals(other.username, ignoreCase = true)
}

@Composable
internal fun rememberProfilePopupStack(): ProfilePopupStack = remember { ProfilePopupStack() }
