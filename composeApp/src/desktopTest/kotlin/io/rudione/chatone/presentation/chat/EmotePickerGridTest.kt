package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import kotlin.test.Test

@OptIn(ExperimentalComposeUiApi::class)
class EmotePickerGridTest {
    @Test
    fun channelAndSubscriptionEmotesCanShareAnId() {
        renderPicker(listOf(emote("Kappa")), listOf(emote("Kappa")))
    }

    @Test
    fun repeatedEmotesAndAliasesHaveUniqueKeys() {
        renderPicker(listOf(emote("Kappa"), emote("Kappa"), emote("Alias")), emptyList())
    }

    private fun emote(code: String) = GenericEmote(
        id = "25", code = code, url1x = "", url2x = "", url3x = "", provider = EmoteProvider.TWITCH
    )

    private fun renderPicker(channel: List<GenericEmote>, personal: List<GenericEmote>) {
        val scene = ImageComposeScene(width = 500, height = 500, density = Density(1f))
        try {
            scene.setContent {
                CompositionLocalProvider(LocalWallpaperController provides WallpaperController()) {
                    MaterialTheme {
                        Box(Modifier.size(500.dp)) {
                            EmoteTab(
                                channelEmotes = ChannelEmotes(twitchEmotes = channel),
                                personalEmotes = personal,
                                allEmotes = channel + personal,
                                favoriteEmotes = emptyList(),
                                favoriteIds = emptySet(),
                                searchQuery = "",
                                onSearchQueryChange = {},
                                onEmoteSelected = {},
                                onToggleFavorite = {}
                            )
                        }
                    }
                }
            }
            scene.render()
            scene.render()
        } finally {
            scene.close()
        }
    }
}
