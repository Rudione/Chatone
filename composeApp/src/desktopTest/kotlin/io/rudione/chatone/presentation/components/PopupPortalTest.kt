package io.rudione.chatone.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalComposeUiApi::class)
class PopupPortalTest {

    private val marker = Color(0xFFFF0000)

    @Test
    fun popupIsDrawnNextToItsAnchor() {
        var shown by mutableStateOf(true)
        val scene = ImageComposeScene(width = 200, height = 200, density = Density(1f))
        try {
            scene.setContent {
                PopupPortalHost {
                    Box(Modifier.offset(100.dp, 100.dp).size(40.dp)) {
                        if (shown) {
                            PortalPopup(offset = IntOffset(5, 5)) {
                                Box(Modifier.size(10.dp).background(marker))
                            }
                        }
                    }
                }
            }
            scene.render()
            assertEquals(marker.toArgb(), pixel(scene.render(), 109, 109))
            assertNotEquals(marker.toArgb(), pixel(scene.render(), 103, 103))

            shown = false
            scene.render()
            assertNotEquals(marker.toArgb(), pixel(scene.render(), 109, 109))
        } finally {
            scene.close()
        }
    }

    @Test
    fun scrollingThePopupRowAwayDuringInputIsSafe() {
        val list = LazyListState()
        val scene = ImageComposeScene(width = 300, height = 300, density = Density(1f))
        try {
            scene.setContent {
                PopupPortalHost {
                    LazyColumn(state = list, modifier = Modifier.size(300.dp)) {
                        items(200, key = { it }, contentType = { "row" }) { index ->
                            Box(Modifier.fillMaxWidth().height(40.dp)) {
                                if (index == 0) PortalPopup(offset = IntOffset(4, 4)) { Box(Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            }
            scene.render()
            list.requestScrollToItem(150)
            scene.sendPointerEvent(PointerEventType.Move, Offset(150f, 150f))
            scene.render()
            list.requestScrollToItem(0)
            scene.sendPointerEvent(PointerEventType.Move, Offset(120f, 120f))
            scene.render()
        } finally {
            scene.close()
        }
    }

    private fun pixel(image: Image, x: Int, y: Int): Int = Bitmap.makeFromImage(image).getColor(x, y)
}
