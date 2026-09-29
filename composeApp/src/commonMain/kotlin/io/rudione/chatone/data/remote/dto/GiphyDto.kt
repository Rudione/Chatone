package io.rudione.chatone.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiphyResponse(
    val data: List<GiphyGif> = emptyList(),
    val pagination: GiphyPagination? = null,
    val meta: GiphyMeta? = null
)

@Serializable
data class GiphyMeta(
    val status: Int = 0,
    val msg: String = ""
)

@Serializable
data class GiphyPagination(
    @SerialName("total_count") val totalCount: Int = 0,
    val count: Int = 0,
    val offset: Int = 0
)

@Serializable
data class GiphyGif(
    val id: String = "",
    val title: String = "",
    val images: GiphyImages = GiphyImages()
)

@Serializable
data class GiphyImages(
    val original: GiphyImage? = null,
    @SerialName("fixed_height") val fixedHeight: GiphyImage? = null,
    @SerialName("fixed_height_downsampled") val fixedHeightDownsampled: GiphyImage? = null,
    @SerialName("fixed_height_small") val fixedHeightSmall: GiphyImage? = null,
    @SerialName("fixed_width") val fixedWidth: GiphyImage? = null,
    @SerialName("fixed_width_small") val fixedWidthSmall: GiphyImage? = null
)

@Serializable
data class GiphyImage(
    val url: String = "",
    val width: String = "",
    val height: String = ""
)
