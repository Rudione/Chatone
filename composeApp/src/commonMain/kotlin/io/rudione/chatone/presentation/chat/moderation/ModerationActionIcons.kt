package io.rudione.chatone.presentation.chat.moderation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import io.rudione.chatone.domain.model.IrcEvent


object ModerationActionIcons {
    fun iconFor(action: String): ImageVector = when (action) {
        IrcEvent.ModeratorAction.ACTION_BAN -> Icons.Outlined.Block
        IrcEvent.ModeratorAction.ACTION_TIMEOUT -> Icons.Outlined.Timer
        IrcEvent.ModeratorAction.ACTION_UNBAN -> Icons.Outlined.CheckCircle
        IrcEvent.ModeratorAction.ACTION_UNTIMEOUT -> Icons.Outlined.PlayCircle
        IrcEvent.ModeratorAction.ACTION_DELETE -> Icons.Outlined.Delete
        IrcEvent.ModeratorAction.ACTION_MOD -> Icons.Outlined.PersonAddAlt
        IrcEvent.ModeratorAction.ACTION_UNMOD -> Icons.Outlined.PersonRemove
        IrcEvent.ModeratorAction.ACTION_VIP -> Icons.Outlined.Star
        IrcEvent.ModeratorAction.ACTION_UNVIP -> Icons.Outlined.StarBorder
        else -> Icons.Outlined.CheckCircle
    }
}
