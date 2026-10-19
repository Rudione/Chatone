package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.GqlIntegrityRejectedException
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.domain.follow.ChannelFollowGateway
import io.rudione.chatone.domain.follow.FollowFailure
import io.rudione.chatone.domain.follow.FollowResult
import io.rudione.chatone.util.Result

class ChannelFollowRepository(
    private val gql: TwitchGqlClient,
    private val accountManager: AccountManager,
    private val authRepository: AuthRepository,
    private val moderationAuth: ModerationAuthStore
) : ChannelFollowGateway {

    override suspend fun isFollowing(channelId: String): FollowResult<Boolean> {
        val token = activeToken() ?: return FollowResult.Failed(FollowFailure.SIGNED_OUT)
        return when (val r = gql.isFollowing(channelId, token)) {
            is Result.Success -> FollowResult.Done(r.data)
            else -> FollowResult.Failed(FollowFailure.FAILED)
        }
    }

    override suspend fun setFollowing(channelId: String, follow: Boolean): FollowResult<Unit> {
        val token = activeToken() ?: return FollowResult.Failed(FollowFailure.SIGNED_OUT)
        return when (val r = gql.setFollowing(channelId, follow, token)) {
            is Result.Success -> FollowResult.Done(Unit)
            is Result.Error ->
                if (r.exception is GqlIntegrityRejectedException) FollowResult.Failed(FollowFailure.BLOCKED)
                else FollowResult.Failed(FollowFailure.FAILED)
            else -> FollowResult.Failed(FollowFailure.FAILED)
        }
    }

    private suspend fun activeToken(): String? {
        val activeId = accountManager.activeAccountId.value
        val account = activeId.takeIf { it.isNotBlank() }?.let { authRepository.getAccountById(it) }
            ?: authRepository.getFirstValidAccount()
            ?: return null
        return moderationAuth.resolveToken(account.accessToken).takeIf { it.isNotBlank() }
    }
}
