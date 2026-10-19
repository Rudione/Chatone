package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.dto.ChannelData
import io.rudione.chatone.icons.lucide.Eye
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.presentation.stream.StreamFormatting
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private val PreviewShape = RoundedCornerShape(8.dp)
private val ChipShape = RoundedCornerShape(6.dp)
private const val MATURE_LABEL = "18+"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StreamInfoContent(
    liveStream: ChannelData,
    showPreview: Boolean,
    footer: String?,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val content = LocalContentColor.current
    val muted = content.copy(alpha = 0.72f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val preview = liveStream.thumbnailUrl.replace("{width}", "320").replace("{height}", "180")
        if (showPreview && preview.isNotBlank()) {
            AsyncImage(
                model = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(PreviewShape)
            )
        }
        Text(
            liveStream.title.ifBlank { liveStream.userName },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (liveStream.gameName.isNotBlank()) {
            Text(
                liveStream.gameName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            if (liveStream.viewerCount > 0) {
                InfoItem(Lucide.Eye, StreamFormatting.compactCount(liveStream.viewerCount, s.player.decimalSeparator), muted)
            }
            val uptime = formatStreamUptime(liveStream.startedAt, s.unitHourShort, s.unitMinuteShort)
            if (uptime.isNotEmpty()) InfoItem(Lucide.Timer, uptime, muted)
            if (liveStream.language.isNotBlank()) InfoChip(liveStream.language.uppercase(), muted)
            if (liveStream.isMature) InfoChip(MATURE_LABEL, muted)
        }
        if (footer != null) {
            Text(footer, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun InfoItem(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun InfoChip(text: String, tint: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, tint.copy(alpha = 0.4f), ChipShape)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}
