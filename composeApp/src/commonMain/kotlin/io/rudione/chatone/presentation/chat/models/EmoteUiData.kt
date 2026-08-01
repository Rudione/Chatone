package io.rudione.chatone.presentation.chat.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.domain.model.GenericEmote

@Immutable
data class EmoteUiData(
    val displayCode: String,
    val imageUrl: String,
    val cacheKey: String,
    val listKey: String,
    val isFavorite: Boolean,
    val providerColor: Color
) {
    companion object {
        private val PROVIDER_COLORS = mapOf(
            "TWITCH" to Color(0xFF9147FF),
            "SEVEN_TV" to Color(0xFF00FF9D),
            "BTTV" to Color(0xFFB8001F),
            "FFZ" to Color(0xFF6441A5)
        )

        fun fromEmote(emote: GenericEmote, isFavorite: Boolean): EmoteUiData {
            return EmoteUiData(
                displayCode = emote.code,
                imageUrl = emote.url1x.ifEmpty { emote.url2x },
                cacheKey = "${emote.provider.name}_${emote.id}",
                listKey = "${emote.provider.name}_${emote.id}",
                isFavorite = isFavorite,
                providerColor = if (isFavorite) Color(0xFFFFD700)
                else PROVIDER_COLORS[emote.provider.name] ?: Color.Unspecified
            )
        }
    }
}
