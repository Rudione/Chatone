package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.GqlChannelPointRedeemResult
import io.rudione.chatone.data.remote.GqlChannelPointReward
import io.rudione.chatone.data.remote.GqlChannelPointRewardsInfo
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.util.Result

interface ChannelPointsGateway {
    val hasRights: Boolean
    suspend fun context(channelLogin: String, accountToken: String): GqlChannelPointRewardsInfo?
    suspend fun claim(channelId: String, claimId: String, accountToken: String): Result<Unit>
    suspend fun redeem(
        channelId: String,
        reward: GqlChannelPointReward,
        textInput: String,
        accountToken: String
    ): Result<GqlChannelPointRedeemResult>
    suspend fun unlockRandomSubEmote(channelId: String, cost: Long, accountToken: String): Result<Unit>
}

class ChannelPointsRepository(
    private val gql: TwitchGqlClient,
    private val moderationAuth: ModerationAuthStore
) : ChannelPointsGateway {

    override val hasRights: Boolean get() = moderationAuth.hasCustomToken()

    override suspend fun context(channelLogin: String, accountToken: String): GqlChannelPointRewardsInfo? {
        val token = moderationAuth.resolveToken(accountToken)
        if (token.isBlank()) return null
        return gql.getChannelPointRewardsGql(channelLogin, token)
    }

    override suspend fun claim(channelId: String, claimId: String, accountToken: String): Result<Unit> =
        gql.claimCommunityPoints(channelId, claimId, moderationAuth.resolveToken(accountToken))

    override suspend fun redeem(
        channelId: String,
        reward: GqlChannelPointReward,
        textInput: String,
        accountToken: String
    ): Result<GqlChannelPointRedeemResult> =
        gql.redeemCustomRewardGql(channelId, reward, textInput, moderationAuth.resolveToken(accountToken))

    override suspend fun unlockRandomSubEmote(channelId: String, cost: Long, accountToken: String): Result<Unit> =
        gql.unlockRandomSubEmoteGql(channelId, cost, moderationAuth.resolveToken(accountToken))
}
