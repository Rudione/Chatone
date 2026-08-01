package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import io.ktor.client.HttpClient
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.luminance
import io.rudione.chatone.util.media.LinkPreview
import io.rudione.chatone.util.media.LinkPreviewCache
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun LinkHoverPopup(url: String) {
    val httpClient: HttpClient = koinInject()
    var preview by remember(url) { mutableStateOf(LinkPreviewCache.cached(url)) }

    LaunchedEffect(url) {
        if (preview == null) {
            delay(250)
            preview = LinkPreviewCache.fetch(httpClient, url)
        }
    }

    val displayUrl = if (url.length > 60) url.take(57) + "…" else url
    val p = preview

    val baseColor = MaterialTheme.colorScheme.surface
    val isDark = baseColor.luminance() < 0.5f
    val textColor = if (isDark) Color.White else Color.Black
    val textSecondary = textColor.copy(alpha = 0.75f)
    val textTertiary = textColor.copy(alpha = 0.50f)

    LiquidGlassSurface(
        shape = RoundedCornerShape(10.dp),
        glassIntensity = 0.1f,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.widthIn(min = 130.dp, max = 320.dp)
    ) {
        when {

            p == null || p.isEmpty -> {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(displayUrl, style = MaterialTheme.typography.labelSmall, color = textColor, maxLines = 1)
                }
            }

            p.mediaType == LinkPreview.MediaType.AUDIO -> {
                MediaFileCard(
                    icon = Icons.Outlined.VolumeUp,
                    iconTint = Color(0xFF43A047),
                    typeLine = buildString {
                        append("Audio")
                        if (!p.fileExtension.isNullOrBlank()) append(" (${p.fileExtension!!.uppercase()})")
                    },
                    fileName = p.title ?: displayUrl,
                    fileSize = p.fileSize,
                    textColor = textColor,
                    textSecondary = textSecondary
                )
            }

            p.mediaType == LinkPreview.MediaType.VIDEO -> {
                MediaFileCard(
                    icon = Icons.Outlined.PlayArrow,
                    iconTint = Color(0xFF1E88E5),
                    typeLine = buildString {
                        append("Video")
                        if (!p.fileExtension.isNullOrBlank()) append(" (${p.fileExtension!!.uppercase()})")
                    },
                    fileName = p.title ?: displayUrl,
                    fileSize = p.fileSize,
                    textColor = textColor,
                    textSecondary = textSecondary
                )
            }

            p.isImage -> {
                AsyncImage(
                    model = p.imageUrl ?: url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            else -> {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (!p.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = p.imageUrl,
                            contentDescription = p.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(7.dp))
                        )
                    }
                    if (!p.title.isNullOrBlank()) {
                        Text(
                            p.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (p.isYouTube) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!p.channelName.isNullOrBlank()) {
                                Text(p.channelName, style = MaterialTheme.typography.labelSmall, color = textSecondary, maxLines = 1)
                            }
                            if (!p.duration.isNullOrBlank()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        p.duration,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    val host = runCatching { io.ktor.http.Url(url).host }.getOrNull() ?: displayUrl
                    Text(
                        p.siteName?.takeIf { it.isNotBlank() } ?: host,
                        style = MaterialTheme.typography.labelSmall,
                        color = textTertiary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaFileCard(
    icon: ImageVector,
    iconTint: Color,
    typeLine: String,
    fileName: String,
    fileSize: String?,
    textColor: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconTint.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                fileName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(typeLine, style = MaterialTheme.typography.labelSmall, color = textSecondary)
                if (!fileSize.isNullOrBlank()) {
                    Text("·", style = MaterialTheme.typography.labelSmall, color = textSecondary.copy(alpha = 0.5f))
                    Text(fileSize, style = MaterialTheme.typography.labelSmall, color = textSecondary)
                }
            }
        }
    }
}
