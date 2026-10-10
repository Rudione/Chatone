package io.rudione.chatone.presentation.main.components.sidebar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.ChannelFolder
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.presentation.main.MainEvent

internal sealed interface SidebarDropTarget {
    data class Folder(val folderId: String) : SidebarDropTarget
    data class Channel(val login: String, val folderId: String?) : SidebarDropTarget
    data object Unfoldered : SidebarDropTarget
}

internal data class SidebarDragSource(val login: String, val folderId: String?)

internal class SidebarDropIndex {
    val folderRects = mutableStateMapOf<String, Rect>()
    val channelRects = mutableStateMapOf<String, Rect>()
    val channelFolderIds = mutableStateMapOf<String, String?>()
    var unfolderedSection by mutableStateOf<Rect?>(null)

    fun putChannel(login: String, folderId: String?, rect: Rect) {
        channelRects[login] = rect
        channelFolderIds[login] = folderId
    }

    fun forget(login: String) {
        channelRects.remove(login)
        channelFolderIds.remove(login)
    }

    fun resolve(pointInRoot: Offset): SidebarDropTarget? {
        folderRects.entries.firstOrNull { it.value.contains(pointInRoot) }?.let {
            return SidebarDropTarget.Folder(it.key)
        }
        channelRects.entries.firstOrNull { it.value.contains(pointInRoot) }?.let {
            return SidebarDropTarget.Channel(it.key, channelFolderIds[it.key])
        }
        if (unfolderedSection?.contains(pointInRoot) == true) return SidebarDropTarget.Unfoldered
        return null
    }

    fun hoveredFolderId(pointInRoot: Offset): String? = when (val t = resolve(pointInRoot)) {
        is SidebarDropTarget.Folder -> t.folderId
        is SidebarDropTarget.Channel -> t.folderId
        else -> null
    }
}

@Stable
internal class SidebarDragState {
    var source: SidebarDragSource? by mutableStateOf(null)
        private set
    var pointInRoot: Offset by mutableStateOf(Offset.Zero)
        private set
    var isDragging: Boolean by mutableStateOf(false)
        private set

    internal var pointerId: PointerId? = null
        private set
    internal var viewport: Rect = Rect.Zero

    private var holdPoint = Offset.Zero
    private var onHoldReleased: (() -> Unit)? = null

    fun hold(source: SidebarDragSource, pointerId: PointerId, point: Offset, onReleased: () -> Unit) {
        engage(source, pointerId, point)
        onHoldReleased = onReleased
    }

    fun start(source: SidebarDragSource, pointerId: PointerId, point: Offset) {
        engage(source, pointerId, point)
        isDragging = true
    }

    fun moveTo(point: Offset, touchSlop: Float) {
        pointInRoot = point
        if (!isDragging && (point - holdPoint).getDistance() > touchSlop) {
            isDragging = true
            onHoldReleased = null
        }
    }

    fun finish(): SidebarDragSource? {
        val dropped = source.takeIf { isDragging }
        val released = onHoldReleased.takeIf { !isDragging }
        reset()
        released?.invoke()
        return dropped
    }

    fun cancel() = reset()

    private fun engage(source: SidebarDragSource, pointerId: PointerId, point: Offset) {
        this.source = source
        this.pointerId = pointerId
        holdPoint = point
        pointInRoot = point
        isDragging = false
        onHoldReleased = null
    }

    private fun reset() {
        source = null
        pointerId = null
        isDragging = false
        onHoldReleased = null
    }
}

internal object SidebarAutoScroll {
    val EdgeZone = 56.dp
    val MaxSpeedPerSecond = 1400.dp

    fun velocity(pointY: Float, top: Float, bottom: Float, edgePx: Float, maxSpeedPx: Float): Float {
        if (bottom - top <= edgePx * 2f || edgePx <= 0f) return 0f
        val towardsTop = (top + edgePx - pointY) / edgePx
        val towardsBottom = (pointY - (bottom - edgePx)) / edgePx
        return when {
            towardsTop > 0f -> -maxSpeedPx * eased(towardsTop)
            towardsBottom > 0f -> maxSpeedPx * eased(towardsBottom)
            else -> 0f
        }
    }

    private fun eased(depth: Float): Float {
        val clamped = depth.coerceIn(0f, 1f)
        return clamped * clamped
    }
}

