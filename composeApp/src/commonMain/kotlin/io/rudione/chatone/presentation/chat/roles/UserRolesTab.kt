package io.rudione.chatone.presentation.chat.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.ChannelRole
import io.rudione.chatone.data.remote.RoleChannel
import io.rudione.chatone.data.remote.RolesTvClient
import io.rudione.chatone.icons.lucide.BadgeCheck
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Users
import io.rudione.chatone.presentation.chat.compactCount
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.chat.formatDate
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.ProfileInsightStrings

private val PartnerColor = Color(0xFF9146FF)
private val AffiliateColor = Color(0xFF34B3B8)
private const val LOAD_MORE_THRESHOLD = 6

@Composable
internal fun UserRolesTab(
    state: UserRolesState,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val insights = LocalStrings.current.insights
    LaunchedEffect(state) { state.ensureLoaded() }

    Column(modifier = modifier) {
        when (state.status) {
            UserRolesState.Status.Idle, UserRolesState.Status.Loading -> CenteredSpinner(Modifier.weight(1f))

            UserRolesState.Status.Failed -> CenteredMessage(
                text = insights.rolesFailed,
                action = insights.rolesRetry,
                onAction = state::ensureLoaded,
                modifier = Modifier.weight(1f)
            )

            UserRolesState.Status.Ready -> {
                val held = state.counts.held
                val selected = state.selected
                if (held.isEmpty() || selected == null) {
                    CenteredMessage(text = insights.rolesEmpty, modifier = Modifier.weight(1f))
                } else {
                    RoleTabs(
                        roles = held,
                        selected = selected,
                        countOf = state.counts::of,
                        onSelect = state::select
                    )
                    RoleChannelsList(
                        list = state.channelsOf(selected),
                        onLoadMore = { state.loadMore(selected) },
                        onOpenChannel = onOpenChannel,
                        modifier = Modifier.weight(1f)
                    )
                    RolesFooter(
                        total = state.channelsOf(selected).total.takeIf { it > 0 } ?: state.counts.of(selected),
                        login = state.profileLogin
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleTabs(
    roles: List<ChannelRole>,
    selected: ChannelRole,
    countOf: (ChannelRole) -> Int,
    onSelect: (ChannelRole) -> Unit
) {
    val insights = LocalStrings.current.insights
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedIndex = roles.indexOf(selected).coerceAtLeast(0)
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = accent,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                width = Dp.Unspecified,
                height = 2.dp,
                color = accent,
                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
            )
        },
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)) }
    ) {
        roles.forEach { role ->
            val isSelected = role == selected
            Tab(
                selected = isSelected,
                onClick = { onSelect(role) },
                selectedContentColor = accent,
                unselectedContentColor = muted
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        roleTitle(insights, role),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        countOf(role).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) accent.copy(alpha = 0.75f) else muted.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleChannelsList(
    list: RoleChannelList,
    onLoadMore: () -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val insights = LocalStrings.current.insights
    if (list.channels.isEmpty()) {
        when {
            list.failed -> CenteredMessage(insights.rolesFailed, insights.rolesRetry, onLoadMore, modifier)
            else -> CenteredSpinner(modifier)
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(listState, list) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }.collect { nearEnd -> if (nearEnd) onLoadMore() }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 3.dp)
    ) {
        items(list.channels, key = { it.id }) { channel ->
            RoleChannelRow(channel = channel, onClick = { onOpenChannel(channel.login) })
        }
        if (list.isLoading || list.failed) {
            item(key = "more") {
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    if (list.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onLoadMore) {
                            Text(insights.rolesRetry, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleChannelRow(channel: RoleChannel, onClick: () -> Unit) {
    val insights = LocalStrings.current.insights
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else Color.Transparent)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(muted.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (channel.avatarUrl != null) {
                AsyncImage(
                    model = channel.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(channel.displayName.take(1).uppercase(), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    channel.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (channel.isPartner) {
                    Icon(Lucide.BadgeCheck, contentDescription = null, modifier = Modifier.size(11.dp), tint = PartnerColor)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                channel.grantedAtMs?.let { granted ->
                    LiquidGlassTooltipBox(tooltip = datesTooltip(insights, granted, channel.createdAtMs)) {
                        Text(
                            insights.rolesSince(formatDate(granted)),
                            style = MaterialTheme.typography.labelSmall,
                            color = muted.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }
                if (channel.followers > 0) {
                    LiquidGlassTooltipBox(
                        tooltip = insights.rolesFollowers(
                            channel.followers,
                            groupDigits(channel.followers, insights.thousandsSeparator)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Lucide.Users, contentDescription = null, modifier = Modifier.size(10.dp), tint = muted.copy(alpha = 0.55f))
                            Text(
                                compactCount(channel.followers),
                                style = MaterialTheme.typography.labelSmall,
                                color = muted.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        StatusChip(channel, insights)
    }
}

@Composable
private fun StatusChip(channel: RoleChannel, insights: ProfileInsightStrings) {
    val (label, color) = when {
        channel.isPartner -> insights.rolesPartner to PartnerColor
        channel.isAffiliate -> insights.rolesAffiliate to AffiliateColor
        else -> return
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun RolesFooter(total: Int, login: String) {
    val insights = LocalStrings.current.insights
    val uriHandler = LocalUriHandler.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            insights.rolesChannels(total),
            style = MaterialTheme.typography.labelSmall,
            color = muted.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f)
        )
        if (login.isNotBlank()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { uriHandler.openUri(RolesTvClient.profileUrl(login)) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("roles.tv", style = MaterialTheme.typography.labelSmall, color = muted.copy(alpha = 0.7f))
                Icon(
                    Lucide.ExternalLink,
                    contentDescription = insights.rolesOpenWeb,
                    modifier = Modifier.size(10.dp),
                    tint = muted.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CenteredSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun CenteredMessage(
    text: String,
    action: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private fun roleTitle(insights: ProfileInsightStrings, role: ChannelRole): String = when (role) {
    ChannelRole.MODERATOR -> insights.roleModerators
    ChannelRole.VIP -> insights.roleVips
    ChannelRole.FOUNDER -> insights.roleFounders
    ChannelRole.ARTIST -> insights.roleArtists
}

private fun datesTooltip(insights: ProfileInsightStrings, grantedMs: Long, createdMs: Long?): String =
    listOfNotNull(
        insights.rolesGranted(formatDate(grantedMs)),
        createdMs?.let { insights.rolesAccountCreated(formatDate(it)) }
    ).joinToString("\n")

internal fun groupDigits(value: Int, separator: String): String =
    value.toString().reversed().chunked(3).joinToString(separator).reversed()
