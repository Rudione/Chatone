package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.util.media.TwitchClipInfo
import kotlin.time.Clock
import kotlin.time.Instant

private fun clipRelativeTime(createdAt: String): String {
    val instant = runCatching { Instant.parse(createdAt) }.getOrNull() ?: return ""
    val diffSec = (Clock.System.now() - instant).inWholeSeconds.coerceAtLeast(0)
    return when {
        diffSec < 60 -> "только что"
        diffSec < 3600 -> "${diffSec / 60}м назад"
        diffSec < 86400 -> "${diffSec / 3600}ч назад"
        diffSec < 2592000 -> "${diffSec / 86400}д назад"
        else -> "${diffSec / 2592000}мес назад"
    }
}

private fun formatClipDuration(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@Composable
fun TwitchClipCard(
    clip: TwitchClipInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailWidth: Dp = 140.dp
) {
    Row(
        modifier = modifier
            .widthIn(max = thumbnailWidth * 3)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(thumbnailWidth)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = clip.thumbnailUrl,
                contentDescription = clip.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(6.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                )
            }
            if (clip.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        formatClipDuration(clip.durationSeconds),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                clip.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!clip.gameName.isNullOrBlank()) {
                Text(
                    clip.gameName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val meta = buildString {
                if (clip.broadcasterName.isNotBlank()) append(clip.broadcasterName)
                if (clip.viewCount > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${clip.viewCount} просмотров")
                }
                val rel = clipRelativeTime(clip.createdAt)
                if (rel.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(rel)
                }
            }
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
