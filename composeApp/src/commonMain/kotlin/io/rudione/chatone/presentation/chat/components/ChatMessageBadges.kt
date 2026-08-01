package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.chat.LocalThirdPartyBadges
import io.rudione.chatone.presentation.chat.RoomState
import io.rudione.chatone.presentation.theme.i18n.AppStrings

internal data class BadgeVisual(val imageUrl: String, val tooltip: String)

@Composable
internal fun collectBadgeVisuals(
    username: String,
    userId: String,
    badges: List<Badge>,
    sevenTvBadge: SevenTvCosmetics.Badge?
): List<BadgeVisual> {
    val maps = LocalThirdPartyBadges.current
    return buildList {
        maps.chatoneByUserId[userId]?.forEach { add(BadgeVisual(it.imageUrl, it.title)) }
        maps.ffzByLogin[username.lowercase()]?.forEach { add(BadgeVisual(it.imageUrl, it.title)) }
        maps.bttvByUserId[userId]?.let { add(BadgeVisual(it.imageUrl, it.title)) }
        badges.forEach { badge ->
            if (badge.imageUrl.isNotEmpty()) add(BadgeVisual(badge.imageUrl, badge.tooltip.ifBlank { badge.id }))
        }
        sevenTvBadge?.let { stv ->
            val url = stv.url2x.ifEmpty { stv.url1x }
            if (url.isNotEmpty()) add(BadgeVisual(url, stv.tooltip.ifBlank { "7TV Badge" }))
        }
    }
}

internal fun roomModeLabels(roomState: RoomState, s: AppStrings): List<String> = buildList {
    if (roomState.emoteOnly) add("Emote-only")
    if (roomState.subsOnly) add("Sub-only")
    if (roomState.slowMode > 0) add("Slow ${roomState.slowMode}s")
    if (roomState.followersOnly >= 0) {
        if (roomState.followersOnly == 0) add(s.profileFollow)
        else add(s.chatFollowersOnly.replace("{0}", roomState.followersOnly.toString()))
    }
    if (roomState.r9k) add("R9K")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

@Composable
internal fun ThirdPartyBadgeIcons(username: String, userId: String) {
    val maps = LocalThirdPartyBadges.current
    maps.chatoneByUserId[userId]?.forEach { badge ->
        LiquidGlassTooltipBox(tooltip = badge.title) {
            AsyncImage(
                model = badge.imageUrl,
                contentDescription = badge.title,
                modifier = Modifier.size(18.dp)
            )
        }
    }
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
