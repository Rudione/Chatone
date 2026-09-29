package io.rudione.chatone.util.emote

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SharedEmoteAnimatorTest {

    private fun frames(vararg durations: Int) = AnimatedFrames(
        frames = durations.map { ImageBitmap(1, 1) },
        durations = durations,
        byteSize = durations.size * 4L
    )

    @Test
    fun frameFollowsTheLoopTimeline() {
        val player = EmotePlayer(frames(100, 50, 200))
        assertEquals(0, player.frameAt(0))
        assertEquals(0, player.frameAt(99))
        assertEquals(1, player.frameAt(100))
        assertEquals(1, player.frameAt(149))
        assertEquals(2, player.frameAt(150))
        assertEquals(2, player.frameAt(349))
        assertEquals(0, player.frameAt(350))
        assertEquals(2, player.frameAt(-1))
    }

    @Test
    fun nextChangeLandsOnTheFrameBoundary() {
        val player = EmotePlayer(frames(100, 50, 200))
        assertEquals(100, player.nextChangeAt(0))
        assertEquals(150, player.nextChangeAt(120))
        assertEquals(350, player.nextChangeAt(349))
        assertEquals(450, player.nextChangeAt(350))
    }

    @Test
    fun copiesOfOneEmoteShareAFrameAtTheSameMoment() {
        val data = frames(40, 60, 80, 20)
        val first = EmotePlayer(data)
        val second = EmotePlayer(data)
        (0L..2_000L step 7).forEach { time ->
            assertEquals(first.frameAt(time), second.frameAt(time))
        }
    }

    @Test
    fun singleFrameNeverSchedulesWork() {
        val player = EmotePlayer(frames(100))
        assertFalse(player.isAnimated)
        assertEquals(0, player.frameAt(12_345))
        assertEquals(Long.MAX_VALUE, player.nextChangeAt(12_345))
    }

    @Test
    fun playerIsSharedWhileHeldAndDroppedAfterRelease() {
        val data = frames(100)
        val first = SharedEmoteAnimator.playerFor(data)
        assertSame(first, SharedEmoteAnimator.playerFor(data))
        SharedEmoteAnimator.retain(first)
        assertSame(first, SharedEmoteAnimator.playerFor(data))
        SharedEmoteAnimator.release(first)
        val second = SharedEmoteAnimator.playerFor(data)
        assertNotSame(first, second)
        assertTrue(second.holders == 0)
        SharedEmoteAnimator.retain(second)
        SharedEmoteAnimator.release(second)
    }
}
