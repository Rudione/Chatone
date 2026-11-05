package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.data.remote.HelixModerationException
import io.rudione.chatone.data.repository.ChannelBanGateway
import io.rudione.chatone.data.repository.ModerationActor
import io.rudione.chatone.util.Result

enum class TimeoutOutcome { APPLIED, REPLACED_BAN }

class ChannelTimeouts(private val gateway: ChannelBanGateway) {

    suspend fun timeout(
        actor: ModerationActor,
        userId: String,
        seconds: Int,
        reason: String?,
        knownBanned: Boolean
    ): Result<TimeoutOutcome> {
        if (knownBanned) return replaceBan(actor, userId, seconds, reason)
        val failure = gateway.ban(actor, userId, seconds, reason).exceptionOrNull()
            ?: return Result.Success(TimeoutOutcome.APPLIED)
        return if ((failure as? HelixModerationException)?.isAlreadyBanned == true) {
            replaceBan(actor, userId, seconds, reason)
        } else {
            Result.Error(failure)
        }
    }

    private suspend fun replaceBan(actor: ModerationActor, userId: String, seconds: Int, reason: String?): Result<TimeoutOutcome> {
        gateway.unban(actor, userId).exceptionOrNull()?.let { return Result.Error(it) }
        gateway.ban(actor, userId, seconds, reason).exceptionOrNull()?.let { return Result.Error(it) }
        return Result.Success(TimeoutOutcome.REPLACED_BAN)
    }
}

fun Set<String>.withBanState(userId: String?, banned: Boolean): Set<String> = when {
    userId.isNullOrBlank() -> this
    banned -> if (userId in this) this else this + userId
    else -> if (userId in this) this - userId else this
}
