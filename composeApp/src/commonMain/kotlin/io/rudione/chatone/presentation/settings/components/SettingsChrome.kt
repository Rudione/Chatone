package io.rudione.chatone.presentation.settings.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.icon
import androidx.compose.foundation.interaction.MutableInteractionSource
import io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox
import io.rudione.chatone.presentation.theme.ChatoneIndication
import io.rudione.chatone.presentation.window.windowDragArea
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Pin
import io.rudione.chatone.icons.lucide.PinOff
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.util.system.isDesktopPlatform

@Composable
internal fun SettingsSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .settingsCard()
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
internal fun Modifier.settingsCard(): Modifier {
    val shape = RoundedCornerShape(SettingsCardRadius)
    val edge = MaterialTheme.colorScheme.onSurface
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
        .border(1.dp, Brush.verticalGradient(listOf(edge.copy(alpha = 0.10f), edge.copy(alpha = 0.02f))), shape)
}

private val SettingsCardRadius = if (isDesktopPlatform) 18.dp else 26.dp

@Composable
internal fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            HighlightedSettingsText(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp, start = 6.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .settingsCard()
                .padding(vertical = 4.dp),
            content = content
        )
        Spacer(Modifier.height(if (isDesktopPlatform) 8.dp else 12.dp))
    }
}

@Composable
internal fun SettingsPaneHeader(
    isPinned: Boolean?,
    onTogglePin: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .windowDragArea()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPinned != null) {
            SettingsPaneIcon(
                icon = if (isPinned) Lucide.PinOff else Lucide.Pin,
                label = if (isPinned) s.settingsUnpinWindow else s.settingsPinWindow,
                tint = if (isPinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onTogglePin
            )
        }
        SettingsPaneIcon(
            icon = Lucide.X,
            label = s.close,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onClose
        )
    }
}

@Composable
private fun SettingsPaneIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    LiquidGlassTooltipBox(tooltip = label) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ChatoneIndication,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
        }
    }
}

private val TwoColumnBreakpoint = 700.dp
private val ColumnGap = 10.dp

@Composable
internal fun SettingsPair(
    first: @Composable () -> Unit,
    second: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (second == null) {
        Box(modifier = modifier.fillMaxWidth()) { first() }
        return
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= TwoColumnBreakpoint) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ColumnGap),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.weight(1f)) { first() }
                Box(modifier = Modifier.weight(1f)) { second() }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ColumnGap)
            ) {
                first()
                second()
            }
        }
    }
}
