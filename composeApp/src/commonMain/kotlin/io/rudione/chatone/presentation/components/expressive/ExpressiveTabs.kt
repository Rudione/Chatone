package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class ExpressiveTabSize(
    internal val horizontalPadding: Dp,
    internal val verticalPadding: Dp,
    internal val indicatorHeight: Dp,
    internal val iconSize: Dp
) {
    Regular(horizontalPadding = 14.dp, verticalPadding = 9.dp, indicatorHeight = 3.dp, iconSize = 17.dp),
    Compact(horizontalPadding = 9.dp, verticalPadding = 6.dp, indicatorHeight = 2.5.dp, iconSize = 14.dp)
}

@Composable
fun <T> ExpressiveTabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    size: ExpressiveTabSize = ExpressiveTabSize.Regular,
    divider: Boolean = true,
    badge: (T) -> Int = { 0 },
    icon: (T) -> ImageVector? = { null },
    accentOf: (T) -> Color? = { null }
) {
    val textStyle = if (size == ExpressiveTabSize.Regular) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium
    ExpressiveTabRow(
        tabs = tabs,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        fill = fill,
        size = size,
        divider = divider,
        accentOf = accentOf
    ) { tab, _, color ->
        ExpressiveTabLabel(
            text = label(tab),
            color = color,
            style = textStyle,
            icon = icon(tab),
            iconSize = size.iconSize,
            badge = badge(tab)
        )
    }
}

@Composable
fun <T> ExpressiveTabRow(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    size: ExpressiveTabSize = ExpressiveTabSize.Regular,
    divider: Boolean = true,
    accentOf: (T) -> Color? = { null },
    content: @Composable (tab: T, selected: Boolean, color: Color) -> Unit
) {
    if (tabs.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    val indicator = remember { TabIndicatorState(selectedIndex) }
    val scroll = if (fill) null else rememberScrollState()
    val accent = accentOf(tabs[selectedIndex]) ?: scheme.primary
    val indicatorColor = animateColorAsState(accent, tween(INDICATOR_COLOR_MS), label = "tabIndicator")
    val shimmerColor = lerp(accent, scheme.tertiary, SHIMMER_TINT)
    val dividerColor = scheme.outlineVariant.copy(alpha = DIVIDER_ALPHA)
    val indicatorHeight = size.indicatorHeight
    val horizontalPadding = size.horizontalPadding

    LaunchedEffect(selectedIndex) {
        indicator.moveTo(selectedIndex)
        scroll?.reveal(indicator, selectedIndex)
    }

    Layout(
        content = {
            tabs.forEachIndexed { index, tab ->
                key(tab) {
                    val isSelected = index == selectedIndex
                    ExpressiveTabItem(
                        selected = isSelected,
                        accent = accentOf(tab) ?: scheme.primary,
                        size = size,
                        onClick = { onSelect(tab) }
                    ) { color -> content(tab, isSelected, color) }
                }
            }
        },
        modifier = modifier
            .drawBehind {
                if (divider) {
                    val stroke = 1.dp.toPx()
                    drawRect(dividerColor, topLeft = Offset(0f, this.size.height - stroke), size = Size(this.size.width, stroke))
                }
            }
            .then(if (scroll != null) Modifier.horizontalScroll(scroll) else Modifier)
            .drawWithContent {
                drawContent()
                drawIndicator(
                    indicator = indicator,
                    color = indicatorColor,
                    shimmer = shimmerColor,
                    height = indicatorHeight.toPx()
                )
            }
    ) { measurables, constraints ->
        val pad = horizontalPadding.roundToPx()
        val count = measurables.size
        val intrinsic = IntArray(count) { measurables[it].maxIntrinsicWidth(Constraints.Infinity) }
        val bounded = constraints.hasBoundedWidth
        val widths = if (fill && bounded) {
            val cell = constraints.maxWidth / count
            val remainder = constraints.maxWidth - cell * count
            IntArray(count) { cell + if (it < remainder) 1 else 0 }
        } else intrinsic
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints(minWidth = widths[index], maxWidth = widths[index], maxHeight = constraints.maxHeight))
        }
        val height = placeables.maxOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)
        val total = widths.sum()
        val width = if (fill && bounded) constraints.maxWidth else total.coerceIn(constraints.minWidth, constraints.maxWidth)
        val lefts = FloatArray(count)
        val rights = FloatArray(count)
        val starts = IntArray(count)
        var x = 0
        for (index in 0 until count) {
            val cell = widths[index]
            val contentWidth = (minOf(intrinsic[index], cell) - pad * 2).coerceAtLeast(pad)
            val center = x + cell / 2f
            lefts[index] = center - contentWidth / 2f
            rights[index] = center + contentWidth / 2f
            starts[index] = x
            x += cell
        }
        indicator.bounds(lefts, rights, starts, widths)
        layout(width, height) {
            var placeX = 0
            placeables.forEach { placeable ->
                placeable.place(placeX, 0)
                placeX += placeable.width
            }
        }
    }
}

