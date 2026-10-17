package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.AccountBadge
import io.rudione.chatone.data.remote.GqlDisplayBadge
import io.rudione.chatone.data.repository.ThirdPartyBadge
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.icons.lucide.ChevronLeft
import io.rudione.chatone.icons.lucide.Lucide

internal data class ProfileBadgeItem(
    val key: String,
    val imageUrl: String,
    val tooltip: String
)

private const val EXPANDED_MAX_ROWS = 6
private const val MAX_COUNTER = 99

internal fun buildProfileBadges(
    chatBadges: List<Badge>,
    gqlBadges: List<GqlDisplayBadge>,
    sevenTvBadge: SevenTvCosmetics.Badge?,
    thirdPartyBadges: List<ThirdPartyBadge>,
    accountBadges: List<AccountBadge> = emptyList()
): List<ProfileBadgeItem> {
    val seen = mutableSetOf<String>()
    val result = mutableListOf<ProfileBadgeItem>()

    fun add(key: String, imageUrl: String, tooltip: String) {
        if (imageUrl.isBlank()) return
        if (!seen.add(key.lowercase())) return
        result += ProfileBadgeItem(key = key, imageUrl = imageUrl, tooltip = tooltip)
    }

    gqlBadges.forEach { badge ->
        add(
            key = "tw_${badge.setId}_${badge.version}",
            imageUrl = badge.imageUrl,
            tooltip = badge.description?.takeIf { it.isNotBlank() } ?: badge.title
        )
    }
    chatBadges.forEach { badge ->
        val setId = badge.setId.ifBlank { badge.id }
        add(
            key = "tw_${setId}_${badge.version}",
            imageUrl = badge.imageUrl,
            tooltip = badge.tooltip.ifBlank { setId }
        )
    }
    sevenTvBadge?.let { stv ->
        add(
            key = "7tv_${stv.id}",
            imageUrl = stv.url2x.ifEmpty { stv.url1x },
            tooltip = stv.tooltip.ifBlank { stv.name }
        )
    }
    thirdPartyBadges.forEach { badge ->
        add(key = "3rd_${badge.title}", imageUrl = badge.imageUrl, tooltip = badge.title)
    }
    accountBadges.forEach { badge ->
        add(key = badge.key, imageUrl = badge.imageUrl, tooltip = badge.title)
    }

    return result
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileBadgeStrip(
    badges: List<ProfileBadgeItem>,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 18.dp,
    spacing: Dp = 3.dp
) {
    if (badges.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val perRow = ((maxWidth + spacing) / (badgeSize + spacing))
            .toInt().coerceAtLeast(1)
        val arrangement = Arrangement.spacedBy(spacing)
        when {
            badges.size <= perRow -> Row(horizontalArrangement = arrangement) {
                badges.forEach { ProfileBadge(it, badgeSize) }
            }
            expanded -> FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (badgeSize + spacing) * EXPANDED_MAX_ROWS)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = arrangement,
                verticalArrangement = arrangement
            ) {
                badges.forEach { ProfileBadge(it, badgeSize) }
                BadgeOverflowTile(size = badgeSize, onClick = { expanded = false }) {
                    Icon(
                        Lucide.ChevronLeft,
                        contentDescription = null,
                        modifier = Modifier.size(badgeSize * 0.6f),
                        tint = overflowContentColor()
                    )
                }
            }
            else -> {
                val visible = perRow - 1
                Row(horizontalArrangement = arrangement) {
                    badges.take(visible).forEach { ProfileBadge(it, badgeSize) }
                    BadgeOverflowTile(size = badgeSize, onClick = { expanded = true }) {
                        val hidden = badges.size - visible
                        val label = if (hidden > MAX_COUNTER) "$MAX_COUNTER+" else hidden.toString()
                        val fontSize = (badgeSize.value * if (label.length > 2) 0.38f else 0.5f).sp
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = fontSize,
                                lineHeight = fontSize
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = overflowContentColor(),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBadge(badge: ProfileBadgeItem, size: Dp) {
    LiquidGlassTooltipBox(tooltip = badge.tooltip) {
        AsyncImage(
            model = badge.imageUrl,
            contentDescription = badge.tooltip,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun BadgeOverflowTile(
    size: Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun overflowContentColor() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
