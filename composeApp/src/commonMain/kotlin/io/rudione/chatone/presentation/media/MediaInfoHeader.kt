package io.rudione.chatone.presentation.media

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_ago
import chatone.composeapp.generated.resources.media_ban_curator
import chatone.composeapp.generated.resources.media_clip
import chatone.composeapp.generated.resources.media_clipped_by
import chatone.composeapp.generated.resources.media_delete_clip
import chatone.composeapp.generated.resources.media_link_copied
import chatone.composeapp.generated.resources.media_loop
import chatone.composeapp.generated.resources.media_moderation
import chatone.composeapp.generated.resources.media_open_twitch
import chatone.composeapp.generated.resources.media_quality
import chatone.composeapp.generated.resources.media_share
import chatone.composeapp.generated.resources.media_speed
import chatone.composeapp.generated.resources.media_views
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.media.MediaDetails
import io.rudione.chatone.domain.media.MediaKind
import io.rudione.chatone.domain.media.MediaPerson
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.Clapperboard
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Gauge
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Repeat
import io.rudione.chatone.icons.lucide.Settings
import io.rudione.chatone.icons.lucide.Share2
import io.rudione.chatone.icons.lucide.Trash2
import io.rudione.chatone.presentation.chat.compactCount
import io.rudione.chatone.presentation.chat.relativeAge
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.channelmenu.ChannelMenuItem
import io.rudione.chatone.util.link.shareLink
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.launch

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val AvatarSize = 40.dp

@Composable
internal fun MediaInfoHeader(
    state: MediaViewerState,
    details: MediaDetails,
    onEvent: (MediaViewerEvent) -> Unit,
    onAsk: (PendingAction) -> Unit,
    onNotice: (String) -> Unit,
    onClip: (() -> Unit)?,
    navigation: MediaViewerNavigation,
    modifier: Modifier = Modifier
) {
    val mediaLinkCopiedText = stringResource(Res.string.media_link_copied)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column(
        modifier.padding(top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BroadcasterAvatar(details.broadcaster) { navigation.onOpenProfile(details.broadcaster.login) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    details.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(
                        details.broadcaster.displayName.takeIf { it.isNotBlank() },
                        stringResource(Res.string.media_views, compactCount(details.views)),
                        details.createdAtMs?.let {
                            stringResource(Res.string.channel_page_ago, relativeAge(it))
                        },
                        details.category.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                details.curator?.takeIf { it.login != details.broadcaster.login }?.let { curator ->
                    Text(
                        stringResource(Res.string.media_clipped_by, curator.displayName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .clickable { navigation.onOpenProfile(curator.login) }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            onClip?.let { clip ->
                ActionChip(Lucide.Clapperboard, stringResource(Res.string.media_clip), onClick = clip)
            }
            ActionChip(Lucide.Share2, stringResource(Res.string.media_share)) {
                if (!shareLink(details.pageUrl, details.title)) {
                    scope.launch { clipboard.setText(details.pageUrl) }
                    onNotice(mediaLinkCopiedText)
                }
            }
            if (details.sources.size > 1) {
                MenuChip(
                    Lucide.Settings,
                    "${stringResource(Res.string.media_quality)}: ${
                        details.sources.getOrNull(state.sourceIndex)?.label.orEmpty()
                    }"
                ) { close ->
                    details.sources.forEachIndexed { index, source ->
                        ChannelMenuItem(
                            source.label.ifBlank { stringResource(Res.string.media_quality) },
                            null
                        ) {
                            close()
                            onEvent(MediaViewerEvent.SelectSource(index))
                        }
                    }
                }
            }
            MenuChip(
                Lucide.Gauge,
                "${stringResource(Res.string.media_speed)}: ${formatSpeed(state.speed)}"
            ) { close ->
                SPEEDS.forEach { speed ->
                    ChannelMenuItem(formatSpeed(speed), null) {
                        close()
                        onEvent(MediaViewerEvent.SetSpeed(speed))
                    }
                }
            }
            if (details.kind == MediaKind.CLIP) {
                ActionChip(
                    Lucide.Repeat,
                    stringResource(Res.string.media_loop),
                    active = state.looping
                ) { onEvent(MediaViewerEvent.ToggleLooping) }
            }
            ActionChip(
                Lucide.ExternalLink,
                stringResource(Res.string.media_open_twitch)
            ) { navigation.onOpenUrl(details.pageUrl) }
        }
        if (state.canModerate && details.kind == MediaKind.CLIP && !state.deleted) {
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(Res.string.media_moderation),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip(
                        Lucide.Trash2,
                        stringResource(Res.string.media_delete_clip),
                        danger = true,
                        enabled = !state.busy
                    ) { onAsk(PendingAction.DELETE) }
                    if (details.curator != null && details.curator.id != details.broadcaster.id) {
                        ActionChip(
                            Lucide.Ban,
                            stringResource(Res.string.media_ban_curator),
                            danger = true,
                            enabled = !state.busy
                        ) { onAsk(PendingAction.BAN) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcasterAvatar(person: MediaPerson, onClick: () -> Unit) {
    Box(
        Modifier.size(AvatarSize).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = person.displayName },
        contentAlignment = Alignment.Center
    ) {
        Text(
            person.displayName.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (person.avatarUrl.isNotEmpty()) {
            AsyncImage(
                model = person.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AvatarSize).clip(CircleShape)
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val accent = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        if (active) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(180),
        label = "mediaChip"
    )
    Row(
        Modifier.clip(CircleShape).background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger || active) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (danger) accent else Color.Unspecified,
            maxLines = 1
        )
    }
}

@Composable
private fun MenuChip(
    icon: ImageVector,
    label: String,
    items: @Composable (close: () -> Unit) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        ActionChip(icon, label) { open = true }
        ChatoneDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items {
                open = false
            }
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "${speed}×"

