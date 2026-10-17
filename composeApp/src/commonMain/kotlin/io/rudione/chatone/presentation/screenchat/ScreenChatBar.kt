package io.rudione.chatone.presentation.screenchat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.AppWindow
import io.rudione.chatone.icons.lucide.Lock
import io.rudione.chatone.icons.lucide.LockOpen
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Settings
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.window.WindowGrip
import io.rudione.chatone.presentation.window.windowGrip

@Composable
internal fun ScreenChatBar(
    channelLogin: String,
    locked: Boolean,
    onHeldChange: (Boolean) -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApp: (() -> Unit)?,
    onClose: () -> Unit
) {
    val strings = LocalStrings.current.screenChat
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .background(Color.Black.copy(alpha = BAR_ALPHA))
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(if (locked) Modifier else Modifier.windowGrip(WindowGrip.MOVE, onHeldChange))
                .semantics { contentDescription = strings.dragHint },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "#$channelLogin",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!locked) {
                Box(
                    Modifier
                        .width(28.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.45f))
                )
            }
        }
        if (onOpenApp != null) BarButton(icon = Lucide.AppWindow, description = strings.openApp, active = false, onClick = onOpenApp)
        BarButton(icon = Lucide.Settings, description = strings.settings, active = false, onClick = onOpenSettings)
        BarButton(
            icon = if (locked) Lucide.Lock else Lucide.LockOpen,
            description = if (locked) strings.unlock else strings.lock,
            active = locked,
            onClick = onToggleLock
        )
        BarButton(icon = Lucide.X, description = strings.close, active = false, onClick = onClose)
    }
}

@Composable
private fun BarButton(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (hovered) 0.16f else 0f))
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
internal fun ScreenChatCornerGrip(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(10.dp)) {
            val stroke = 2.dp.toPx()
            val color = Color.White.copy(alpha = 0.8f)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), stroke, StrokeCap.Round)
        }
    }
}

private val BAR_HEIGHT = 28.dp
private const val BAR_ALPHA = 0.55f
