package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import io.rudione.chatone.util.link.TwitchLinks
import io.rudione.chatone.icons.material.outlined.Flag
import io.rudione.chatone.icons.material.Icons
import chatone.composeapp.generated.resources.report_on_twitch
import chatone.composeapp.generated.resources.ic_seventv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.close
import chatone.composeapp.generated.resources.ic_twitch
import chatone.composeapp.generated.resources.profile_detach_card
import chatone.composeapp.generated.resources.profile_editor_edit
import chatone.composeapp.generated.resources.profile_more_actions
import chatone.composeapp.generated.resources.profile_open_profile
import chatone.composeapp.generated.resources.profile_open_seventv
import chatone.composeapp.generated.resources.ui_copy_username
import chatone.composeapp.generated.resources.ui_open_twitch_profile
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.AccountBadge
import io.rudione.chatone.data.remote.GqlDisplayBadge
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.icons.lucide.CircleUser
import io.rudione.chatone.icons.lucide.Copy
import io.rudione.chatone.icons.lucide.EllipsisVertical
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Pencil
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Immutable
internal class PhoneProfileActions(
    val onOpenProfile: (() -> Unit)?,
    val onEdit: (() -> Unit)?,
    val onDetach: (() -> Unit)?,
    val reportUrl: String?,
    val onDismiss: () -> Unit
)

