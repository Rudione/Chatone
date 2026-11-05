package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.close
import chatone.composeapp.generated.resources.profile_ban
import chatone.composeapp.generated.resources.profile_block
import chatone.composeapp.generated.resources.profile_detach_card
import chatone.composeapp.generated.resources.profile_mute_chip_global
import chatone.composeapp.generated.resources.profile_note
import chatone.composeapp.generated.resources.profile_pin_card
import chatone.composeapp.generated.resources.profile_send_whisper
import chatone.composeapp.generated.resources.profile_timeouts_label
import chatone.composeapp.generated.resources.profile_unban
import chatone.composeapp.generated.resources.profile_unblock
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.BellOff
import io.rudione.chatone.icons.lucide.CalendarDays
import io.rudione.chatone.icons.lucide.CircleCheck
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Gem
import io.rudione.chatone.icons.lucide.Heart
import io.rudione.chatone.icons.lucide.LockOpen
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageCircle
import io.rudione.chatone.icons.lucide.NotebookPen
import io.rudione.chatone.icons.lucide.Pin
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.icons.lucide.ShieldCheck
import io.rudione.chatone.icons.lucide.Star
import io.rudione.chatone.icons.lucide.UserX
import io.rudione.chatone.icons.lucide.X

internal val PROFILE_CARD_MIN_WIDTH = 250.dp
internal val PROFILE_CARD_MIN_HEIGHT = 260.dp
internal val PROFILE_CARD_MAX_WIDTH = 720.dp
internal val PROFILE_CARD_MAX_HEIGHT = 900.dp
internal val PROFILE_CARD_CORNER = 14.dp
internal val PROFILE_CARD_COMPACT_WIDTH = 340.dp
internal val PROFILE_CARD_DEFAULT_WIDTH = 300.dp
internal val PROFILE_CARD_DEFAULT_HEIGHT = 420.dp

internal data class ProfileTimeoutPreset(val seconds: Int, val label: String)

internal val PROFILE_TIMEOUT_PRESETS = listOf(
    ProfileTimeoutPreset(1, "1s"),
    ProfileTimeoutPreset(30, "30s"),
    ProfileTimeoutPreset(60, "1m"),
    ProfileTimeoutPreset(300, "5m"),
    ProfileTimeoutPreset(600, "10m"),
    ProfileTimeoutPreset(1800, "30m"),
    ProfileTimeoutPreset(3600, "1h"),
    ProfileTimeoutPreset(86400, "1d"),
    ProfileTimeoutPreset(604800, "1w"),
    ProfileTimeoutPreset(1_209_600, "2w")
)

private val ProfileStripSingleRowMinWidth = 340.dp

