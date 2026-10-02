package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun Path.addScallop(size: Size, lobes: Int, depth: Float, phase: Float = 0f) {
    val radius = min(size.width, size.height) / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val steps = SCALLOP_STEPS
    for (i in 0..steps) {
        val theta = (i.toFloat() / steps) * 2f * PI.toFloat()
        val wave = 0.5f + 0.5f * cos(lobes * theta + phase)
        val r = radius * (1f - depth + depth * wave)
        val x = cx + r * cos(theta)
        val y = cy + r * sin(theta)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

class ScallopShape(private val lobes: Int = 9, private val depth: Float = 0.08f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline =
        Outline.Generic(Path().apply { addScallop(size, lobes, depth) })
}

fun Modifier.scallopFill(
    color: () -> Color,
    lobes: Int,
    depth: () -> Float,
    phase: () -> Float = { 0f }
): Modifier = drawWithCache {
    val path = Path()
    onDrawBehind {
        path.reset()
        path.addScallop(size, lobes, depth(), phase())
        drawPath(path, color())
    }
}

@Composable
fun ScallopBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 156.dp,
    busy: Boolean = false,
    animated: Boolean = true,
    pulseKey: Any? = null,
    iconFraction: Float = ICON_FRACTION,
    container: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        .compositeOver(MaterialTheme.colorScheme.surface),
    content: Color = MaterialTheme.colorScheme.primary
) {
    val breathState: State<Float>
    val wobbleState: State<Float>
    if (animated) {
        val motion = rememberInfiniteTransition(label = "scallopMotion")
        breathState = motion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BREATH_MILLIS), RepeatMode.Reverse),
            label = "scallopBreath"
        )
        wobbleState = motion.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(WOBBLE_MILLIS, easing = LinearEasing)),
            label = "scallopWobble"
        )
    } else {
        breathState = remember { mutableFloatStateOf(STILL_BREATH) }
        wobbleState = remember { mutableFloatStateOf(0f) }
    }
    val breath by breathState
    val wobble by wobbleState
    val energy by animateFloatAsState(
        targetValue = if (busy) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scallopEnergy"
    )
    val angle = remember { mutableFloatStateOf(0f) }
    val currentEnergy by rememberUpdatedState(energy)
    if (animated) LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val seconds = (now - last) / 1_000_000_000f
                last = now
                val speed = IDLE_DEG_PER_SEC + BUSY_DEG_PER_SEC * currentEnergy
                angle.floatValue = (angle.floatValue + seconds * speed) % 360f
            }
        }
    }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        pop.snapTo(0.86f)
        pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer { rotationZ = -angle.floatValue * 0.5f }
                .scallopFill(
                    color = { container.copy(alpha = 0.22f + 0.12f * breath) },
                    lobes = HALO_LOBES,
                    depth = { 0.06f + 0.06f * energy * (0.5f + 0.5f * sin(wobble)) }
                )
        )
        Box(
            Modifier
                .size(size * 0.8f)
                .graphicsLayer { rotationZ = angle.floatValue }
                .scallopFill(
                    color = { container },
                    lobes = CORE_LOBES,
                    depth = { 0.06f + 0.03f * breath + 0.04f * energy * (0.5f + 0.5f * cos(wobble)) }
                )
        )
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (scaleIn(
                    spring(dampingRatio = 0.5f),
                    initialScale = 0.4f
                ) + fadeIn(tween(160))) togetherWith
                        (scaleOut(tween(140), targetScale = 0.4f) + fadeOut(tween(120)))
            },
            label = "scallopIcon"
        ) { vector ->
            Icon(
                vector,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(size * iconFraction)
            )
        }
    }
}

private const val SCALLOP_STEPS = 180
private const val ICON_FRACTION = 0.3f
private const val CORE_LOBES = 9
private const val HALO_LOBES = 12
private const val STILL_BREATH = 0.5f
private const val IDLE_DEG_PER_SEC = 14f
private const val BUSY_DEG_PER_SEC = 110f
private const val BREATH_MILLIS = 2_400
private const val WOBBLE_MILLIS = 1_600
