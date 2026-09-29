package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.chat.rendering.bottomOverflow
import io.rudione.chatone.presentation.components.BackdropState
import io.rudione.chatone.presentation.components.backdropTarget
import io.rudione.chatone.util.system.isBackdropBlurSupported
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PROGRESS_ANIMATION_MS = 140
private val GlassRamp = 24.dp
private const val RIM_ALPHA = 0.08f
private const val FALLBACK_GLASS_ALPHA = 0.9f
private val RestingElevation = 1.dp
private val FocusedElevation = 8.dp
private val GlassShadow = Color.Black.copy(alpha = 0.45f)

@Immutable
data class ChatInputGlassStyle(
    val cornerRadius: Dp,
    val horizontalMargin: Dp,
    val topMargin: Dp,
    val bottomMargin: Dp,
    val blurRadius: Dp,
    val saturation: Float,
    val glassAlpha: Float,
    val fadeAlpha: Float,
    val shadowBoost: Dp,
    val solidBarAtRest: Boolean
) {
    companion object {
        val Desktop = ChatInputGlassStyle(
            cornerRadius = 16.dp,
            horizontalMargin = 8.dp,
            topMargin = 4.dp,
            bottomMargin = 6.dp,
            blurRadius = 9.dp,
            saturation = 1.6f,
            glassAlpha = 0.5f,
            fadeAlpha = 0.8f,
            shadowBoost = 10.dp,
            solidBarAtRest = false
        )
        val Mobile = ChatInputGlassStyle(
            cornerRadius = 26.dp,
            horizontalMargin = 8.dp,
            topMargin = 6.dp,
            bottomMargin = 8.dp,
            blurRadius = 12.dp,
            saturation = 1.5f,
            glassAlpha = 0.5f,
            fadeAlpha = 0.7f,
            shadowBoost = 12.dp,
            solidBarAtRest = true
        )

        fun forPlatform(desktop: Boolean): ChatInputGlassStyle = if (desktop) Desktop else Mobile
    }
}

@Stable
class ChatInputGlass internal constructor(
    val style: ChatInputGlassStyle,
    val backdrop: BackdropState,
    private val animatedProgress: Animatable<Float, AnimationVector1D>
) {
    fun progress(): Float = animatedProgress.value

    fun fill(resting: Color): Color {
        val factor = if (isBackdropBlurSupported) style.glassAlpha else FALLBACK_GLASS_ALPHA
        return lerp(resting, resting.copy(alpha = resting.alpha * factor), progress())
    }
}

internal fun glassProgressFor(overflowPx: Float?, thresholdPx: Int, itemCount: Int): Float {
    if (itemCount == 0 || thresholdPx <= 0) return 0f
    if (overflowPx == null) return 1f
    val t = (overflowPx / thresholdPx).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
fun rememberChatInputGlass(
    listState: LazyListState,
    followingLive: Boolean,
    desktop: Boolean = isDesktopPlatform
): ChatInputGlass {
    val style = remember(desktop) { ChatInputGlassStyle.forPlatform(desktop) }
    val rampPx = with(LocalDensity.current) { GlassRamp.roundToPx() }
    val backdrop = remember { BackdropState() }
    val animatedProgress = remember { Animatable(0f) }
    val following by rememberUpdatedState(followingLive)
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    var userScrolling by remember(listState) { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { dragged to listState.isScrollInProgress }
            .collect { (isDragged, inProgress) ->
                userScrolling = when {
                    isDragged -> true
                    !inProgress -> false
                    else -> userScrolling
                }
            }
    }

    LaunchedEffect(listState, rampPx) {
        snapshotFlow {
            val info = listState.layoutInfo
            if (following && !userScrolling) 0f
            else glassProgressFor(
                overflowPx = listState.bottomOverflow(),
                thresholdPx = minOf(info.afterContentPadding, rampPx),
                itemCount = info.totalItemsCount
            )
        }
            .distinctUntilChanged()
            .collectLatest { target -> animatedProgress.animateTo(target, tween(PROGRESS_ANIMATION_MS)) }
    }

    return remember(style, backdrop, animatedProgress) { ChatInputGlass(style, backdrop, animatedProgress) }
}

fun Modifier.chatInputGlassSurface(
    glass: ChatInputGlass,
    resting: Color,
    border: Color,
    borderWidth: Dp,
    focused: Boolean,
    glow: Color
): Modifier {
    val shape = RoundedCornerShape(glass.style.cornerRadius)
    return this
        .backdropTarget(glass.backdrop, glass.style.cornerRadius)
        .graphicsLayer {
            val amount = glass.progress()
            shadowElevation = (if (focused) FocusedElevation else RestingElevation).toPx() +
                glass.style.shadowBoost.toPx() * amount
            this.shape = shape
            clip = false
            ambientShadowColor = lerp(glow, GlassShadow, amount)
            spotShadowColor = lerp(glow, GlassShadow, amount)
        }
        .border(borderWidth, border, shape)
        .clip(shape)
        .drawBehind {
            val amount = glass.progress()
            drawRect(glass.fill(resting))
            if (amount > 0f) {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = RIM_ALPHA * amount),
                        0.55f to Color.Transparent
                    )
                )
            }
        }
}

fun Modifier.chatInputBackdrop(glass: ChatInputGlass, restingBar: Color, scrim: Color): Modifier = drawBehind {
    val amount = glass.progress()
    if (glass.style.solidBarAtRest && amount < 1f) {
        drawRect(restingBar.copy(alpha = restingBar.alpha * (1f - amount)))
    }
    if (amount <= 0f) return@drawBehind
    drawRect(
        Brush.verticalGradient(
            0f to Color.Transparent,
            1f to scrim.copy(alpha = scrim.alpha * glass.style.fadeAlpha * amount)
        )
    )
}
