package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.domain.moderation.ChannelRole
import io.rudione.chatone.domain.moderation.RoleChange
import io.rudione.chatone.domain.moderation.RoleTarget
import io.rudione.chatone.util.Result

data class RoleActor(
    val channelId: String,
    val accessToken: String,
    val isBroadcaster: Boolean
)

interface ChannelRoleGateway {
    suspend fun apply(change: RoleChange, target: RoleTarget, actor: RoleActor): Result<Unit>
}

class ChannelRoleRepository(
    private val helix: TwitchApiClient,
    private val gql: TwitchGqlClient,
    private val moderationAuth: ModerationAuthStore
) : ChannelRoleGateway {

    override suspend fun apply(change: RoleChange, target: RoleTarget, actor: RoleActor): Result<Unit> =
        if (actor.isBroadcaster) viaHelix(change, target, actor) else viaGql(change, target, actor)

    private suspend fun viaHelix(change: RoleChange, target: RoleTarget, actor: RoleActor): Result<Unit> =
        when (change.role) {
            ChannelRole.MODERATOR ->
                if (change.granted) helix.addModerator(actor.accessToken, actor.channelId, target.userId)
                else helix.removeModerator(actor.accessToken, actor.channelId, target.userId)
            ChannelRole.VIP ->
                if (change.granted) helix.addVip(actor.accessToken, actor.channelId, target.userId)
                else helix.removeVip(actor.accessToken, actor.channelId, target.userId)
        }

    private suspend fun viaGql(change: RoleChange, target: RoleTarget, actor: RoleActor): Result<Unit> {
        if (target.login.isBlank()) return Result.Error(IllegalArgumentException("Unknown login"))
        return gql.setChannelRole(
            channelId = actor.channelId,
            targetLogin = target.login,
            role = change.role,
            granted = change.granted,
            token = moderationAuth.resolveToken(actor.accessToken)
        )
    }
}
