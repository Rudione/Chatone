package io.rudione.chatone.presentation.window

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.streamer_mode_title
import chatone.composeapp.generated.resources.update_available_title
import chatone.composeapp.generated.resources.window_pin
import chatone.composeapp.generated.resources.window_unpin
import io.rudione.chatone.data.repository.StreamerModeController
import io.rudione.chatone.icons.lucide.Download
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Shield
import io.rudione.chatone.presentation.components.AnimatedLockIcon
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.system.AutoUpdater
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun DesktopTitleBarActions(
    settings: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    background: Color
) {
    val streamerMode = koinInject<StreamerModeController>()
    val privacy by streamerMode.state.collectAsState()
    val update by AutoUpdater.available.collectAsState()
    val foreground = if (background.luminance() < 0.5f) Color(0xFFE6E6EC) else Color(0xFF1A1A20)

    if (update != null) {
        val label = "${stringResource(Res.string.update_available_title)} · v${update?.version.orEmpty()}"
        TitleBarAction(label, true, foreground, AutoUpdater::showAvailable) { tint ->
            Icon(Lucide.Download, null, Modifier.size(13.dp), tint = tint)
        }
    }
    TitleBarAction(
        label = stringResource(Res.string.streamer_mode_title),
        active = privacy.enabled,
        foreground = foreground,
        onClick = { streamerMode.setEnabled(!privacy.enabled) }
    ) { tint ->
        Icon(Lucide.Shield, null, Modifier.size(13.dp), tint = tint)
    }
    TitleBarAction(
        label = stringResource(if (settings.alwaysOnTop) Res.string.window_unpin else Res.string.window_pin),
        active = settings.alwaysOnTop,
        foreground = foreground,
        onClick = { onEvent(SettingsEvent.OnAlwaysOnTopChanged(!settings.alwaysOnTop)) }
    ) { tint ->
        AnimatedLockIcon(settings.alwaysOnTop, tint, Modifier.size(14.dp))
    }
}

@Composable
private fun TitleBarAction(
    label: String,
    active: Boolean,
    foreground: Color,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val fill by animateColorAsState(
        foreground.copy(alpha = if (hovered) 0.18f else if (active) 0.12f else 0f),
        label = "captionActionFill"
    )
    LiquidGlassTooltipBox(tooltip = label) {
        Box(
            modifier = Modifier.padding(vertical = 3.dp).size(28.5.dp, 18.dp)
                .clip(RoundedCornerShape(5.dp)).background(fill)
                .hoverable(interaction)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = label
                    selected = active
                },
            contentAlignment = Alignment.Center
        ) {
            content(foreground.copy(alpha = if (active || hovered) 1f else 0.7f))
        }
    }
}
