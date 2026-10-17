package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.moderation.ChannelRole
import io.rudione.chatone.domain.moderation.RoleTarget
import io.rudione.chatone.domain.moderation.UserRoles

internal fun List<Badge>.matching(roles: UserRoles): List<Badge> = filterNot { badge ->
    (badge.id == VIP_BADGE && !roles.vip) || (badge.id == MODERATOR_BADGE && !roles.moderator)
}

private const val VIP_BADGE = "vip"
private const val MODERATOR_BADGE = "moderator"

@Immutable
internal class ProfileRoleActions(
    val onMod: () -> Unit,
    val onUnmod: () -> Unit,
    val onVip: () -> Unit,
    val onUnvip: () -> Unit
)

@Composable
internal fun rememberRoleActions(
    viewModel: ChatViewModel,
    target: DisplayMessage.PrivMsg,
    roles: UserRoles
): ProfileRoleActions {
    val currentRoles by rememberUpdatedState(roles)
    return remember(viewModel, target.userId, target.username) {
        val roleTarget = RoleTarget(target.userId, target.username)
        val set: (ChannelRole, Boolean) -> Unit = { role, granted ->
            viewModel.sendEvent(ChatEvent.OnSetUserRole(roleTarget, currentRoles, role, granted))
        }
        ProfileRoleActions(
            onMod = { set(ChannelRole.MODERATOR, true) },
            onUnmod = { set(ChannelRole.MODERATOR, false) },
            onVip = { set(ChannelRole.VIP, true) },
            onUnvip = { set(ChannelRole.VIP, false) }
        )
    }
}
