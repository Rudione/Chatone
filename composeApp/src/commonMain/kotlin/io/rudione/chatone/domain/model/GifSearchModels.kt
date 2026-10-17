package io.rudione.chatone.domain.model

import androidx.compose.runtime.Immutable
import io.rudione.chatone.util.Result
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class GifSearchItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val sendUrl: String,
    val width: Int = 0,
    val height: Int = 0
) {
    val listKey: String get() = id
}

@Immutable
data class GifSearchPage(
    val items: List<GifSearchItem>,
    val nextOffset: Int?
)

@Immutable
data class GifSearchConfig(val apiKey: String, val rating: String)

data class ChannelGifConfig(val enabled: Boolean, val apiKey: String, val rating: String)

fun interface ChannelGifConfigSource {
    suspend fun channelGifConfig(channelId: String, token: String): Result<ChannelGifConfig>
}

sealed interface GifAccess {
    data object Loading : GifAccess
    data class Ready(val config: GifSearchConfig, val fromChannel: Boolean) : GifAccess
    data object Disabled : GifAccess
    data object NeedsKey : GifAccess
}
