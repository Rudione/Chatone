package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.IrcEvent

fun IrcEvent.ModeratorAction.toDisplayActionFallback(): DisplayMessage.ModerationMsg.ModerationAction {
    return when (action) {
        IrcEvent.ModeratorAction.ACTION_BAN -> DisplayMessage.ModerationMsg.ModerationAction.BAN
        IrcEvent.ModeratorAction.ACTION_TIMEOUT -> DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT
        IrcEvent.ModeratorAction.ACTION_UNBAN -> DisplayMessage.ModerationMsg.ModerationAction.UNBAN
        IrcEvent.ModeratorAction.ACTION_UNTIMEOUT -> DisplayMessage.ModerationMsg.ModerationAction.UNTIMEOUT
        IrcEvent.ModeratorAction.ACTION_MOD -> DisplayMessage.ModerationMsg.ModerationAction.MOD
        IrcEvent.ModeratorAction.ACTION_UNMOD -> DisplayMessage.ModerationMsg.ModerationAction.UNMOD
        IrcEvent.ModeratorAction.ACTION_VIP -> DisplayMessage.ModerationMsg.ModerationAction.VIP
        IrcEvent.ModeratorAction.ACTION_UNVIP -> DisplayMessage.ModerationMsg.ModerationAction.UNVIP
        IrcEvent.ModeratorAction.ACTION_DELETE -> DisplayMessage.ModerationMsg.ModerationAction.DELETE
        IrcEvent.ModeratorAction.ACTION_CLEAR -> DisplayMessage.ModerationMsg.ModerationAction.CLEAR
        else -> DisplayMessage.ModerationMsg.ModerationAction.UNBAN
    }
}

fun IrcEvent.ModeratorAction.isVisibleNotice(): Boolean {
    return action !in setOf(
        IrcEvent.ModeratorAction.ACTION_BAN,
        IrcEvent.ModeratorAction.ACTION_TIMEOUT,
        IrcEvent.ModeratorAction.ACTION_DELETE
    )
}
