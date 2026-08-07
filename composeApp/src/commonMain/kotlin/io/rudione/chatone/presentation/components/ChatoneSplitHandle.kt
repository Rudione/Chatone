package io.rudione.chatone.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.theme.ChatoneTheme

@Composable
fun ChatoneSplitHandle(
    onDelta: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    onDragEnd: () -> Unit = {},
    onTearOut: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val currentDelta by rememberUpdatedState(onDelta)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    val currentTearOut by rememberUpdatedState(onTearOut)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint by animateColorAsState(
        if (hovered) ChatoneTheme.extraColors.accentGradient.first().copy(alpha = 0.85f)
        else Color.Transparent,
        label = "splitHandle"
    )

    Box(
        modifier = modifier
            .width(ChatoneTileDefaults.gap)
            .fillMaxHeight()
            .hoverable(interaction)
            .pointerInput(onTearOut != null) {
                if (currentTearOut != null) {
                    detectTapGestures(onLongPress = { currentTearOut?.invoke() })
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { currentDragEnd() },
                    onDragCancel = { currentDragEnd() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        currentDelta(with(density) { dragAmount.toDp() })
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(tint)
        )
    }
}