@Composable
internal fun ProfileCardSurface(
    width: Dp,
    height: Dp,
    onResize: ((Dp, Dp) -> Unit)?,
    drawBorder: Boolean = true,
    cornerRadius: Dp = PROFILE_CARD_CORNER,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (drawBorder) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    shape
                ) else Modifier
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }

        if (onResize != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .pointerInput(width, height) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            with(density) {
                                onResize(
                                    (width + drag.x.toDp())
                                        .coerceIn(PROFILE_CARD_MIN_WIDTH, PROFILE_CARD_MAX_WIDTH),
                                    (height + drag.y.toDp())
                                        .coerceIn(PROFILE_CARD_MIN_HEIGHT, PROFILE_CARD_MAX_HEIGHT)
                                )
                            }
                        }
                    }
            ) {
                ResizeGrip(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ResizeGrip(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    androidx.compose.foundation.Canvas(modifier = modifier.size(10.dp)) {
        val step = size.width / 3f
        val stroke = 1.2f
        for (i in 1..2) {
            drawLine(
                color = color,
                start = Offset(size.width - i * step, size.height),
                end = Offset(size.width, size.height - i * step),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
internal fun ProfileCardHeaderActions(
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onDetach: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatoneIconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
            Icon(
                Lucide.Pin,
                contentDescription = stringResource(Res.string.profile_pin_card),
                modifier = Modifier.size(14.dp),
                tint = if (isPinned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
        if (onDetach != null) {
            ChatoneIconButton(onClick = onDetach, modifier = Modifier.size(24.dp)) {
                Icon(
                    Lucide.ExternalLink,
                    contentDescription = stringResource(Res.string.profile_detach_card),
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
        if (isPinned) {
            ChatoneIconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    Lucide.X,
                    contentDescription = stringResource(Res.string.close),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
internal fun ProfileModerationStrip(
    canModerate: Boolean,
    targetIsBroadcaster: Boolean,
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!canModerate || targetIsBroadcaster) return
    val extra = ChatoneTheme.extraColors
    val pillHeight = if (compact) 22.dp else 26.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useGrid = compact || maxWidth < ProfileStripSingleRowMinWidth
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp)
        ) {
            if (!compact) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.profile_unban),
                        style = MaterialTheme.typography.labelSmall,
                        color = extra.modUnban.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(Res.string.profile_timeouts_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(Res.string.profile_ban),
                        style = MaterialTheme.typography.labelSmall,
                        color = extra.modBan.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            if (useGrid) {
                ProfileTimeoutGrid(
                    pillHeight = pillHeight,
                    onTimeout = onTimeout,
                    onBan = onBan,
                    onUnban = onUnban
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModerationSquare(
                        icon = Lucide.CircleCheck,
                        tint = extra.modUnban,
                        contentDescription = stringResource(Res.string.profile_unban),
                        height = pillHeight,
                        onClick = onUnban
                    )
                    PROFILE_TIMEOUT_PRESETS.forEach { preset ->
                        ModerationPill(
                            label = preset.label,
                            tint = extra.modTimeout,
                            height = pillHeight,
                            onClick = { onTimeout(preset.seconds) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModerationSquare(
                        icon = Lucide.Ban,
                        tint = extra.modBan,
                        contentDescription = stringResource(Res.string.profile_ban),
                        height = pillHeight,
                        onClick = onBan
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTimeoutGrid(
    pillHeight: Dp,
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit
) {
    val extra = ChatoneTheme.extraColors
    val half = (PROFILE_TIMEOUT_PRESETS.size + 1) / 2

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModerationSquare(
                icon = Lucide.CircleCheck,
                tint = extra.modUnban,
                contentDescription = stringResource(Res.string.profile_unban),
                height = pillHeight,
                onClick = onUnban
            )
            PROFILE_TIMEOUT_PRESETS.take(half).forEach { preset ->
                ModerationPill(
                    label = preset.label,
                    tint = extra.modTimeout,
                    height = pillHeight,
                    onClick = { onTimeout(preset.seconds) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PROFILE_TIMEOUT_PRESETS.drop(half).forEach { preset ->
                ModerationPill(
                    label = preset.label,
                    tint = extra.modTimeout,
                    height = pillHeight,
                    onClick = { onTimeout(preset.seconds) },
                    modifier = Modifier.weight(1f)
                )
            }
            ModerationSquare(
                icon = Lucide.Ban,
                tint = extra.modBan,
                contentDescription = stringResource(Res.string.profile_ban),
                height = pillHeight,
                onClick = onBan
            )
        }
    }
}

@Composable
private fun ModerationPill(
    label: String,
    tint: Color,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = moderationTintAlpha(hovered, pressed, 0.13f)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun ModerationSquare(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    height: Dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = height)
            .clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = moderationTintAlpha(hovered, pressed, 0.15f)))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(15.dp), tint = tint)
    }
}

private fun moderationTintAlpha(hovered: Boolean, pressed: Boolean, resting: Float): Float = when {
    pressed -> resting + 0.24f
    hovered -> resting + 0.15f
    else -> resting
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileQuickActions(
    canModerate: Boolean,
    canManageRoles: Boolean,
    targetIsBroadcaster: Boolean,
    targetIsModerator: Boolean,
    targetIsVip: Boolean,
    isBlocked: Boolean,
    mentionsMuted: Boolean,
    noteFilled: Boolean,
    notesOpen: Boolean,
    onToggleNotes: () -> Unit,
    onToggleMentionMute: () -> Unit,
    onBlockToggle: () -> Unit,
    onWhisper: () -> Unit,
    onMod: () -> Unit,
    onUnmod: () -> Unit,
    onVip: () -> Unit,
    onUnvip: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(
            horizontal = if (compact) 6.dp else 10.dp,
            vertical = if (compact) 4.dp else 5.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        QuickActionChip(
            icon = Lucide.MessageCircle,
            label = stringResource(Res.string.profile_send_whisper),
            tint = MaterialTheme.colorScheme.primary,
            active = false,
            compact = compact,
            onClick = onWhisper
        )
        QuickActionChip(
            icon = Lucide.NotebookPen,
            label = stringResource(Res.string.profile_note),
            tint = MaterialTheme.colorScheme.tertiary,
            active = notesOpen || noteFilled,
            compact = compact,
            onClick = onToggleNotes
        )
        QuickActionChip(
            icon = Lucide.BellOff,
            label = stringResource(Res.string.profile_mute_chip_global),
            tint = MaterialTheme.colorScheme.error,
            active = mentionsMuted,
            compact = compact,
            onClick = onToggleMentionMute
        )
        if (!targetIsBroadcaster) {
            QuickActionChip(
                icon = if (isBlocked) Lucide.LockOpen else Lucide.UserX,
                label = if (isBlocked) stringResource(Res.string.profile_unblock) else stringResource(Res.string.profile_block),
                tint = MaterialTheme.colorScheme.error,
                active = isBlocked,
                compact = compact,
                onClick = onBlockToggle
            )
        }
        if (canModerate && canManageRoles && !targetIsBroadcaster) {
            QuickActionChip(
                icon = if (targetIsModerator) Lucide.ShieldCheck else Lucide.Shield,
                label = "Mod",
                tint = extra.connected,
                active = targetIsModerator,
                compact = compact,
                onClick = { if (targetIsModerator) onUnmod() else onMod() }
            )
            QuickActionChip(
                icon = Lucide.Gem,
                label = "VIP",
                tint = Color(0xFFE005B9),
                active = targetIsVip,
                compact = compact,
                onClick = { if (targetIsVip) onUnvip() else onVip() }
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    active: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        active -> tint.copy(alpha = 0.18f)
        hovered -> tint.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(
                1.dp,
                if (active) tint.copy(alpha = 0.42f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                RoundedCornerShape(8.dp)
            )
            .hoverable(interaction)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 5.dp else 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(12.dp),
            tint = if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ProfileIdentityLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = tint)
        Spacer(Modifier.width(3.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal val ProfileIconJoined = Lucide.CalendarDays
internal val ProfileIconFollow = Lucide.Heart
internal val ProfileIconSub = Lucide.Star