@Composable
internal fun PhoneProfileHeader(
    avatarUrl: String,
    displayName: String,
    username: String,
    userId: String,
    color: String?,
    badges: List<Badge>,
    gqlBadges: List<GqlDisplayBadge>,
    accountBadges: List<AccountBadge>,
    sevenTvBadge: SevenTvCosmetics.Badge?,
    stats: List<ProfileStat>,
    collapse: () -> Float,
    actions: PhoneProfileActions,
    modifier: Modifier = Modifier
) {
    val sevenTv = rememberSevenTvCosmetic(userId)
    val thirdPartyMaps = LocalThirdPartyBadges.current
    val chatoneBadge = rememberEquippedChatoneBadge(userId)
    val activeSevenTvBadge = if (sevenTv != null) sevenTv.badge else sevenTvBadge
    val badgeItems = remember(
        userId,
        badges,
        gqlBadges,
        activeSevenTvBadge,
        thirdPartyMaps,
        accountBadges,
        chatoneBadge
    ) {
        buildProfileBadges(
            chatBadges = badges,
            gqlBadges = gqlBadges,
            sevenTvBadge = activeSevenTvBadge,
            thirdPartyBadges = buildList {
                thirdPartyMaps.ffzByLogin[username.lowercase()]?.let { addAll(it) }
                thirdPartyMaps.bttvByUserId[userId]?.let { add(it) }
            },
            accountBadges = accountBadges,
            chatoneBadge = chatoneBadge
        )
    }
    val progress = collapse()
    val open = 1f - progress
    val showLogin = username.isNotBlank() && !username.equals(displayName, ignoreCase = true)

    Column(
        modifier = modifier.padding(
            start = lerp(14.dp, 10.dp, progress),
            end = 4.dp,
            top = lerp(12.dp, 6.dp, progress),
            bottom = lerp(10.dp, 6.dp, progress)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                url = avatarUrl,
                displayName = displayName,
                size = lerp(AVATAR_EXPANDED, AVATAR_COLLAPSED, progress),
                onClick = actions.onOpenProfile
            )
            Spacer(Modifier.width(lerp(12.dp, 8.dp, progress)))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NameWithBadges(
                        modifier = Modifier.weight(1f),
                        name = {
                            ProfileDisplayName(
                                displayName = displayName,
                                color = color,
                                fallbackLogin = username,
                                paint = sevenTv?.paint,
                                style = lerp(
                                    MaterialTheme.typography.titleMedium,
                                    MaterialTheme.typography.titleSmall,
                                    progress
                                )
                            )
                        },
                        badges = if (progress > INLINE_BADGES_FROM && badgeItems.isNotEmpty()) {
                            {
                                ProfileBadgeStrip(
                                    badges = badgeItems,
                                    badgeSize = INLINE_BADGE_SIZE,
                                    spacing = INLINE_BADGE_SPACING,
                                    expandable = false,
                                    modifier = Modifier.foldAlpha(progress)
                                )
                            }
                        } else null
                    )
                    ProfileOverflowButton(
                        username = username,
                        sevenTvUrl = sevenTv?.profile?.profileUrl,
                        actions = actions
                    )
                    ChatoneIconButton(
                        onClick = actions.onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Lucide.X,
                            contentDescription = stringResource(Res.string.close),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(Modifier.foldVertically(open)) {
                    if (showLogin) {
                        Text(
                            "@$username",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (stats.isNotEmpty()) {
                        Spacer(Modifier.height(if (showLogin) 5.dp else 3.dp))
                        ProfileStatPills(stats)
                    }
                }
            }
        }
        if (badgeItems.isNotEmpty()) {
            Column(Modifier.foldVertically(open)) {
                Spacer(Modifier.height(10.dp))
                ProfileBadgeStrip(badges = badgeItems, badgeSize = 20.dp, spacing = 4.dp)
            }
        }
    }
}

@Composable
private fun NameWithBadges(
    name: @Composable () -> Unit,
    badges: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    Layout(
        contents = listOf(name, badges ?: {}),
        modifier = modifier
    ) { (nameParts, badgeParts), constraints ->
        val gap = INLINE_BADGES_GAP.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val reserve = if (badgeParts.isEmpty()) 0
        else (INLINE_BADGES_MIN_WIDTH.roundToPx() + gap).coerceAtMost(constraints.maxWidth / 2)
        val namePlaceable = nameParts.first()
            .measure(loose.copy(maxWidth = (constraints.maxWidth - reserve).coerceAtLeast(0)))
        val badgeWidth = constraints.maxWidth - namePlaceable.width - gap
        val badgePlaceable = badgeParts.firstOrNull()?.takeIf { badgeWidth > 0 }
            ?.measure(loose.copy(minWidth = badgeWidth, maxWidth = badgeWidth))
        val height = maxOf(namePlaceable.height, badgePlaceable?.height ?: 0)
        layout(constraints.maxWidth, height) {
            namePlaceable.placeRelative(0, (height - namePlaceable.height) / 2)
            badgePlaceable?.placeRelative(
                namePlaceable.width + gap,
                (height - badgePlaceable.height) / 2
            )
        }
    }
}

@Composable
private fun ProfileAvatar(url: String, displayName: String, size: Dp, onClick: (() -> Unit)?) {
    val avatar = Modifier.size(size).clip(CircleShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    if (url.isNotEmpty()) {
        AsyncImage(
            model = url,
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = avatar
        )
    } else {
        Box(
            avatar.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayName.take(2).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProfileOverflowButton(
    username: String,
    sevenTvUrl: String?,
    actions: PhoneProfileActions
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val pick: (() -> Unit) -> Unit = { action ->
        expanded = false
        action()
    }
    val twitchUrl = TwitchLinks.channelUrl(username)
    Box {
        ChatoneIconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
            Icon(
                Lucide.EllipsisVertical,
                contentDescription = stringResource(Res.string.profile_more_actions),
                modifier = Modifier.size(18.dp),
                tint = if (expanded) MaterialTheme.colorScheme.primary else muted
            )
        }
        ChatoneDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.widthIn(min = MENU_MIN_WIDTH)
        ) {
            actions.onOpenProfile?.let { open ->
                ProfileMenuRow(
                    stringResource(Res.string.profile_open_profile),
                    rememberVectorPainter(Lucide.CircleUser),
                    muted
                ) { pick(open) }
            }
            twitchUrl?.let { url ->
                ProfileMenuRow(
                    stringResource(Res.string.ui_open_twitch_profile),
                    painterResource(Res.drawable.ic_twitch),
                    TwitchPurple
                ) {
                    pick { uriHandler.openUri(url) }
                }
            }
            sevenTvUrl?.let { url ->
                ProfileMenuRow(
                    stringResource(Res.string.profile_open_seventv),
                    painterResource(Res.drawable.ic_seventv),
                    SEVEN_TV_COLOR
                ) {
                    pick { uriHandler.openUri(url) }
                }
            }
            MenuDivider()
            if (username.isNotBlank()) {
                ProfileMenuRow(
                    stringResource(Res.string.ui_copy_username),
                    rememberVectorPainter(Lucide.Copy),
                    muted
                ) {
                    pick { scope.launch { clipboard.setText(username) } }
                }
            }
            actions.onEdit?.let { edit ->
                ProfileMenuRow(
                    stringResource(Res.string.profile_editor_edit),
                    rememberVectorPainter(Lucide.Pencil),
                    muted
                ) { pick(edit) }
            }
            actions.onDetach?.let { detach ->
                ProfileMenuRow(
                    stringResource(Res.string.profile_detach_card),
                    rememberVectorPainter(Lucide.ExternalLink),
                    muted
                ) { pick(detach) }
            }
            actions.reportUrl?.let { url ->
                MenuDivider()
                ProfileMenuRow(
                    stringResource(Res.string.report_on_twitch),
                    rememberVectorPainter(Icons.Outlined.Flag),
                    MaterialTheme.colorScheme.error,
                    labelColor = MaterialTheme.colorScheme.error
                ) { pick { uriHandler.openUri(url) } }
            }
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun ProfileMenuRow(
    label: String,
    icon: Painter,
    tint: Color,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MENU_ROW_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = tint)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Modifier.foldVertically(fraction: Float): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val visible = fraction.coerceIn(0f, 1f)
        layout(placeable.width, (placeable.height * visible).roundToInt()) {
            placeable.placeWithLayer(0, 0) { alpha = visible * visible }
        }
    }

private fun Modifier.foldAlpha(fraction: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeWithLayer(0, 0) {
            alpha = ((fraction - INLINE_BADGES_FROM) / (1f - INLINE_BADGES_FROM)).coerceIn(0f, 1f)
        }
    }
}

private val AVATAR_EXPANDED = 48.dp
private val AVATAR_COLLAPSED = 30.dp
private val INLINE_BADGE_SIZE = 16.dp
private val INLINE_BADGE_SPACING = 3.dp
private val INLINE_BADGES_GAP = 6.dp
private val INLINE_BADGES_MIN_WIDTH = 35.dp
private const val INLINE_BADGES_FROM = 0.35f
private val TwitchPurple = Color(0xFF9146FF)
private val MENU_MIN_WIDTH = 196.dp
private val MENU_ROW_HEIGHT = 38.dp
