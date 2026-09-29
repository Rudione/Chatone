package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.remote.NameHistoryEntry
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.delay

private const val COPY_FEEDBACK_MS = 1_400L

@Composable
internal fun ProfileAkaChip(names: List<NameHistoryEntry>) {
    if (names.isEmpty()) return
    val insights = LocalStrings.current.insights
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = 3.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("aka", style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic, color = muted.copy(alpha = 0.5f))
            Text(
                names.first().login,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = muted.copy(alpha = 0.8f),
                maxLines = 1
            )
            if (names.size > 1) {
                Text(
                    "+${names.size - 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
        ChatoneDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            Text(
                insights.akaTitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = muted.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            names.forEach { entry ->
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text(entry.login, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    val from = entry.firstSeenMs?.let(::formatDate)
                    val to = entry.lastSeenMs?.let(::formatDate)
                    if (from != null && to != null) {
                        Text(
                            insights.akaSeen(from, to),
                            style = MaterialTheme.typography.labelSmall,
                            color = muted.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileColorChip(hex: String?) {
    val color = parseHexColor(hex) ?: return
    val insights = LocalStrings.current.insights
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPY_FEEDBACK_MS)
            copied = false
        }
    }
    LiquidGlassTooltipBox(tooltip = insights.nameColor) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(hex.orEmpty().uppercase()))
                    copied = true
                }
                .padding(horizontal = 3.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (copied) {
                Icon(Lucide.Check, contentDescription = null, modifier = Modifier.size(9.dp), tint = MaterialTheme.colorScheme.primary)
            } else {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            }
            Text(
                hex.orEmpty().uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
