package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── 7TV User Connection (full user data) ───────────────────────────────

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

// ─── 7TV Paints ─────────────────────────────────────────────────────────

@Serializable
data class SevenTvPaint(
    val id: String,
    val name: String = "",
    val function: String = "LINEAR_GRADIENT",  // LINEAR_GRADIENT, RADIAL_GRADIENT, URL
    val color: Int? = null,
    val stops: List<SevenTvPaintStop> = emptyList(),
    val repeat: Boolean = false,
    val angle: Int = 0,
    @SerialName("image_url") val imageUrl: String = "",
    val shape: String = "",  // for radial: circle, ellipse
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

// ─── 7TV Badges ─────────────────────────────────────────────────────────

@Serializable
data class SevenTvBadge(
    val id: String,
    val name: String = "",
    val tooltip: String = "",
    val tag: String = "",
    val host: SevenTvHost = SevenTvHost()
)

// ─── 7TV Cosmetics Response ─────────────────────────────────────────────

@Serializable
data class SevenTvCosmeticsResponse(
    val paints: List<SevenTvPaint> = emptyList(),
    val badges: List<SevenTvBadge> = emptyList()
)

// ─── 7TV EventAPI Messages ──────────────────────────────────────────────

@Serializable
data class SevenTvEventMessage(
    val op: Int,       // 0=dispatch, 1=hello, 2=heartbeat, 5=subscribe, 34=ack, 35=error
    val d: SevenTvEventData? = null,
    val t: Int? = null  // timestamp
)

@Serializable
data class SevenTvEventData(
    val type: String = "",      // emote_set.update, cosmetic.create, user.update
    val body: SevenTvEventBody? = null,
    @SerialName("condition") val condition: Map<String, String> = emptyMap(),
    // Hello data
    @SerialName("heartbeat_interval") val heartbeatInterval: Long? = null,
    @SerialName("session_id") val sessionId: String? = null
)

@Serializable
data class SevenTvEventBody(
    val id: String = "",
    val kind: Int = 0,         // 1=add, 2=update, 3=remove
    val key: String = "",
    val actor: SevenTvEventActor? = null,
    val old: SevenTvEventValue? = null,
    @SerialName("new") val new_: SevenTvEventValue? = null,  // renamed to avoid Kotlin keyword
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
