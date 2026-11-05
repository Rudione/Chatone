package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.util.Result

data class ModerationActor(val accessToken: String, val channelId: String, val moderatorId: String)

interface ChannelBanGateway {
    suspend fun ban(actor: ModerationActor, userId: String, durationSeconds: Int?, reason: String?): Result<Unit>
    suspend fun unban(actor: ModerationActor, userId: String): Result<Unit>
}

class ChannelBanRepository(private val helix: TwitchApiClient) : ChannelBanGateway {

    override suspend fun ban(
        actor: ModerationActor,
        userId: String,
        durationSeconds: Int?,
        reason: String?
    ): Result<Unit> = helix.banUser(actor.accessToken, actor.channelId, actor.moderatorId, userId, durationSeconds, reason)

    override suspend fun unban(actor: ModerationActor, userId: String): Result<Unit> =
        helix.unbanUser(actor.accessToken, actor.channelId, actor.moderatorId, userId)
}
