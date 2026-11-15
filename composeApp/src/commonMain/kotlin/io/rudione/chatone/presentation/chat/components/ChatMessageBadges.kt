package io.rudione.chatone.presentation.chat.components

import chatone.composeapp.generated.resources.chat_slow_mode
import chatone.composeapp.generated.resources.chat_subs_only
import chatone.composeapp.generated.resources.chat_emote_only
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_followers_only
import chatone.composeapp.generated.resources.profile_follow
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.Image
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import chatone.composeapp.generated.resources.shared_chat_badge
import chatone.composeapp.generated.resources.badge_shared_chat
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import io.rudione.chatone.domain.model.displayPriority
import io.rudione.chatone.presentation.chat.LocalSevenTvCosmetics
import io.rudione.chatone.presentation.chat.bundledDrawable
import io.rudione.chatone.presentation.chat.rememberEquippedChatoneBadge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.subscriptionTierOverlay
import io.rudione.chatone.presentation.chat.LocalThirdPartyBadges
import io.rudione.chatone.presentation.chat.RoomState

internal data class BadgeVisual(
    val imageUrl: String,
    val tooltip: String,
    val tierOverlay: Int? = null,
    val drawable: DrawableResource? = null
)

@Composable
internal fun BadgeIcon(visual: BadgeVisual, size: Dp = 18.dp) {
    LiquidGlassTooltipBox(tooltip = visual.tooltip) {
        Box(contentAlignment = Alignment.BottomEnd) {
            BadgeArtwork(visual.imageUrl, visual.drawable, visual.tooltip, Modifier.size(size))
            visual.tierOverlay?.let { tier ->
                Text(
                    text = tier.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 2.5.dp)
                )
            }
        }
    }
}

@Composable
internal fun BadgeArtwork(
    imageUrl: String,
    drawable: DrawableResource?,
    description: String,
    modifier: Modifier = Modifier
) {
    if (drawable != null) Image(painterResource(drawable), description, modifier)
    else AsyncImage(model = imageUrl, contentDescription = description, modifier = modifier)
}

@Composable
internal fun collectBadgeVisuals(
    username: String,
    userId: String,
    badges: List<Badge>,
    sevenTvBadge: SevenTvCosmetics.Badge?,
    sharedChat: Boolean = false
): List<BadgeVisual> {
    val maps = LocalThirdPartyBadges.current
    val cosmetics = LocalSevenTvCosmetics.current
    val liveCosmetic by remember(cosmetics, userId) { derivedStateOf { cosmetics.value[userId] } }
    val equippedSevenTvBadge = if (liveCosmetic != null) liveCosmetic?.badge else sevenTvBadge
    val chatoneBadge = rememberEquippedChatoneBadge(userId)
    val sharedLabel = stringResource(Res.string.badge_shared_chat)
    return remember(username, userId, badges, equippedSevenTvBadge, maps, sharedChat, sharedLabel, chatoneBadge) {
        buildList {
            val ordered = badges.sortedBy { it.displayPriority() }
            fun appendTwitch(badge: Badge) {
                if (badge.imageUrl.isNotBlank()) add(
                    BadgeVisual(badge.imageUrl, badge.tooltip.ifBlank { badge.id }, badge.subscriptionTierOverlay())
                )
            }
            ordered.filter { it.displayPriority() < 40 }.forEach(::appendTwitch)
            if (sharedChat && ordered.none { it.displayPriority() == 30 }) {
                add(BadgeVisual("", sharedLabel, drawable = Res.drawable.shared_chat_badge))
            }
            ordered.filter { it.displayPriority() >= 40 }.forEach(::appendTwitch)
            val ffzBadges = maps.ffzByLogin[username.lowercase()].orEmpty()
            val (bots, otherFfz) = ffzBadges.partition { it.title.equals("bot", ignoreCase = true) }
            (bots + otherFfz).forEach { add(BadgeVisual(it.imageUrl, it.title)) }
            maps.bttvByUserId[userId]?.let { add(BadgeVisual(it.imageUrl, it.title)) }
            equippedSevenTvBadge?.let { badge ->
                val url = badge.url2x.ifEmpty { badge.url1x }
                if (url.isNotBlank()) add(BadgeVisual(url, badge.tooltip.ifBlank { "7TV Badge" }))
            }
            chatoneBadge?.let { badge ->
                val drawable = badge.bundledDrawable()
                val url = badge.imageUrl2x.ifBlank { badge.imageUrl1x }
                if (drawable != null || url.isNotBlank()) add(BadgeVisual(url, badge.title, drawable = drawable))
            }
        }
    }
}

@Composable
internal fun roomModeLabels(roomState: RoomState): List<String> = buildList {
    if (roomState.emoteOnly) add(stringResource(Res.string.chat_emote_only))
    if (roomState.subsOnly) add(stringResource(Res.string.chat_subs_only))
    if (roomState.slowMode > 0) add(stringResource(Res.string.chat_slow_mode, roomState.slowMode))
    if (roomState.followersOnly >= 0) {
        if (roomState.followersOnly == 0) add(stringResource(Res.string.profile_follow))
        else add(stringResource(Res.string.chat_followers_only, roomState.followersOnly))
    }
    if (roomState.r9k) add("R9K")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

@Composable
internal fun ThirdPartyBadgeIcons(username: String, userId: String) {
    val maps = LocalThirdPartyBadges.current
    maps.ffzByLogin[username.lowercase()]?.forEach { badge ->
        LiquidGlassTooltipBox(tooltip = badge.title) {
            AsyncImage(
                model = badge.imageUrl,
                contentDescription = badge.title,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    maps.bttvByUserId[userId]?.let { badge ->
        LiquidGlassTooltipBox(tooltip = badge.title) {
            AsyncImage(
                model = badge.imageUrl,
                contentDescription = badge.title,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
