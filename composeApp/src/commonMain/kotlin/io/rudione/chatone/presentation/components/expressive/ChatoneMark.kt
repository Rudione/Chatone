package io.rudione.chatone.presentation.components.expressive

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ChatoneMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChatoneMark",
        defaultWidth = 66.dp,
        defaultHeight = 66.dp,
        viewportWidth = 66f,
        viewportHeight = 66f
    ).apply {
        path(
            stroke = Brush.linearGradient(
                0f to Color(0xFFD9A2FF),
                0.38f to Color(0xFFA063FF),
                0.7f to Color(0xFF6D3AF0),
                1f to Color(0xFF2B7FFF),
                start = Offset(9.5f, 10.6f),
                end = Offset(58.7f, 64f)
            ),
            strokeLineWidth = 10.85f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(50.69f, 15.91f)
            arcTo(24.6f, 24.6f, 0f, true, false, 50.69f, 50.09f)
        }
    }.build()
}
