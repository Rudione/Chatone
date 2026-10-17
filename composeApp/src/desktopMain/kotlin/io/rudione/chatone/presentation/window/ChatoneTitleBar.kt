package io.rudione.chatone.presentation.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import io.rudione.chatone.util.system.WindowsTitleBar

private val TITLE_BAR_HEIGHT = 24.dp

@Composable
fun FrameWindowScope.ChatoneTitleBar(
    title: String,
    icon: Painter?,
    background: Color,
    windowState: WindowState,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    centerContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val isDark = background.luminance() < 0.5f
    val fg = if (isDark) Color(0xFFE6E6EC) else Color(0xFF1A1A20)
    val fgDim = fg.copy(alpha = 0.7f)

    val nativeCaption = isWindowsOs
    var captionHeightPx by remember { mutableStateOf(0) }
    var maximizeRect by remember { mutableStateOf<java.awt.Rectangle?>(null) }
    var buttonsRect by remember { mutableStateOf<java.awt.Rectangle?>(null) }
    var nativeMaximizeHovered by remember { mutableStateOf(false) }

    val toggleMaximizeLatest by rememberUpdatedState(onToggleMaximize)
    SideEffect(window, nativeCaption) {
        if (nativeCaption) {
            WindowsTitleBar.setCaptionCallbacks(
                window = window,
                onToggleMaximize = { toggleMaximizeLatest() },
                onMaximizeHoverChanged = { nativeMaximizeHovered = it }
            )
        }
    }

    SideEffect(window, nativeCaption, captionHeightPx, maximizeRect, buttonsRect) {
        if (nativeCaption && captionHeightPx > 0) {
            WindowsTitleBar.setCaptionLayout(
                window = window,
                layout = WindowsTitleBar.CaptionLayout(
                    captionHeightPx = captionHeightPx,
                    maximizeButtonPx = maximizeRect,
                    interactivePx = listOfNotNull(buttonsRect)
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TITLE_BAR_HEIGHT)
            .background(background)
            .onSizeChanged { captionHeightPx = it.height }
    ) {
        val titleRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Image(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    color = fg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                if (centerContent != null) {
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) { centerContent() }
                }
            }
        }

        WindowDraggableArea(
            modifier = Modifier
                .fillMaxWidth()
                .height(TITLE_BAR_HEIGHT)
                .pointerInput(window, nativeCaption) {
                    awaitPointerEventScope {
                        var lastClick = 0L
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Press) continue
                            val now = System.currentTimeMillis()
                            if (now - lastClick < 380) {
                                lastClick = 0L
                                toggleMaximizeLatest()
                            } else {
                                lastClick = now
                                if (nativeCaption) {
                                    runCatching { WindowsTitleBar.beginWindowDrag(window) }
                                }
                            }
                        }
                    }
                }
        ) { titleRow() }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 4.dp)
                .onLayoutRectChanged { bounds -> buttonsRect = bounds.boundsInWindow.toAwtRectangle() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            trailingContent?.invoke()
            CaptionButton(
                fg = fgDim,
                hoverFg = fg,
                hoverBg = fg.copy(alpha = 0.10f),
                onClick = onMinimize,
                kind = CaptionKind.Minimize
            )
            CaptionButton(
                fg = fgDim,
                hoverFg = fg,
                hoverBg = fg.copy(alpha = 0.10f),
                onClick = onToggleMaximize,
                kind = if (windowState.placement == WindowPlacement.Maximized)
                    CaptionKind.Restore
                else
                    CaptionKind.Maximize,
                forceHovered = nativeMaximizeHovered,
                modifier = Modifier.onLayoutRectChanged { bounds ->
                    if (nativeCaption) maximizeRect = bounds.boundsInWindow.toAwtRectangle()
                }
            )
            CaptionButton(
                fg = fgDim,
                hoverFg = Color.White,
                hoverBg = Color(0xFFE81123),
                onClick = onClose,
                kind = CaptionKind.Close
            )
        }
    }
}

private enum class CaptionKind { Minimize, Maximize, Restore, Close }

@Composable
private fun CaptionButton(
    fg: Color,
    hoverFg: Color,
    hoverBg: Color,
    onClick: () -> Unit,
    kind: CaptionKind,
    modifier: Modifier = Modifier,
    forceHovered: Boolean = false,
    clickable: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val hovered = isHovered || forceHovered
    val glyphColor = if (hovered) hoverFg else fg
    Box(
        modifier = modifier
            .padding(vertical = 3.dp)
            .size(width = 28.5.dp, height = 18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (hovered) hoverBg else Color.Transparent)
            .hoverable(interaction)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(7.5.dp)) {
            val stroke = 1.2f
            val s = size
            when (kind) {
                CaptionKind.Minimize -> {
                    drawLine(
                        color = glyphColor,
                        start = Offset(0f, s.height / 2),
                        end = Offset(s.width, s.height / 2),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
                CaptionKind.Maximize -> {
                    drawRect(
                        color = glyphColor,
                        topLeft = Offset(0f, 0f),
                        size = s,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                    )
                }
                CaptionKind.Restore -> {
                    val inset = s.width * 0.25f
                    drawRect(
                        color = glyphColor,
                        topLeft = Offset(0f, inset),
                        size = androidx.compose.ui.geometry.Size(s.width - inset, s.height - inset),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                    )
                    drawRect(
                        color = glyphColor,
                        topLeft = Offset(inset, 0f),
                        size = androidx.compose.ui.geometry.Size(s.width - inset, s.height - inset),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                    )
                }
                CaptionKind.Close -> {
                    drawLine(
                        color = glyphColor,
                        start = Offset(0f, 0f),
                        end = Offset(s.width, s.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = glyphColor,
                        start = Offset(s.width, 0f),
                        end = Offset(0f, s.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun IntRect.toAwtRectangle(): java.awt.Rectangle = java.awt.Rectangle(left, top, width, height)
