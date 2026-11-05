package io.rudione.chatone.presentation.points

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.points_streak_counts_here
import chatone.composeapp.generated.resources.points_streak_next
import chatone.composeapp.generated.resources.points_streak_title
import chatone.composeapp.generated.resources.points_streak_top
import io.rudione.chatone.domain.points.WatchStreak
import io.rudione.chatone.domain.points.WatchStreakSource
import io.rudione.chatone.icons.lucide.Flame
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.util.format.groupedNumber
import io.rudione.chatone.util.system.isDesktopPlatform
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val FlameHot = Color(0xFFFFC84A)
private val FlameCore = Color(0xFFFF7A1A)
private val FlameEdge = Color(0xFFE5361B)
private val FlameIdle = Color(0xFF9A9AA6)

@Composable
fun rememberWatchStreak(channelLogin: String?): State<WatchStreak?> {
    val source: WatchStreakSource = koinInject()
    val login = channelLogin?.trim()?.removePrefix("#")?.lowercase().orEmpty()
    LaunchedEffect(source, login) { if (login.isNotEmpty()) source.refresh(login) }
    val all = source.streaks.collectAsState()
    return remember(all, login) { derivedStateOf { all.value[login] } }
}

@Composable
fun StreakFlame(
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    animated: Boolean = !isDesktopPlatform
) {
    val flicker: State<Float> = if (active && animated) {
        rememberInfiniteTransition(label = "streakFlame").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(900, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "streakFlicker"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val heat = animateFloatAsState(
        if (active) 1f else 0f,
        tween(400, easing = LinearEasing),
        label = "streakHeat"
    )
    Box(
        modifier
            .size(size)
            .drawBehind {
                val glow = heat.value
                if (glow > 0f) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(FlameCore.copy(alpha = 0.45f * glow), Color.Transparent),
                            center = Offset(this.size.width / 2, this.size.height * 0.62f),
                            radius = this.size.minDimension * (0.7f + 0.12f * flicker.value)
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Lucide.Flame,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val wave = flicker.value * heat.value
                    scaleY = 1f + 0.08f * wave
                    scaleX = 1f - 0.03f * wave
                    rotationZ = (flicker.value - 0.5f) * 6f * heat.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val hot = Brush.verticalGradient(listOf(FlameHot, FlameCore, FlameEdge))
                    val idle = Brush.verticalGradient(listOf(FlameIdle, FlameIdle))
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            if (heat.value > 0.5f) hot else idle,
                            blendMode = BlendMode.SrcAtop
                        )
                    }
                }
        )
    }
}

@Composable
fun StreakBadge(count: Int, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(FlameCore.copy(alpha = if (count > 0) 0.16f else 0.08f))
            .padding(start = 4.dp, end = 7.dp, top = 2.dp, bottom = 2.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        StreakFlame(active = count > 0, size = 14.dp)
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) FlameHot else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WatchStreakCard(streak: WatchStreak, modifier: Modifier = Modifier) {
    val next = streak.nextReward
    val progress by animateFloatAsState(
        streak.progressToNextReward,
        tween(600),
        label = "streakProgress"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(FlameCore.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            StreakFlame(active = streak.current > 0, size = 22.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.points_streak_title, streak.current),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val subtitle = when {
                next != null -> {
                    val streams = next.streakLength - streak.current
                    pluralStringResource(
                        Res.plurals.points_streak_next,
                        streams,
                        streams,
                        "+" + groupedNumber(next.points.toLong())
                    )
                }

                streak.rewards.isNotEmpty() -> stringResource(
                    Res.string.points_streak_top,
                    groupedNumber(streak.rewards.maxOf { it.points }.toLong())
                )

                else -> stringResource(Res.string.points_streak_counts_here)
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (next != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .drawBehind {
                            drawRect(
                                Brush.horizontalGradient(listOf(FlameCore, FlameHot)),
                                size = this.size.copy(width = this.size.width * progress)
                            )
                        }
                )
            }
        }
    }
}
