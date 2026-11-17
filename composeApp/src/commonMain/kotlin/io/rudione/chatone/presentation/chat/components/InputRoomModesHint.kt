package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.close
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.ShieldCheck
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.ChatoneIconButton
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InputRoomModesHint(modes: List<String>, channelKey: String, eligible: Boolean = true) {
    var shown by remember(channelKey) { mutableStateOf(false) }
    var visible by remember(channelKey) { mutableStateOf(false) }
    val progress = remember(channelKey) { Animatable(1f) }
    LaunchedEffect(channelKey, eligible, modes.isNotEmpty()) {
        if (!eligible || modes.isEmpty()) {
            visible = false
        } else if (!shown) {
            delay(150)
            shown = true
            visible = true
        }
    }
    LaunchedEffect(channelKey, visible) {
        if (visible) {
            progress.snapTo(1f)
            progress.animateTo(0f, tween(10_000, easing = LinearEasing))
            visible = false
        }
    }
    if (!eligible || !visible || modes.isEmpty()) return
    val shape = RoundedCornerShape(topStart = 14.dp, topEnd = 5.dp, bottomEnd = 14.dp, bottomStart = 5.dp)
    val accent = MaterialTheme.colorScheme.outline
    Row(
        Modifier.floatAbove(4.dp).fillMaxWidth().padding(horizontal = 8.dp)
            .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .drawBehind { drawLine(accent, Offset(8.dp.toPx(), size.height - 1.dp.toPx()), Offset(8.dp.toPx() + (size.width - 16.dp.toPx()) * progress.value, size.height - 1.dp.toPx()), 2.dp.toPx()) }
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Lucide.ShieldCheck, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(modes.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f).basicMarquee(iterations = 1))
        ChatoneIconButton(onClick = { visible = false }, modifier = Modifier.size(24.dp)) {
            Icon(Lucide.X, stringResource(Res.string.close), Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.floatAbove(gap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    layout(placeable.width, 0) { placeable.place(0, -placeable.height - gap.roundToPx()) }
}
