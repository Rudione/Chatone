package io.rudione.chatone.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val CRUMBLE_MS = 620
private const val PARTICLE_COUNT = 42
private const val SWEEP = 0.32f
private const val CONTENT_FADE_END = 0.42f
private const val CONTENT_SHRINK = 0.18f

@Stable
class CrumbleState internal constructor() {
    private val level = Animatable(0f)
    var isCrumbling by mutableStateOf(false)
        private set
    internal var seed by mutableIntStateOf(0)
        private set

    internal val progress: Float get() = level.value

    suspend fun crumble() {
        if (isCrumbling) return
        seed = Random.nextInt()
        isCrumbling = true
        level.snapTo(0f)
        level.animateTo(1f, tween(CRUMBLE_MS, easing = LinearEasing))
    }

    suspend fun reset() {
        level.snapTo(0f)
        isCrumbling = false
    }
}

@Composable
fun rememberCrumbleState(): CrumbleState = remember { CrumbleState() }

@Immutable
private class Grain(
    val origin: Offset,
    val velocity: Offset,
    val radius: Float,
    val start: Float,
    val color: Color
)

fun Modifier.crumble(state: CrumbleState, palette: List<Color>): Modifier = this
    .drawWithCache {
        val unit = density
        val seed = state.seed
        val random = Random(seed)
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = minOf(size.width, size.height) / 2f
        val grains = if (palette.isEmpty()) emptyList() else List(PARTICLE_COUNT) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val distance = sqrt(random.nextFloat()) * maxRadius
            val origin = center + Offset(cos(angle) * distance, sin(angle) * distance)
            val speed = (36f + random.nextFloat() * 96f) * unit
            val spread = angle + (random.nextFloat() - 0.5f) * 0.9f
            Grain(
                origin = origin,
                velocity = Offset(cos(spread) * speed, sin(spread) * speed - 46f * unit),
                radius = (0.9f + random.nextFloat() * 1.6f) * unit,
                start = (1f - origin.x / size.width.coerceAtLeast(1f)) * SWEEP + random.nextFloat() * 0.08f,
                color = palette[random.nextInt(palette.size)]
            )
        }
        val gravity = 300f * unit
        val lifeSeconds = CRUMBLE_MS / 1000f
        onDrawWithContent {
            drawContent()
            if (!state.isCrumbling) return@onDrawWithContent
            val progress = state.progress
            for (grain in grains) {
                val local = ((progress - grain.start) / (1f - grain.start)).coerceIn(0f, 1f)
                if (local <= 0f || local >= 1f) continue
                val t = local * lifeSeconds * (1f - grain.start)
                val position = grain.origin + grain.velocity * t + Offset(0f, gravity * t * t / 2f)
                val fade = 1f - local * local
                drawCircle(grain.color.copy(alpha = grain.color.alpha * fade), grain.radius * (1f - 0.4f * local), position)
            }
        }
    }
    .graphicsLayer {
        if (!state.isCrumbling) return@graphicsLayer
        val progress = state.progress
        val fade = (1f - progress / CONTENT_FADE_END).coerceIn(0f, 1f)
        alpha = fade * fade
        val scale = 1f - CONTENT_SHRINK * progress
        scaleX = scale
        scaleY = scale
    }
