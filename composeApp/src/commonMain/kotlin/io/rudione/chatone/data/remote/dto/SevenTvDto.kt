package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SevenTvUserResponse(
    @SerialName("emote_set") val emoteSet: SevenTvEmoteSet? = null
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
    val host: SevenTvHost = SevenTvHost()
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
