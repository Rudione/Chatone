package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class ChatFollowSceneTest {

    private val rows = mutableStateListOf<Int>().apply { repeat(INITIAL_ROWS) { add(it) } }
    private var viewportHeight by mutableIntStateOf(HEIGHT)
    private lateinit var listState: LazyListState
    private lateinit var follow: ChatFollowState
    private var frameNanos = 0L
    private var clockMillis = 0L
    private val dispatcher = StandardTestDispatcher()

    private val scene = ImageComposeScene(
        width = WIDTH,
        height = HEIGHT,
        density = Density(1f),
        coroutineContext = dispatcher
    ) {
        val state = remember { LazyListState() }
        val followState = rememberChatFollowState(state)
        listState = state
        follow = followState
        ChatFollowEffects(
            listState = state,
            newestItemKey = rows.lastOrNull(),
            paused = followState.isPausedByScroll,
            onPinnedToBottom = {}
        )
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth().height(viewportHeight.dp).nestedScroll(followState.nestedScrollConnection)
        ) {
            items(rows, key = { it }) { Box(Modifier.fillMaxWidth().height(ROW_HEIGHT.dp)) }
        }
    }

    @AfterTest
    fun tearDown() = scene.close()

    private fun frames(count: Int = 40) = repeat(count) {
        Snapshot.sendApplyNotifications()
        frameNanos += FRAME_NANOS
        clockMillis += FRAME_MILLIS
        scene.render(frameNanos)
        dispatcher.scheduler.advanceTimeBy(FRAME_MILLIS)
        dispatcher.scheduler.runCurrent()
    }

    private fun append(count: Int) {
        repeat(count) { rows.add(rows.size) }
    }

    private fun atBottom(): Boolean {
        val overflow = listState.bottomOverflow() ?: return false
        return overflow <= 0.5f
    }

    private fun wheel(notches: Float) {
        scene.sendPointerEvent(PointerEventType.Enter, CENTER, timeMillis = clockMillis)
        scene.sendPointerEvent(PointerEventType.Move, CENTER, timeMillis = clockMillis)
        scene.sendPointerEvent(
            PointerEventType.Scroll,
            CENTER,
            scrollDelta = Offset(0f, notches),
            timeMillis = clockMillis
        )
        settle()
    }

    private fun settle() {
        var rounds = 0
        do {
            frames(10)
            rounds++
        } while (listState.isScrollInProgress && rounds < MAX_SETTLE_ROUNDS)
        frames()
    }

    private fun touchDrag(distancePx: Float, steps: Int = 12) {
        var y = CENTER.y
        scene.sendPointerEvent(PointerEventType.Press, Offset(CENTER.x, y), type = PointerType.Touch, timeMillis = clockMillis)
        repeat(steps) {
            y += distancePx / steps
            clockMillis += 40
            scene.sendPointerEvent(PointerEventType.Move, Offset(CENTER.x, y), type = PointerType.Touch, timeMillis = clockMillis)
            frames(2)
        }
        clockMillis += 400
        scene.sendPointerEvent(PointerEventType.Move, Offset(CENTER.x, y), type = PointerType.Touch, timeMillis = clockMillis)
        scene.sendPointerEvent(PointerEventType.Release, Offset(CENTER.x, y), type = PointerType.Touch, timeMillis = clockMillis)
        settle()
    }

    @Test
    fun startsPinnedToTheNewestMessage() {
        frames()
        assertTrue(atBottom())
        assertFalse(follow.isPausedByScroll)
    }

    @Test
    fun burstsOfMessagesNeverKnockItOffTheBottom() {
        frames()
        repeat(6) {
            append(12)
            frames(3)
        }
        frames()
        assertTrue(atBottom())
        assertFalse(follow.isPausedByScroll)
    }

    @Test
    fun wheelingUpPastTheThresholdPausesAndIncomingMessagesDoNotPullItBack() {
        frames()
        wheel(-3f)
        assertTrue(follow.isPausedByScroll)
        val anchor = listState.firstVisibleItemIndex
        append(20)
        frames()
        assertTrue(follow.isPausedByScroll)
        assertFalse(atBottom())
        assertTrue(listState.firstVisibleItemIndex == anchor)
    }

    @Test
    fun wheelingBackDownToTheBottomResumesFollowing() {
        frames()
        wheel(-2f)
        assertTrue(follow.isPausedByScroll)
        wheel(10f)
        assertFalse(follow.isPausedByScroll)
        append(5)
        frames()
        assertTrue(atBottom())
    }

    @Test
    fun aLongDragUpPausesAndStaysPausedAfterRelease() {
        frames()
        touchDrag(distancePx = 160f)
        assertTrue(follow.isPausedByScroll)
        append(10)
        frames()
        assertTrue(follow.isPausedByScroll)
        assertFalse(atBottom())
    }

    @Test
    fun aTinyWheelNudgeSpringsBackToTheBottom() {
        frames()
        wheel(-1f)
        assertFalse(follow.isPausedByScroll)
        assertTrue(atBottom())
    }

    @Test
    fun aTinyDragSpringsBackToTheBottom() {
        frames()
        touchDrag(distancePx = TOUCH_SLOP_PX + 6f)
        assertFalse(follow.isPausedByScroll)
        assertTrue(atBottom())
    }

    @Test
    fun resumeJumpsBackToTheNewestMessage() {
        frames()
        wheel(-3f)
        append(15)
        frames()
        assertTrue(follow.isPausedByScroll)
        follow.resume()
        frames()
        assertTrue(atBottom())
        append(4)
        frames()
        assertTrue(atBottom())
    }

    @Test
    fun shrinkingTheViewportKeepsTheNewestMessageInView() {
        frames()
        viewportHeight = HEIGHT - SHRINK_PX
        frames()
        assertTrue(atBottom())
        assertFalse(follow.isPausedByScroll)
    }

    @Test
    fun shrinkingTheViewportWhilePausedKeepsTheReadingPosition() {
        frames()
        wheel(-3f)
        assertTrue(follow.isPausedByScroll)
        val anchor = listState.firstVisibleItemIndex
        viewportHeight = HEIGHT - SHRINK_PX
        frames()
        assertTrue(follow.isPausedByScroll)
        assertTrue(listState.firstVisibleItemIndex == anchor)
    }

    private companion object {
        const val WIDTH = 360
        const val HEIGHT = 480
        const val ROW_HEIGHT = 24
        const val INITIAL_ROWS = 60
        const val FRAME_MILLIS = 16L
        const val FRAME_NANOS = FRAME_MILLIS * 1_000_000L
        const val TOUCH_SLOP_PX = 18f
        const val SHRINK_PX = 300
        const val MAX_SETTLE_ROUNDS = 2_000
        val CENTER = Offset(WIDTH / 2f, HEIGHT / 2f)
    }
}