internal suspend fun SidebarDragState.autoScroll(listState: LazyListState, density: Density) {
    val edgePx = with(density) { SidebarAutoScroll.EdgeZone.toPx() }
    val maxSpeedPx = with(density) { SidebarAutoScroll.MaxSpeedPerSecond.toPx() }
    var lastFrame = withFrameNanos { it }
    while (isDragging) {
        val frame = withFrameNanos { it }
        val seconds = (frame - lastFrame) / 1_000_000_000f
        lastFrame = frame
        val area = viewport
        val speed = SidebarAutoScroll.velocity(pointInRoot.y, area.top, area.bottom, edgePx, maxSpeedPx)
        if (speed != 0f) listState.scrollBy(speed * seconds)
    }
}

internal fun applySidebarDrop(
    target: SidebarDropTarget?,
    draggedLogin: String,
    sourceFolderId: String?,
    folders: List<ChannelFolder>,
    unfoldered: List<ChannelTab>,
    onEvent: (MainEvent) -> Unit
) {
    when (target) {
        null -> return

        is SidebarDropTarget.Folder -> {
            if (target.folderId != sourceFolderId) {
                onEvent(MainEvent.MoveChannelToFolder(draggedLogin, target.folderId))
            }
        }

        SidebarDropTarget.Unfoldered -> {
            if (sourceFolderId != null) {
                onEvent(MainEvent.MoveChannelToFolder(draggedLogin, null))
            }
        }

        is SidebarDropTarget.Channel -> {
            if (target.login.equals(draggedLogin, ignoreCase = true)) return
            if (target.folderId != sourceFolderId) {
                onEvent(MainEvent.MoveChannelToFolder(draggedLogin, target.folderId))
                return
            }
            val siblings = if (sourceFolderId == null) unfoldered
            else folders.firstOrNull { it.id == sourceFolderId }?.channels ?: return
            val from = siblings.indexOfFirst { it.login.equals(draggedLogin, ignoreCase = true) }
            val to = siblings.indexOfFirst { it.login.equals(target.login, ignoreCase = true) }
            if (from < 0 || to < 0 || from == to) return
            if (sourceFolderId == null) {
                onEvent(MainEvent.ReorderUnfolderedChannels(from, to))
            } else {
                onEvent(MainEvent.ReorderFolderChannels(sourceFolderId, from, to))
            }
        }
    }
}

internal fun Modifier.sidebarDragSource(
    state: SidebarDragState,
    source: SidebarDragSource,
    onHold: () -> Unit
): Modifier = this then SidebarDragSourceElement(state, source, onHold)

internal fun Modifier.sidebarDragTracking(
    state: SidebarDragState,
    listState: LazyListState,
    onDrop: (SidebarDragSource, Offset) -> Unit
): Modifier = this then SidebarDragTrackingElement(state, listState, onDrop)

internal fun Modifier.channelDropZone(index: SidebarDropIndex, login: String, folderId: String?): Modifier =
    this then DropZoneElement(index, DropZone.Channel(login, folderId))

internal fun Modifier.folderDropZone(index: SidebarDropIndex, folderId: String): Modifier =
    this then DropZoneElement(index, DropZone.Folder(folderId))

internal fun Modifier.unfolderedDropZone(index: SidebarDropIndex): Modifier =
    this then DropZoneElement(index, DropZone.Unfoldered)

private sealed interface DropZone {
    data class Channel(val login: String, val folderId: String?) : DropZone
    data class Folder(val folderId: String) : DropZone
    data object Unfoldered : DropZone
}

private data class DropZoneElement(
    val index: SidebarDropIndex,
    val zone: DropZone
) : ModifierNodeElement<DropZoneNode>() {
    override fun create(): DropZoneNode = DropZoneNode(index, zone)

    override fun update(node: DropZoneNode) {
        if (node.index !== index || node.zone != zone) node.forget()
        node.index = index
        node.zone = zone
    }
}

private class DropZoneNode(
    var index: SidebarDropIndex,
    var zone: DropZone
) : Modifier.Node(), GlobalPositionAwareModifierNode {

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val bounds = coordinates.boundsInRoot()
        when (val current = zone) {
            is DropZone.Channel -> index.putChannel(current.login, current.folderId, bounds)
            is DropZone.Folder -> index.folderRects[current.folderId] = bounds
            DropZone.Unfoldered -> index.unfolderedSection = bounds
        }
    }

    override fun onDetach() = forget()

    fun forget() {
        when (val current = zone) {
            is DropZone.Channel -> index.forget(current.login)
            is DropZone.Folder -> index.folderRects.remove(current.folderId)
            DropZone.Unfoldered -> index.unfolderedSection = null
        }
    }
}

