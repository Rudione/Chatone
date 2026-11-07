package io.rudione.chatone.presentation.chat.components

import chatone.composeapp.generated.resources.ui_menu
import chatone.composeapp.generated.resources.ui_mod_mode
import chatone.composeapp.generated.resources.ui_mod_panel
import chatone.composeapp.generated.resources.ui_refresh
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_macro_tooltip
import chatone.composeapp.generated.resources.main_channel_badges_bits
import chatone.composeapp.generated.resources.points_balance_description
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.rudione.chatone.presentation.components.animatedBorder
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.chat.LocalChatSurfaceStyle
import io.rudione.chatone.presentation.chat.RoomState
import io.rudione.chatone.presentation.chat.multichat.ChatTopBarAddPanelButton
import io.rudione.chatone.presentation.chat.multichat.horizontalMouseWheelScrollModifier
import io.rudione.chatone.presentation.components.ChatoneCountBadge
import io.rudione.chatone.presentation.components.TactileIconButton
import io.rudione.chatone.presentation.components.TactileIconButtonDefaults
import io.rudione.chatone.presentation.components.onSecondaryClick
import io.rudione.chatone.presentation.components.tactilePress
import io.rudione.chatone.presentation.components.onUnclaimedSecondaryClick
import io.rudione.chatone.presentation.channelmenu.ChannelOptionsMenu
import io.rudione.chatone.presentation.channelmenu.busChannelMenu
import io.rudione.chatone.presentation.settings.components.MacroIcon
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.presentation.stream.StreamWatchButton
import io.rudione.chatone.util.format.groupedNumber
import io.rudione.chatone.util.icons.TwitchPointsIcon
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Menu
import io.rudione.chatone.icons.lucide.RefreshCw
import io.rudione.chatone.icons.lucide.Swords
import io.rudione.chatone.icons.lucide.Wrench

private val DefaultBarHeight = 36.dp
private val MinButtonSize = 22.dp
private val MaxButtonSize = 44.dp
private val MacroButtonShape = RoundedCornerShape(7.dp)

@Immutable
private data class TopBarMetrics(
    val barHeight: Dp,
    val button: Dp,
    val icon: Dp,
    val macro: Dp
) {
    companion object {
        fun forHeight(height: Dp): TopBarMetrics {
            val button = (height - 6.dp).coerceIn(MinButtonSize, MaxButtonSize)
            return TopBarMetrics(
                barHeight = height,
                button = button,
                icon = button * 0.54f,
                macro = (button - 4.dp).coerceAtLeast(18.dp)
            )
        }
    }
}

