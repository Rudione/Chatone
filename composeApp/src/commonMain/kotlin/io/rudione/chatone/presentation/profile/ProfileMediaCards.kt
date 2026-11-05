package io.rudione.chatone.presentation.profile

import chatone.composeapp.generated.resources.channel_page_clipped_by
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_ago
import chatone.composeapp.generated.resources.channel_page_featured
import chatone.composeapp.generated.resources.channel_page_views
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.profile.CategoryShare
import io.rudione.chatone.domain.profile.ChannelClip
import io.rudione.chatone.domain.profile.ChannelVideo
import io.rudione.chatone.icons.lucide.Eye
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Star
import io.rudione.chatone.icons.lucide.Video
import io.rudione.chatone.presentation.chat.compactCount
import io.rudione.chatone.presentation.chat.relativeAge
import io.rudione.chatone.presentation.components.expressive.pressSquish

private val CardShape = RoundedCornerShape(14.dp)

@Composable
internal fun VideoCard(video: ChannelVideo, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp? = null) {
    MediaCard(
        imageUrl = video.thumbnailUrl,
        onClick = onClick,
        modifier = modifier,
        width = width,
        title = video.title,
        subtitle = listOfNotNull(
            video.category.takeIf { it.isNotBlank() },
            video.createdAtMs?.let { stringResource(Res.string.channel_page_ago, relativeAge(it)) }
        ).joinToString(" · ")
    ) {
        MediaPill(formatDuration(video.lengthSeconds), Modifier.align(Alignment.TopStart).padding(8.dp))
        MediaPill(
            stringResource(Res.string.channel_page_views, compactCount(video.views)),
            Modifier.align(Alignment.BottomStart).padding(8.dp),
            icon = true
        )
    }
}

@Composable
internal fun ClipCard(clip: ChannelClip, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp? = null) {
    MediaCard(
        imageUrl = clip.thumbnailUrl,
        onClick = onClick,
        modifier = modifier,
        width = width,
        title = clip.title,
        subtitle = listOfNotNull(
            clip.curator.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.channel_page_clipped_by, it) },
            clip.createdAtMs?.let { stringResource(Res.string.channel_page_ago, relativeAge(it)) }
        ).joinToString(" · ")
    ) {
        if (clip.isFeatured) FeaturedPill(Modifier.align(Alignment.TopStart).padding(8.dp))
        MediaPill(formatDuration(clip.durationSeconds), Modifier.align(Alignment.TopEnd).padding(8.dp))
        MediaPill(
            compactCount(clip.views),
            Modifier.align(Alignment.BottomStart).padding(8.dp),
            icon = true
        )
    }
}

@Composable
private fun MediaCard(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier,
    width: Dp?,
    title: String,
    subtitle: String,
    overlay: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .pressSquish(interaction)
            .clip(CardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(CardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Lucide.Video,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp).align(Alignment.Center)
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))
                )
            )
            overlay()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
internal fun MediaPill(text: String, modifier: Modifier = Modifier, icon: Boolean = false) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon) Icon(Lucide.Eye, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun FeaturedPill(modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(Lucide.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(10.dp))
        Text(
            stringResource(Res.string.channel_page_featured),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
internal fun CategoryTile(category: CategoryShare, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (category.artUrl.isNotEmpty()) {
                AsyncImage(
                    model = category.artUrl,
                    contentDescription = category.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.streams.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun formatDuration(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:$mm:$ss" else "$m:$ss"
}

internal fun formatHours(totalSeconds: Long): String {
    val tenths = totalSeconds / 360
    return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
}
