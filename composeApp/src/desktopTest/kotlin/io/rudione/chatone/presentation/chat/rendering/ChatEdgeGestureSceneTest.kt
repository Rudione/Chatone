package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.SwipeDirection
import io.rudione.chatone.presentation.components.directionalSwipe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class ChatEdgeGestureSceneTest {
    @Test
    fun mobileChatPreservesGesturesWhenThePlatformEdgeEffectWouldBeStuck() {
        assertEquals(Gestures(1, 1, 1), interact(desktop = false))
    }

    @Test
    fun platformEdgeEffectControlReproducesTheBlockedGestures() {
        assertEquals(Gestures(0, 0, 0), interact(desktop = true))
    }

    private fun interact(desktop: Boolean): Gestures {
        var taps = 0
        var holds = 0
        var swipes = 0
        val dispatcher = StandardTestDispatcher()
        val factory = StuckEdgeFactory()
        val scene = ImageComposeScene(360, 480, Density(1f), coroutineContext = dispatcher) {
            CompositionLocalProvider(LocalOverscrollFactory provides factory) {
                val list = remember { LazyListState() }
                val follow = rememberChatFollowState(list)
                ChatFollowEffects(list, newestItemKey = 99, paused = follow.isPausedByScroll, onPinnedToBottom = {}, follow = follow)
                Box(Modifier.fillMaxSize().holdChatFollowing(follow).directionalSwipe(SwipeDirection.Right) { swipes++ }) {
                    LazyColumn(
                        state = list,
                        overscrollEffect = rememberChatOverscrollEffect(desktop),
                        modifier = Modifier.fillMaxSize().nestedScroll(follow.nestedScrollConnection)
                    ) {
                        items((0 until 100).toList(), key = { it }, contentType = { "row" }) {
                            Box(Modifier.fillMaxWidth().height(24.dp).pointerInput(Unit) {
                                detectTapGestures(onTap = { taps++ }, onLongPress = { holds++ })
                            })
                        }
                    }
                }
            }
        }
        var clock = 0L
        fun frames(count: Int) = repeat(count) {
            Snapshot.sendApplyNotifications()
            scene.render(clock * 1_000_000)
            clock += 16
            dispatcher.scheduler.advanceTimeBy(16)
            dispatcher.scheduler.runCurrent()
        }
        fun send(type: PointerEventType, at: Offset = Offset(180f, 240f)) {
            scene.sendPointerEvent(type, at, type = PointerType.Touch, timeMillis = clock)
        }
        try {
            frames(12)
            send(PointerEventType.Press)
            frames(2)
            send(PointerEventType.Release)
            frames(8)
            send(PointerEventType.Press)
            frames(40)
            send(PointerEventType.Release)
            frames(8)
            send(PointerEventType.Press)
            frames(1)
            send(PointerEventType.Move, Offset(183f, 241f))
            frames(2)
            send(PointerEventType.Move, Offset(220f, 244f))
            frames(2)
            send(PointerEventType.Move, Offset(300f, 250f))
            frames(2)
            send(PointerEventType.Release, Offset(300f, 250f))
            frames(8)
            return Gestures(taps, holds, swipes)
        } finally {
            scene.close()
        }
    }

    private data class Gestures(val taps: Int, val holds: Int, val swipes: Int)

    private class StuckEdgeFactory : OverscrollFactory {
        override fun createOverscrollEffect(): OverscrollEffect = StuckEdgeEffect()
        override fun equals(other: Any?): Boolean = other === this
        override fun hashCode(): Int = 1
    }

    private class StuckEdgeEffect : OverscrollEffect {
        override val isInProgress: Boolean = true
        override val node = object : Modifier.Node() {}

        override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset =
            performScroll(delta)

        override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
            performFling(velocity)
        }
    }
}
