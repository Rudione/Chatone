package io.rudione.chatone.presentation.chat.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import kotlin.test.Test

class EmoteAutocompleteRowTest {

    private fun emote(id: String, code: String) = GenericEmote(
        id = id,
        code = code,
        url1x = "",
        url2x = "",
        url3x = "",
        provider = EmoteProvider.SEVEN_TV
    )

    @Test
    fun theSameEmoteUnderTwoNamesDoesNotCrash() {
        val emotes = listOf(emote("01KT41PXKZC7JAMW2E5CEKHVQY", "catJam"), emote("01KT41PXKZC7JAMW2E5CEKHVQY", "catJAMMER"))
        val scene = ImageComposeScene(width = 400, height = 200, density = Density(1f)) {
            CompositionLocalProvider(LocalWallpaperController provides WallpaperController()) {
                EmoteAutocompleteRow(emotes = emotes, onSelect = {}, onDismiss = {})
            }
        }
        try {
            scene.render()
            scene.render()
        } finally {
            scene.close()
        }
    }
}
