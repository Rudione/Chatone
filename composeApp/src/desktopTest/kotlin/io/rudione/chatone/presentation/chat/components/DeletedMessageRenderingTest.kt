package io.rudione.chatone.presentation.chat.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import io.ktor.client.HttpClient
import io.rudione.chatone.data.remote.TranslationClient
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.TranslationStore
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import io.rudione.chatone.util.chat.MessageToken
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class DeletedMessageRenderingTest {
    private val paint = SevenTvCosmetics.Paint("blue", "Blue", "LINEAR_GRADIENT", 0x0000FFFF, emptyList(), false, 0, "", emptyList())
    private val base = DisplayMessage.PrivMsg(
        id = "message", timestamp = 0, channel = "channel", userId = "42", username = "alice", displayName = "Alice",
        tokens = listOf(MessageToken.Text("hello")), color = "#FF0000", badges = emptyList(),
        isModerator = false, isSubscriber = false, isVip = false, isBroadcaster = false, isMention = false, isAction = false,
        sevenTvPaint = paint
    )

    @BeforeTest
    fun startDi() { startKoin { modules(module { single { TranslationStore(TranslationClient(HttpClient())) } }) } }

    @AfterTest
    fun stopDi() = stopKoin()

    @Test
    fun deletingAMessageKeepsThePaintedNickname() {
        for (compact in listOf(false, true)) {
            var message by mutableStateOf(base)
            val scene = scene(compact, { message })
            try {
                scene.render()
                assertTrue(bluePixels(scene.render()) > 20)
                message = base.copy(isDeleted = true)
                scene.render()
                assertTrue(bluePixels(scene.render()) > 20)
            } finally { scene.close() }
        }
    }

    @Test
    fun deletingAnEmoteMessageKeepsItsInlineLayoutAndOverlays() {
        val overlay = GenericEmote("overlay", "Overlay", "", "", "", EmoteProvider.SEVEN_TV, width = 40, height = 40)
        val emote = overlay.copy(id = "emote", code = "catJam", width = 30, height = 60)
        for (compact in listOf(false, true)) {
            var message by mutableStateOf(base.copy(tokens = listOf(MessageToken.ThirdPartyEmoteToken(emote, listOf(overlay)))))
            var height = 0
            val scene = scene(compact, { message }, Modifier.onSizeChanged { height = it.height })
            try {
                scene.render()
                scene.render()
                val before = height
                assertTrue(before > 25)
                message = message.copy(isDeleted = true)
                scene.render()
                scene.render()
                assertEquals(before, height)
            } finally { scene.close() }
        }
    }

    private fun scene(compact: Boolean, message: () -> DisplayMessage.PrivMsg, modifier: Modifier = Modifier) = ImageComposeScene(
        width = 400, height = 120, density = Density(1f)
    ) {
        CompositionLocalProvider(LocalWallpaperController provides WallpaperController()) {
            ChatoneTheme {
                PrivMsgItem(
                    message(), isCompact = compact, showModActions = compact, actorCanModerate = compact,
                    modButtonsVisible = false, timestampFormat = SettingsState.TimestampFormat.OFF,
                    emoteSize = SettingsState.EmoteSize.LARGE, modifier = modifier
                )
            }
        }
    }

    private fun bluePixels(image: Image): Int {
        val bitmap = Bitmap.makeFromImage(image)
        var count = 0
        for (y in 0 until bitmap.height) for (x in 0 until 150) {
            val color = bitmap.getColor(x, y)
            if ((color and 255) > 160 && ((color ushr 16) and 255) < 70 && ((color ushr 8) and 255) < 100) count++
        }
        return count
    }
}
