package io.rudione.chatone.presentation.chat.moderation

import androidx.compose.ui.graphics.vector.ImageVector
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.CircleCheck
import io.rudione.chatone.icons.lucide.Gem
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.ShieldMinus
import io.rudione.chatone.icons.lucide.ShieldPlus
import io.rudione.chatone.icons.lucide.StarOff
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.icons.lucide.TimerOff
import io.rudione.chatone.icons.lucide.Trash2

object ModerationActionIcons {
    fun iconFor(action: String): ImageVector = when (action) {
        IrcEvent.ModeratorAction.ACTION_BAN -> Lucide.Ban
        IrcEvent.ModeratorAction.ACTION_TIMEOUT -> Lucide.Timer
        IrcEvent.ModeratorAction.ACTION_UNBAN -> Lucide.CircleCheck
        IrcEvent.ModeratorAction.ACTION_UNTIMEOUT -> Lucide.TimerOff
        IrcEvent.ModeratorAction.ACTION_DELETE -> Lucide.Trash2
        IrcEvent.ModeratorAction.ACTION_MOD -> Lucide.ShieldPlus
        IrcEvent.ModeratorAction.ACTION_UNMOD -> Lucide.ShieldMinus
        IrcEvent.ModeratorAction.ACTION_VIP -> Lucide.Gem
        IrcEvent.ModeratorAction.ACTION_UNVIP -> Lucide.StarOff
        else -> Lucide.CircleCheck
    }
}
