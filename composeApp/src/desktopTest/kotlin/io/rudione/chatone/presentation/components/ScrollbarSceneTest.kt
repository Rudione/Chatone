package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import org.jetbrains.skia.Bitmap
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class ScrollbarSceneTest {
    @Test
    fun actualBottomOverridesEstimatedMessageHeights() = withList({ if (it < 50) 400 else 24 }) { state, frames, dispatcher, _ ->
        dispatcher.scheduler.runCurrent()
        CoroutineScope(dispatcher).launch { state.scrollToItem(99) }
        frames(8)
        assertFalse(state.canScrollForward)
        assertEquals(1f, computeLazyThumb(state, averageHeight = 100f, frozenAverage = null).offset)
    }

    @Test
    fun draggingTheThumbToTheBottomReachesTheActualEnd() = withList({ if (it < 70) 20 else 160 }) { state, frames, _, scene ->
        val start = Offset(354f, 10f)
        val end = Offset(354f, 479f)
        scene.sendPointerEvent(PointerEventType.Press, start, type = PointerType.Touch, timeMillis = 200)
        frames(2)
        scene.sendPointerEvent(PointerEventType.Move, end, type = PointerType.Touch, timeMillis = 300)
        frames(4)
        scene.sendPointerEvent(PointerEventType.Release, end, type = PointerType.Touch, timeMillis = 400)
        frames(8)
        assertFalse(state.canScrollForward)
    }

    @Test
    fun idleScrollbarHidesItsThumbButKeepsEventTicksAndReappearsOnScroll() = withList(
        height = { 24 },
        autoHideDelayMillis = 1_000L,
        ticks = listOf(ScrollbarTick(0.5f, Color.Magenta))
    ) { _, frames, _, scene ->
        val before = Bitmap.makeFromImage(scene.render(128_000_000L))
        assertNotEquals(Color.Black.toArgb(), before.getColor(354, 10))
        frames(80)
        val after = Bitmap.makeFromImage(scene.render(1_408_000_000L))
        assertEquals(Color.Black.toArgb(), after.getColor(354, 10))
        assertEquals(Color.Magenta.toArgb(), after.getColor(354, 240))
        scene.sendPointerEvent(PointerEventType.Enter, Offset(200f, 240f), timeMillis = 1_408L)
        scene.sendPointerEvent(PointerEventType.Move, Offset(200f, 240f), timeMillis = 1_408L)
        scene.sendPointerEvent(PointerEventType.Scroll, Offset(200f, 240f), scrollDelta = Offset(0f, 4f), timeMillis = 1_408L)
        frames(8)
        val resumed = Bitmap.makeFromImage(scene.render(1_536_000_000L))
        assertNotEquals(Color.Black.toArgb(), resumed.getColor(354, 470))
    }

    private fun withList(
        height: (Int) -> Int,
        autoHideDelayMillis: Long? = null,
        ticks: List<ScrollbarTick> = emptyList(),
        test: (LazyListState, (Int) -> Unit, TestDispatcher, ImageComposeScene) -> Unit
    ) {
        val dispatcher = StandardTestDispatcher()
        lateinit var state: LazyListState
        val scene = ImageComposeScene(360, 480, Density(1f), coroutineContext = dispatcher) {
            val list = remember { LazyListState() }
            state = list
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                    items((0 until 100).toList(), key = { it }, contentType = { "row" }) { row ->
                        Box(Modifier.fillMaxWidth().height(height(row).dp))
                    }
                }
                ChatoneLazyScrollbar(
                    list, itemCount = 100,
                    modifier = Modifier.align(Alignment.CenterEnd).width(12.dp).height(480.dp),
                    ticks = ticks,
                    autoHideDelayMillis = autoHideDelayMillis
                )
            }
        }
        var clock = 0L
        val frames: (Int) -> Unit = { count ->
            repeat(count) {
                Snapshot.sendApplyNotifications()
                scene.render(clock * 1_000_000)
                clock += 16
                dispatcher.scheduler.advanceTimeBy(16)
                dispatcher.scheduler.runCurrent()
            }
        }
        try {
            frames(8)
            test(state, frames, dispatcher, scene)
        } finally {
            scene.close()
        }
    }
}
