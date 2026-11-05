package io.rudione.chatone.presentation.stream.channel

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.stream_channel_follow
import chatone.composeapp.generated.resources.stream_channel_follow_confirm_hint
import chatone.composeapp.generated.resources.stream_channel_follow_confirm_title
import chatone.composeapp.generated.resources.stream_channel_unfollow_confirm_hint
import chatone.composeapp.generated.resources.stream_channel_unfollow_confirm_title
import chatone.composeapp.generated.resources.stream_channel_unfollow_hold
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ScallopShape

private val CardShape = RoundedCornerShape(32.dp)
private val AvatarShape = ScallopShape(lobes = 10, depth = 0.07f)
private val AvatarSize = 76.dp

@Composable
internal fun FollowConfirmDialog(
    channel: ChannelSnapshot,
    following: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val name = channel.displayName.ifBlank { channel.login }
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    var afterClose by remember { mutableStateOf<(() -> Unit)?>(null) }
    val dismiss by rememberUpdatedState(onDismiss)
    val confirm by rememberUpdatedState(onConfirm)
    fun close(then: () -> Unit) {
        if (afterClose != null) return
        afterClose = then
        shown.targetState = false
    }
    LaunchedEffect(shown.isIdle, shown.currentState) {
        if (shown.isIdle && !shown.currentState) afterClose?.invoke()
    }

    Dialog(
        onDismissRequest = { close { dismiss() } },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    close { dismiss() }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = shown,
                enter = fadeIn(tween(160)) + scaleIn(
                    spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                    initialScale = 0.82f,
                    transformOrigin = TransformOrigin.Center
                ),
                exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.86f)
                        .clip(CardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CardShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScallopAvatar(channel)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(if (following) Res.string.stream_channel_unfollow_confirm_title else Res.string.stream_channel_follow_confirm_title, name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (following) stringResource(Res.string.stream_channel_unfollow_confirm_hint) else stringResource(Res.string.stream_channel_follow_confirm_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    if (following) {
                        HoldToBreakHeart(
                            label = stringResource(Res.string.stream_channel_unfollow_hold),
                            onBroken = { close { confirm(); dismiss() } }
                        )
                        Spacer(Modifier.height(16.dp))
                        PillButton(
                            text = stringResource(Res.string.cancel),
                            onClick = { close { dismiss() } },
                            tone = PillTone.Ghost,
                            height = 44.dp
                        )
                    } else {
                        PillButton(
                            text = stringResource(Res.string.stream_channel_follow),
                            onClick = { close { confirm(); dismiss() } },
                            icon = Lucide.Heart,
                            height = 52.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        PillButton(
                            text = stringResource(Res.string.cancel),
                            onClick = { close { dismiss() } },
                            tone = PillTone.Ghost,
                            height = 44.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScallopAvatar(channel: ChannelSnapshot) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(AvatarShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                channel.login.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (channel.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = channel.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AvatarSize)
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = 4.dp, y = 4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Heart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
        }
    }
}
