package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FfzRoomResponse(
    val room: FfzRoom? = null,
    val sets: Map<String, FfzSet> = emptyMap()
)

@Serializable
data class FfzRoom(
    @SerialName("_id") val id: Int = 0,
    @SerialName("twitch_id") val twitchId: Int = 0,
    val set: Int = 0,
    @SerialName("moderator_badge") val moderatorBadge: String? = null,
    @SerialName("vip_badge") val vipBadge: FfzVipBadge? = null
)

@Serializable
data class FfzVipBadge(
    @SerialName("1") val url1x: String? = null,
    @SerialName("2") val url2x: String? = null,
    @SerialName("4") val url4x: String? = null
)

@Serializable
data class FfzGlobalResponse(
    @SerialName("default_sets") val defaultSets: List<Int> = emptyList(),
    val sets: Map<String, FfzSet> = emptyMap()
)

@Serializable
data class FfzSet(
    val id: Int,
    val title: String = "",
    val emoticons: List<FfzEmote> = emptyList()
)

@Serializable
data class FfzEmote(
    val id: Int,
    val name: String,
    val height: Int = 0,
    val width: Int = 0,
    val urls: Map<String, String> = emptyMap(),
    val animated: Map<String, String>? = null
)