@Composable
internal fun ChatTopBar(
    channelLogin: String,
    channelDisplayName: String = "",
    liveStream: io.rudione.chatone.data.remote.dto.ChannelData? = null,
    connectionStatus: String,
    isConnected: Boolean,
    roomState: RoomState,
    isMod: Boolean,
    modModeEnabled: Boolean,
    modPanelOpen: Boolean = false,
    pinnedMacros: List<Macro> = emptyList(),
    onBack: () -> Unit,
    onToggleModMode: () -> Unit,
    onOpenModPanel: () -> Unit = {},
    onExecuteMacro: (Macro) -> Unit = {},
    onEditMacro: (Macro) -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenPointsBits: () -> Unit = {},
    points: TopBarPoints = TopBarPoints(),
    isCompact: Boolean = false,
    showMenuButton: Boolean = false,
    menuBadgeCount: Int = 0,
    barHeight: Dp = DefaultBarHeight
) {
    val topBarWallpaper = LocalWallpaperController.current.state
    val topBarBlur =
        if (topBarWallpaper.glowEffectsEnabled) topBarWallpaper.panelColorConfig.topBarBlurRadius else 0f
    val topBarColor = topBarBackgroundColor(topBarWallpaper, MaterialTheme.colorScheme.surfaceContainer)
    val surfaceStyle = LocalChatSurfaceStyle.current
    val metrics = remember(barHeight) { TopBarMetrics.forHeight(barHeight) }
    val actions = remember(onRefresh, onOpenPointsBits, onToggleModMode, onOpenModPanel, onExecuteMacro, onEditMacro, points.onWanted) {
        TopBarActionCallbacks(onRefresh, onOpenPointsBits, onToggleModMode, onOpenModPanel, onExecuteMacro, onEditMacro, points.onWanted)
    }
    val channelMenu = remember(channelLogin, isMod) { busChannelMenu(channelLogin, canSeeViewers = isMod) }
    var barMenuAt by remember { mutableStateOf<Dp?>(null) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val narrowBar = maxWidth < NarrowBarWidth
        if (topBarBlur > 0f) {
            Box(modifier = Modifier.matchParentSize().background(topBarColor).panelBlur(topBarBlur))
        } else {
            Box(modifier = Modifier.matchParentSize().background(topBarColor))
        }
        val headerScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .height(metrics.barHeight)
                .onUnclaimedSecondaryClick { at -> barMenuAt = with(density) { at.x.toDp() } }
                .then(
                    if (surfaceStyle.scrollableHeader) {
                        Modifier.horizontalScroll(headerScrollState)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showMenuButton) {
                Box {
                    TactileIconButton(
                        onClick = onBack,
                        size = metrics.button,
                        colors = TactileIconButtonDefaults.colors(content = MaterialTheme.colorScheme.onSurface)
                    ) { tint ->
                        Icon(
                            Lucide.Menu,
                            contentDescription = stringResource(Res.string.ui_menu),
                            tint = tint,
                            modifier = Modifier.size(metrics.icon + 2.dp)
                        )
                    }
                    ChatoneCountBadge(
                        count = menuBadgeCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-2).dp)
                    )
                }
            }
            ChannelHeaderBlock(
                channelLogin = channelLogin,
                channelDisplayName = channelDisplayName,
                liveStream = liveStream,
                connectionStatus = connectionStatus,
                isConnected = isConnected,
                channelMenu = channelMenu,
                singleLine = metrics.barHeight < SingleLineHeaderBelow,
                compactLatency = narrowBar
            )
            if (surfaceStyle.scrollableHeader) {
                TopBarActions(
                    channelLogin = channelLogin,
                    metrics = metrics,
                    isMod = isMod,
                    modModeEnabled = modModeEnabled,
                    modPanelOpen = modPanelOpen,
                    pinnedMacros = pinnedMacros,
                    showWatchButton = surfaceStyle.showWatchButton,
                    points = points,
                    callbacks = actions,
                    modifier = Modifier
                )
            } else {
                val toolbarScrollState = rememberScrollState()
                TopBarActions(
                    channelLogin = channelLogin,
                    metrics = metrics,
                    isMod = isMod,
                    modModeEnabled = modModeEnabled,
                    modPanelOpen = modPanelOpen,
                    pinnedMacros = pinnedMacros,
                    showWatchButton = surfaceStyle.showWatchButton,
                    points = points,
                    callbacks = actions,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .horizontalScroll(toolbarScrollState)
                        .then(horizontalMouseWheelScrollModifier(toolbarScrollState))
                )
            }
        }
        ChannelOptionsMenu(
            expanded = barMenuAt != null,
            onDismissRequest = { barMenuAt = null },
            model = channelMenu,
            offset = DpOffset(barMenuAt ?: 0.dp, 0.dp)
        )
    }
}

@Immutable
private class TopBarActionCallbacks(
    val onRefresh: () -> Unit,
    val onOpenPointsBits: () -> Unit,
    val onToggleModMode: () -> Unit,
    val onOpenModPanel: () -> Unit,
    val onExecuteMacro: (Macro) -> Unit,
    val onEditMacro: (Macro) -> Unit,
    val onPointsWanted: () -> Unit
)

@Immutable
data class TopBarPoints(
    val balance: Long? = null,
    val iconUrl: String? = null,
    val onWanted: () -> Unit = {}
)

