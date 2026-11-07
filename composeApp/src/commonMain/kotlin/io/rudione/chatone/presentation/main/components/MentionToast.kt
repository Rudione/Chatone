package io.rudione.chatone.presentation.main.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.mention_in_channel
import chatone.composeapp.generated.resources.ui_dismiss
import io.rudione.chatone.icons.lucide.AtSign
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageCircle
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.TactileIconButton
import io.rudione.chatone.presentation.components.tactilePress
import org.jetbrains.compose.resources.stringResource

const val MENTION_TOAST_MS = 7_000L

private val WhisperAccent = Color(0xFF9B59B6)
private val ToastShape = RoundedCornerShape(16.dp)
private val ToastPadding = 14.dp
private val TimerInset = 4.dp
private val TimerThickness = 2.5.dp

@Immutable
data class MentionToastData(
    val id: Long,
    val fromDisplayName: String,
    val channelLogin: String,
    val text: String,
    val messageId: String? = null,
    val isWhisper: Boolean = false
)

@Composable
fun MentionToast(
    data: MentionToastData,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val remaining = remember(data.id) { Animatable(1f) }
    LaunchedEffect(data.id, hovered || pressed) {
        if (hovered || pressed) return@LaunchedEffect
        remaining.animateTo(0f, tween((remaining.value * MENTION_TOAST_MS).toInt(), easing = LinearEasing))
        onDismiss()
    }
    val accent = if (data.isWhisper) WhisperAccent else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    LiquidGlassSurface(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(horizontal = 16.dp)
            .tactilePress(interaction, pressedScale = 0.97f)
            .drawWithContent {
                drawContent()
                val inset = ToastPadding.toPx()
                val y = size.height - TimerInset.toPx()
                val stroke = TimerThickness.toPx()
                val end = size.width - inset
                drawLine(track, Offset(inset, y), Offset(end, y), stroke, StrokeCap.Round)
                val left = remaining.value
                if (left > 0f) {
                    drawLine(accent, Offset(inset, y), Offset(inset + (end - inset) * left, y), stroke, StrokeCap.Round)
                }
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onOpen)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = ToastShape,
        contentPadding = PaddingValues(start = ToastPadding, end = 6.dp, top = 10.dp, bottom = 10.dp),
        tintColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        glassIntensity = 0.98f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (data.isWhisper) Lucide.MessageCircle else Lucide.AtSign,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (data.isWhisper) data.fromDisplayName
                    else "@${data.fromDisplayName} ${stringResource(Res.string.mention_in_channel, data.channelLogin)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.text.take(120),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TactileIconButton(onClick = onDismiss, size = 28.dp) { tint ->
                Icon(
                    Lucide.X,
                    contentDescription = stringResource(Res.string.ui_dismiss),
                    modifier = Modifier.size(15.dp),
                    tint = tint
                )
            }
        }
    }
}