@Composable
fun ExpressiveTabLabel(
    text: String,
    color: Color,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    icon: ImageVector? = null,
    iconSize: Dp = 17.dp,
    badge: Int = 0
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
        Text(
            text,
            style = style,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (badge > 0) {
            Text(
                if (badge > MAX_BADGE) "$MAX_BADGE+" else badge.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .drawBehind { drawRect(color.copy(alpha = BADGE_ALPHA)) }
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun ExpressiveTabItem(
    selected: Boolean,
    accent: Color,
    size: ExpressiveTabSize,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val color by animateColorAsState(
        when {
            selected -> accent
            hovered -> scheme.onSurface
            else -> scheme.onSurfaceVariant
        },
        tween(LABEL_COLOR_MS),
        label = "tabLabel"
    )
    val hover = animateColorAsState(
        if (hovered && !selected) scheme.onSurface.copy(alpha = HOVER_ALPHA) else Color.Transparent,
        tween(LABEL_COLOR_MS),
        label = "tabHover"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(TAB_CORNER))
            .drawBehind { drawRect(hover.value) }
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(
                start = size.horizontalPadding,
                end = size.horizontalPadding,
                top = size.verticalPadding,
                bottom = size.verticalPadding + size.indicatorHeight
            ),
        contentAlignment = Alignment.Center
    ) { content(color) }
}

@Stable
private class TabIndicatorState(initialIndex: Int) {
    private var lefts = FloatArray(0)
    private var rights = FloatArray(0)
    private var starts = IntArray(0)
    private var widths = IntArray(0)

    var shown by mutableIntStateOf(initialIndex)
        private set

    var animatedLeft by mutableFloatStateOf(0f)
        private set
    var animatedRight by mutableFloatStateOf(0f)
        private set
    var moving by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(1f)
        private set
    var forward = true
        private set

    fun bounds(lefts: FloatArray, rights: FloatArray, starts: IntArray, widths: IntArray) {
        this.lefts = lefts
        this.rights = rights
        this.starts = starts
        this.widths = widths
    }

    fun has(index: Int): Boolean = index in lefts.indices

    fun left(index: Int): Float = lefts[index]

    fun right(index: Int): Float = rights[index]

    fun start(index: Int): Int = starts[index]

    fun end(index: Int): Int = starts[index] + widths[index]

    suspend fun moveTo(index: Int) {
        val previous = shown
        if (previous == index && !moving) return
        if (!has(index) || !has(previous)) {
            shown = index
            return
        }
        val fromLeft = if (moving) animatedLeft else lefts[previous]
        val fromRight = if (moving) animatedRight else rights[previous]
        val toLeft = lefts[index]
        val toRight = rights[index]
        forward = toLeft >= fromLeft
        animatedLeft = fromLeft
        animatedRight = fromRight
        progress = 0f
        moving = true
        shown = index
        coroutineScope {
            launch {
                animate(
                    initialValue = fromLeft,
                    targetValue = toLeft,
                    animationSpec = tween(if (forward) TRAIL_MS else LEAD_MS, easing = EdgeEasing)
                ) { value, _ ->
                    animatedLeft = value
                }
            }
            launch {
                animate(
                    initialValue = fromRight,
                    targetValue = toRight,
                    animationSpec = tween(if (forward) LEAD_MS else TRAIL_MS, easing = EdgeEasing)
                ) { value, _ ->
                    animatedRight = value
                }
            }
            launch {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(SHIMMER_MS, easing = FastOutSlowInEasing)
                ) { value, _ -> progress = value }
            }
        }
        moving = false
        progress = 1f
    }
}

private suspend fun ScrollState.reveal(indicator: TabIndicatorState, index: Int) {
    if (!indicator.has(index)) return
    val margin = REVEAL_MARGIN
    val start = indicator.start(index)
    val end = indicator.end(index)
    val viewport = viewportSize
    when {
        start - margin < value -> animateScrollTo((start - margin).coerceAtLeast(0))
        viewport > 0 && end + margin > value + viewport -> animateScrollTo(end + margin - viewport)
    }
}

private fun DrawScope.drawIndicator(
    indicator: TabIndicatorState,
    color: State<Color>,
    shimmer: Color,
    height: Float
) {
    val index = indicator.shown
    if (!indicator.has(index)) return
    val moving = indicator.moving
    val left = if (moving) minOf(indicator.animatedLeft, indicator.animatedRight) else indicator.left(index)
    val right = if (moving) maxOf(indicator.animatedLeft, indicator.animatedRight) else indicator.right(index)
    val progress = indicator.progress
    val bump = if (moving) sin(PI.toFloat() * progress) else 0f
    val thickness = height * (1f + THICKNESS_BUMP * bump)
    val top = size.height - thickness
    val base = color.value
    val brush = if (moving) {
        val sweep = if (indicator.forward) progress else 1f - progress
        val center = -SHIMMER_BAND + sweep * (1f + SHIMMER_BAND * 2f)
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to base,
                (center - SHIMMER_BAND).coerceIn(0f, 1f) to base,
                center.coerceIn(0f, 1f) to shimmer,
                (center + SHIMMER_BAND).coerceIn(0f, 1f) to base,
                1f to base
            ),
            startX = left,
            endX = right
        )
    } else SolidColor(base)
    drawRoundRect(
        brush = brush,
        topLeft = Offset(left, top),
        size = Size((right - left).coerceAtLeast(thickness), thickness),
        cornerRadius = CornerRadius(thickness / 2f)
    )
}

private const val LEAD_MS = 260
private const val TRAIL_MS = 380
private val EdgeEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val SHIMMER_MS = 560
private const val SHIMMER_BAND = 0.32f
private const val SHIMMER_TINT = 0.65f
private const val THICKNESS_BUMP = 0.25f
private const val INDICATOR_COLOR_MS = 220
private const val LABEL_COLOR_MS = 180
private const val HOVER_ALPHA = 0.05f
private const val DIVIDER_ALPHA = 0.35f
private const val BADGE_ALPHA = 0.16f
private const val MAX_BADGE = 999
private const val REVEAL_MARGIN = 24
private val TAB_CORNER = 10.dp
