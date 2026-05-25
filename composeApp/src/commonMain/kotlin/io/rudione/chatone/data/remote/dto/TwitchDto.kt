package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("scope") val scope: List<String> = emptyList(),
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class ValidateTokenResponse(
    @SerialName("client_id") val clientId: String,
    @SerialName("login") val login: String,
    @SerialName("scopes") val scopes: List<String>,
    @SerialName("user_id") val userId: String,
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class UsersResponse(
    val data: List<UserData>
)

@Serializable
data class UserData(
    val id: String,
    val login: String,
    @SerialName("display_name") val displayName: String,
    val type: String,
    @SerialName("broadcaster_type") val broadcasterType: String,
    val description: String,
    @SerialName("profile_image_url") val profileImageUrl: String,
    @SerialName("offline_image_url") val offlineImageUrl: String,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ChannelsResponse(
    val data: List<ChannelData>
)

@Serializable
data class ChannelData(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_login") val userLogin: String,
    @SerialName("user_name") val userName: String,
    @SerialName("game_id") val gameId: String = "",
    @SerialName("game_name") val gameName: String = "",
    val type: String,
    val title: String,
    @SerialName("viewer_count") val viewerCount: Int = 0,
    @SerialName("started_at") val startedAt: String = "",
    val language: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    @SerialName("tag_ids") val tagIds: List<String> = emptyList(),
    @SerialName("is_mature") val isMature: Boolean = false
)

@Serializable
data class SearchChannelsResponse(
    val data: List<SearchChannelData>
)

@Serializable
data class SearchChannelData(
    @SerialName("broadcaster_language") val broadcasterLanguage: String,
    @SerialName("broadcaster_login") val broadcasterLogin: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("game_id") val gameId: String,
    @SerialName("game_name") val gameName: String,
    val id: String,
    @SerialName("is_live") val isLive: Boolean,
    @SerialName("tag_ids") val tagIds: List<String> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    val title: String,
    @SerialName("started_at") val startedAt: String = ""
)

@Serializable
data class ChatSettingsResponse(
    val data: List<ChatSettingsData>
)

@Serializable
data class ChatSettingsData(
    @SerialName("broadcaster_id") val broadcasterId: String,
    @SerialName("moderator_id") val moderatorId: String? = null,
    @SerialName("slow_mode") val slowMode: Boolean = false,
    @SerialName("slow_mode_wait_time") val slowModeWaitTime: Int? = null,
    @SerialName("follower_mode") val followerMode: Boolean = false,
    @SerialName("follower_mode_duration") val followerModeDuration: Int? = null,
    @SerialName("subscriber_mode") val subscriberMode: Boolean = false,
    @SerialName("emote_mode") val emoteMode: Boolean = false,
    @SerialName("unique_chat_mode") val uniqueChatMode: Boolean = false
)

@Serializable
data class BadgesResponse(
    val data: List<BadgeSetDto>
)

@Serializable
data class BadgeSetDto(
    @SerialName("set_id") val setId: String,
    val versions: List<BadgeVersionDto>
)

@Serializable
data class BadgeVersionDto(
    val id: String,
    @SerialName("image_url_1x") val imageUrl1x: String,
    @SerialName("image_url_2x") val imageUrl2x: String,
    @SerialName("image_url_4x") val imageUrl4x: String,
    val title: String = "",
    val description: String = ""
)

@Serializable
data class FollowersResponse(
    val data: List<FollowerData>,
    val total: Int = 0
)

@Serializable
data class FollowerData(
    @SerialName("user_id") val userId: String,
    @SerialName("user_login") val userLogin: String,
    @SerialName("user_name") val userName: String,
    @SerialName("followed_at") val followedAt: String
)

@Serializable
data class TwitchErrorResponse(
    val error: String,
    val status: Int,
    val message: String
)

@Serializable
data class ModeratorsResponse(
    val data: List<ModeratorData>,
    val pagination: PaginationData? = null
)

@Serializable
data class ModeratorData(
    @SerialName("user_id") val userId: String,
    @SerialName("user_login") val userLogin: String,
    @SerialName("user_name") val userName: String
)

@Serializable
data class ModeratedChannelsResponse(
    val data: List<ModeratedChannelData> = emptyList(),
    val pagination: PaginationData? = null
)

@Serializable
data class ModeratedChannelData(
    @SerialName("broadcaster_id") val broadcasterId: String,
    @SerialName("broadcaster_login") val broadcasterLogin: String,
    @SerialName("broadcaster_name") val broadcasterName: String
)

@Serializable
data class ChattersResponse(
    val data: List<ChatterData> = emptyList(),
    val pagination: PaginationData? = null,
    val total: Int = 0
)

@Serializable
data class ChatterData(
    @SerialName("user_id") val userId: String,
    @SerialName("user_login") val userLogin: String,
    @SerialName("user_name") val userName: String
)

@Serializable
data class BlockedUsersResponse(
    val data: List<BlockedUserData>,
    val pagination: PaginationData? = null
)

@Serializable
data class BlockedUserData(
    @SerialName("user_id") val userId: String,
    @SerialName("user_login") val userLogin: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("profile_image_url") val profileImageUrl: String = ""
)

@Serializable
data class PaginationData(
    val cursor: String? = null
)
@Serializable
data class SendChatMessageResponse(
    val data: List<SendChatMessageData>
)

@Serializable
data class SendChatMessageData(
    @SerialName("message_id") val messageId: String,
    @SerialName("is_sent") val isSent: Boolean,
    @SerialName("drop_reason") val dropReason: SendChatMessageDropReason? = null
)

@Serializable
data class SendChatMessageDropReason(
    val code: String,
    val message: String
)
@Serializable
data class UserEmotesResponse(
    val data: List<UserEmoteData>,
    val template: String = ""
)

@Serializable
data class UserEmoteData(
    val id: String,
    val name: String,
    @SerialName("emote_type") val emoteType: String = "",
    @SerialName("emote_set_id") val emoteSetId: String = "",
    @SerialName("owner_id") val ownerId: String = "",
    val format: List<String> = emptyList(),
    val scale: List<String> = emptyList(),
    @SerialName("theme_mode") val themeMode: List<String> = emptyList()
)


@Serializable
data class PollChoice(
    val id: String = "",
    val title: String = "",
    val votes: Int = 0,
    @SerialName("channel_points_votes") val channelPointsVotes: Int = 0,
    @SerialName("bits_votes") val bitsVotes: Int = 0
)

@Serializable
data class PollData(
    val id: String,
    @SerialName("broadcaster_id") val broadcasterId: String = "",
    @SerialName("broadcaster_name") val broadcasterName: String = "",
    @SerialName("broadcaster_login") val broadcasterLogin: String = "",
    val title: String,
    val choices: List<PollChoice> = emptyList(),
    @SerialName("bits_voting_enabled") val bitsVotingEnabled: Boolean = false,
    @SerialName("bits_per_vote") val bitsPerVote: Int = 0,
    @SerialName("channel_points_voting_enabled") val channelPointsVotingEnabled: Boolean = false,
    @SerialName("channel_points_per_vote") val channelPointsPerVote: Int = 0,
    val status: String = "",
    val duration: Int = 0,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null
)

@Serializable
data class PollsResponse(val data: List<PollData> = emptyList())

@Serializable
data class PredictionOutcome(
    val id: String = "",
    val title: String = "",
    val users: Int = 0,
    @SerialName("channel_points") val channelPoints: Int = 0,
    val color: String = "BLUE"
)

@Serializable
data class PredictionData(
    val id: String,
    @SerialName("broadcaster_id") val broadcasterId: String = "",
    @SerialName("broadcaster_name") val broadcasterName: String = "",
    @SerialName("broadcaster_login") val broadcasterLogin: String = "",
    val title: String,
    @SerialName("winning_outcome_id") val winningOutcomeId: String? = null,
    val outcomes: List<PredictionOutcome> = emptyList(),
    @SerialName("prediction_window") val predictionWindow: Int = 0,
    val status: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("locked_at") val lockedAt: String? = null
)

@Serializable
data class PredictionsResponse(val data: List<PredictionData> = emptyList())
