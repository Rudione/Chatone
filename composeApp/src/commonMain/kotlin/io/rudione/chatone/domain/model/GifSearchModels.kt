package io.rudione.chatone.domain.model

import androidx.compose.runtime.Immutable
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
