package io.rudione.chatone.presentation.chat.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.chat.LocalChatSurfaceStyle
import io.rudione.chatone.presentation.chat.RoomState
import io.rudione.chatone.presentation.chat.multichat.ChatTopBarAddPanelButton
import io.rudione.chatone.presentation.chat.multichat.horizontalMouseWheelScrollModifier
import io.rudione.chatone.presentation.components.ChatoneCountBadge
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.onSecondaryClick
import io.rudione.chatone.presentation.settings.components.MacroIcon
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.presentation.stream.StreamWatchButton
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
    val actions = remember(onRefresh, onOpenPointsBits, onToggleModMode, onOpenModPanel, onExecuteMacro, onEditMacro) {
        TopBarActionCallbacks(onRefresh, onOpenPointsBits, onToggleModMode, onOpenModPanel, onExecuteMacro, onEditMacro)
    }

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
                .height(metrics.barHeight)
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
                    ChatoneIconButton(onClick = onBack, modifier = Modifier.size(metrics.button)) {
                        Icon(
                            Lucide.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface,
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
                    callbacks = actions,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .horizontalScroll(toolbarScrollState)
                        .then(horizontalMouseWheelScrollModifier(toolbarScrollState))
                )
            }
        }
    }
}

@Immutable
private class TopBarActionCallbacks(
    val onRefresh: () -> Unit,
    val onOpenPointsBits: () -> Unit,
    val onToggleModMode: () -> Unit,
    val onOpenModPanel: () -> Unit,
    val onExecuteMacro: (Macro) -> Unit,
    val onEditMacro: (Macro) -> Unit
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
    callbacks: TopBarActionCallbacks,
    modifier: Modifier
) {
    val s = LocalStrings.current
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
        val refreshRotation by animateFloatAsState(
            targetValue = if (refreshSpinning) 360f else 0f,
            animationSpec = tween(500),
            finishedListener = { refreshSpinning = false }
        )
        ChatoneIconButton(
            onClick = {
                refreshSpinning = true
                callbacks.onRefresh()
            },
            modifier = Modifier.size(metrics.button)
        ) {
            Icon(
                Lucide.RefreshCw,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(metrics.icon).rotate(refreshRotation)
            )
        }
        ChatTopBarAddPanelButton(currentChannel = channelLogin)
        LiquidGlassTooltipBox(tooltip = s.mainChannelBadgesBits) {
            ChatoneIconButton(onClick = callbacks.onOpenPointsBits, modifier = Modifier.size(metrics.button)) {
                Icon(
                    TwitchPointsIcon,
                    contentDescription = s.mainChannelBadgesBits,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(metrics.icon)
                )
            }
        }
        if (!isMod) return@Row
        if (pinnedMacros.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                pinnedMacros.forEach { macro ->
                    LiquidGlassTooltipBox(
                        tooltip = s.format(s.chatMacroTooltip, macro.name.ifBlank { macro.icon })
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
        LiquidGlassTooltipBox(tooltip = "Mod Mode") {
            FilledIconToggleButton(
                checked = modModeEnabled,
                onCheckedChange = { callbacks.onToggleModMode() },
                modifier = Modifier.size(metrics.button),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    checkedContentColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Lucide.Swords,
                    contentDescription = "Mod Mode",
                    modifier = Modifier.size(metrics.icon)
                )
            }
        }
        LiquidGlassTooltipBox(tooltip = "Mod Panel") {
            FilledIconToggleButton(
                checked = modPanelOpen,
                onCheckedChange = { callbacks.onOpenModPanel() },
                modifier = Modifier.size(metrics.button),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    Lucide.Wrench,
                    contentDescription = "Mod Panel",
                    modifier = Modifier.size(metrics.icon)
                )
            }
        }
    }
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
    val container by animateColorAsState(
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
    val border by animateColorAsState(
        accent.copy(alpha = if (hovered) 0.6f else 0.3f),
        tween(120),
        label = "macroBorder"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(MacroButtonShape)
            .background(container)
            .border(1.dp, border, MacroButtonShape)
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
