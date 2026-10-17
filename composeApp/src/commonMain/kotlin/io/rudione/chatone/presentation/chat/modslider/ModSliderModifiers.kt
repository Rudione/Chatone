package io.rudione.chatone.presentation.chat.modslider

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.moderation.ModSliderAction
import io.rudione.chatone.domain.moderation.ModSliderGeometry
import kotlinx.coroutines.CancellationException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

internal fun Modifier.modSliderRow(state: ModSliderState, style: ModSliderStyle): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.rowWidth = placeable.width.toFloat()
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0, zIndex = if (state.raised) RAISED_Z_INDEX else 0f)
        }
    }
    .drawWithContent {
        val geometry = state.geometry
        val offset = state.offset
        if (geometry != null && offset != 0f) drawTrack(state, style, geometry, offset)
        drawContent()
        if (geometry != null) drawRail(state, style, geometry, offset)
        val action = state.selected
        if (action != null && offset != 0f) drawActionLabel(style, action, offset, state.lift.value)
    }
    .graphicsLayer {
        val lift = state.lift.value
        val offset = state.offset
        val progress = state.geometry?.progress(offset) ?: 0f
        val scale = 1f + lift * (LIFT_SCALE + LIFT_SCALE_PULL * progress)
        translationX = offset
        scaleX = scale
        scaleY = scale
        shadowElevation = LIFT_SHADOW.toPx() * lift
        shape = CardShape
    }
    .drawBehind {
        val lift = state.lift.value
        if (lift > 0f) {
            drawRoundRect(
                color = style.card.copy(alpha = CARD_ALPHA * lift),
                cornerRadius = CornerRadius(CARD_RADIUS.toPx())
            )
        }
    }

internal fun Modifier.modSliderGrab(handle: ModSliderHandle): Modifier = pointerInput(handle) {
    awaitEachGesture { trackSliderGesture(handle) }
}

private suspend fun AwaitPointerEventScope.trackSliderGesture(handle: ModSliderHandle) {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val mouse = down.type == PointerType.Mouse
    if (mouse && !currentEvent.buttons.isPrimaryPressed) return
    if (handle.track.isEmpty) return
    down.consume()
    val state = handle.state
    val slop = if (mouse) 0f else viewConfiguration.touchSlop
    var pending = 0f
    var dragging = false
    try {
        if (mouse) {
            dragging = state.begin(handle.track, modSliderMetrics(), handle.binding.travelFraction)
            if (!dragging) return
        }
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null) {
                if (dragging) state.cancel()
                return
            }
            if (mouse && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                if (dragging) state.cancel()
                return
            }
            if (!change.pressed) {
                change.consume()
                if (dragging) state.release()?.let(handle.onAction)
                return
            }
            val dx = change.position.x - change.previousPosition.x
            change.consume()
            if (dragging) {
                state.dragBy(dx)
                continue
            }
            pending += dx
            if (abs(pending) <= slop) continue
            dragging = state.begin(handle.track, modSliderMetrics(), handle.binding.travelFraction)
            if (!dragging) return
            state.dragBy(pending - sign(pending) * slop)
        }
    } catch (cancelled: CancellationException) {
        if (dragging) state.cancel()
        throw cancelled
    }
}

private fun DrawScope.drawTrack(
    state: ModSliderState,
    style: ModSliderStyle,
    geometry: ModSliderGeometry,
    offset: Float
) {
    val action = state.selected
    val color = style.colorOf(action)
    val progress = geometry.progress(offset)
    val inset = TRACK_INSET.toPx()
    val radius = CARD_RADIUS.toPx()
    val alpha = if (action != null) 0.18f + 0.30f * progress else 0.05f + 0.10f * progress
    val width = min(abs(offset) + radius * 2f, size.width - inset * 2f)
    val left = if (offset > 0f) inset else size.width - inset - width
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(left, inset),
        size = Size(width, size.height - inset * 2f),
        cornerRadius = CornerRadius(radius)
    )
}

