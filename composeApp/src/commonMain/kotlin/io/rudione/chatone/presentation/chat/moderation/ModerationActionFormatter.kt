package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.domain.model.IrcEvent

object ModerationActionFormatter {

    fun shortLabel(event: IrcEvent.ModeratorAction): String = when (event.action) {
        IrcEvent.ModeratorAction.ACTION_BAN -> "banned"
        IrcEvent.ModeratorAction.ACTION_TIMEOUT -> "timed out"
        IrcEvent.ModeratorAction.ACTION_UNBAN -> "unbanned"
        IrcEvent.ModeratorAction.ACTION_UNTIMEOUT -> "untimed"
        IrcEvent.ModeratorAction.ACTION_DELETE -> "msg deleted"
        IrcEvent.ModeratorAction.ACTION_CLEAR -> "chat cleared"
        IrcEvent.ModeratorAction.ACTION_MOD -> "modded"
        IrcEvent.ModeratorAction.ACTION_UNMOD -> "unmodded"
        IrcEvent.ModeratorAction.ACTION_VIP -> "vipped"
        IrcEvent.ModeratorAction.ACTION_UNVIP -> "unvipped"
        else -> "moderation"
    }

    fun severity(action: String): Int = when (action) {
        IrcEvent.ModeratorAction.ACTION_BAN -> 3
        IrcEvent.ModeratorAction.ACTION_TIMEOUT -> 2
        IrcEvent.ModeratorAction.ACTION_DELETE,
        IrcEvent.ModeratorAction.ACTION_CLEAR -> 1
        else -> 0
    }
}
