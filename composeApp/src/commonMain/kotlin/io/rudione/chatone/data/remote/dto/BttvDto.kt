package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BttvUserResponse(
    val id: String = "",
    val bots: List<String> = emptyList(),
    @SerialName("channelEmotes") val channelEmotes: List<BttvEmote> = emptyList(),
    @SerialName("sharedEmotes") val sharedEmotes: List<BttvEmote> = emptyList()
)

@Serializable
data class BttvEmote(
    val id: String,
    val code: String,
    @SerialName("imageType") val imageType: String = "png",
    val animated: Boolean = false,
    val userId: String? = null
)

@Serializable
data class BttvGlobalEmote(
    val id: String,
    val code: String,
    @SerialName("imageType") val imageType: String = "png",
    val animated: Boolean = false
)
