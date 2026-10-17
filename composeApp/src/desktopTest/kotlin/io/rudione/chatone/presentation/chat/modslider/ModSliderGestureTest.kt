package io.rudione.chatone.presentation.chat.modslider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.rudione.chatone.data.remote.TranslationClient
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.moderation.ModSliderAction
import io.rudione.chatone.domain.moderation.ModSliderDurations
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.TranslationStore
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.StringsRu
import io.rudione.chatone.util.chat.MessageToken
import kotlinx.coroutines.test.StandardTestDispatcher
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class ModSliderGestureTest {

    private val actions = mutableListOf<ModSliderAction>()
    private val dispatcher = StandardTestDispatcher()
    private var frame = 0L

    private fun message(id: Int, user: String, text: String) = DisplayMessage.PrivMsg(
        id = "m$id",
        timestamp = 1_760_000_000_000L + id * 60_000L,
        channel = "c",
        userId = "u$id",
        username = user,
        displayName = user,
        tokens = listOf(MessageToken.Text(text)),
        color = "#7CC7FF",
        badges = emptyList(),
        isModerator = false,
        isSubscriber = false,
        isVip = false,
        isBroadcaster = false,
        isMention = false,
        isAction = false
    )

    private var durations = listOf(60, 600, 3600)
    private val host = ModSliderHost()

    private val scene = ImageComposeScene(width = 720, height = 260, density = Density(1.5f), coroutineContext = dispatcher) {
        val store = remember { TranslationStore(TranslationClient(HttpClient(MockEngine { respondOk() }))) }
        KoinApplication(application = { modules(module { single { store } }) }) {
            CompositionLocalProvider(LocalStrings provides StringsRu) {
                ChatoneTheme(darkTheme = true) {
                    val style = rememberModSliderStyle()
                    val binding = remember(style) {
                        ModSliderBinding(host, style, durations, deleteAtStart = true, banAtEnd = true, travelFraction = 0.4f)
                    }
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(8.dp)) {
                        listOf(
                            message(1, "viewer_one", "первое сообщение в чате"),
                            message(2, "spammer", "купите подписчиков дешево"),
                            message(3, "viewer_two", "ахах ну и стрим сегодня")
                        ).forEach { msg ->
                            PrivMsgItem(
                                message = msg,
                                showModActions = true,
                                actorCanModerate = true,
                                isMod = true,
                                modSlider = binding,
                                onDelete = { actions += ModSliderAction.Delete },
                                onCustomTimeout = { actions += ModSliderAction.Timeout(it) },
                                onBan = { actions += ModSliderAction.Ban },
                                onUnban = { actions += ModSliderAction.Unban },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun frames(count: Int = 30) = repeat(count) {
        Snapshot.sendApplyNotifications()
        frame += 16_000_000L
        scene.render(frame)
        dispatcher.scheduler.advanceTimeBy(16)
        dispatcher.scheduler.runCurrent()
    }


    private fun press(at: Offset) = scene.sendPointerEvent(
        PointerEventType.Press, at, type = PointerType.Mouse,
        buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary
    )

    private fun move(at: Offset) = scene.sendPointerEvent(
        PointerEventType.Move, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true)
    )

    private fun release(at: Offset) = scene.sendPointerEvent(
        PointerEventType.Release, at, type = PointerType.Mouse,
        buttons = PointerButtons(isPrimaryPressed = false), button = PointerButton.Primary
    )

    private fun drag(from: Offset, dx: Float) {
        press(from)
        frames(2)
        var x = from.x
        val target = from.x + dx
        val step = if (dx > 0) 12f else -12f
        while ((dx > 0 && x < target) || (dx < 0 && x > target)) {
            x += step
            move(Offset(x, from.y))
            frames(1)
        }
        frames(16)
        release(Offset(x, from.y))
        frames(60)
    }

    @Test
    fun pullingTheHandlePicksTheActionByDistance() = withScene {
        val grip = Offset(26f, 32f)
        drag(grip, 40f)
        drag(grip, 90f)
        drag(grip, 420f)
        drag(Offset(26f, 64f), -150f)
        assertEquals(
            listOf(ModSliderAction.Delete, ModSliderAction.Timeout(60), ModSliderAction.Ban, ModSliderAction.Unban),
            actions
        )
    }

    @Test
    fun twitchScaleLandsOnMinutesInTheMiddleOfTheTrack() {
        durations = ModSliderDurations.TWITCH_SCALE
        withScene {
            drag(Offset(26f, 32f), 130f)
            val action = actions.single()
            assertTrue(action is ModSliderAction.Timeout && action.seconds in 60 until 3_600, action.toString())
        }
    }

    @Test
    fun theTimestampIsAGrabAreaToo() = withScene {
        drag(Offset(40f, 96f), 90f)
        assertEquals(listOf<ModSliderAction>(ModSliderAction.Timeout(60)), actions)
    }

    @Test
    fun grabbingEngagesBeforeTheHandleMoves() = withScene {
        val grip = Offset(26f, 32f)
        press(grip)
        frames(2)
        assertTrue(host.isEngaged)
        assertEquals(PointerIcon.Hand, host.cursor)
        move(Offset(grip.x + 4f, grip.y))
        frames(2)
        assertTrue(host.isEngaged)
        assertEquals(PointerIcon.Hand, host.cursor)
        release(Offset(grip.x + 4f, grip.y))
        frames(40)
        assertFalse(host.isEngaged)
        assertEquals(emptyList(), actions)
    }

    @Test
    fun rightClickCancelsTheDrag() = withScene {
        val grip = Offset(26f, 32f)
        press(grip)
        frames(2)
        for (step in 1..12) {
            move(Offset(grip.x + step * 12f, grip.y))
            frames(1)
        }
        scene.sendPointerEvent(
            PointerEventType.Press, Offset(170f, 32f), type = PointerType.Mouse,
            buttons = PointerButtons(isPrimaryPressed = true, isSecondaryPressed = true), button = PointerButton.Secondary
        )
        frames(10)
        release(Offset(170f, 32f))
        frames(40)
        assertEquals(emptyList(), actions)
    }

    @Test
    fun returningTheHandleToTheStartDoesNothing() = withScene {
        val grip = Offset(26f, 32f)
        press(grip)
        frames(2)
        for (step in 1..10) {
            move(Offset(grip.x + step * 12f, grip.y))
            frames(1)
        }
        for (step in 9 downTo 0) {
            move(Offset(grip.x + step * 12f, grip.y))
            frames(1)
        }
        release(grip)
        frames(40)
        assertEquals(emptyList(), actions)
    }

    private fun withScene(block: () -> Unit) {
        try {
            frames(20)
            block()
        } finally {
            scene.close()
        }
    }
}
