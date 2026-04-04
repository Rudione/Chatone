package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Response from GET /v3/users/twitch/{twitch_user_id} ────────────────
// Содержит emote_set И user (с style внутри)

@Serializable
data class SevenTvUserResponse(
    @SerialName("emote_set") val emoteSet: SevenTvEmoteSet? = null,
    // user присутствует в ответе и содержит style с badge_id/paint_id
    val user: SevenTvFullUserInline? = null
)

// Инлайн-версия полного пользователя прямо в ответе /v3/users/twitch/{id}
@Serializable
data class SevenTvFullUserInline(
    val id: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    val style: SevenTvUserStyleInline = SevenTvUserStyleInline(),
    val roles: List<String> = emptyList()
)

// Style содержит либо вложенные объекты, либо только id-шники
@Serializable
data class SevenTvUserStyleInline(
    val color: Int? = null,
    // Полные объекты (если вернулись)
    val badge: SevenTvBadge? = null,
    val paint: SevenTvPaint? = null,
    // Только id (чаще всего приходит только это)
    @SerialName("badge_id") val badgeId: String? = null,
    @SerialName("paint_id") val paintId: String? = null
)

@Serializable
data class SevenTvEmoteSet(
    val id: String,
    val name: String = "",
    val emotes: List<SevenTvEmote> = emptyList()
)

@Serializable
data class SevenTvEmote(
    val id: String,
    val name: String,
    val flags: Int = 0,
    val data: SevenTvEmoteData? = null
)

@Serializable
data class SevenTvEmoteData(
    val id: String = "",
    val name: String = "",
    val flags: Int = 0,
    val host: SevenTvHost = SevenTvHost(),
    val owner: SevenTvEmoteOwner? = null
)

@Serializable
data class SevenTvEmoteOwner(
    val id: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = ""
)

@Serializable
data class SevenTvHost(
    val url: String = "",
    val files: List<SevenTvFile> = emptyList()
)

@Serializable
data class SevenTvFile(
    val name: String,
    val format: String = "",
    val width: Int = 0,
    val height: Int = 0
)