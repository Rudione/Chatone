package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.presentation.components.PopupPortalHost
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.profile.PageNavigator
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalAppLocale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.jetbrains.skia.EncodedImageFormat
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class ChannelContextMenuSceneTest {

    private val channels = List(8) { ChannelTab(login = "chan$it", displayName = "Channel $it", unreadCount = if (it == 0) 3 else 0) }
    private val folders = listOf(ChannelFolder(id = "a", name = "Friends"), ChannelFolder(id = "b", name = "Esports"))

    private class Sidebar(
        val scene: ImageComposeScene,
        val dispatcher: kotlinx.coroutines.test.TestDispatcher,
        val events: MutableList<MainEvent>,
        val opened: MutableList<String>,
        val drag: SidebarDragState
    ) {
        private var frame = 0L

        fun frames(count: Int = 8) = repeat(count) {
            scene.render(frame * 16_000_000L)
            dispatcher.scheduler.advanceTimeBy(16)
            dispatcher.scheduler.runCurrent()
            frame++
        }

        fun bounds(text: String): Rect? = scene.semanticsOwners
            .flatMap { it.getAllSemanticsNodes(mergingEnabled = true) }
            .firstOrNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == text } == true }
            ?.boundsInWindow

        fun rightClick(at: Offset) {
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Press, at, buttons = PointerButtons(isSecondaryPressed = true), button = PointerButton.Secondary)
            scene.sendPointerEvent(PointerEventType.Release, at, button = PointerButton.Secondary)
            frames()
        }

        fun click(text: String) {
            val at = assertNotNull(bounds(text), "Missing menu item: $text").center
            scene.sendPointerEvent(PointerEventType.Move, at)
            scene.sendPointerEvent(PointerEventType.Press, at, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, button = PointerButton.Primary)
            frames()
        }

        fun snapshot(path: String) {
            Files.write(Path.of(path), scene.render(frame * 16_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    private fun sidebar(
        folderList: List<ChannelFolder> = folders,
        inFolder: String? = null,
        language: String = "en",
        height: Int = 420,
        listAlignment: Alignment = Alignment.TopStart,
        block: Sidebar.() -> Unit
    ) {
        val dispatcher = StandardTestDispatcher()
        val events = mutableListOf<MainEvent>()
        val opened = mutableListOf<String>()
        val drag = SidebarDragState()
        val koin = koinApplication { modules(module { single { PageNavigator(isSupported = true) } }) }
        val uris = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        val scene = ImageComposeScene(width = 520, height = height, density = Density(1f), coroutineContext = dispatcher) {
            KoinIsolatedContext(koin) {
                CompositionLocalProvider(
                    LocalAppLocale provides language,
                    LocalWallpaperController provides remember { WallpaperController() },
                    LocalUriHandler provides uris
                ) {
                    ChatoneTheme {
                        PopupPortalHost {
                            Box(Modifier.fillMaxSize().background(Color(0xFF101014)), contentAlignment = listAlignment) {
                                Column(Modifier.width(260.dp)) {
                                    channels.forEach { channel ->
                                        ChannelItemWithDrag(
                                            modifier = Modifier,
                                            channel = channel,
                                            isActive = channel.login == "chan0",
                                            folders = folderList,
                                            currentFolderId = inFolder,
                                            liveNotifyEnabled = false,
                                            actions = remember(channel.login) { sidebarChannelActions(channel.login) { events += it } },
                                            dragState = drag
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        try {
            Sidebar(scene, dispatcher, events, opened, drag).apply {
                frames(4)
                block()
            }
        } finally {
            scene.close()
            koin.close()
        }
    }

    @Test
    fun rightClickOpensTheChannelMenuAtTheCursor() = sidebar(inFolder = "a") {
        assertNull(bounds("Open on Twitch"))
        rightClick(Offset(150f, 20f))
        val twitch = assertNotNull(bounds("Open on Twitch"), "Right click must open the channel menu")
        assertTrue(twitch.left >= 150f && twitch.top >= 20f, "The menu must open at the cursor, was $twitch")
        listOf(
            "Channel profile", "Copy channel link", "Move to folder", "Remove from folder",
            "Notify when live", "Mute notifications in this channel", "Close channel"
        ).forEach { assertNotNull(bounds(it), "Missing menu item: $it") }
        assertNull(bounds("Open in player (streamlink)"), "Without the in-app player Twitch is the only way to open the stream")
        assertNull(bounds("Esports"), "The folder list stays folded in the right-click menu")
        snapshot("/tmp/chatone-channel-menu-en.png")
        click("Open on Twitch")
        assertEquals(listOf("https://www.twitch.tv/chan0"), opened)
        assertNull(bounds("Open on Twitch"), "Picking an item closes the menu")
        assertTrue(events.isEmpty())
    }

    @Test
    fun closeAndFolderActionsSendTheirEvents() = sidebar(inFolder = "a") {
        rightClick(Offset(150f, 60f))
        click("Close channel")
        assertEquals(listOf<MainEvent>(MainEvent.CloseChannel("chan1")), events)

        rightClick(Offset(150f, 60f))
        click("Move to folder")
        assertNull(bounds("Friends"), "The current folder is not offered again")
        click("Esports")
        assertEquals(MainEvent.MoveChannelToFolder("chan1", "b"), events.last())

        rightClick(Offset(150f, 60f))
        click("Remove from folder")
        assertEquals(MainEvent.MoveChannelToFolder("chan1", null), events.last())

        rightClick(Offset(150f, 60f))
        click("Mute notifications in this channel")
        assertEquals(MainEvent.MuteMentionsChannel("chan1"), events.last())
    }

    @Test
    fun withoutFoldersTheMenuCreatesAFolderWithTheChannel() = sidebar(folderList = emptyList()) {
        rightClick(Offset(150f, 20f))
        assertNull(bounds("Move to folder"))
        assertNull(bounds("Remove from folder"))
        click("New folder with this channel")
        assertEquals(listOf<MainEvent>(MainEvent.ShowCreateFolderDialog("chan0")), events)
    }

    @Test
    fun nearTheWindowBottomTheMenuOpensAboveTheCursor() = sidebar(height = 760, listAlignment = Alignment.BottomStart) {
        val y = 740f
        rightClick(Offset(150f, y))
        val close = assertNotNull(bounds("Close channel"))
        val profile = assertNotNull(bounds("Channel profile"))
        assertTrue(close.bottom <= y + 1f, "The menu must flip above the cursor, close item was $close")
        assertTrue(profile.top >= 8f, "The menu must stay inside the window, profile item was $profile")
        snapshot("/tmp/chatone-channel-menu-bottom.png")
    }

    @Test
    fun folderButtonOpensTheMenuWithFoldersUnfolded() = sidebar(language = "ru") {
        val button = Offset(214f, 20f)
        scene.sendPointerEvent(PointerEventType.Move, button)
        scene.sendPointerEvent(PointerEventType.Press, button, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, button, button = PointerButton.Primary)
        frames()
        assertNotNull(bounds("Friends"), "The folder button opens the menu with folders listed")
        assertNotNull(bounds("Открыть на Twitch"))
        snapshot("/tmp/chatone-channel-menu-ru.png")
    }

    @Test
    fun onlyTheLeftButtonDragsTheChannel() {
        sidebar { assertTrue(dragWith(PointerButtons(isPrimaryPressed = true), PointerButton.Primary), "A left-button drag picks up the channel") }
        sidebar { assertFalse(dragWith(PointerButtons(isSecondaryPressed = true), PointerButton.Secondary), "A right-button drag must not pick up the channel") }
    }

    private fun Sidebar.dragWith(buttons: PointerButtons, button: PointerButton): Boolean {
        val start = Offset(150f, 100f)
        scene.sendPointerEvent(PointerEventType.Move, start)
        scene.sendPointerEvent(PointerEventType.Press, start, buttons = buttons, button = button)
        repeat(6) { step ->
            scene.sendPointerEvent(PointerEventType.Move, start + Offset(0f, 12f * (step + 1)), buttons = buttons)
            frames(1)
        }
        return drag.isDragging
    }
}
