package io.rudione.chatone.presentation.chat.emote

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.emote_info_hold_seventv
import chatone.composeapp.generated.resources.ui_by
import chatone.composeapp.generated.resources.ui_original
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val AUTO_HIDE_MS = 5_000L
private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun EmoteInfoPopup(
    emote: GenericEmote,
    onDismiss: () -> Unit,
    image: @Composable (Modifier) -> Unit
) {
    val gapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val position = remember(gapPx) { AboveAnchorPosition(gapPx) }
    LaunchedEffect(emote.id) {
        delay(AUTO_HIDE_MS)
        onDismiss()
    }
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
    ) {
        val appear = remember { Animatable(0.6f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) }
        EmoteInfoCard(
            emote = emote,
            image = image,
            modifier = Modifier.graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
                alpha = ((appear.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
        )
    }
}

@Composable
fun EmoteInfoCard(emote: GenericEmote, image: @Composable (Modifier) -> Unit, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.97f), surface.copy(alpha = 0.9f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))), CardShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        image(Modifier.widthIn(min = 56.dp, max = 200.dp).heightIn(min = 40.dp, max = 112.dp))
        Text(emote.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (emote.originalName.isNotEmpty() && emote.originalName != emote.code) {
            InfoLine(stringResource(Res.string.ui_original), emote.originalName, MaterialTheme.colorScheme.onSurface)
        }
        if (emote.authorName.isNotEmpty()) {
            InfoLine(stringResource(Res.string.ui_by), emote.authorName, MaterialTheme.colorScheme.primary)
        }
        val (label, color) = providerBadge(emote.provider)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)
        )
        if (emote.provider == EmoteProvider.SEVEN_TV) {
            Text(
                stringResource(Res.string.emote_info_hold_seventv),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

private fun providerBadge(provider: EmoteProvider): Pair<String, Color> = when (provider) {
    EmoteProvider.SEVEN_TV -> "7TV" to Color(0xFF0288D1)
    EmoteProvider.BTTV -> "BTTV" to Color(0xFF43A047)
    EmoteProvider.FFZ -> "FFZ" to Color(0xFF8E24AA)
    EmoteProvider.TWITCH -> "Twitch" to Color(0xFF9146FF)
}

private class AboveAnchorPosition(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val above = anchorBounds.top - gap - popupContentSize.height
        val y = if (above >= 0) above else (anchorBounds.bottom + gap).coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}
