package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class DirectionalSwipeSceneTest {
    @Test
    fun removingTheGestureSurfaceReleasesAnActiveDrawerDrag() {
        var shown by mutableStateOf(true)
        var releases = 0
        var drags = 0
        val handler = object : SwipeDragHandler {
            override fun onSwipeDrag(deltaPx: Float) { drags++ }
            override fun onSwipeRelease(velocityPx: Float) { releases++ }
        }
        val dispatcher = StandardTestDispatcher()
        val scene = ImageComposeScene(360, 480, Density(1f), coroutineContext = dispatcher) {
            if (shown) Box(Modifier.fillMaxSize().directionalDrag(SwipeDirection.Right, handler))
        }
        var clock = 0L
        fun frames() = repeat(4) {
            Snapshot.sendApplyNotifications()
            scene.render(clock * 1_000_000)
            clock += 16
            dispatcher.scheduler.advanceTimeBy(16)
            dispatcher.scheduler.runCurrent()
        }
        try {
            frames()
            scene.sendPointerEvent(PointerEventType.Press, Offset(50f, 200f), type = PointerType.Touch, timeMillis = clock)
            scene.sendPointerEvent(PointerEventType.Move, Offset(130f, 200f), type = PointerType.Touch, timeMillis = clock + 20)
            frames()
            assertEquals(1, drags)
            shown = false
            frames()
            assertEquals(1, releases)
        } finally {
            scene.close()
        }
    }
}
