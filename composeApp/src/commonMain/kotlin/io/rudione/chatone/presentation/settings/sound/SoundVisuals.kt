package io.rudione.chatone.presentation.settings.sound

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.sound_builtin_beep
import chatone.composeapp.generated.resources.sound_builtin_bell
import chatone.composeapp.generated.resources.sound_builtin_chime
import chatone.composeapp.generated.resources.sound_builtin_drop
import chatone.composeapp.generated.resources.sound_builtin_tag
import chatone.composeapp.generated.resources.sound_custom_fallback
import chatone.composeapp.generated.resources.sound_mute
import chatone.composeapp.generated.resources.sound_play
import chatone.composeapp.generated.resources.sound_seconds
import chatone.composeapp.generated.resources.sound_stop
import io.rudione.chatone.domain.sound.BuiltInSound
import io.rudione.chatone.domain.sound.MentionSoundIds
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Play
import io.rudione.chatone.icons.lucide.Square
import io.rudione.chatone.presentation.components.expressive.addScallop
import io.rudione.chatone.presentation.components.expressive.pressSquish
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

internal fun BuiltInSound.labelRes(): StringResource = when (this) {
    BuiltInSound.Tag -> Res.string.sound_builtin_tag
    BuiltInSound.Chime -> Res.string.sound_builtin_chime
    BuiltInSound.Drop -> Res.string.sound_builtin_drop
    BuiltInSound.Bell -> Res.string.sound_builtin_bell
    BuiltInSound.Beep -> Res.string.sound_builtin_beep
}

@Composable
internal fun soundName(id: String, state: MentionSoundUiState): String {
    if (id == MentionSoundIds.MUTE) return stringResource(Res.string.sound_mute)
    BuiltInSound.byId(id)?.let { return stringResource(it.labelRes()) }
    val custom = state.library.firstOrNull { it.id == id }
    return custom?.name?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.sound_custom_fallback)
}

@Composable
internal fun secondsLabel(ms: Long): String {
    val tenths = ((ms + 50) / 100).coerceAtLeast(0)
    val text = "${tenths / 10},${tenths % 10}"
    return stringResource(Res.string.sound_seconds, text)
}

internal fun secondsValue(ms: Long): String {
    val hundredths = ((ms + 5) / 10).coerceAtLeast(0)
    val rest = (hundredths % 100).toString().padStart(2, '0')
    return "${hundredths / 100},$rest"
}

@Composable
internal fun rememberPlaybackProgress(playback: SoundPlayback?, id: String): State<Float?> {
    val progress = remember { mutableFloatStateOf(-1f) }
    val active = playback?.takeIf { it.id == id }
    LaunchedEffect(active?.token) {
        if (active == null) {
            progress.floatValue = -1f
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        progress.floatValue = 0f
        anim.animateTo(1f, tween(active.durationMs.toInt().coerceAtLeast(1), easing = LinearEasing)) {
            progress.floatValue = value
        }
        progress.floatValue = -1f
    }
    return remember { derivedProgress(progress) }
}

private fun derivedProgress(source: MutableFloatState): State<Float?> =
    object : State<Float?> {
        override val value: Float? get() = source.floatValue.takeIf { it >= 0f }
    }

@Composable
internal fun SoundWaveform(
    peaks: List<Float>,
    modifier: Modifier = Modifier,
    progress: () -> Float? = { null },
    active: Color = MaterialTheme.colorScheme.primary,
    idle: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
    barCount: Int = 40,
    minBar: Float = 0.08f
) {
    val heights = remember { List(barCount) { Animatable(minBar) } }
    val target = remember(peaks, barCount) { resample(peaks, barCount, minBar) }
    LaunchedEffect(target) {
        heights.forEachIndexed { index, anim ->
            launch {
                delay(index * 6L)
                anim.animateTo(target[index], spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }
    Canvas(modifier) {
        val gap = size.width / barCount * 0.38f
        val bar = (size.width - gap * (barCount - 1)) / barCount
        val head = progress()
        for (i in 0 until barCount) {
            val h = heights[i].value.coerceIn(0f, 1f) * size.height
            val x = i * (bar + gap)
            val fraction = (i + 0.5f) / barCount
            val lit = head != null && fraction <= head
            val glow = if (head != null) max(0f, 1f - abs(fraction - head) * 9f) else 0f
            val color = when {
                head == null -> active.copy(alpha = active.alpha * 0.85f)
                lit -> active
                else -> idle
            }
            val lift = h * (1f + glow * 0.22f)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - lift) / 2f),
                size = Size(bar, lift.coerceAtLeast(bar)),
                cornerRadius = CornerRadius(bar / 2f, bar / 2f)
            )
        }
    }
}

private fun resample(peaks: List<Float>, count: Int, minBar: Float): List<Float> {
    if (peaks.isEmpty()) return List(count) { minBar }
    return List(count) { i ->
        val from = i * peaks.size / count
        val to = ((i + 1) * peaks.size / count).coerceAtLeast(from + 1).coerceAtMost(peaks.size)
        var top = 0f
        for (k in from until to) top = max(top, peaks[k])
        (minBar + (1f - minBar) * sqrt(top)).coerceIn(minBar, 1f)
    }
}

@Composable
internal fun ScallopPlayButton(
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val energy by animateFloatAsState(
        if (playing) 1f else 0f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "scallopEnergy"
    )
    val dim by animateFloatAsState(if (enabled) 1f else 0.45f, tween(260), label = "scallopDim")
    val spin = remember { mutableFloatStateOf(0f) }
    val rings = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        val spinner = Animatable(spin.floatValue)
        val pulse = Animatable(0f)
        coroutineScope {
            launch {
                while (isActive) {
                    pulse.snapTo(0f)
                    pulse.animateTo(1f, tween(RING_MILLIS, easing = LinearEasing)) { rings.floatValue = value }
                }
            }
            spinner.animateTo(
                spin.floatValue + 2f * PI.toFloat() * 40f,
                infiniteRepeatable(tween(SPIN_MILLIS * 40, easing = LinearEasing), RepeatMode.Restart)
            ) { spin.floatValue = value }
        }
    }
    val label = stringResource(if (playing) Res.string.sound_stop else Res.string.sound_play)
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = label }
            .graphicsLayer { alpha = dim }
            .pressSquish(interaction)
            .drawBehind {
                val path = Path()
                if (energy > 0.01f) {
                    for (k in 0 until 2) {
                        val t = (rings.floatValue + k * 0.5f) % 1f
                        val radius = this.size.minDimension / 2f * (1f + t * 0.55f)
                        drawCircle(
                            color = primary.copy(alpha = (1f - t) * 0.35f * energy),
                            radius = radius,
                            style = Stroke(width = 2.dp.toPx() * (1f - t * 0.5f))
                        )
                    }
                }
                path.addScallop(this.size, lobes = 9, depth = 0.07f + 0.06f * energy, phase = spin.floatValue)
                drawPath(path, container)
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), initialScale = 0.4f) + fadeIn()) togetherWith
                    (scaleOut(targetScale = 0.4f) + fadeOut())
            },
            label = "scallopIcon"
        ) { isPlaying ->
            Icon(
                if (isPlaying) Lucide.Square else Lucide.Play,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(size * 0.36f)
            )
        }
    }
}

private const val RING_MILLIS = 1100
private const val SPIN_MILLIS = 5200
