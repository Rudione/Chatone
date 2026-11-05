package io.rudione.chatone.presentation.chat.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import io.ktor.client.HttpClient
import io.rudione.chatone.data.remote.TranslationClient
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.TranslationStore
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import io.rudione.chatone.util.chat.MessageToken
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class NickRightClickTest {

    private val message = DisplayMessage.PrivMsg(
        id = "m", timestamp = 0L, channel = "c", userId = "1", username = "rudionee", displayName = "Rudionee",
        tokens = listOf(MessageToken.Text("hi "), MessageToken.Mention("user2"), MessageToken.Text(" ok")),
        color = "#FF0000", badges = emptyList(), isModerator = false, isSubscriber = false, isVip = false,
        isBroadcaster = false, isMention = false, isAction = false
    )

    @BeforeTest
    fun startDi() {
        startKoin { modules(module { single { TranslationStore(TranslationClient(HttpClient())) } }) }
    }

    @AfterTest
    fun stopDi() = stopKoin()

    @Test
    fun rightClickOnTheAuthorInsertsTheAuthor() {
        assertEquals(listOf("Rudionee"), rightClickAt(Offset(12f, 12f), compact = false))
        assertEquals(listOf("Rudionee"), rightClickAt(Offset(12f, 12f), compact = true))
    }

    @Test
    fun rightClickOnAMentionInsertsTheMentionedUser() {
        assertEquals(listOf("user2"), rightClickAt(Offset(140f, 12f), compact = false))
        assertEquals(listOf("user2"), rightClickAt(Offset(140f, 12f), compact = true))
    }

    private fun rightClickAt(position: Offset, compact: Boolean): List<String> {
        val inserted = mutableListOf<String>()
        val scene = ImageComposeScene(width = 500, height = 60, density = Density(1f)) {
            CompositionLocalProvider(LocalWallpaperController provides WallpaperController()) {
                ChatoneTheme {
                    PrivMsgItem(
                        message = message,
                        isCompact = compact,
                        timestampFormat = SettingsState.TimestampFormat.OFF,
                        chatFontSizeSp = 15f,
                        onRightClickUsername = { inserted += it }
                    )
                }
            }
        }
        try {
            scene.render()
            scene.sendPointerEvent(PointerEventType.Move, position)
            scene.sendPointerEvent(
                PointerEventType.Press,
                position,
                buttons = PointerButtons(isSecondaryPressed = true),
                button = PointerButton.Secondary
            )
            scene.sendPointerEvent(PointerEventType.Release, position, buttons = PointerButtons(), button = PointerButton.Secondary)
            scene.render()
        } finally {
            scene.close()
        }
        return inserted
    }
}
