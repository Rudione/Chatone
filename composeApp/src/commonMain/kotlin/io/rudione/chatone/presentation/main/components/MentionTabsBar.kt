package io.rudione.chatone.presentation.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.presentation.chat.adjustReadableColor
import io.rudione.chatone.presentation.chat.parseColor
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private data class MentionTabData(val login: String, val count: Int, val latest: MentionEntry)

@Composable
fun MentionTabsBar(
    mentions: List<MentionEntry>,
    activeLogin: String?,
    onSelect: (login: String, messageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channels = remember(mentions) {
        mentions.asSequence()
            .filter { !it.isRead }
            .groupBy { it.channelLogin.lowercase() }
            .map { (login, list) ->
                MentionTabData(login, list.size, list.maxBy { it.timestamp })
            }
            .sortedByDescending { it.latest.timestamp }
    }
    if (channels.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Outlined.AlternateEmail,
            contentDescription = LocalStrings.current.mentionTabsTitle,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(channels, key = { it.login }) { tab ->
                MentionTab(
                    data = tab,
                    active = tab.login.equals(activeLogin, ignoreCase = true),
                    onClick = { onSelect(tab.login, tab.latest.messageId) }
                )
            }
        }
    }
}

@Composable
private fun MentionTab(data: MentionTabData, active: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val surfaceBg = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val senderColor = remember(data.latest.fromColor, surfaceBg) {
        parseColor(data.latest.fromColor)
    }?.let { adjustReadableColor(it, surfaceBg) } ?: MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (active) accent.copy(alpha = 0.20f) else accent.copy(alpha = 0.10f),
        modifier = Modifier
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${data.login}",
                style = MaterialTheme.typography.labelSmall,
                color = if (active) accent else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (data.count > 1) {
                Spacer(Modifier.width(4.dp))
                Surface(color = accent, shape = CircleShape) {
                    Text(
                        if (data.count > 99) "99+" else data.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                    )
                }
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = senderColor, fontWeight = FontWeight.Medium)) {
                        append(data.latest.fromDisplayName)
                    }
                    withStyle(SpanStyle(color = onSurfaceVariant)) {
                        append(": ")
                    }
                    append(data.latest.text)
                },
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
