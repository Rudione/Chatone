package io.rudione.chatone.presentation.settings.sound

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.sound.SoundEdit
import io.rudione.chatone.domain.sound.SoundLimits
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private enum class TrimHandle { Start, End, Window }

private val EDGE_INSET = 9.dp

@Composable
internal fun SoundTrimmer(
    peaks: List<Float>,
    durationMs: Long,
    edit: SoundEdit,
    progress: () -> Float?,
    onChange: (SoundEdit) -> Unit,
    modifier: Modifier = Modifier
) {
    var working by remember { mutableStateOf(edit) }
    var active by remember { mutableStateOf<TrimHandle?>(null) }
    LaunchedEffect(edit) { if (active == null) working = edit }
    val change by rememberUpdatedState(onChange)
    val startGrip by animateFloatAsState(if (active == TrimHandle.Start) 1.3f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "startGrip")
    val endGrip by animateFloatAsState(if (active == TrimHandle.End) 1.3f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "endGrip")
    val windowLift by animateFloatAsState(if (active == TrimHandle.Window) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "windowLift")
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
    val playhead = MaterialTheme.colorScheme.onSurface
    val total = durationMs.coerceAtLeast(1L)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .pointerInput(total) {
                val reach = 26.dp.toPx()
                val inset = EDGE_INSET.toPx()
                var carry = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        carry = 0f
                        val track = size.width - inset * 2
                        active = pick(offset.x - inset, track, working, total, reach)
                        if (active != TrimHandle.Window) {
                            working = moveEdge(working, active, msAt(offset.x - inset, track, total), total)
                            change(working)
                        }
                    },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                    onHorizontalDrag = { pointer, dx ->
                        pointer.consume()
                        val exact = dx / (size.width - inset * 2).coerceAtLeast(1f) * total + carry
                        val deltaMs = exact.toLong()
                        carry = exact - deltaMs
                        working = when (active) {
                            TrimHandle.Window -> shiftWindow(working, deltaMs, total)
                            TrimHandle.Start -> moveEdge(working, TrimHandle.Start, working.startMs + deltaMs, total)
                            TrimHandle.End -> moveEdge(working, TrimHandle.End, working.endMs + deltaMs, total)
                            null -> working
                        }
                        change(working)
                    }
                )
            }
            .pointerInput(total) {
                val inset = EDGE_INSET.toPx()
                detectTapGestures { offset ->
                    val ms = msAt(offset.x - inset, size.width - inset * 2, total)
                    val handle = if (abs(ms - working.startMs) <= abs(ms - working.endMs)) TrimHandle.Start else TrimHandle.End
                    working = moveEdge(working, handle, ms, total)
                    change(working)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = EDGE_INSET)) {
            val startX = working.startMs.toFloat() / total * size.width
            val endX = working.endMs.toFloat() / total * size.width
            drawRoundRect(
                color = primary.copy(alpha = 0.10f + 0.06f * windowLift),
                topLeft = Offset(startX, 0f),
                size = Size((endX - startX).coerceAtLeast(1f), size.height),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            drawBars(peaks, working, total, primary, idle)
            val head = progress()
            if (head != null) {
                val x = startX + (endX - startX) * head
                drawLine(playhead.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 8.dp.toPx())
                drawLine(playhead, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
            }
            drawGrip(startX, startGrip, primary)
            drawGrip(endX, endGrip, primary)
        }
    }
}

private fun DrawScope.drawBars(peaks: List<Float>, edit: SoundEdit, total: Long, active: Color, idle: Color) {
    if (peaks.isEmpty()) return
    val count = peaks.size
    val slot = size.width / count
    val bar = max(1f, slot * 0.62f)
    val fadeIn = edit.fadeInMs.toFloat() / total * size.width
    val fadeOut = edit.fadeOutMs.toFloat() / total * size.width
    val startX = edit.startMs.toFloat() / total * size.width
    val endX = edit.endMs.toFloat() / total * size.width
    val usable = size.height * 0.84f
    for (i in 0 until count) {
        val x = i * slot + (slot - bar) / 2f
        val center = x + bar / 2f
        val inside = center in startX..endX
        val envelope = when {
            !inside -> 1f
            fadeIn > 0f && center < startX + fadeIn -> 0.3f + 0.7f * ((center - startX) / fadeIn)
            fadeOut > 0f && center > endX - fadeOut -> 0.3f + 0.7f * ((endX - center) / fadeOut)
            else -> 1f
        }.coerceIn(0.3f, 1f)
        val h = max(bar, (0.06f + 0.94f * sqrt(peaks[i].coerceIn(0f, 1f))) * usable * if (inside) envelope else 1f)
        drawRoundRect(
            color = if (inside) active else idle,
            topLeft = Offset(x, (size.height - h) / 2f),
            size = Size(bar, h),
            cornerRadius = CornerRadius(bar / 2f)
        )
    }
}

private fun DrawScope.drawGrip(x: Float, scale: Float, color: Color) {
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    val width = 10.dp.toPx() * scale
    val height = 38.dp.toPx() * scale
    drawRoundRect(
        color = color,
        topLeft = Offset(x - width / 2f, (size.height - height) / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(width / 2f)
    )
    val notch = 3.dp.toPx()
    drawLine(
        Color.White.copy(alpha = 0.85f),
        Offset(x, size.height / 2f - notch * 2),
        Offset(x, size.height / 2f + notch * 2),
        strokeWidth = 1.5.dp.toPx()
    )
}

private fun msAt(x: Float, width: Float, total: Long): Long =
    (x / width.coerceAtLeast(1f) * total).toLong().coerceIn(0L, total)

private fun pick(x: Float, width: Float, edit: SoundEdit, total: Long, reach: Float): TrimHandle {
    val startX = edit.startMs.toFloat() / total * width
    val endX = edit.endMs.toFloat() / total * width
    val toStart = abs(x - startX)
    val toEnd = abs(x - endX)
    return when {
        toStart <= reach || toEnd <= reach -> if (toStart <= toEnd) TrimHandle.Start else TrimHandle.End
        x in startX..endX -> TrimHandle.Window
        x < startX -> TrimHandle.Start
        else -> TrimHandle.End
    }
}

private fun moveEdge(edit: SoundEdit, handle: TrimHandle?, ms: Long, total: Long): SoundEdit = when (handle) {
    TrimHandle.Start -> {
        val start = ms.coerceIn(0L, (edit.endMs - SoundLimits.MIN_CLIP_MS).coerceAtLeast(0L))
        val end = min(edit.endMs, start + SoundLimits.MAX_CLIP_MS)
        edit.copy(startMs = start, endMs = end)
    }
    TrimHandle.End -> {
        val end = ms.coerceIn(edit.startMs + SoundLimits.MIN_CLIP_MS, total)
        val start = max(edit.startMs, end - SoundLimits.MAX_CLIP_MS)
        edit.copy(startMs = start, endMs = end)
    }
    else -> edit
}

private fun shiftWindow(edit: SoundEdit, deltaMs: Long, total: Long): SoundEdit {
    val length = edit.endMs - edit.startMs
    val start = (edit.startMs + deltaMs).coerceIn(0L, (total - length).coerceAtLeast(0L))
    return edit.copy(startMs = start, endMs = start + length)
}
