package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.rudione.chatone.di.UntrustedContentClientQualifier
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.luminance
import io.rudione.chatone.util.media.LinkPreview
import io.rudione.chatone.util.media.LinkPreviewCache
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Volume2
import io.ktor.http.Url

@Stable
internal class LinkHoverPreviewState {
    var url by mutableStateOf<String?>(null)
    var offset by mutableStateOf(IntOffset.Zero)
    var popupHovered by mutableStateOf(false)
}

@Composable
internal fun rememberLinkHoverPreview(url: String?, offset: IntOffset): LinkHoverPreviewState {
    val state = remember { LinkHoverPreviewState() }
    SideEffect {
        if (url != null) {
            state.url = url
            state.offset = offset
        }
    }
    LaunchedEffect(url, state.popupHovered) {
        if (url == null && !state.popupHovered) {
            delay(350)
            state.url = null
        }
    }
    return state
}

@Composable
fun LinkHoverPopup(url: String, onHoverChanged: (Boolean) -> Unit = {}) {
    val hoverChanged by rememberUpdatedState(onHoverChanged)
    val httpClient: HttpClient = koinInject(UntrustedContentClientQualifier)
    var preview by remember(url) { mutableStateOf(LinkPreviewCache.cached(url)) }

    LaunchedEffect(url) {
        if (preview == null) {
            delay(250.milliseconds)
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
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hoverChanged(true)
                            PointerEventType.Exit -> hoverChanged(false)
                        }
                    }
                }
            }
    ) {
        when {

            p == null || p.isEmpty -> {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Lucide.Link,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(displayUrl, style = MaterialTheme.typography.labelSmall, color = textColor, maxLines = 1)
                }
            }

            p.mediaType == LinkPreview.MediaType.AUDIO -> {
                MediaFileCard(
                    icon = Lucide.Volume2,
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
                    icon = Lucide.Play,
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
                ExpandableImagePreview(
                    imageUrl = p.imageUrl ?: url,
                    description = p.title ?: displayUrl,
                    contentScale = ContentScale.Fit,
                    imageModifier = Modifier
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
                        ExpandableImagePreview(
                            imageUrl = p.imageUrl,
                            description = p.title ?: displayUrl,
                            contentScale = ContentScale.Crop,
                            imageModifier = Modifier
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

                    val host = runCatching { Url(url).host }.getOrNull() ?: displayUrl
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
