package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SevenTvPaint(
    val id: String,
    val name: String = "",
    val function: String = "LINEAR_GRADIENT",
    val color: Int? = null,
    val stops: List<SevenTvPaintStop> = emptyList(),
    val repeat: Boolean = false,
    val angle: Int = 0,
    @SerialName("image_url") val imageUrl: String = "",
    val shape: String = "",
    val shadows: List<SevenTvPaintShadow> = emptyList()
)

@Serializable
data class SevenTvPaintStop(
    val at: Double = 0.0,
    val color: Int = 0,
    val center: Double? = null
)

@Serializable
data class SevenTvPaintShadow(
    @SerialName("x_offset") val xOffset: Double = 0.0,
    @SerialName("y_offset") val yOffset: Double = 0.0,
    val radius: Double = 0.0,
    val color: Int = 0
)


@Serializable
data class SevenTvBadge(
    val id: String,
    val name: String = "",
    val tooltip: String = "",
    val tag: String = "",
    val host: SevenTvHost = SevenTvHost()
)


@Serializable
data class SevenTvGqlRequest(
    val query: String,
    val variables: Map<String, String> = emptyMap()
)

@Serializable
data class SevenTvGqlResponse(
    val data: SevenTvGqlData? = null,
    val errors: List<SevenTvGqlError>? = null
)

@Serializable
data class SevenTvGqlData(
    @SerialName("user") val user: SevenTvGqlUser? = null
)

@Serializable
data class SevenTvGqlUser(
    val id: String = "",
    val style: SevenTvGqlStyle = SevenTvGqlStyle()
)

@Serializable
data class SevenTvGqlStyle(
    val color: Int? = null,
    val badge: SevenTvBadge? = null,
    val paint: SevenTvPaint? = null
)

@Serializable
data class SevenTvGqlError(
    val message: String = ""
)


@Serializable
data class SevenTvUserConnection(
    val id: String = "",
    val platform: String = "TWITCH",
    @SerialName("linked_at") val linkedAt: Long = 0,
    @SerialName("emote_capacity") val emoteCapacity: Int = 0,
    @SerialName("emote_set_id") val emoteSetId: String? = null,
    @SerialName("emote_set") val emoteSet: SevenTvEmoteSet? = null,
    val user: SevenTvFullUser? = null
)

@Serializable
data class SevenTvFullUser(
    val id: String,
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val style: SevenTvUserStyle = SevenTvUserStyle(),
    val roles: List<String> = emptyList(),
    val connections: List<SevenTvPlatformConnection> = emptyList()
)

@Serializable
data class SevenTvPlatformConnection(
    val id: String,
    val platform: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("linked_at") val linkedAt: Long = 0,
    @SerialName("emote_capacity") val emoteCapacity: Int = 0,
    @SerialName("emote_set_id") val emoteSetId: String? = null
)

@Serializable
data class SevenTvUserStyle(
    val color: Int? = null,
    val paint: SevenTvPaint? = null,
    @SerialName("paint_id") val paintId: String? = null,
    val badge: SevenTvBadge? = null,
    @SerialName("badge_id") val badgeId: String? = null
)


@Serializable
data class SevenTvCosmeticsResponse(
    val paints: List<SevenTvPaint> = emptyList(),
    val badges: List<SevenTvBadge> = emptyList()
)


@Serializable
data class SevenTvEventMessage(
    val op: Int,
    val d: SevenTvEventData? = null,
    val t: Int? = null
)

@Serializable
data class SevenTvEventData(
    val type: String = "",
    val body: SevenTvEventBody? = null,
    @SerialName("condition") val condition: Map<String, String> = emptyMap(),
    @SerialName("heartbeat_interval") val heartbeatInterval: Long? = null,
    @SerialName("session_id") val sessionId: String? = null
)

@Serializable
data class SevenTvEventBody(
    val id: String = "",
    val kind: Int = 0,
    val key: String = "",
    val actor: SevenTvEventActor? = null,
    val old: SevenTvEventValue? = null,
    @SerialName("new") val new_: SevenTvEventValue? = null,
    val pushed: List<SevenTvEventPushedItem> = emptyList(),
    val pulled: List<SevenTvEventPulledItem> = emptyList(),
    val updated: List<SevenTvEventUpdatedItem> = emptyList()
)

@Serializable
data class SevenTvEventActor(
    val id: String = "",
    val username: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class SevenTvEventValue(
    val name: String = "",
    val value: String = ""
)

@Serializable
data class SevenTvEventPushedItem(
    val key: String = "",
    val value: SevenTvEventEmoteValue? = null
)

@Serializable
data class SevenTvEventPulledItem(
    val key: String = "",
    val old_value: SevenTvEventEmoteValue? = null
)

@Serializable
data class SevenTvEventUpdatedItem(
    val key: String = "",
    val old_value: SevenTvEventEmoteValue? = null,
    val value: SevenTvEventEmoteValue? = null
)

@Serializable
data class SevenTvEventEmoteValue(
    val id: String = "",
    val name: String = "",
    val flags: Int = 0,
    val data: SevenTvEmoteData? = null
)