package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.theme.radialAura

@Composable
fun GlowSurface(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    centerX: Float = 0.5f,
    centerY: Float = 0.5f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .radialAura(
                color = dominantColor,
                centerX = centerX,
                centerY = centerY
            )
    ) {
        content()
    }
}