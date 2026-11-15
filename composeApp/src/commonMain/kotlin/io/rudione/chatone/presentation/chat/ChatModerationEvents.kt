package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.DisplayMessage.ModerationMsg
import io.rudione.chatone.domain.model.DisplayMessage.ModerationMsg.ModerationAction
import kotlin.math.abs

internal object ChatModerationEvents {

    private const val FALLBACK_WINDOW_MS = 1_000L

    fun isSameEvent(first: ModerationMsg, second: ModerationMsg): Boolean {
        if (!first.channel.removePrefix("#").equals(second.channel.removePrefix("#"), ignoreCase = true)) return false
        if (first.action != second.action) return false
        if (first.id == second.id) return true
        if (first.action == ModerationAction.DELETE) {
            return !first.targetMessageId.isNullOrBlank() && first.targetMessageId == second.targetMessageId
        }
        if (first.action != ModerationAction.BAN && first.action != ModerationAction.TIMEOUT &&
            first.action != ModerationAction.CLEAR && first.action != ModerationAction.UNBAN &&
            first.action != ModerationAction.UNTIMEOUT
        ) return false
        val sameTarget = if (!first.targetUserId.isNullOrBlank() && !second.targetUserId.isNullOrBlank()) {
            first.targetUserId == second.targetUserId
        } else first.targetUser.equals(second.targetUser, ignoreCase = true)
        if (!sameTarget || first.duration != second.duration) return false
        if (first.serverTimestamp != null && second.serverTimestamp != null) {
            return first.serverTimestamp == second.serverTimestamp
        }
        return abs(first.timestamp - second.timestamp) <= FALLBACK_WINDOW_MS
    }

    fun enrich(existing: ModerationMsg, incoming: ModerationMsg): ModerationMsg {
        val moderator = existing.moderatorLogin?.takeIf { it.isNotBlank() }
            ?: incoming.moderatorLogin?.takeIf { it.isNotBlank() }
        val targetId = existing.targetUserId ?: incoming.targetUserId
        val target = existing.targetUser?.takeIf { it.isNotBlank() && it != targetId }
            ?: incoming.targetUser?.takeIf { it.isNotBlank() }
            ?: existing.targetUser?.takeIf { it.isNotBlank() }
        return existing.copy(
            text = if (moderator != existing.moderatorLogin || target != existing.targetUser) incoming.text else existing.text,
            moderatorLogin = moderator,
            targetUser = target,
            targetUserId = existing.targetUserId ?: incoming.targetUserId,
            serverTimestamp = existing.serverTimestamp ?: incoming.serverTimestamp
        )
    }
}
