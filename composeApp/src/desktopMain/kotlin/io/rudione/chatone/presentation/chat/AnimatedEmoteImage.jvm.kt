package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.theme.ChatoneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import java.net.URI

// ─── Frame cache ─────────────────────────────────────────────────────────

private val animatedCache = mutableMapOf<String, AnimatedFrames>()

private data class AnimatedFrames(
    val frames: List<ImageBitmap>,
    val durations: IntArray
)

// ─── Public API ──────────────────────────────────────────────────────────

/**
 * Simple animated image without tooltip or interaction.
 * Used for badges, emote picker, reply bar, etc.
 */
@Composable
actual fun AnimatedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier
) {
    AnimatedEmoteImageCore(url = url, contentDescription = contentDescription, modifier = modifier)
}

/**
 * Third-party emote in chat messages.
 * - Hover: shows tooltip with preview, name, original name, author, provider
 * - Right-click: opens 7TV page in browser (for 7TV emotes) + triggers context menu
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun EmoteImageWithTooltip(
    emote: GenericEmote,
    modifier: Modifier,
    onShowContextMenu: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current

    TooltipArea(
        tooltip = { EmoteTooltip(emote = emote) },
        delayMillis = 500
    ) {
        AnimatedEmoteImageCore(
            url = emote.url2x.ifEmpty { emote.url1x },
            contentDescription = emote.code,
            modifier = modifier
                .onPointerEvent(PointerEventType.Press) { event ->
                    if (event.buttons.isSecondaryPressed) {
                        if (emote.provider == EmoteProvider.SEVEN_TV && emote.id.isNotEmpty()) {
                            try {
                                uriHandler.openUri("https://7tv.app/emotes/${emote.id}")
                            } catch (_: Exception) {}
                        }
                        onShowContextMenu?.invoke()
                    }
                }
        )
    }
}

// ─── Tooltip — liquid glass Material 3 style ─────────────────────────────

@Composable
fun EmoteTooltip(emote: GenericEmote) {
    val extra = ChatoneTheme.extraColors

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        extra.sidebarSurface.copy(alpha = 0.92f),
                        extra.sidebarSurface.copy(alpha = 0.80f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Large emote preview (3x for crispness)
            AnimatedEmoteImageCore(
                url = emote.url3x.ifEmpty { emote.url2x.ifEmpty { emote.url1x } },
                contentDescription = emote.code,
                modifier = Modifier.sizeIn(
                    minWidth = 80.dp,
                    minHeight = 80.dp,
                    maxWidth = 160.dp,
                    maxHeight = 160.dp
                )
            )

            // Channel alias — how user types it
            Text(
                text = emote.code,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Original name row — only if different from alias
            if (emote.originalName.isNotEmpty() && emote.originalName != emote.code) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Original:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                        Text(
                            text = emote.originalName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Author row
            if (emote.authorName.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "By",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = emote.authorName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Provider chip
            val (providerLabel, providerColor) = when (emote.provider) {
                EmoteProvider.SEVEN_TV -> "7TV" to Color(0xFF0288D1)
                EmoteProvider.BTTV -> "BTTV" to Color(0xFF43A047)
                EmoteProvider.FFZ -> "FFZ" to Color(0xFF8E24AA)
                EmoteProvider.TWITCH -> "Twitch" to Color(0xFF9146FF)
            }
            Surface(
                color = providerColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = providerLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = providerColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Right-click hint for 7TV
            if (emote.provider == EmoteProvider.SEVEN_TV) {
                Text(
                    text = "Right-click to open on 7TV",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

// ─── Core renderer ───────────────────────────────────────────────────────

@Composable
fun AnimatedEmoteImageCore(
    url: String,
    contentDescription: String?,
    modifier: Modifier
) {
    var animData by remember(url) { mutableStateOf(animatedCache[url]) }
    var isStatic by remember(url) { mutableStateOf(false) }
    var currentFrame by remember(url) { mutableIntStateOf(0) }

    LaunchedEffect(url) {
        if (animData != null) return@LaunchedEffect
        if (isStatic) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                val cached = animatedCache[url]
                if (cached != null) {
                    animData = cached
                    return@withContext
                }

                val bytes = URI(url).toURL().readBytes()
                val data = Data.makeFromBytes(bytes)
                val codec = Codec.makeFromData(data)

                if (codec.frameCount > 1) {
                    val infos = codec.framesInfo
                    val durations = IntArray(codec.frameCount) { i ->
                        infos[i].duration.coerceAtLeast(16)
                    }
                    val frames = (0 until codec.frameCount).map { i ->
                        val bmp = Bitmap()
                        bmp.allocPixels(codec.imageInfo)
                        codec.readPixels(bmp, i)
                        bmp.toComposeImageBitmap()
                    }
                    val result = AnimatedFrames(frames, durations)
                    animatedCache[url] = result
                    animData = result
                } else {
                    isStatic = true
                }
            } catch (_: Exception) {
                isStatic = true
            }
        }
    }

    val data = animData
    if (data != null && data.frames.isNotEmpty()) {
        LaunchedEffect(data) {
            while (true) {
                delay(data.durations[currentFrame].toLong())
                currentFrame = (currentFrame + 1) % data.frames.size
            }
        }
        Image(
            bitmap = data.frames[currentFrame],
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

private fun Bitmap.toComposeImageBitmap(): ImageBitmap {
    val image = org.jetbrains.skia.Image.makeFromBitmap(this)
    return image.toComposeImageBitmap()
}