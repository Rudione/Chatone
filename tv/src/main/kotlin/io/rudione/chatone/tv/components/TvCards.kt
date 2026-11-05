package io.rudione.chatone.tv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.browse.BrowseCategory
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.presentation.browse.BrowseFormatting
import io.rudione.chatone.tv.theme.TvDimens
import io.rudione.chatone.tv.theme.TvShapes
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.tv_live
import org.jetbrains.compose.resources.stringResource

private const val PREVIEW_RATIO = 16f / 9f
private const val BOX_ART_RATIO = 3f / 4f
private val LiveRed = Color(0xFFEB0400)

@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(
            shape = TvShapes.Card,
            focusedShape = TvShapes.CardFocused
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f, pressedScale = 0.98f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, outline), shape = TvShapes.CardFocused)
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = accent.copy(alpha = 0.55f), elevation = 18.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) { content() }
}

@Composable
fun TvLiveBadge(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.tv_live),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .background(LiveRed, TvShapes.Chip)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun TvStreamCard(
    stream: BrowseStream,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = TvDimens.StreamCardWidth,
    onFocused: () -> Unit = {}
) {
    Column(modifier.width(width), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TvFocusCard(
            onClick = onClick,
            onFocused = onFocused,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PREVIEW_RATIO)
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = stream.previewUrl,
                    contentDescription = stream.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                TvLiveBadge(Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp))
                Text(
                    BrowseFormatting.compact(stream.viewers),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.65f), TvShapes.Chip)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        TvStreamCaption(stream)
    }
}

@Composable
private fun TvStreamCaption(stream: BrowseStream) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = stream.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        Column {
            Text(
                stream.displayName.ifBlank { stream.login },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stream.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvCategoryCard(
    category: BrowseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = TvDimens.CategoryCardWidth
) {
    Column(modifier.width(width), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvFocusCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BOX_ART_RATIO)
        ) {
            AsyncImage(
                model = category.boxArtUrl,
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            category.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            BrowseFormatting.compact(category.viewers),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun TvMediaCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    width: Dp = TvDimens.StreamCardWidth
) {
    Column(modifier.width(width), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvFocusCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PREVIEW_RATIO)
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (badge != null) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.65f), TvShapes.Chip)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
