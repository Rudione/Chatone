package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import org.jetbrains.skia.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.theme.i18n.LocalAppLocale
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.jetbrains.skia.EncodedImageFormat
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class ChatNoticeSceneTest {
    private val user = ChatNoticeUser("42", "alice", "Alice", "#55DD88")
    private val paint = SevenTvCosmetics.Paint("blue", "Blue", "LINEAR_GRADIENT", 0x0000FFFF, emptyList(), false, 0, "", emptyList())

    private fun notice(type: String, tags: Map<String, String>, now: Long = 0): DisplayMessage.UserNoticeMsg {
        val all = mapOf("user-id" to user.userId, "login" to user.login, "display-name" to user.displayName, "color" to user.color.orEmpty()) + tags
        val event = IrcEvent.UserNotice("channel", "Alice notification", null, type, all)
        return DisplayMessage.UserNoticeMsg("notice_${tags}", now, "channel", event.systemMsg, noticeType = type, details = ChatNoticeParser.parse(event, now))
    }

    @Test
    fun raidTiersHaveExplicitBoundaries() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), listOf(1, 10, 100, 500, 1_500, 5_000, 15_000).map(::raidLevel))
        assertEquals(listOf(0, 1, 2, 3, 4, 5), listOf(9, 99, 499, 1_499, 4_999, 14_999).map(::raidLevel))
    }

    @Test
    fun authorMatchingDoesNotColorAnotherUsersSubstring() {
        assertEquals(0..4, noticeAuthorRange("Alice watched 3 streams", user))
        assertEquals(null, noticeAuthorRange("Malice watched 3 streams", user))
    }

    @Test
    fun raidArrivalDoesNotScheduleRecompositionEveryFrame() {
        val dispatcher = StandardTestDispatcher()
        val message = notice("raid", mapOf("msg-param-viewerCount" to "15000"), Clock.System.now().toEpochMilliseconds())
        val scene = ImageComposeScene(width = 340, height = 60, density = Density(1f), coroutineContext = dispatcher) {
            ChatoneTheme { UserNoticeMsgItem(message) }
        }
        var frame = 0L
        fun frames(count: Int) = repeat(count) {
            scene.render(frame * 1_000_000)
            dispatcher.scheduler.advanceTimeBy(16)
            dispatcher.scheduler.runCurrent()
            frame += 16
        }
        try {
            frames(8)
            assertTrue(Recomposer.runningRecomposers.value.isNotEmpty(), "The test must observe an active recomposer")
            val before = Recomposer.runningRecomposers.value.sumOf { it.changeCount }
            frames(60)
            assertEquals(before, Recomposer.runningRecomposers.value.sumOf { it.changeCount })
        } finally { scene.close() }
    }

    @Test
    fun giftRowExpandsAndCollapsesInlineWhileNicknameClicksRemainIndependent() {
        var opened = 0
        var height = 0
        val message = notice("submysterygift", mapOf("msg-param-mass-gift-count" to "10"))
        val dispatcher = StandardTestDispatcher()
        val scene = ImageComposeScene(width = 340, height = 200, density = Density(1f), coroutineContext = dispatcher) {
            ChatoneTheme {
                Box(Modifier.onSizeChanged { height = it.height }) { UserNoticeMsgItem(message, onOpenUser = { opened++ }) }
            }
        }
        var frame = 0L
        fun frames(count: Int) = repeat(count) {
            scene.render(frame * 16_000_000L)
            dispatcher.scheduler.advanceTimeBy(16); dispatcher.scheduler.runCurrent(); frame++
        }
        fun click(x: Float, y: Float) {
            scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
            scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
            frames(20)
        }
        try {
            frames(20)
            val collapsed = height
            click(40f, 10f)
            assertEquals(1, opened)
            assertEquals(collapsed, height)
            click(300f, 10f)
            assertTrue(height > collapsed)
            click(300f, 10f)
            assertEquals(collapsed, height)
            assertEquals(1, opened)
        } finally { scene.close() }
    }

    @Test
    fun inlineRecipientsRequestVisibleUsersOnceAndOpenTheirProfilesWithoutCollapsing() {
        val requested = mutableListOf<String>()
        val profiles = ProfilePopupStack()
        val dispatcher = StandardTestDispatcher()
        val summary = notice("submysterygift", mapOf("id" to "summary", "msg-param-mass-gift-count" to "2"))
        val one = notice("subgift", mapOf("id" to "recipient-1", "msg-param-recipient-id" to "101", "msg-param-recipient-user-name" to "bob"))
        val two = notice("subgift", mapOf("id" to "recipient-2", "msg-param-recipient-id" to "102", "msg-param-recipient-user-name" to "carol"))
        val message = ChatGiftBundles.fold(listOf(summary, one, two)).single() as DisplayMessage.UserNoticeMsg
        var height = 0
        val scene = ImageComposeScene(width = 600, height = 300, density = Density(1f), coroutineContext = dispatcher) {
            CompositionLocalProvider(LocalAppLocale provides "ru", LocalWallpaperController provides WallpaperController()) {
                ChatoneTheme {
                    Box(Modifier.onSizeChanged { height = it.height }) {
                        UserNoticeMsgItem(message,
                            onOpenUser = { profiles.open(it.profileMessage(message.channel, message.timestamp)) },
                            onUserVisible = { requested += it })
                    }
                }
            }
        }
        var frame = 0L
        fun frames(count: Int) = repeat(count) {
            scene.render(frame * 16_000_000L)
            dispatcher.scheduler.advanceTimeBy(16); dispatcher.scheduler.runCurrent(); frame++
        }
        fun click(x: Float, y: Float) {
            scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
            scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
            frames(20)
        }
        try {
            frames(20)
            val collapsed = height
            assertTrue(requested.isEmpty())
            click(550f, 10f)
            val expanded = height
            assertTrue(expanded > collapsed)
            assertEquals(listOf("101", "102"), requested)
            Files.write(Path.of("/tmp/chatone-gifts-inline.png"), scene.render(frame * 16_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            click(28f, 65f)
            assertEquals("101", profiles.slots.single().message.userId)
            assertEquals(expanded, height)
            Files.write(Path.of("/tmp/chatone-gifts-inline.png"), scene.render(frame * 16_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            click(550f, 10f)
            assertEquals(collapsed, height)
            assertEquals(listOf("101", "102"), requested)
        } finally { scene.close() }
    }

    @Test
    fun compactNoticesRenderWithCachedPaintAtPhoneAndDesktopWidths() {
        for (language in listOf("en", "ru")) for (width in listOf(280, 600)) {
            val scene = ImageComposeScene(width = width, height = 320, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalAppLocale provides language,
                    LocalWallpaperController provides WallpaperController(),
                    LocalSevenTvPaints provides mutableStateOf(mapOf(user.userId to paint))
                ) {
                    ChatoneTheme {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            for (count in listOf(5, 50, 250, 1_000, 3_000, 10_000, 20_000)) {
                                UserNoticeMsgItem(notice("raid", mapOf("msg-param-viewerCount" to "$count")))
                            }
                            UserNoticeMsgItem(notice("viewermilestone", mapOf("msg-param-category" to "watch-streak", "msg-param-value" to "12")))
                            UserNoticeMsgItem(notice("submysterygift", mapOf("msg-param-mass-gift-count" to "20")))
                        }
                    }
                        }
                }
            }
            try {
                scene.render()
                val image = scene.render()
                val bitmap = Bitmap.makeFromImage(image)
                var blue = 0
                for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                    val pixel = bitmap.getColor(x, y)
                    if ((pixel and 255) > 160 && ((pixel ushr 16) and 255) < 70 && ((pixel ushr 8) and 255) < 100) blue++
                }
                assertTrue(blue > 50, "The cached nickname paint must remain visible")
                val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
                Files.write(Path.of("/tmp/chatone-notices-$language-$width.png"), bytes)
                assertTrue(bytes.isNotEmpty())
            } finally { scene.close() }
        }
    }
}
