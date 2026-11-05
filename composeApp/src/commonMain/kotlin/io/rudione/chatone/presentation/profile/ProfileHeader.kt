package io.rudione.chatone.presentation.profile

import chatone.composeapp.generated.resources.channel_page_ago
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_edit_profile
import chatone.composeapp.generated.resources.channel_page_follow
import chatone.composeapp.generated.resources.channel_page_followers
import chatone.composeapp.generated.resources.channel_page_following
import chatone.composeapp.generated.resources.channel_page_last_live
import chatone.composeapp.generated.resources.channel_page_live
import chatone.composeapp.generated.resources.channel_page_never_live
import chatone.composeapp.generated.resources.channel_page_open_chat
import chatone.composeapp.generated.resources.channel_page_watch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import io.rudione.chatone.presentation.profile.channelLiveLine
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import io.rudione.chatone.domain.profile.ChannelProfile
import io.rudione.chatone.icons.lucide.BadgeCheck
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.icons.lucide.Pencil
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.presentation.chat.ProfileDisplayName
import io.rudione.chatone.presentation.chat.SevenTvLinkButton
import io.rudione.chatone.presentation.chat.compactCount
import io.rudione.chatone.presentation.chat.formatDate
import io.rudione.chatone.presentation.chat.relativeAge
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ambientHaze
import io.rudione.chatone.presentation.components.expressive.pressSquish
import io.rudione.chatone.presentation.theme.ChatoneTheme

private val BannerHeight = 168.dp
private val AvatarSize = 92.dp
private val TwitchPartner = Color(0xFF9146FF)

@Immutable
internal class ProfileHeaderActions(
    val onOpenChat: () -> Unit,
    val onWatch: () -> Unit,
    val onFollowClick: () -> Unit,
    val onEdit: () -> Unit,
    val onOpenLink: (String) -> Unit
)

@Composable
internal fun ProfileBanner(profile: ChannelProfile, accent: Color, scrollOffset: () -> Float) {
    Box(
        Modifier.fillMaxWidth().height(BannerHeight)
            .graphicsLayer {
                val offset = scrollOffset()
                translationY = offset * 0.45f
                alpha = (1f - offset / (BannerHeight.toPx() * 1.4f)).coerceIn(0f, 1f)
            }
    ) {
        val image = profile.bannerUrl.ifEmpty { profile.offlineImageUrl }
        if (image.isNotEmpty()) {
            AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().ambientHaze(tint = accent, accent = MaterialTheme.colorScheme.tertiary, strength = 0.9f))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.45f to Color.Transparent,
                    1f to MaterialTheme.colorScheme.background
                )
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileIdentity(
    profile: ChannelProfile,
    sevenTv: SevenTvUserCosmetic?,
    isOwn: Boolean,
    follow: FollowStatus,
    actions: ProfileHeaderActions
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.offset(y = (-34).dp)) {
            LiveAvatar(profile)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileDisplayName(
                        displayName = profile.displayName,
                        color = profile.chatColorHex?.let { "#$it" },
                        paint = sevenTv?.paint,
                        style = MaterialTheme.typography.headlineSmall,
                        fallbackLogin = profile.login,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.isPartner) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Lucide.BadgeCheck, contentDescription = null, tint = TwitchPartner, modifier = Modifier.size(20.dp))
                    }
                    sevenTv?.profile?.let { seven ->
                        Spacer(Modifier.width(6.dp))
                        SevenTvLinkButton(profile = seven)
                    }
                }
                if (!profile.displayName.equals(profile.login, ignoreCase = true)) {
                    Text("@${profile.login}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BroadcastLine(profile)
            }
        }
        Column(Modifier.offset(y = (-22).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (profile.description.isNotBlank()) {
                Text(profile.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f))
            }
            profile.followers?.let { count ->
                Text(
                    buildAnnotatedString {
                        val formatted = compactCount(count)
                        val label = pluralStringResource(Res.plurals.channel_page_followers, count, formatted)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(formatted) }
                        append(label.removePrefix(formatted))
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (profile.links.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    profile.links.forEach { link -> LinkChip(link.title, onClick = { actions.onOpenLink(link.url) }) }
                }
            }
            Spacer(Modifier.height(4.dp))
            ActionRow(profile, isOwn, follow, actions)
        }
    }
}