@Composable
private fun TopBarActions(
    channelLogin: String,
    metrics: TopBarMetrics,
    isMod: Boolean,
    modModeEnabled: Boolean,
    modPanelOpen: Boolean,
    pinnedMacros: List<Macro>,
    showWatchButton: Boolean,
    points: TopBarPoints,
    callbacks: TopBarActionCallbacks,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (showWatchButton) {
            StreamWatchButton(
                channelLogin = channelLogin,
                buttonSize = metrics.button,
                iconSize = metrics.icon + 2.dp
            )
        }
        var refreshSpinning by remember { mutableStateOf(false) }
        val refreshRotation = animateFloatAsState(
            targetValue = if (refreshSpinning) 360f else 0f,
            animationSpec = tween(500),
            finishedListener = { refreshSpinning = false }
        )
        TactileIconButton(
            onClick = {
                refreshSpinning = true
                callbacks.onRefresh()
            },
            size = metrics.button
        ) { tint ->
            Icon(
                Lucide.RefreshCw,
                contentDescription = stringResource(Res.string.ui_refresh),
                tint = tint,
                modifier = Modifier.size(metrics.icon).graphicsLayer { rotationZ = refreshRotation.value }
            )
        }
        PointsBalanceTooltipBox(points = points, onWanted = callbacks.onPointsWanted) {
            TactileIconButton(onClick = callbacks.onOpenPointsBits, size = metrics.button) { tint ->
                Icon(
                    TwitchPointsIcon,
                    contentDescription = stringResource(Res.string.main_channel_badges_bits),
                    tint = tint,
                    modifier = Modifier.size(metrics.icon)
                )
            }
        }
        if (isMod) {
            if (pinnedMacros.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    pinnedMacros.forEach { macro ->
                        LiquidGlassTooltipBox(
                            tooltip = stringResource(Res.string.chat_macro_tooltip, macro.name.ifBlank { macro.icon })
                        ) {
                            MacroQuickButton(
                                macro = macro,
                                size = metrics.macro,
                                onRun = { callbacks.onExecuteMacro(macro) },
                                onEdit = { callbacks.onEditMacro(macro) }
                            )
                        }
                    }
                }
            }
            val modModeLabel = stringResource(Res.string.ui_mod_mode)
            LiquidGlassTooltipBox(tooltip = modModeLabel) {
                TactileIconButton(
                    onClick = callbacks.onToggleModMode,
                    size = metrics.button,
                    checked = modModeEnabled
                ) { tint ->
                    Icon(
                        imageVector = Lucide.Swords,
                        contentDescription = modModeLabel,
                        tint = tint,
                        modifier = Modifier.size(metrics.icon)
                    )
                }
            }
            val modPanelLabel = stringResource(Res.string.ui_mod_panel)
            LiquidGlassTooltipBox(tooltip = modPanelLabel) {
                TactileIconButton(
                    onClick = callbacks.onOpenModPanel,
                    size = metrics.button,
                    checked = modPanelOpen,
                    colors = TactileIconButtonDefaults.colors(
                        checkedContainer = MaterialTheme.colorScheme.secondaryContainer,
                        checkedContent = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) { tint ->
                    Icon(
                        Lucide.Wrench,
                        contentDescription = modPanelLabel,
                        tint = tint,
                        modifier = Modifier.size(metrics.icon)
                    )
                }
            }
        }
        ChatTopBarAddPanelButton(
            currentChannel = channelLogin,
            buttonSize = metrics.button,
            iconSize = metrics.icon
        )
    }
}

@Composable
private fun PointsBalanceTooltipBox(
    points: TopBarPoints,
    onWanted: () -> Unit,
    content: @Composable () -> Unit
) {
    val fallback = stringResource(Res.string.main_channel_badges_bits)
    LiquidGlassTooltipBox(
        tooltipContent = {
            LaunchedEffect(Unit) { onWanted() }
            val balance = points.balance
            if (balance == null) {
                Text(
                    text = fallback,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                val formatted = groupedNumber(balance)
                val description = stringResource(Res.string.points_balance_description, formatted)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description }
                ) {
                    CurrencyIcon("POINTS", points.iconUrl, size = 14, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        content = content
    )
}

private val NarrowBarWidth = 360.dp
private val SingleLineHeaderBelow = 34.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MacroQuickButton(
    macro: Macro,
    size: Dp,
    onRun: () -> Unit,
    onEdit: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val accent = MaterialTheme.colorScheme.primary
    val container = animateColorAsState(
        accent.copy(
            alpha = when {
                pressed -> 0.34f
                hovered -> 0.26f
                else -> 0.16f
            }
        ),
        tween(120),
        label = "macroContainer"
    )
    val border = animateColorAsState(
        accent.copy(alpha = if (hovered) 0.6f else 0.3f),
        tween(120),
        label = "macroBorder"
    )
    Box(
        modifier = Modifier
            .size(size)
            .tactilePress(interaction, pressedScale = 0.86f)
            .clip(MacroButtonShape)
            .drawBehind { drawRect(container.value) }
            .animatedBorder(1.dp, MacroButtonShape) { border.value }
            .onSecondaryClick(onEdit)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = onEdit,
                onClick = onRun
            )
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        MacroIcon(
            macro.icon,
            size = 14.dp,
            fontSize = 12.sp,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
