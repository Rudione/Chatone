package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.repository.ChannelRoleGateway
import io.rudione.chatone.data.repository.RoleActor
import io.rudione.chatone.domain.moderation.ChannelRole
import io.rudione.chatone.domain.moderation.ChannelRolePlanner
import io.rudione.chatone.domain.moderation.RoleTarget
import io.rudione.chatone.domain.moderation.UserRoles
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal class ChannelRoleController(
    private val scope: CoroutineScope,
    private val gateway: ChannelRoleGateway,
    private val getState: () -> ChatState,
    private val update: ((ChatState) -> ChatState) -> Unit,
    private val onError: (String) -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val overriddenAt = mutableMapOf<String, Long>()

    fun set(target: RoleTarget, current: UserRoles?, role: ChannelRole, granted: Boolean) {
        val s = getState()
        if (!s.canManageRoles || s.channelId.isEmpty() || s.currentAccessToken.isEmpty()) return
        if (target.userId.isEmpty() || target.userId == s.currentUserId || target.userId == s.channelId) return
        val steps = ChannelRolePlanner.plan(current, role, granted)
        if (steps.isEmpty()) return
        val channelId = s.channelId
        val actor = RoleActor(channelId, s.currentAccessToken, s.isBroadcaster)
        val previous = s.roleOverrides[target.userId]
        val base = current ?: previous ?: UserRoles()
        remember(channelId, target.userId, base.after(steps))
        scope.launch {
            var applied = 0
            for (step in steps) {
                val result = gateway.apply(step, target, actor)
                if (result is Result.Error) {
                    when {
                        applied > 0 -> remember(channelId, target.userId, base.after(steps.take(applied)))
                        previous != null -> remember(channelId, target.userId, previous)
                        else -> forget(channelId, target.userId)
                    }
                    onError(failureText(role, granted, result.exception.message))
                    return@launch
                }
                applied++
            }
        }
    }

    fun onMessage(userId: String, roles: UserRoles) {
        val override = getState().roleOverrides[userId] ?: return
        val stale = now() - (overriddenAt[userId] ?: 0L) > OVERRIDE_TTL_MS
        if (override == roles || stale) {
            overriddenAt.remove(userId)
            update { it.copy(roleOverrides = it.roleOverrides - userId) }
        }
    }

    fun reset() {
        overriddenAt.clear()
    }

    private fun remember(channelId: String, userId: String, roles: UserRoles) {
        overriddenAt[userId] = now()
        update { st ->
            if (st.channelId != channelId) st else st.copy(roleOverrides = st.roleOverrides + (userId to roles))
        }
    }

    private fun forget(channelId: String, userId: String) {
        overriddenAt.remove(userId)
        update { st ->
            if (st.channelId != channelId) st else st.copy(roleOverrides = st.roleOverrides - userId)
        }
    }

    private fun failureText(role: ChannelRole, granted: Boolean, reason: String?): String {
        val action = when (role) {
            ChannelRole.MODERATOR -> if (granted) "add moderator" else "remove moderator"
            ChannelRole.VIP -> if (granted) "add VIP" else "remove VIP"
        }
        return if (reason.isNullOrBlank()) "Failed to $action" else "Failed to $action: $reason"
    }

    private companion object {
        const val OVERRIDE_TTL_MS = 120_000L
    }
}
