package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Stable
class PopupPortal internal constructor() {
    internal val entries = mutableStateListOf<PortalEntry>()
    internal var origin: IntOffset = IntOffset.Zero
}

val LocalPopupPortal = staticCompositionLocalOf<PopupPortal?> { null }

@Composable
fun PopupPortalHost(content: @Composable () -> Unit) {
    val portal = remember { PopupPortal() }
    CompositionLocalProvider(LocalPopupPortal provides portal) {
        content()
        PortalLayer(portal)
    }
}

@Composable
fun PortalPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit
) {
    val portal = LocalPopupPortal.current
    if (portal == null) {
        Popup(popupPositionProvider, onDismissRequest, properties, content)
        return
    }
    val locals = currentCompositionLocalContext
    val entry = remember(portal) { PortalEntry() }
    SideEffect { entry.update(popupPositionProvider, onDismissRequest, properties, locals, content) }
    DisposableEffect(portal, entry) {
        portal.entries += entry
        onDispose { portal.entries -= entry }
    }
    Layout(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            val parent = coordinates.parentLayoutCoordinates ?: coordinates
            val bounds = IntRect(parent.positionInWindow().round(), parent.size)
            if (entry.anchor != bounds) entry.anchor = bounds
        }
    ) { _, _ -> layout(0, 0) {} }
}

@Composable
fun PortalPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset.Zero,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit
) {
    val provider = remember(alignment, offset) { AlignedPositionProvider(alignment, offset) }
    PortalPopup(provider, onDismissRequest, properties, content)
}

@Stable
internal class PortalEntry {
    var anchor: IntRect? by mutableStateOf(null)
    private var positionProvider: PopupPositionProvider? by mutableStateOf(null)
    private var onDismissRequest: (() -> Unit)? by mutableStateOf(null)
    private var properties: PopupProperties by mutableStateOf(PopupProperties())
    private var locals: CompositionLocalContext? by mutableStateOf(null)
    private var content: (@Composable () -> Unit)? by mutableStateOf(null)

    fun update(
        positionProvider: PopupPositionProvider,
        onDismissRequest: (() -> Unit)?,
        properties: PopupProperties,
        locals: CompositionLocalContext,
        content: @Composable () -> Unit
    ) {
        this.positionProvider = positionProvider
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        this.locals = locals
        this.content = content
    }

    @Composable
    fun Render(portal: PopupPortal) {
        val bounds = anchor ?: return
        val provider = positionProvider ?: return
        val context = locals ?: return
        val body = content ?: return
        val anchored = remember(portal, bounds, provider) { AnchoredPositionProvider(portal, bounds, provider) }
        Popup(anchored, onDismissRequest, properties) {
            CompositionLocalProvider(context) { body() }
        }
    }
}

@Composable
private fun PortalLayer(portal: PopupPortal) {
    Layout(
        modifier = Modifier.onPlaced { coordinates ->
            portal.origin = (coordinates.parentCoordinates ?: coordinates).positionInWindow().round()
        }
    ) { _, _ -> layout(0, 0) {} }
    portal.entries.toList().forEach { entry -> key(entry) { entry.Render(portal) } }
}

private class AnchoredPositionProvider(
    private val portal: PopupPortal,
    private val anchor: IntRect,
    private val delegate: PopupPositionProvider
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val insetShift = anchorBounds.topLeft - portal.origin
        return delegate.calculatePosition(anchor.translate(insetShift), windowSize, layoutDirection, popupContentSize)
    }
}

private class AlignedPositionProvider(
    private val alignment: Alignment,
    private val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val anchorPoint = alignment.align(IntSize.Zero, anchorBounds.size, layoutDirection)
        val popupPoint = alignment.align(IntSize.Zero, popupContentSize, layoutDirection)
        val direction = if (layoutDirection == LayoutDirection.Ltr) 1 else -1
        return anchorBounds.topLeft + anchorPoint - popupPoint + IntOffset(offset.x * direction, offset.y)
    }
}
