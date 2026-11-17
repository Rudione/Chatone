package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.event_next
import io.rudione.chatone.icons.lucide.EyeOff
import io.rudione.chatone.icons.lucide.ChevronUp
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.FitText
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

internal val LocalEventBannerCompact = compositionLocalOf { false }
internal val LocalEventBannerSelected = compositionLocalOf { true }

internal fun eventBannerPadding(compact: Boolean): PaddingValues =
    if (compact) PaddingValues(horizontal = 1.dp, vertical = 1.dp)
    else PaddingValues(horizontal = 2.dp, vertical = 2.dp)

data class EventBannerItem(
    val key: String,
    val label: String,
    val endsAtMs: Long?,
    val totalDurationMs: Long?,
    val summary: String = "",
    val countdownInContent: Boolean = false,
    val content: @Composable () -> Unit
)

@Composable
fun UnifiedEventBanner(
    items: List<EventBannerItem>,
    horizontalInset: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        val pager = rememberPagerState(pageCount = { items.size })
        val scope = rememberCoroutineScope()
        val selected = pager.currentPage.coerceIn(items.indices)
        val current = items[selected]
        val primary = MaterialTheme.colorScheme.primary
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (compact) 4.dp else horizontalInset.coerceAtMost(12.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (items.size > 1) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FitText(
                        text = current.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        marquee = false,
                        modifier = Modifier.weight(1f)
                    )
                    items.forEachIndexed { index, item ->
                        Box(
                            Modifier.size(if (isDesktopPlatform) 24.dp else 30.dp, 20.dp)
                                .semantics { this.selected = index == selected }
                                .clickable(role = Role.Tab, onClickLabel = item.label) {
                                    scope.launch { pager.animateScrollToPage(index) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.width(if (index == selected) 16.dp else 8.dp).height(3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(primary.copy(alpha = if (index == selected) 1f else 0.25f))
                            )
                        }
                    }
                }
            }
            HorizontalPager(
                state = pager,
                key = { items[it].key },
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().animateContentSize(tween(200))
            ) { page ->
                CompositionLocalProvider(
                    LocalEventBannerCompact provides compact,
                    LocalEventBannerSelected provides (page == selected)
                ) { items[page].content() }
            }
            if (items.size > 1) {
                val nextIndex = (selected + 1) % items.size
                val next = items[nextIndex]
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 12.dp, bottomStart = 4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                        .clickable(role = Role.Button) { scope.launch { pager.animateScrollToPage(nextIndex) } }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.event_next), style = MaterialTheme.typography.labelSmall, color = primary)
                    Spacer(Modifier.width(6.dp))
                    FitText(
                        text = listOf(next.label, next.summary).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        marquee = false,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    next.endsAtMs?.let { EventCountdown(it, next.totalDurationMs, Modifier.width(54.dp).padding(start = 8.dp)) }
                }
            }
            if (!current.countdownInContent) current.endsAtMs?.let { EventCountdown(it, current.totalDurationMs, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
internal fun EventHideButton(tooltip: String, onHide: () -> Unit) {
    LiquidGlassTooltipBox(tooltip = tooltip) {
        ChatoneIconButton(onClick = onHide, modifier = Modifier.size(if (isDesktopPlatform) 24.dp else 28.dp)) {
            Icon(Lucide.EyeOff, tooltip, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun EventCollapseButton(tooltip: String, onClick: () -> Unit) {
    LiquidGlassTooltipBox(tooltip = tooltip) {
        ChatoneIconButton(onClick = onClick, modifier = Modifier.size(if (isDesktopPlatform) 24.dp else 28.dp)) {
            Icon(Lucide.ChevronUp, tooltip, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
