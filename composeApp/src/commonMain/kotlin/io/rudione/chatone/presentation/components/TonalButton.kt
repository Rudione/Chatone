package io.rudione.chatone.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.util.system.handleHover

private val TonalButtonShape = RoundedCornerShape(10.dp)
private const val HOVER_LIGHTEN = 0.1f

@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    var hovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (hovered) 1.03f else 1f, tween(150), label = "tonalButtonScale")
    val container by animateColorAsState(
        if (hovered) contentColor.copy(alpha = HOVER_LIGHTEN).compositeOver(containerColor) else containerColor,
        tween(150),
        label = "tonalButtonContainer"
    )
    Box(
        modifier = modifier
            .height(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(TonalButtonShape)
            .background(container)
            .handleHover(onEnter = { hovered = true }, onExit = { hovered = false })
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
