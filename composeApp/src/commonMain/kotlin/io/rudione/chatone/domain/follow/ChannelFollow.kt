package io.rudione.chatone.domain.follow

enum class FollowFailure { SIGNED_OUT, BLOCKED, FAILED }

sealed interface FollowResult<out T> {
    data class Done<T>(val value: T) : FollowResult<T>
    data class Failed(val reason: FollowFailure) : FollowResult<Nothing>
}

interface ChannelFollowGateway {
    suspend fun isFollowing(channelId: String): FollowResult<Boolean>
    suspend fun setFollowing(channelId: String, follow: Boolean): FollowResult<Unit>
}
