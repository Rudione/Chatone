package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class AnimatedMenuIconSceneTest {

    private fun changes(): Long = Recomposer.runningRecomposers.value.sumOf { it.changeCount }

    private fun pixels(image: Image): IntArray {
        val bitmap = Bitmap.makeFromImage(image)
        return IntArray(bitmap.width * bitmap.height) { bitmap.getColor(it % bitmap.width, it / bitmap.width) }
    }

    @Test
    fun hoverHintsTheDrawerInDrawPhaseAndSettlesBack() {
        for (expanded in listOf(false, true)) {
            val dispatcher = StandardTestDispatcher()
            val scene = ImageComposeScene(width = 140, height = 140, density = Density(1f), coroutineContext = dispatcher) {
                ChatoneTheme {
                    Box {
                        TactileIconButton(onClick = {}, size = 140.dp) { tint ->
                            AnimatedMenuIcon(expanded, tint, null, Modifier.size(100.dp))
                        }
                    }
                }
            }
            var frame = 0L
            val shots = mutableListOf<Image>()
            fun frames(count: Int) = repeat(count) {
                scene.render(frame * 16_000_000L)
                dispatcher.scheduler.advanceTimeBy(16)
                dispatcher.scheduler.runCurrent()
                frame++
            }
            fun shot(): Image = scene.render(frame * 16_000_000L).also { shots += it }
            try {
                frames(30)
                val rest = pixels(shot())
                scene.sendPointerEvent(PointerEventType.Enter, Offset(70f, 70f))
                scene.sendPointerEvent(PointerEventType.Move, Offset(70f, 70f))
                frames(12)
                val before = changes()
                val moving = pixels(shot())
                repeat(5) { frames(3); shot() }
                frames(40)
                assertEquals(before, changes())
                val hovered = pixels(shot())
                if (!expanded) assertFalse(moving.contentEquals(hovered), "The drawer hint must still be animating after the button tint settles")
                if (!expanded) assertFalse(rest.contentEquals(hovered), "Hover must show the drawer hint")
                scene.sendPointerEvent(PointerEventType.Exit, Offset(200f, 200f))
                frames(60)
                assertContentEquals(rest, pixels(shot()))
                writeStrip(shots, "/tmp/chatone-menu-hover-${if (expanded) "open" else "closed"}.png")
            } finally {
                scene.close()
            }
        }
    }

    private fun writeStrip(shots: List<Image>, path: String) {
        val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(shots.sumOf { it.width }, shots.maxOf { it.height })
        var x = 0f
        shots.forEach { surface.canvas.drawImage(it, x, 0f); x += it.width }
        Files.write(Path.of(path), surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
}