private fun DrawScope.drawActionLabel(style: ModSliderStyle, action: ModSliderAction, offset: Float, lift: Float) {
    val color = style.colorOf(action)
    val icon = LABEL_ICON.toPx()
    val gap = LABEL_GAP.toPx()
    val padX = LABEL_PAD_X.toPx()
    val padY = LABEL_PAD_Y.toPx()
    val margin = LABEL_MARGIN.toPx()
    val label = style.chipLabelOf(action)
    val text = if (label.isEmpty()) null else style.measurer.measure(label, style.labelStyle)
    val pillHeight = maxOf(icon, text?.size?.height?.toFloat() ?: 0f) + padY * 2f
    val pillWidth = if (text == null) pillHeight * ICON_PILL_RATIO else padX * 2f + icon + gap + text.size.width
    val reference = style.measurer.measure(style.chipReferenceLabel, style.labelStyle)
    val referenceWidth = maxOf(pillWidth, padX * 2f + icon + gap + reference.size.width)
    val room = abs(offset) - margin * 2f
    val crowding = ((referenceWidth - room) / referenceWidth).coerceIn(0f, 1f)
    val raise = crowding * crowding * (3f - 2f * crowding)
    val restTop = min(size.height, LABEL_LINE.toPx()) / 2f - pillHeight / 2f
    val raisedTop = -pillHeight - LABEL_RAISE_GAP.toPx()
    val top = restTop + (raisedTop - restTop) * raise
    val left = if (offset > 0f) {
        (offset - margin - pillWidth).coerceAtLeast(margin)
    } else {
        (size.width + offset + margin).coerceAtMost(size.width - margin - pillWidth)
    }
    val alpha = lift.coerceIn(0f, 1f)
    val corner = CornerRadius(pillHeight / 2f)
    drawRoundRect(style.card.copy(alpha = alpha), Offset(left, top), Size(pillWidth, pillHeight), corner)
    drawRoundRect(color.copy(alpha = 0.22f * alpha), Offset(left, top), Size(pillWidth, pillHeight), corner)
    val iconLeft = if (text == null) left + (pillWidth - icon) / 2f else left + padX
    translate(left = iconLeft, top = top + (pillHeight - icon) / 2f) {
        with(style.iconOf(action)) {
            draw(Size(icon, icon), alpha = alpha, colorFilter = ColorFilter.tint(color))
        }
    }
    if (text != null) {
        drawText(
            textLayoutResult = text,
            color = color,
            topLeft = Offset(left + padX + icon + gap, top + (pillHeight - text.size.height) / 2f),
            alpha = alpha
        )
    }
}

private fun DrawScope.drawRail(
    state: ModSliderState,
    style: ModSliderStyle,
    geometry: ModSliderGeometry,
    offset: Float
) {
    val lift = state.lift.value
    if (lift <= 0f) return
    val track = state.track
    val y = size.height - RAIL_BOTTOM.toPx()
    val stroke = RAIL_STROKE.toPx()
    val dot = RAIL_DOT.toPx()
    val activeColor = style.colorOf(state.selected)
    if (offset >= 0f || track.backward == null) {
        val end = min(geometry.travel, size.width)
        drawLine(style.neutral.copy(alpha = 0.22f * lift), Offset(0f, y), Offset(end, y), stroke, StrokeCap.Round)
        val filled = offset.coerceIn(0f, end)
        if (filled > 0f) {
            drawLine(activeColor.copy(alpha = 0.9f * lift), Offset(0f, y), Offset(filled, y), stroke, StrokeCap.Round)
        }
        track.milestones.forEach { index ->
            if (index >= geometry.stops) return@forEach
            val x = geometry.threshold(index)
            if (x > size.width) return@forEach
            val reached = offset >= x
            drawCircle(
                color = style.colorOf(track.forward[index]).copy(alpha = (if (reached) 1f else 0.45f) * lift),
                radius = dot,
                center = Offset(x, y)
            )
        }
        if (filled > 0f) {
            drawCircle(activeColor.copy(alpha = lift), radius = dot * 1.6f, center = Offset(filled, y))
        }
    } else {
        val reach = geometry.metrics.backwardReach
        val start = size.width
        drawLine(style.neutral.copy(alpha = 0.22f * lift), Offset(start, y), Offset(start - reach, y), stroke, StrokeCap.Round)
        val filled = (-offset).coerceIn(0f, reach)
        drawLine(style.unban.copy(alpha = 0.9f * lift), Offset(start, y), Offset(start - filled, y), stroke, StrokeCap.Round)
        val armed = state.selected != null
        drawCircle(
            color = style.unban.copy(alpha = (if (armed) 1f else 0.45f) * lift),
            radius = if (armed) dot * 1.5f else dot,
            center = Offset(start - reach, y)
        )
    }
}

private const val RAISED_Z_INDEX = 1f
private const val LIFT_SCALE = 0.04f
private const val LIFT_SCALE_PULL = 0.06f
private const val CARD_ALPHA = 0.97f
private val LIFT_SHADOW = 10.dp
private val CARD_RADIUS = 8.dp
private val CardShape = RoundedCornerShape(CARD_RADIUS)
private val TRACK_INSET = 1.dp
private val LABEL_ICON = 14.dp
private val LABEL_GAP = 5.dp
private val LABEL_PAD_X = 8.dp
private val LABEL_PAD_Y = 3.dp
private val LABEL_MARGIN = 6.dp
private val LABEL_RAISE_GAP = 3.dp
private const val ICON_PILL_RATIO = 1.3f
private val LABEL_LINE = 30.dp
private val RAIL_BOTTOM = 1.5.dp
private val RAIL_STROKE = 2.dp
private val RAIL_DOT = 2.5.dp