@Composable
private fun LiveAvatar(profile: ChannelProfile) {
    val live = ChatoneTheme.extraColors.live
    val pulse = rememberInfiniteTransition(label = "avatarPulse")
    val ring by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "avatarRing"
    )
    Box(contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier.size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp)
                .then(if (profile.isLive) Modifier.border(3.dp, live.copy(alpha = ring), CircleShape).padding(5.dp) else Modifier)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (profile.avatarUrl.isNotEmpty()) {
                AsyncImage(model = profile.avatarUrl, contentDescription = profile.displayName, modifier = Modifier.fillMaxSize())
            }
        }
        if (profile.isLive) {
            Text(
                stringResource(Res.string.channel_page_live),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.offset(y = 6.dp).clip(RoundedCornerShape(6.dp)).background(live)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun BroadcastLine(profile: ChannelProfile) {
    val live = profile.live
    val text = when {
        live != null -> channelLiveLine(live.category, compactCount(live.viewers))
        profile.lastBroadcast != null -> stringResource(Res.string.channel_page_last_live, "${formatDate(profile.lastBroadcast.startedAtMs)} · ${stringResource(Res.string.channel_page_ago, relativeAge(profile.lastBroadcast.startedAtMs))}")
        else -> stringResource(Res.string.channel_page_never_live)
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (live != null) FontWeight.SemiBold else FontWeight.Normal,
        color = if (live != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ActionRow(profile: ChannelProfile, isOwn: Boolean, follow: FollowStatus, actions: ProfileHeaderActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        PillButton(
            text = stringResource(Res.string.channel_page_open_chat),
            onClick = actions.onOpenChat,
            icon = Lucide.MessageSquare,
            height = 48.dp,
            modifier = Modifier.weight(1f)
        )
        if (profile.isLive) {
            PillButton(
                text = stringResource(Res.string.channel_page_watch),
                onClick = actions.onWatch,
                icon = Lucide.Play,
                tone = PillTone.Tonal,
                height = 48.dp,
                modifier = Modifier.weight(1f)
            )
        }
        when {
            isOwn -> RoundAction(Lucide.Pencil, stringResource(Res.string.channel_page_edit_profile), active = false, onClick = actions.onEdit)
            follow != FollowStatus.UNAVAILABLE -> RoundAction(
                icon = Lucide.Heart,
                description = if (follow == FollowStatus.FOLLOWING) stringResource(Res.string.channel_page_following) else stringResource(Res.string.channel_page_follow),
                active = follow == FollowStatus.FOLLOWING,
                busy = follow == FollowStatus.BUSY || follow == FollowStatus.UNKNOWN,
                onClick = actions.onFollowClick
            )
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    active: Boolean,
    busy: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(220),
        label = "roundActionContainer"
    )
    val content by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tween(220),
        label = "roundActionContent"
    )
    val pop by animateFloatAsState(if (active) 1f else 0.92f, spring(dampingRatio = 0.4f), label = "roundActionPop")
    Box(
        Modifier.size(48.dp).pressSquish(interaction).clip(CircleShape).background(container)
            .clickable(interactionSource = interaction, indication = null, enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = content.copy(alpha = if (busy) 0.5f else 1f),
            modifier = Modifier.size(20.dp).graphicsLayer { scaleX = pop; scaleY = pop }
        )
    }
}

@Composable
private fun LinkChip(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Lucide.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
    }
}

internal fun ChannelProfile.accentColor(fallback: Color): Color =
    accentHex?.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: fallback

internal fun ChannelProfile.joinedLabel(): String? = createdAtMs?.let(::formatDate)
