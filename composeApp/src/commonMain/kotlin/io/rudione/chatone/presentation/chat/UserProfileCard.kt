package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.flow.first

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
    ProfileTimeoutPreset(604800, "1w")
)

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
        content()

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
                if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = LocalStrings.current.profilePinCard,
                modifier = Modifier.size(14.dp),
                tint = if (isPinned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
        if (onDetach != null) {
            ChatoneIconButton(onClick = onDetach, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = LocalStrings.current.profileDetachCard,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
        if (isPinned) {
            ChatoneIconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = LocalStrings.current.close,
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
    val s = LocalStrings.current
    val extra = ChatoneTheme.extraColors
    val pillHeight = if (compact) 22.dp else 26.dp

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp)
    ) {
        if (!compact) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.profileUnban,
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.modUnban.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    s.profileTimeoutsLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    s.profileBan,
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.modBan.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        if (compact) {
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
                    icon = Icons.Outlined.CheckCircle,
                    tint = extra.modUnban,
                    contentDescription = s.profileUnban,
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
                    icon = Icons.Outlined.Block,
                    tint = extra.modBan,
                    contentDescription = s.profileBan,
                    height = pillHeight,
                    onClick = onBan
                )
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
    val s = LocalStrings.current
    val extra = ChatoneTheme.extraColors
    val half = (PROFILE_TIMEOUT_PRESETS.size + 1) / 2

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModerationSquare(
                icon = Icons.Outlined.CheckCircle,
                tint = extra.modUnban,
                contentDescription = s.profileUnban,
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
                icon = Icons.Outlined.Block,
                tint = extra.modBan,
                contentDescription = s.profileBan,
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
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = if (hovered) 0.28f else 0.13f))
            .hoverable(interaction)
            .clickable(onClick = onClick),
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
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = height)
            .clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = if (hovered) 0.30f else 0.15f))
            .hoverable(interaction)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(15.dp), tint = tint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileQuickActions(
    canModerate: Boolean,
    currentUserIsBroadcaster: Boolean,
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
    val s = LocalStrings.current
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
            icon = Icons.Outlined.MailOutline,
            label = s.profileSendWhisper,
            tint = MaterialTheme.colorScheme.primary,
            active = false,
            compact = compact,
            onClick = onWhisper
        )
        QuickActionChip(
            icon = Icons.Outlined.Edit,
            label = s.profileNote,
            tint = MaterialTheme.colorScheme.tertiary,
            active = notesOpen || noteFilled,
            compact = compact,
            onClick = onToggleNotes
        )
        QuickActionChip(
            icon = Icons.Outlined.NotificationsOff,
            label = s.profileMuteChipGlobal,
            tint = MaterialTheme.colorScheme.error,
            active = mentionsMuted,
            compact = compact,
            onClick = onToggleMentionMute
        )
        if (!targetIsBroadcaster) {
            QuickActionChip(
                icon = if (isBlocked) Icons.Outlined.LockOpen else Icons.Outlined.Block,
                label = if (isBlocked) s.profileUnblock else s.profileBlock,
                tint = MaterialTheme.colorScheme.error,
                active = isBlocked,
                compact = compact,
                onClick = onBlockToggle
            )
        }
        if (canModerate && currentUserIsBroadcaster && !targetIsBroadcaster) {
            QuickActionChip(
                icon = if (targetIsModerator) Icons.Filled.Shield else Icons.Outlined.Shield,
                label = "Mod",
                tint = extra.connected,
                active = targetIsModerator,
                compact = compact,
                onClick = { if (targetIsModerator) onUnmod() else onMod() }
            )
            QuickActionChip(
                icon = if (targetIsVip) Icons.Filled.WorkspacePremium else Icons.Outlined.WorkspacePremium,
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
internal fun ProfileMessageFeed(
    sessionMessages: List<DisplayMessage.PrivMsg>,
    localHistory: List<io.rudione.chatone.domain.model.ChatMessage>,
    remoteHistory: List<io.rudione.chatone.data.remote.GqlUsercardMessage>,
    displayName: String,
    userColor: String?,
    isHistoryLoading: Boolean,
    hasMoreHistory: Boolean,
    historyLoadFailed: Boolean,
    onLoadHistory: (() -> Unit)?,
    highlightRules: List<HighlightRule>,
    chatFontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    val nameColor = parseHexColor(userColor) ?: MaterialTheme.colorScheme.primary

    val isEmpty = sessionMessages.isEmpty() && localHistory.isEmpty() &&
            remoteHistory.isEmpty() && !isHistoryLoading

    var stickToBottom by remember { mutableStateOf(true) }

    LaunchedEffect(sessionMessages.size) {
        if (!stickToBottom) return@LaunchedEffect
        val total = snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
        listState.scrollToItem(total - 1)
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 2
        }.collect { atBottom -> stickToBottom = atBottom }
    }

    if (onLoadHistory != null && hasMoreHistory) {
        LaunchedEffect(listState, hasMoreHistory, isHistoryLoading) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { first ->
                    if (first <= 2 && !isHistoryLoading) onLoadHistory()
                }
        }
    }

    if (isEmpty) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.MailOutline,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
                Text(
                    s.profileNoMessagesInSession,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (onLoadHistory != null) {
                    TextButton(onClick = onLoadHistory) { Text(s.profileLoadHistory) }
                }
                if (historyLoadFailed) {
                    Text(
                        s.profileHistoryLoadFailed,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        return
    }

    SelectionContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item(key = "history-controls") {
                DisableSelection {
                    when {
                        isHistoryLoading -> Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                s.profileLoadingHistory,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        hasMoreHistory && onLoadHistory != null -> TextButton(
                            onClick = onLoadHistory,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                s.profileLoadMoreHistory,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            items(remoteHistory, key = { "remote_${it.id}" }) { entry ->
                GqlHistoryMessageItem(entry, displayName, nameColor)
            }
            items(localHistory, key = { "local_${it.id}" }) { entry ->
                LocalHistoryMessageItem(entry, displayName, nameColor)
            }
            items(sessionMessages, key = { it.id }) { message ->
                PrivMsgItem(
                    message = message,
                    chatFontSizeSp = chatFontSizeSp,
                    highlightRules = highlightRules,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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

internal val ProfileIconJoined = Icons.Outlined.DateRange
internal val ProfileIconFollow = Icons.Filled.Favorite
internal val ProfileIconSub = Icons.Filled.Star
