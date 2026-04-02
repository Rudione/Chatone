package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.rudione.chatone.presentation.theme.radialAura // <- импортируем правильно

@Composable
fun GlowSurface(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 1f, // можно оставить для будущей прокачки
    centerX: Float = 0.5f,
    centerY: Float = 0.5f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .radialAura( // <- используем реальное имя функции
                color = dominantColor,
                centerX = centerX,
                centerY = centerY
            )
    ) {
        content()
    }
}