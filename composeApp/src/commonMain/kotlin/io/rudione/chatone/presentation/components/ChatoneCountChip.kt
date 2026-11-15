package io.rudione.chatone.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.theme.ChatoneTheme
import androidx.compose.ui.graphics.Color

enum class CountChipTone { Mention, Quiet, Silent }

@Composable
fun ChatoneCountChip(
    count: Int,
    modifier: Modifier = Modifier,
    tone: CountChipTone = CountChipTone.Mention,
    max: Int = 999,
    dense: Boolean = false
) {
    if (count <= 0) return
    val mention = ChatoneTheme.colorTokens.mentionAccent
    val accent = Color(mention)

    val background = when (tone) {
        CountChipTone.Mention -> accent.copy(alpha = 0.17f)
        CountChipTone.Quiet -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
        CountChipTone.Silent -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    }
    val content = when (tone) {
        CountChipTone.Mention -> accent
        CountChipTone.Quiet -> MaterialTheme.colorScheme.onSurfaceVariant
        CountChipTone.Silent -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .defaultMinSize(minWidth = if (dense) 15.dp else 18.dp)
            .padding(horizontal = if (dense) 4.dp else 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (count > max) "$max+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (dense) 9.sp else 10.sp,
                fontFeatureSettings = "tnum"
            ),
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
fun ChatoneCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    max: Int = 9
) {
    if (count <= 0) return
    val accent = Color(ChatoneTheme.colorTokens.mentionAccent)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 13.dp, minHeight = 13.dp)
            .clip(CircleShape)
            .background(accent)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (count > max) "$max+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                fontFeatureSettings = "tnum"
            ),
            fontWeight = FontWeight.Bold,
            color = onAccentColor(accent)
        )
    }
}

private fun onAccentColor(accent: Color): Color =
    if (accent.luminance() > 0.55f) Color(0xFF1A1420)
    else Color.White
