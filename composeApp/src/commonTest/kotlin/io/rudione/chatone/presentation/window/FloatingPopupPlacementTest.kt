package io.rudione.chatone.presentation.window

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class MemoryPositionStore(var saved: Offset? = null) : FloatingPositionStore {
    override fun load(): Offset? = saved
    override fun save(fraction: Offset) {
        saved = fraction
    }
}

class FloatingPopupPlacementTest {

    private val window = IntSize(1080, 2316)
    private val popup = IntSize(800, 900)
    private val safe = SafeInsets(left = 20, top = 120, right = 20, bottom = 150)

    @Test
    fun opensAtTheAnchorPulledIntoTheSafeArea() {
        val placement = FloatingPopupPlacement(MemoryPositionStore())

        assertEquals(IntOffset(20, 120), placement.resolve(IntOffset(0, 40), window, popup, safe))
    }

    @Test
    fun dragNeverLeavesTheSafeArea() {
        val placement = FloatingPopupPlacement(MemoryPositionStore())
        placement.resolve(IntOffset(100, 300), window, popup, safe)

        placement.onDrag(Offset(5_000f, 5_000f))

        assertEquals(IntOffset(260, 1266), placement.resolve(IntOffset(100, 300), window, popup, safe))
    }

    @Test
    fun releasedPositionIsRestoredOnTheNextOpenEvenAfterRotation() {
        val store = MemoryPositionStore()
        val first = FloatingPopupPlacement(store)
        first.resolve(IntOffset(20, 120), window, popup, safe)
        first.onDrag(Offset(130f, 573f))
        first.onDragEnd()

        val reopened = FloatingPopupPlacement(store)

        assertEquals(IntOffset(150, 693), reopened.resolve(IntOffset.Zero, window, popup, safe))
        assertEquals(
            IntOffset(821, 240),
            reopened.resolve(IntOffset.Zero, IntSize(2316, 1080), IntSize(800, 600), SafeInsets(0, 0, 0, 0))
        )
    }

    @Test
    fun nothingIsStoredUntilThePopupIsDragged() {
        val store = MemoryPositionStore()

        FloatingPopupPlacement(store).onDragEnd()

        assertNull(store.saved)
    }
}
