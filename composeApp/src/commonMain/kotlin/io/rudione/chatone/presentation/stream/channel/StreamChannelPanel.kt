package io.rudione.chatone.presentation.stream.channel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.follow.FollowFailure
import io.rudione.chatone.icons.lucide.Bell
import io.rudione.chatone.icons.lucide.BellOff
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquareReply
import io.rudione.chatone.icons.lucide.Pin
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.components.expressive.pressSquish
import io.rudione.chatone.presentation.stream.KeepStreamControlsShown
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Immutable
data class StreamChannelExtras(
    val liveAlerts: Boolean,
    val canPin: Boolean,
    val assistantAvailable: Boolean,
    val chatElsewhere: Boolean
)

@Immutable
class StreamChannelActions(
    val onToggleFollow: () -> Unit,
    val onToggleLiveAlerts: () -> Unit,
    val onPin: () -> Unit,
    val onOpenAssistant: () -> Unit,
    val onReturnToStreamChat: () -> Unit,
    val onOpenOnTwitch: () -> Unit
)

private val AvatarSize = 40.dp
private val ActionSize = 36.dp
private val ActionIconSize = 18.dp
private const val IDLE_CORNER_PERCENT = 50
private const val ACTIVE_CORNER_PERCENT = 32

@Composable
fun StreamChannelPanel(
    state: StreamChannelState,
    extras: StreamChannelExtras,
    actions: StreamChannelActions,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current.streamChannel
    val channel = state.channel
    var confirming by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelAvatar(url = channel.avatarUrl, login = channel.login)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    channel.title.ifBlank { channel.displayName.ifBlank { channel.login } },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.category.isNotBlank()) {
                    Text(
                        channel.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.canToggleFollow) {
                FollowPill(
                    following = state.follow == FollowStatus.FOLLOWING,
                    busy = state.followBusy,
                    onClick = { confirming = true }
                )
            }
            ChannelActionButton(
                icon = if (extras.liveAlerts) Lucide.Bell else Lucide.BellOff,
                label = if (extras.liveAlerts) s.liveAlertsOff else s.liveAlertsOn,
                active = extras.liveAlerts,
                onClick = actions.onToggleLiveAlerts
            )
            if (extras.canPin) {
                ChannelActionButton(icon = Lucide.Pin, label = s.pin, active = false, onClick = actions.onPin)
            }
            if (extras.assistantAvailable) {
                ChannelActionButton(
                    icon = Lucide.Sparkles,
                    label = s.assistant,
                    active = false,
                    accent = true,
                    onClick = actions.onOpenAssistant
                )
            }
            if (extras.chatElsewhere) {
                ChannelActionButton(
                    icon = Lucide.MessageSquareReply,
                    label = s.backToStreamChat,
                    active = false,
                    onClick = actions.onReturnToStreamChat
                )
            }
        }
        FollowFailureLine(failure = state.failure, onOpenOnTwitch = actions.onOpenOnTwitch)
    }
    if (confirming) {
        KeepStreamControlsShown()
        FollowConfirmDialog(
            channel = channel,
            following = state.follow == FollowStatus.FOLLOWING,
            onConfirm = actions.onToggleFollow,
            onDismiss = { confirming = false }
        )
    }
}

@Composable
private fun ChannelAvatar(url: String, login: String) {
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            login.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AvatarSize).clip(CircleShape)
            )
        }
    }
}

@Composable
private fun actionShape(active: Boolean): Shape {
    val corner by animateIntAsState(
        targetValue = if (active) ACTIVE_CORNER_PERCENT else IDLE_CORNER_PERCENT,
        animationSpec = tween(220),
        label = "channelActionCorner"
    )
    return RoundedCornerShape(percent = corner)
}

@Composable
private fun actionColors(active: Boolean, accent: Boolean): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = when {
            active -> colors.primaryContainer
            accent -> colors.tertiaryContainer
            else -> colors.surfaceContainerHighest
        },
        animationSpec = tween(180),
        label = "channelActionContainer"
    )
    val content = when {
        active -> colors.onPrimaryContainer
        accent -> colors.onTertiaryContainer
        else -> colors.onSurfaceVariant
    }
    return container to content
}

@Composable
private fun FollowPill(following: Boolean, busy: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current.streamChannel
    val interaction = remember { MutableInteractionSource() }
    val (container, content) = actionColors(active = following, accent = false)
    val label = if (following) s.unfollow else s.follow
    Row(
        modifier = Modifier
            .height(ActionSize)
            .pressSquish(interaction)
            .clip(actionShape(following))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, enabled = !busy, role = Role.Button, onClick = onClick)
            .animateContentSize(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
            .padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (busy) {
            CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(ActionIconSize))
        } else {
            Icon(Lucide.Heart, contentDescription = null, tint = content, modifier = Modifier.size(ActionIconSize))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun ChannelActionButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val (container, content) = actionColors(active, accent)
    LiquidGlassTooltipBox(tooltip = label) {
        Box(
            modifier = Modifier
                .size(ActionSize)
                .pressSquish(interaction)
                .clip(actionShape(active))
                .background(container)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(ActionIconSize))
        }
    }
}

@Composable
private fun FollowFailureLine(failure: FollowFailure?, onOpenOnTwitch: () -> Unit) {
    val s = LocalStrings.current.streamChannel
    AnimatedVisibility(
        visible = failure != null && failure != FollowFailure.SIGNED_OUT,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val blocked = failure == FollowFailure.BLOCKED
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (blocked) s.followBlocked else s.followFailed,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (blocked) {
                Text(
                    s.openOnTwitch,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onOpenOnTwitch)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
