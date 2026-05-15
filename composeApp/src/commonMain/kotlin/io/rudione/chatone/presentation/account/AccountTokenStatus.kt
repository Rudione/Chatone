package io.rudione.chatone.presentation.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color


@Composable
fun AccountTokenStatusDot(
    isValid: Boolean,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val color = if (isValid) Color(0xFF4CAF50) else Color(0xFFE53935)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
