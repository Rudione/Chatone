package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Build
import io.rudione.chatone.util.icons.TwitchPointsIcon
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ic_sword
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.presentation.chat.RoomState
import io.rudione.chatone.presentation.chat.multichat.ChatTopBarAddPanelButton
import io.rudione.chatone.presentation.chat.multichat.horizontalMouseWheelScrollModifier
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import org.jetbrains.compose.resources.painterResource
import io.rudione.chatone.presentation.components.ChatoneIconButton

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
    onRefresh: () -> Unit = {},
    onOpenPointsBits: () -> Unit = {},
    isCompact: Boolean = false,
    showMenuButton: Boolean = false,
) {
    val topBarWallpaper = LocalWallpaperController.current.state
    val topBarBlur =
        if (topBarWallpaper.glowEffectsEnabled) topBarWallpaper.panelColorConfig.topBarBlurRadius else 0f
    val topBarColor = topBarBackgroundColor(topBarWallpaper, MaterialTheme.colorScheme.surfaceContainer)
    val s = LocalStrings.current

    Box(modifier = Modifier.fillMaxWidth()) {
        if (topBarBlur > 0f) {
            Box(modifier = Modifier.matchParentSize().background(topBarColor).panelBlur(topBarBlur))
        } else {
            Box(modifier = Modifier.matchParentSize().background(topBarColor))
        }
        Column {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showMenuButton) {
                        ChatoneIconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    ChannelHeaderBlock(
                        channelLogin = channelLogin,
                        channelDisplayName = channelDisplayName,
                        liveStream = liveStream,
                        connectionStatus = connectionStatus,
                        isConnected = isConnected
                    )
                    val toolbarScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .horizontalScroll(toolbarScrollState)
                            .then(
                                horizontalMouseWheelScrollModifier(
                                    toolbarScrollState
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        var refreshSpinning by remember { mutableStateOf(false) }
                        val refreshRotation by animateFloatAsState(
                            targetValue = if (refreshSpinning) 360f else 0f,
                            animationSpec = tween(500),
                            finishedListener = { refreshSpinning = false }
                        )
                        ChatoneIconButton(
                            onClick = {
                                refreshSpinning = true
                                onRefresh()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp).rotate(refreshRotation)
                            )
                        }
                        ChatTopBarAddPanelButton(
                            currentChannel = channelLogin
                        )
                        LiquidGlassTooltipBox(tooltip = s.mainChannelBadgesBits) {
                            ChatoneIconButton(onClick = onOpenPointsBits, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    TwitchPointsIcon,
                                    contentDescription = s.mainChannelBadgesBits,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (isMod) {

                            if (pinnedMacros.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    pinnedMacros.forEach { macro ->
                                        LiquidGlassTooltipBox(
                                            tooltip = macro.name.ifBlank { macro.icon }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                                            alpha = 0.5f
                                                        )
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onExecuteMacro(macro) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    macro.icon,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            LiquidGlassTooltipBox(
                                tooltip = "Mod Mode"
                            ) {
                                FilledIconToggleButton(
                                    checked = modModeEnabled,
                                    onCheckedChange = { onToggleModMode() },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.15f
                                        ),
                                        checkedContentColor = MaterialTheme.colorScheme.primary,
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_sword),
                                        contentDescription = "Mod Mode",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            LiquidGlassTooltipBox(
                                tooltip = "Mod Panel"
                            ) {
                                FilledIconToggleButton(
                                    checked = modPanelOpen,
                                    onCheckedChange = { onOpenModPanel() },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.Build,
                                        contentDescription = "Mod Panel",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
