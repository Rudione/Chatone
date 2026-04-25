package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.luminance
import io.rudione.chatone.util.LinkPreviewCache
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
    val isDarkBackground = baseColor.luminance() < 0.5f
    val textColor = if (isDarkBackground) Color.White else Color.Black
    val textColorSecondary = textColor.copy(alpha = 0.82f)
    val textColorTertiary = textColor.copy(alpha = 0.55f)

    LiquidGlassSurface(
        shape = RoundedCornerShape(10.dp),
        glassIntensity = 0.1f,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .widthIn(min = 130.dp, max = 356.dp)
            .heightIn(min = 130.dp, max = 356.dp)
    ) {
        if (p == null || p.isEmpty) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = displayUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    maxLines = 1
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(8.dp).widthIn(max = 356.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
               
                if (!p.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = p.imageUrl,
                        contentDescription = p.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                if (!p.isImage) {
                   
                    if (!p.title.isNullOrBlank()) {
                        Text(
                            text = p.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                   
                    if (p.isYouTube && !p.channelName.isNullOrBlank()) {
                        Text(
                            text = p.channelName,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColorSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                   
                    if (!p.isYouTube || p.channelName.isNullOrBlank()) {
                        val host = runCatching { io.ktor.http.Url(url).host }.getOrNull() ?: displayUrl
                        Text(
                            text = p.siteName?.takeIf { it.isNotBlank() } ?: host,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColorTertiary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}