package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SidebarDragAndDropTest {

    private val source = SidebarDragSource(login = "streamer", folderId = "favourites")
    private val pointer = PointerId(7L)

    @Test
    fun releasingAHeldItemWithoutMovingOpensItsMenu() {
        val state = SidebarDragState()
        var menuOpened = false
        state.hold(source, pointer, Offset(10f, 10f)) { menuOpened = true }
        state.moveTo(Offset(12f, 11f), touchSlop = 8f)

        assertNull(state.finish())
        assertTrue(menuOpened)
        assertNull(state.pointerId)
    }

    @Test
    fun movingAHeldItemPastTheSlopStartsADragAndDropsIt() {
        val state = SidebarDragState()
        var menuOpened = false
        state.hold(source, pointer, Offset(10f, 10f)) { menuOpened = true }
        state.moveTo(Offset(10f, 40f), touchSlop = 8f)

        assertTrue(state.isDragging)
        assertEquals(Offset(10f, 40f), state.pointInRoot)
        assertEquals(source, state.finish())
        assertFalse(menuOpened)
        assertFalse(state.isDragging)
    }

    @Test
    fun mouseDragsStartImmediatelyAndCancelLeavesNothingBehind() {
        val state = SidebarDragState()
        state.start(source, pointer, Offset(5f, 5f))
        assertTrue(state.isDragging)

        state.cancel()
        assertNull(state.source)
        assertNull(state.finish())
    }

    @Test
    fun autoScrollSpeedsUpTowardsTheEdges() {
        val edge = 50f
        val speed = 1000f
        assertEquals(0f, SidebarAutoScroll.velocity(300f, 0f, 600f, edge, speed))
        assertTrue(SidebarAutoScroll.velocity(590f, 0f, 600f, edge, speed) > SidebarAutoScroll.velocity(560f, 0f, 600f, edge, speed))
        assertTrue(SidebarAutoScroll.velocity(10f, 0f, 600f, edge, speed) < 0f)
        assertEquals(speed, SidebarAutoScroll.velocity(700f, 0f, 600f, edge, speed))
        assertEquals(0f, SidebarAutoScroll.velocity(10f, 0f, 80f, edge, speed))
    }

    @Test
    fun dropIndexPrefersFoldersOverChannelsOverTheUnfolderedSection() {
        val index = SidebarDropIndex()
        index.unfolderedSection = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 300f)
        index.putChannel("a", null, androidx.compose.ui.geometry.Rect(0f, 100f, 100f, 130f))
        index.folderRects["f"] = androidx.compose.ui.geometry.Rect(0f, 0f, 100f, 30f)

        assertEquals(SidebarDropTarget.Folder("f"), index.resolve(Offset(10f, 10f)))
        assertEquals(SidebarDropTarget.Channel("a", null), index.resolve(Offset(10f, 110f)))
        assertEquals(SidebarDropTarget.Unfoldered, index.resolve(Offset(10f, 200f)))

        index.forget("a")
        assertEquals(SidebarDropTarget.Unfoldered, index.resolve(Offset(10f, 110f)))
    }
}
