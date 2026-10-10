package io.rudione.chatone.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

internal const val TEXT_MIN_CONTRAST = 4.5f
internal const val ACCENT_MIN_CONTRAST = 3f

internal fun contrastBetween(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
}

internal fun readableOn(preferred: Color, background: Color, minContrast: Float = TEXT_MIN_CONTRAST): Color {
    val opaque = preferred.copy(alpha = 1f)
    if (contrastBetween(opaque, background) >= minContrast) return opaque
    val target = if (background.luminance() < DARK_BACKGROUND_LUMINANCE) Color.White else Color.Black
    var fraction = STEP
    while (fraction < 1f) {
        val candidate = lerp(opaque, target, fraction)
        if (contrastBetween(candidate, background) >= minContrast) return candidate
        fraction += STEP
    }
    return target
}

private const val DARK_BACKGROUND_LUMINANCE = 0.4f
private const val STEP = 0.2f
