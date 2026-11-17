package io.rudione.chatone.presentation.components.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Settings
import io.rudione.chatone.presentation.chat.points.ClaimChestButton
import io.rudione.chatone.presentation.chat.points.PointsClaim
import io.rudione.chatone.presentation.chat.points.PointsGain
import io.rudione.chatone.presentation.chat.points.PointsButton
import io.rudione.chatone.presentation.chat.points.PointsGainReason
import io.rudione.chatone.presentation.chat.points.pointsGainBurst
import io.rudione.chatone.presentation.components.TactileIconButton
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.icons.TwitchPointsIcon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class MotionIconSceneTest {

    private class Harness(val scene: ImageComposeScene, val dispatcher: kotlinx.coroutines.test.TestDispatcher) {
        private var frame = 0L
        fun frames(count: Int) = repeat(count) {
            scene.render(frame * 16_000_000L)
            dispatcher.scheduler.advanceTimeBy(16)
            dispatcher.scheduler.runCurrent()
            frame++
        }
    }

    private fun changes(): Long = Recomposer.runningRecomposers.value.sumOf { it.changeCount }

    @Test
    fun pulseAnimatesInDrawPhaseOnly() {
        val dispatcher = StandardTestDispatcher()
        val pulse = mutableIntStateOf(0)
        val scene = ImageComposeScene(width = 120, height = 60, density = Density(1f), coroutineContext = dispatcher) {
            ChatoneTheme {
                Row {
                    MotionIcon(Lucide.Settings, IconMotion.Gear, null, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp), pulse = pulse.intValue)
                    MotionIcon(TwitchPointsIcon, IconMotion.Points, null, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp), pulse = pulse.intValue)
                }
            }
        }
        val harness = Harness(scene, dispatcher)
        try {
            harness.frames(5)
            pulse.intValue = 1
            harness.frames(3)
            val before = changes()
            harness.frames(50)
            assertEquals(before, changes())
        } finally {
            scene.close()
        }
    }

    @Test
    fun hoverInsideIconButtonDoesNotRecomposeEveryFrame() {
        val dispatcher = StandardTestDispatcher()
        val scene = ImageComposeScene(width = 80, height = 80, density = Density(1f), coroutineContext = dispatcher) {
            ChatoneTheme {
                Box {
                    TactileIconButton(onClick = {}, size = 40.dp) { tint ->
                        MotionIcon(Lucide.Settings, IconMotion.Gear, null, tint, Modifier.size(20.dp))
                    }
                }
            }
        }
        val harness = Harness(scene, dispatcher)
        try {
            harness.frames(5)
            scene.sendPointerEvent(PointerEventType.Enter, Offset(20f, 20f))
            scene.sendPointerEvent(PointerEventType.Move, Offset(20f, 20f))
            harness.frames(20)
            val before = changes()
            harness.frames(40)
            assertEquals(before, changes())
        } finally {
            scene.close()
        }
    }

    @Test
    fun gainBurstAndChestIdleStayInDrawPhase() {
        val dispatcher = StandardTestDispatcher()
        val gain = mutableStateOf<PointsGain?>(null)
        val scene = ImageComposeScene(width = 200, height = 60, density = Density(1f), coroutineContext = dispatcher) {
            ChatoneTheme {
                Row {
                    Box(
                        Modifier.size(30.dp).pointsGainBurst(
                            gain.value,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    ) { Text("") }
                    ClaimChestButton(PointsClaim("c", "42", 50), claiming = false, size = 30.dp, iconSize = 16.dp, onClaim = {})
                }
            }
        }
        val harness = Harness(scene, dispatcher)
        try {
            harness.frames(5)
            gain.value = PointsGain(1, 10, PointsGainReason.WATCH)
            harness.frames(3)
            val before = changes()
            harness.frames(60)
            assertEquals(before, changes())
            assertTrue(Recomposer.runningRecomposers.value.isNotEmpty())
        } finally {
            scene.close()
        }
    }

    @Test
    fun chestTakesThePointsButtonPlaceUntilClaimed() {
        val dispatcher = StandardTestDispatcher()
        val claim = mutableStateOf<PointsClaim?>(PointsClaim("c1", "42", 50))
        var claims = 0
        var opens = 0
        var width = 0
        val scene = ImageComposeScene(width = 80, height = 80, density = Density(1f), coroutineContext = dispatcher) {
            ChatoneTheme {
                Box(Modifier.onSizeChanged { width = it.width }) {
                    PointsButton(
                        claim = claim.value,
                        claiming = false,
                        gainSeq = 0,
                        size = 32.dp,
                        iconSize = 18.dp,
                        contentDescription = "points",
                        onOpen = { opens++ },
                        onClaim = { claims++ }
                    )
                }
            }
        }
        val harness = Harness(scene, dispatcher)
        fun click() {
            scene.sendPointerEvent(PointerEventType.Press, Offset(16f, 16f))
            scene.sendPointerEvent(PointerEventType.Release, Offset(16f, 16f))
            harness.frames(5)
        }
        try {
            harness.frames(10)
            val chestWidth = width
            click()
            assertEquals(1, claims)
            assertEquals(0, opens)
            claim.value = null
            harness.frames(40)
            assertEquals(chestWidth, width)
            click()
            assertEquals(1, claims)
            assertEquals(1, opens)
        } finally {
            scene.close()
        }
    }
}
