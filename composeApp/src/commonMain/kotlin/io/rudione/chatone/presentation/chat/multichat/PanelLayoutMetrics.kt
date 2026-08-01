package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import io.rudione.chatone.domain.model.PanelLayoutMode

@Stable
data class PanelLayoutMetrics(
    val isCompact: Boolean,
    val panelGap: Dp,
    val panelMinWidth: Dp,
    val headerHeight: Dp,
    val fontDelta: TextUnit,
    val badgeScale: Float,
    val emoteScale: Float,
    val itemVerticalPadding: Dp
) {
    companion object {
        val Single = PanelLayoutMetrics(
            isCompact = false, panelGap = 0.dp, panelMinWidth = 280.dp,
            headerHeight = 0.dp, fontDelta = 0.sp, badgeScale = 1f,
            emoteScale = 1f, itemVerticalPadding = 2.dp
        )
        val Split = PanelLayoutMetrics(
            isCompact = false, panelGap = 4.dp, panelMinWidth = 240.dp,
            headerHeight = 28.dp, fontDelta = 0.sp, badgeScale = 1f,
            emoteScale = 1f, itemVerticalPadding = 2.dp
        )
        val Compact = PanelLayoutMetrics(
            isCompact = true, panelGap = 2.dp, panelMinWidth = 180.dp,
            headerHeight = 22.dp, fontDelta = (-1).sp, badgeScale = 0.85f,
            emoteScale = 0.85f, itemVerticalPadding = 1.dp
        )
    }
}

@Composable
fun rememberPanelLayoutMetrics(mode: PanelLayoutMode): PanelLayoutMetrics = remember(mode) {
    when (mode) {
        PanelLayoutMode.SINGLE -> PanelLayoutMetrics.Single
        PanelLayoutMode.SPLIT -> PanelLayoutMetrics.Split
        PanelLayoutMode.COMPACT -> PanelLayoutMetrics.Compact
    }
}
