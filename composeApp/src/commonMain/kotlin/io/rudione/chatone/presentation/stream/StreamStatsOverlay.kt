package io.rudione.chatone.presentation.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.data.repository.StreamerModeController
import io.rudione.chatone.domain.stream.StreamStats
import org.koin.compose.koinInject
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.StreamPlayerStrings

@Composable
internal fun BoxScope.StreamStatsOverlay(
    host: StreamPlayerHost,
    onClose: () -> Unit,
    compact: Boolean
) {
    val stats by host.stats.collectAsState()
    val strings = LocalStrings.current.player
    val streamerMode by koinInject<StreamerModeController>().state.collectAsState()
    val maskIdentifiers = streamerMode.enabled && streamerMode.options.hideTokens
    val rows = remember(stats, strings, maskIdentifiers) {
        stats?.let { statsRows(it, strings, maskIdentifiers) }.orEmpty()
    }
    val labelSize = if (compact) 9.5.sp else 11.5.sp
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(
                start = if (compact) 6.dp else 14.dp,
                end = if (compact) 6.dp else 14.dp,
                top = if (compact) 48.dp else 72.dp,
                bottom = if (compact) 6.dp else 72.dp
            )
            .widthIn(max = if (compact) 340.dp else 360.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.videoStats,
                color = Color.White,
                fontSize = labelSize,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp, top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp)
        ) {
            rows.forEach { (label, value) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = labelSize,
                        lineHeight = labelSize * 1.25f,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.42f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = labelSize,
                        lineHeight = labelSize * 1.25f,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.58f)
                    )
                }
            }
        }
    }
}

private fun statsRows(stats: StreamStats, strings: StreamPlayerStrings, maskIdentifiers: Boolean): List<Pair<String, String>> {
    fun identifier(value: String): String = if (maskIdentifiers) MASK else value
    val separator = strings.decimalSeparator
    fun seconds(value: Float?): String =
        value?.let { "${StreamFormatting.decimal(it, 2, separator)} ${strings.unitSeconds}" } ?: DASH
    fun kbps(value: Int?): String = value?.let { "$it ${strings.unitKbps}" } ?: DASH
    return listOfNotNull(
        strings.statDownloadResolution to stats.downloadResolution,
        strings.statRenderResolution to stats.renderResolution,
        strings.statViewportResolution to stats.viewportResolution,
        strings.statDownloadBitrate to kbps(stats.downloadBitrateKbps),
        strings.statBandwidth to kbps(stats.bandwidthEstimateKbps),
        strings.statFps to (stats.framesPerSecond?.let { StreamFormatting.decimal(it, 0, separator) } ?: DASH),
        strings.statSkippedFrames to stats.droppedFrames.toString(),
        strings.statBuffer to seconds(stats.bufferSeconds),
        strings.statLatency to seconds(stats.latencySeconds),
        strings.statCodecs to stats.codecs,
        strings.statProtocol to stats.protocol,
        strings.statLatencyMode to if (stats.lowLatency) strings.statLatencyLow else strings.statLatencyNormal,
        strings.statRenderSurface to stats.renderSurface,
        strings.statBackend to stats.backendVersion,
        strings.statPlaySession to identifier(stats.playSessionId),
        stats.servingId?.let { strings.statServingId to identifier(it) }
    )
}

private const val DASH = "—"
private const val MASK = "••••••••"