private data class SidebarDragSourceElement(
    val state: SidebarDragState,
    val source: SidebarDragSource,
    val onHold: () -> Unit
) : ModifierNodeElement<SidebarDragSourceNode>() {
    override fun create(): SidebarDragSourceNode = SidebarDragSourceNode(state, source, onHold)

    override fun update(node: SidebarDragSourceNode) {
        node.state = state
        node.source = source
        node.onHold = onHold
    }
}

private class SidebarDragSourceNode(
    var state: SidebarDragState,
    var source: SidebarDragSource,
    var onHold: () -> Unit
) : DelegatingNode(), GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {

    private var originInRoot = Offset.Zero

    init {
        delegate(SuspendingPointerInputModifierNode { detectDragStart() })
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        originInRoot = coordinates.positionInRoot()
    }

    private suspend fun PointerInputScope.detectDragStart() {
        val slop = viewConfiguration.touchSlop
        val holdTimeout = viewConfiguration.longPressTimeoutMillis
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (state.pointerId != null) return@awaitEachGesture
            var latest = down.position
            val outcome = withTimeoutOrNull(holdTimeout) {
                awaitEndOrMove(down, slop) { latest = it }
            }
            when {
                outcome == null -> {
                    currentValueOf(LocalHapticFeedback).performHapticFeedback(HapticFeedbackType.LongPress)
                    state.hold(source, down.id, originInRoot + latest, onHold)
                }
                outcome is PressOutcome.Moved && down.type == PointerType.Mouse -> {
                    outcome.change.consume()
                    state.start(source, down.id, originInRoot + outcome.change.position)
                }
            }
        }
    }
}

private sealed interface PressOutcome {
    data object Ended : PressOutcome
    class Moved(val change: PointerInputChange) : PressOutcome
}

private suspend fun AwaitPointerEventScope.awaitEndOrMove(
    down: PointerInputChange,
    slop: Float,
    onPosition: (Offset) -> Unit
): PressOutcome {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return PressOutcome.Ended
        if (!change.pressed || change.isConsumed) return PressOutcome.Ended
        onPosition(change.position)
        if ((change.position - down.position).getDistance() > slop) return PressOutcome.Moved(change)
    }
}

private data class SidebarDragTrackingElement(
    val state: SidebarDragState,
    val listState: LazyListState,
    val onDrop: (SidebarDragSource, Offset) -> Unit
) : ModifierNodeElement<SidebarDragTrackingNode>() {
    override fun create(): SidebarDragTrackingNode = SidebarDragTrackingNode(state, listState, onDrop)

    override fun update(node: SidebarDragTrackingNode) {
        node.state = state
        node.listState = listState
        node.onDrop = onDrop
    }
}

private class SidebarDragTrackingNode(
    var state: SidebarDragState,
    var listState: LazyListState,
    var onDrop: (SidebarDragSource, Offset) -> Unit
) : DelegatingNode(), GlobalPositionAwareModifierNode {

    private var originInRoot = Offset.Zero

    init {
        delegate(SuspendingPointerInputModifierNode { trackDrag() })
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        originInRoot = coordinates.positionInRoot()
        state.viewport = coordinates.boundsInRoot()
    }

    override fun onDetach() {
        state.cancel()
    }

    private suspend fun PointerInputScope.trackDrag() {
        val slop = viewConfiguration.touchSlop
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val dragPointer = state.pointerId ?: continue
                if (event.type == PointerEventType.Scroll) continue
                val dragChange = event.changes.firstOrNull { it.id == dragPointer }
                if (dragChange == null) {
                    if (event.changes.none { it.pressed }) state.cancel()
                    continue
                }
                val point = originInRoot + dragChange.position
                state.moveTo(point, slop)
                dragChange.consume()
                if (!dragChange.pressed) {
                    state.finish()?.let { dropped -> onDrop(dropped, point) }
                    continue
                }
                event.changes.forEach { change ->
                    if (change.id == dragPointer) return@forEach
                    if (change.pressed && change.previousPressed && change.positionChanged()) {
                        listState.dispatchRawDelta(-change.positionChange().y)
                    }
                    change.consume()
                }
            }
        }
    }
}
