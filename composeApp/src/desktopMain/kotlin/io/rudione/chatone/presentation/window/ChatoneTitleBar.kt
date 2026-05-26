package io.rudione.chatone.presentation.window

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

private val TITLE_BAR_HEIGHT = 32.dp

@Composable
fun FrameWindowScope.ChatoneTitleBar(
    title: String,
    icon: Painter?,
    background: Color,
    windowState: WindowState,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    val isDark = background.luminance() < 0.5f
    val fg = if (isDark) Color(0xFFE6E6EC) else Color(0xFF1A1A20)
    val fgDim = fg.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TITLE_BAR_HEIGHT)
            .background(background)
    ) {
        WindowDraggableArea(
            modifier = Modifier
                .fillMaxWidth()
                .height(TITLE_BAR_HEIGHT)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var lastClick = 0L
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) {
                                val now = System.currentTimeMillis()
                                if (now - lastClick < 380) {
                                    onToggleMaximize()
                                    lastClick = 0L
                                } else {
                                    lastClick = now
                                }
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Image(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    color = fg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
                    CaptionKind.Maximize
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
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val glyphColor = if (isHovered) hoverFg else fg
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(width = 38.dp, height = 24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (isHovered) hoverBg else Color.Transparent)
            .hoverable(interaction)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
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
