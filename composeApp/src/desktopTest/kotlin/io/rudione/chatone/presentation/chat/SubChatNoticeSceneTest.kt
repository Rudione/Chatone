package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.skia.Image
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.rudione.chatone.data.remote.TranslationClient
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.ChatSubTier
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalAppLocale
import io.rudione.chatone.util.chat.MessageToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.jetbrains.skia.EncodedImageFormat
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class SubChatNoticeSceneTest {
    private val user = ChatNoticeUser("42", "oskasnothappy", "oskasnothappy", "#E05A4F")

    private fun sub(plan: String, months: Int, now: Long = 0, text: String? = null, type: String = if (months > 1) "resub" else "sub"): DisplayMessage.UserNoticeMsg {
        val tags = mapOf(
            "user-id" to user.userId, "login" to user.login, "display-name" to user.displayName, "color" to user.color.orEmpty(),
            "msg-param-sub-plan" to plan, "msg-param-cumulative-months" to "$months"
        )
        val event = IrcEvent.UserNotice("channel", "${user.displayName} subscribed", null, type, tags)
        val inner = text?.let {
            DisplayMessage.PrivMsg(
                id = "inner_$plan$months", timestamp = now, channel = "channel", userId = user.userId, username = user.login,
                displayName = user.displayName, tokens = listOf(MessageToken.Text(it)), color = user.color, badges = emptyList(),
                isModerator = false, isSubscriber = true, isVip = false, isBroadcaster = false, isMention = false, isAction = false
            )
        }
        return DisplayMessage.UserNoticeMsg(
            "sub_${plan}_${months}_$text", now, "channel", event.systemMsg, innerMessage = inner, noticeType = type,
            details = ChatNoticeParser.parse(event, now)
        )
    }

    @BeforeTest
    fun startDi() { startKoin { modules(module { single { TranslationStore(TranslationClient(HttpClient())) } }) } }

    @AfterTest
    fun stopDi() = stopKoin()

    @Test
    fun planAndCumulativeMonthsBecomeTierAndMonths() {
        val plans = listOf("Prime" to ChatSubTier.Prime, "1000" to ChatSubTier.Tier1, "2000" to ChatSubTier.Tier2, "3000" to ChatSubTier.Tier3, "" to ChatSubTier.Tier1)
        for ((plan, tier) in plans) assertEquals(tier, sub(plan, 3).details?.subscription?.tier)
        assertEquals(14, sub("2000", 14).details?.subscription?.months)
        assertEquals(1, sub("1000", 0, type = "sub").details?.subscription?.months)
        val gift = IrcEvent.UserNotice("channel", "gift", null, "subgift", mapOf("msg-param-sub-plan" to "1000", "login" to "a"))
        assertNull(ChatNoticeParser.parse(gift, 0).subscription)
        assertEquals(sub("3000", 5).details?.subscription, sub("3000", 5).compactNoticeData()?.subscription)
    }

    @Test
    fun subArrivalDoesNotScheduleRecompositionEveryFrame() {
        for (plan in listOf("1000", "3000")) {
            val dispatcher = StandardTestDispatcher()
            val message = sub(plan, 12, Clock.System.now().toEpochMilliseconds(), text = "здаров")
            val scene = ImageComposeScene(width = 340, height = 90, density = Density(1f), coroutineContext = dispatcher) {
                ChatoneTheme { UserNoticeMsgItem(message) }
            }
            var frame = 0L
            fun frames(count: Int) = repeat(count) {
                scene.render(frame * 16_000_000L)
                dispatcher.scheduler.advanceTimeBy(16)
                dispatcher.scheduler.runCurrent()
                frame++
            }
            try {
                frames(8)
                assertTrue(Recomposer.runningRecomposers.value.isNotEmpty(), "The test must observe an active recomposer")
                val before = Recomposer.runningRecomposers.value.sumOf { it.changeCount }
                frames(120)
                assertEquals(before, Recomposer.runningRecomposers.value.sumOf { it.changeCount })
            } finally { scene.close() }
        }
    }

    @Test
    fun headerAndMessageNicknamesOpenTheSubscriber() {
        val opened = mutableListOf<ChatNoticeUser>()
        val dispatcher = StandardTestDispatcher()
        val message = sub("2000", 2, text = "здаров мирон")
        val scene = ImageComposeScene(width = 420, height = 90, density = Density(1f), coroutineContext = dispatcher) {
            ChatoneTheme { UserNoticeMsgItem(message, onOpenUser = { opened += it }) }
        }
        var frame = 0L
        fun frames(count: Int) = repeat(count) {
            scene.render(frame * 16_000_000L)
            dispatcher.scheduler.advanceTimeBy(16); dispatcher.scheduler.runCurrent(); frame++
        }
        fun click(x: Float, y: Float) {
            scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
            scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
            frames(10)
        }
        try {
            frames(10)
            Files.write(Path.of("/tmp/chatone-sub-click.png"), scene.render(frame * 16_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            click(50f, 11f)
            assertEquals(listOf(user.userId), opened.map { it.userId })
        } finally { scene.close() }
    }

    @Test
    fun subTiersRenderSettledAndMidArrivalAtPhoneAndDesktopWidths() {
        val plans = listOf("Prime", "1000", "2000", "3000")
        for (language in listOf("en", "ru")) for (width in listOf(300, 560)) {
            val dispatcher = StandardTestDispatcher()
            val now = Clock.System.now().toEpochMilliseconds()
            val settled = plans.mapIndexed { index, plan -> sub(plan, if (index == 1) 1 else 2 + index * 11, text = if (index % 2 == 1) "здаров мирон" else null) }
            val fresh = plans.map { sub(it, 7, now) }
            val scene = ImageComposeScene(width = width, height = 720, density = Density(1f), coroutineContext = dispatcher) {
                CompositionLocalProvider(LocalAppLocale provides language, LocalWallpaperController provides WallpaperController()) {
                    ChatoneTheme {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                settled.forEach { UserNoticeMsgItem(it) }
                                fresh.forEach { UserNoticeMsgItem(it) }
                            }
                        }
                    }
                }
            }
            var frame = 0L
            fun frames(count: Int) = repeat(count) {
                scene.render(frame * 16_000_000L)
                dispatcher.scheduler.advanceTimeBy(16); dispatcher.scheduler.runCurrent(); frame++
            }
            try {
                frames(4)
                for (stop in listOf(30, 85, 130)) {
                    frames((stop - frame).toInt().coerceAtLeast(0))
                    val bytes = scene.render(frame * 16_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
                    Files.write(Path.of("/tmp/chatone-subs-$language-$width-$stop.png"), bytes)
                    assertTrue(bytes.isNotEmpty())
                }
            } finally { scene.close() }
        }
    }

    @Test
    fun giftLidOpensDuringArrivalAndSettlesClosed() {
        val arrivals = listOf(0f, 0.07f, 0.21f, 0.35f, 0.5f, 0.6f, 0.7f, 0.8f, 1f)
        val cell = 96
        val sheet = ImageBitmap(cell * arrivals.size, cell * ChatSubTier.entries.size)
        val canvas = Canvas(sheet)
        canvas.drawRect(0f, 0f, sheet.width.toFloat(), sheet.height.toFloat(), Paint().apply { color = Color(0xFF101014) })
        ChatSubTier.entries.forEachIndexed { row, tier ->
            val look = subTierLook(tier)
            fun pixels(arrival: Float): IntArray {
                val frame = ImageBitmap(cell, cell)
                CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(frame), Size(cell.toFloat(), cell.toFloat())) {
                    drawSubGift(look, arrival)
                }
                return frame.toPixelMap().buffer
            }
            assertTrue(!pixels(0.6f).contentEquals(pixels(1f)), "The lid must move while the gift opens")
            assertTrue(pixels(0f).contentEquals(pixels(1f)), "The gift must rest in the same pose before and after arrival")
            arrivals.forEachIndexed { column, arrival ->
                CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(cell.toFloat(), cell.toFloat())) {
                    translate(column * cell.toFloat(), row * cell.toFloat()) { drawSubGift(look, arrival) }
                }
            }
        }
        val png = Image.makeFromBitmap(sheet.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes
        Files.write(Path.of("/tmp/chatone-sub-gift-frames.png"), png)
    }
}
