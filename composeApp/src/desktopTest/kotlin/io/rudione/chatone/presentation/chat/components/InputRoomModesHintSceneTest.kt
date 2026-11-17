package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.PopupPortalHost
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class InputRoomModesHintSceneTest {

    private val chat = Color(0xFF101014)
    private val drawer = Color(0xFF2A6B3C)

    private fun pixel(image: Image, x: Int, y: Int): Int = Bitmap.makeFromImage(image).getColor(x, y)

    @Test
    fun hintFloatsAboveTheInputUnderAnOpenDrawerAndCloses() {
        for (width in listOf(360, 900)) {
            var drawerOpen by mutableStateOf(false)
            val dispatcher = StandardTestDispatcher()
            val scene = ImageComposeScene(width = width, height = 300, density = Density(1f), coroutineContext = dispatcher) {
                ChatoneTheme {
                    PopupPortalHost {
                        Box(Modifier.fillMaxSize().background(chat)) {
                            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                                InputRoomModesHint(listOf("Subscribers only", "R9K"), "user:channel")
                                Spacer(Modifier.fillMaxWidth().height(48.dp))
                            }
                            if (drawerOpen) Box(Modifier.fillMaxHeight().width((width * 0.6f).dp).background(drawer))
                        }
                    }
                }
            }
            var frame = 0L
            fun frames(count: Int) = repeat(count) {
                scene.render(frame * 16_000_000L)
                dispatcher.scheduler.advanceTimeBy(16)
                dispatcher.scheduler.runCurrent()
                frame++
            }
            try {
                frames(20)
                val hintY = 300 - 48 - 4 - 10
                val shown = scene.render(frame * 16_000_000L)
                assertNotEquals(chat.toArgb(), pixel(shown, width / 2, hintY), "The hint must sit right above the input")
                assertEquals(chat.toArgb(), pixel(shown, width / 2, 300 - 20), "The hint must not cover the input")

                drawerOpen = true
                frames(2)
                val covered = scene.render(frame * 16_000_000L)
                assertEquals(drawer.toArgb(), pixel(covered, 20, hintY), "An open drawer must cover the hint")
                assertNotEquals(chat.toArgb(), pixel(covered, width - 40, hintY), "The hint stays visible next to the drawer")
                Files.write(
                    Path.of("/tmp/chatone-room-hint-drawer-$width.png"),
                    covered.encodeToData(EncodedImageFormat.PNG)!!.bytes
                )

                drawerOpen = false
                frames(2)
                val close = Offset(width - 8f - 4f - 12f, hintY.toFloat())
                scene.sendPointerEvent(PointerEventType.Enter, close)
                scene.sendPointerEvent(PointerEventType.Press, close, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release, close, button = PointerButton.Primary)
                frames(5)
                assertEquals(chat.toArgb(), pixel(scene.render(frame * 16_000_000L), width / 2, hintY), "The close button must dismiss the hint")
            } finally {
                scene.close()
            }
        }
    }
}
