package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import io.rudione.chatone.data.remote.dto.ChannelData
import io.rudione.chatone.icons.lucide.Eye
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.stream.StreamFormatting
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private val PreviewShape = RoundedCornerShape(8.dp)
private val ChipShape = RoundedCornerShape(6.dp)
private val CardShape = RoundedCornerShape(16.dp)
private val CardGap = 8.dp
private val CardMargin = 8.dp
private const val MATURE_LABEL = "18+"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StreamInfoContent(
    liveStream: ChannelData,
    showPreview: Boolean,
    footer: String?,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val content = LocalContentColor.current
    val muted = content.copy(alpha = 0.72f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val preview = liveStream.thumbnailUrl.replace("{width}", "320").replace("{height}", "180")
        if (showPreview && preview.isNotBlank()) {
            AsyncImage(
                model = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(PreviewShape)
            )
        }
        Text(
            liveStream.title.ifBlank { liveStream.userName },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (liveStream.gameName.isNotBlank()) {
            Text(
                liveStream.gameName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            if (liveStream.viewerCount > 0) {
                InfoItem(Lucide.Eye, StreamFormatting.compactCount(liveStream.viewerCount, s.player.decimalSeparator), muted)
            }
            val uptime = formatStreamUptime(liveStream.startedAt, s.unitHourShort, s.unitMinuteShort)
            if (uptime.isNotEmpty()) InfoItem(Lucide.Timer, uptime, muted)
            if (liveStream.language.isNotBlank()) InfoChip(liveStream.language.uppercase(), muted)
            if (liveStream.isMature) InfoChip(MATURE_LABEL, muted)
        }
        if (footer != null) {
            Text(footer, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.5f))
        }
    }
}

@Composable
internal fun StreamInfoPopup(liveStream: ChannelData, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val gapPx = with(density) { CardGap.roundToPx() }
    val marginPx = with(density) { CardMargin.roundToPx() }
    val position = remember(gapPx, marginPx) { AboveAnchor(gapPx, marginPx) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)) }
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .widthIn(min = 240.dp, max = 360.dp)
                .graphicsLayer {
                    val progress = appear.value
                    alpha = progress.coerceIn(0f, 1f)
                    scaleX = 0.94f + 0.06f * progress
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(0f, 1f)
                },
            shape = CardShape,
            contentPadding = PaddingValues(12.dp),
            backgroundAlphaHigh = 0.95f,
            backgroundAlphaLow = 0.88f,
            forceGlass = true
        ) {
            StreamInfoContent(liveStream = liveStream, showPreview = false, footer = null)
        }
    }
}

@Composable
private fun InfoItem(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun InfoChip(text: String, tint: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, tint.copy(alpha = 0.4f), ChipShape)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

private class AboveAnchor(private val gapPx: Int, private val marginPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val x = anchorBounds.left.coerceIn(marginPx, maxX)
        val above = anchorBounds.top - popupContentSize.height - gapPx
        val y = if (above >= marginPx) above else anchorBounds.bottom + gapPx
        return IntOffset(x, y)
    }
}
