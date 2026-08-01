package io.rudione.chatone.presentation.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.chat.rendering.LocalScrollActivity
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.EmoteAnimationCache
import io.rudione.chatone.util.emote.AnimatedEmoteLoader
import io.rudione.chatone.util.emote.AnimatedFrames
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun AnimatedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    isScrolling: Boolean
) {
    AnimatedEmoteImageCore(url = url, contentDescription = contentDescription, modifier = modifier, isScrolling = isScrolling)
}

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
            AnimatedEmoteImageCore(
                url = emote.url2x.ifEmpty { emote.url1x },
                contentDescription = emote.code,
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 240.dp)
                    .heightIn(min = 40.dp, max = 120.dp)
            )

            Text(
                text = emote.code,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

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

@Composable
fun AnimatedEmoteImageCore(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    emoteId: String? = null,
    entranceAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 150),
    isScrolling: Boolean = false
) {
    var animData by remember(url) { mutableStateOf(AnimatedEmoteLoader.peek(url)) }
    var currentFrame by remember(url) { mutableIntStateOf(0) }

    LaunchedEffect(url) {
        if (animData != null || AnimatedEmoteLoader.isKnownStatic(url)) return@LaunchedEffect
        animData = AnimatedEmoteLoader.load(url)
    }

    val entranceProgress = if (emoteId != null) {
        val anim = remember(emoteId) {
            EmoteAnimationCache.getOrCreateAnimation(emoteId, animationSpec = entranceAnimationSpec)
        }
        LaunchedEffect(emoteId) {
            EmoteAnimationCache.startAnimation(emoteId)
        }
        anim.value
    } else {
        val localAnim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            localAnim.animateTo(1f, animationSpec = entranceAnimationSpec)
        }
        localAnim.value
    }

    val data = animData
    if (data != null && data.frames.isNotEmpty()) {
        val listScrolling = LocalScrollActivity.current.isScrolling
        val paused = isScrolling || listScrolling
        LaunchedEffect(data, paused) {
            if (paused) return@LaunchedEffect
            while (isActive) {
                val frameDuration = data.durations.getOrElse(currentFrame) { 100 }
                delay(frameDuration.toLong())
                currentFrame = (currentFrame + 1) % data.frames.size
            }
        }
        Image(
            bitmap = data.frames[currentFrame.coerceIn(0, data.frames.lastIndex)],
            contentDescription = contentDescription,
            modifier = modifier.alpha(entranceProgress)
        )
    } else {

        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier.alpha(entranceProgress),
            contentScale = ContentScale.Fit
        )
    }
}
