package io.rudione.chatone.presentation.components.expressive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.ChatoneButtonText

enum class PillTone { Primary, Tonal, Ghost }

@Composable
fun Modifier.expressiveGlass(shape: Shape = CircleShape, elevation: Dp = 14.dp): Modifier {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val edge = MaterialTheme.colorScheme.onSurface
    return this
        .shadow(elevation, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
        .clip(shape)
        .background(surface.copy(alpha = 0.80f))
        .border(1.dp, Brush.verticalGradient(listOf(edge.copy(alpha = 0.16f), edge.copy(alpha = 0.03f))), shape)
}

@Composable
fun Modifier.pressSquish(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "pressSquish"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: PillTone = PillTone.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 56.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = when (tone) {
        PillTone.Primary -> ButtonDefaults.buttonColors()
        PillTone.Tonal -> ButtonDefaults.filledTonalButtonColors()
        PillTone.Ghost -> ButtonDefaults.textButtonColors()
    }
    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier.height(height).pressSquish(interaction),
        enabled = enabled,
        shape = CircleShape,
        colors = colors,
        contentPadding = PaddingValues(horizontal = if (tone == PillTone.Ghost) 16.dp else 26.dp),
        interactionSource = interaction
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "pillLoading"
        ) { busy ->
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    ChatoneButtonText(
                        text,
                        style = if (tone == PillTone.Ghost) MaterialTheme.typography.labelLarge
                        else MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
fun HeroPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 60.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val dim by animateFloatAsState(if (enabled) 1f else 0.45f, tween(200), label = "heroDim")
    val container = MaterialTheme.colorScheme.primary
    val content = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier
            .height(height)
            .pressSquish(interaction)
            .graphicsLayer { alpha = dim }
            .shadow(8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "heroLoading"
        ) { busy ->
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = content)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = content,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressSquish(interaction)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun <T> PillSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val container by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                tween(220),
                label = "segmentContainer"
            )
            val content by animateColorAsState(
                if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(220),
                label = "segmentContent"
            )
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(container)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = content)
            }
        }
    }
}
