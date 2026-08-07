package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.GqlDisplayBadge
import io.rudione.chatone.data.repository.ThirdPartyBadge
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox

internal data class ProfileBadgeItem(
    val key: String,
    val imageUrl: String,
    val tooltip: String
)

private const val COLLAPSED_ROWS = 2
private const val EXPANDED_ROWS = 4

internal fun buildProfileBadges(
    chatBadges: List<Badge>,
    gqlBadges: List<GqlDisplayBadge>,
    sevenTvBadge: SevenTvCosmetics.Badge?,
    thirdPartyBadges: List<ThirdPartyBadge>
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

    return result
}

@Composable
internal fun ProfileBadgeStrip(
    badges: List<ProfileBadgeItem>,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 18.dp,
    spacing: Dp = 3.dp
) {
    if (badges.isEmpty()) return
    var expanded by remember(badges.size) { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val perRow = ((maxWidth + spacing) / (badgeSize + spacing))
            .toInt().coerceAtLeast(1)
        val rows = badges.chunked(perRow)
        val rowHeight = badgeSize + spacing
        val visibleRows = if (expanded) minOf(rows.size, EXPANDED_ROWS)
        else minOf(rows.size, COLLAPSED_ROWS)
        val needsToggle = rows.size > COLLAPSED_ROWS
        val scrollable = expanded && rows.size > EXPANDED_ROWS
        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (scrollable) Modifier
                            .heightIn(max = rowHeight * EXPANDED_ROWS)
                            .verticalScroll(scrollState)
                        else Modifier
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                val shown = if (scrollable) rows else rows.take(visibleRows)
                shown.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        row.forEach { badge ->
                            LiquidGlassTooltipBox(tooltip = badge.tooltip) {
                                AsyncImage(
                                    model = badge.imageUrl,
                                    contentDescription = badge.tooltip,
                                    modifier = Modifier.size(badgeSize)
                                )
                            }
                        }
                    }
                }
            }

            if (needsToggle) {
                Spacer(Modifier.height(2.dp))
                BadgeExpandToggle(
                    expanded = expanded,
                    hiddenCount = badges.size - (perRow * COLLAPSED_ROWS).coerceAtMost(badges.size),
                    onClick = { expanded = !expanded }
                )
            }
        }
    }
}

@Composable
private fun BadgeExpandToggle(
    expanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (expanded) "−" else "+$hiddenCount",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}
