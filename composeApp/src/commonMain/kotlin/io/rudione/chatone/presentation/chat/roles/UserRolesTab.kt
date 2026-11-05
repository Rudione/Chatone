package io.rudione.chatone.presentation.chat.roles

import chatone.composeapp.generated.resources.insights_thousands_separator
import chatone.composeapp.generated.resources.insights_roles_granted
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.insights_role_artists
import chatone.composeapp.generated.resources.insights_role_founders
import chatone.composeapp.generated.resources.insights_role_moderators
import chatone.composeapp.generated.resources.insights_role_vips
import chatone.composeapp.generated.resources.insights_roles_account_created
import chatone.composeapp.generated.resources.insights_roles_affiliate
import chatone.composeapp.generated.resources.insights_roles_channels
import chatone.composeapp.generated.resources.insights_roles_empty
import chatone.composeapp.generated.resources.insights_roles_failed
import chatone.composeapp.generated.resources.insights_roles_followers
import chatone.composeapp.generated.resources.insights_roles_open_web
import chatone.composeapp.generated.resources.insights_roles_partner
import chatone.composeapp.generated.resources.insights_roles_retry
import chatone.composeapp.generated.resources.insights_roles_since
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabs
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabSize
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

private val PartnerColor = Color(0xFF9146FF)
private val AffiliateColor = Color(0xFF34B3B8)
private const val LOAD_MORE_THRESHOLD = 6

@Composable
internal fun UserRolesTab(
    state: UserRolesState,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state) { state.ensureLoaded() }

    Column(modifier = modifier) {
        when (state.status) {
            UserRolesState.Status.Idle, UserRolesState.Status.Loading -> CenteredSpinner(Modifier.weight(1f))

            UserRolesState.Status.Failed -> CenteredMessage(
                text = stringResource(Res.string.insights_roles_failed),
                action = stringResource(Res.string.insights_roles_retry),
                onAction = state::ensureLoaded,
                modifier = Modifier.weight(1f)
            )

            UserRolesState.Status.Ready -> {
                val held = state.counts.held
                val selected = state.selected
                if (held.isEmpty() || selected == null) {
                    CenteredMessage(text = stringResource(Res.string.insights_roles_empty), modifier = Modifier.weight(1f))
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

@Composable
private fun RoleTabs(
    roles: List<ChannelRole>,
    selected: ChannelRole,
    countOf: (ChannelRole) -> Int,
    onSelect: (ChannelRole) -> Unit
) {
    ExpressiveTabs(
        tabs = roles,
        selected = selected,
        onSelect = onSelect,
        label = { roleTitle(it) },
        badge = countOf,
        size = ExpressiveTabSize.Compact
    )
}

@Composable
private fun RoleChannelsList(
    list: RoleChannelList,
    onLoadMore: () -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (list.channels.isEmpty()) {
        when {
            list.failed -> CenteredMessage(stringResource(Res.string.insights_roles_failed), stringResource(Res.string.insights_roles_retry), onLoadMore, modifier)
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
        items(list.channels, key = { it.id }, contentType = { "channel" }) { channel ->
            RoleChannelRow(channel = channel, onClick = { onOpenChannel(channel.login) })
        }
        if (list.isLoading || list.failed) {
            item(key = "more", contentType = "more") {
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    if (list.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onLoadMore) {
                            Text(stringResource(Res.string.insights_roles_retry), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleChannelRow(channel: RoleChannel, onClick: () -> Unit) {
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
                    LiquidGlassTooltipBox(tooltip = datesTooltip(granted, channel.createdAtMs)) {
                        Text(
                            stringResource(Res.string.insights_roles_since, formatDate(granted)),
                            style = MaterialTheme.typography.labelSmall,
                            color = muted.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }
                if (channel.followers > 0) {
                    LiquidGlassTooltipBox(
                        tooltip = pluralStringResource(Res.plurals.insights_roles_followers, channel.followers, groupDigits(channel.followers, stringResource(Res.string.insights_thousands_separator)))
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
        StatusChip(channel)
    }
}

@Composable
private fun StatusChip(channel: RoleChannel) {
    val (label, color) = when {
        channel.isPartner -> stringResource(Res.string.insights_roles_partner) to PartnerColor
        channel.isAffiliate -> stringResource(Res.string.insights_roles_affiliate) to AffiliateColor
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
    val uriHandler = LocalUriHandler.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            pluralStringResource(Res.plurals.insights_roles_channels, total, total),
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
                    contentDescription = stringResource(Res.string.insights_roles_open_web),
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

@Composable
private fun roleTitle(role: ChannelRole): String = when (role) {
    ChannelRole.MODERATOR -> stringResource(Res.string.insights_role_moderators)
    ChannelRole.VIP -> stringResource(Res.string.insights_role_vips)
    ChannelRole.FOUNDER -> stringResource(Res.string.insights_role_founders)
    ChannelRole.ARTIST -> stringResource(Res.string.insights_role_artists)
}

@Composable
private fun datesTooltip(grantedMs: Long, createdMs: Long?): String =
    listOfNotNull(
        stringResource(Res.string.insights_roles_granted, formatDate(grantedMs)),
        createdMs?.let { stringResource(Res.string.insights_roles_account_created, formatDate(it)) }
    ).joinToString("\n")

internal fun groupDigits(value: Int, separator: String): String =
    value.toString().reversed().chunked(3).joinToString(separator).reversed()
