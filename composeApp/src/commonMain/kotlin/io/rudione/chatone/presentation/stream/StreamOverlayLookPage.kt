package io.rudione.chatone.presentation.stream

import chatone.composeapp.generated.resources.player_chat_background
import chatone.composeapp.generated.resources.player_chat_hidden
import chatone.composeapp.generated.resources.player_chat_messages_opacity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.player_overlay_backdrop
import chatone.composeapp.generated.resources.player_overlay_backdrop_hint
import chatone.composeapp.generated.resources.player_overlay_backdrop_opacity
import chatone.composeapp.generated.resources.player_overlay_auto_lock
import chatone.composeapp.generated.resources.player_overlay_auto_lock_hint
import chatone.composeapp.generated.resources.player_overlay_hide_delay
import chatone.composeapp.generated.resources.player_overlay_locked
import chatone.composeapp.generated.resources.player_overlay_locked_hint
import chatone.composeapp.generated.resources.player_overlay_preview_first
import chatone.composeapp.generated.resources.player_overlay_preview_second
import chatone.composeapp.generated.resources.player_overlay_scale
import chatone.composeapp.generated.resources.player_overlay_seconds
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.icons.lucide.Lock
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Rows3
import io.rudione.chatone.presentation.components.ChatoneSlider
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val PreviewVideo = Brush.linearGradient(listOf(Color(0xFFFFE08A), Color(0xFF7FD3FF), Color(0xFFF6F7FB)))
private val PreviewNames = listOf("chatone" to Color(0xFF9146FF), "viewer" to Color(0xFF1E9E5A))

@Composable
internal fun OverlayLookPage(preferences: StreamPlayerPreferences, onEvent: (StreamPlayerEvent) -> Unit) {
    OverlayPreview(preferences)
    PercentSlider(
        stringResource(Res.string.player_chat_background),
        preferences.overlayOpacity,
        StreamPlayerPreferences.MIN_OVERLAY_OPACITY..StreamPlayerPreferences.MAX_OVERLAY_OPACITY
    ) { onEvent(StreamPlayerEvent.SetOverlayOpacity(it)) }
    PercentSlider(
        stringResource(Res.string.player_chat_messages_opacity),
        preferences.overlayMessageOpacity,
        StreamPlayerPreferences.MIN_OVERLAY_MESSAGE_OPACITY..StreamPlayerPreferences.MAX_OVERLAY_MESSAGE_OPACITY
    ) { onEvent(StreamPlayerEvent.SetOverlayMessageOpacity(it)) }
    ToggleRow(
        Lucide.Rows3,
        stringResource(Res.string.player_overlay_backdrop),
        stringResource(Res.string.player_overlay_backdrop_hint),
        preferences.overlayBackdrop
    ) { onEvent(StreamPlayerEvent.SetOverlayBackdrop(it)) }
    AnimatedVisibility(
        visible = preferences.overlayBackdrop,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        PercentSlider(
            stringResource(Res.string.player_overlay_backdrop_opacity),
            preferences.overlayBackdropOpacity,
            StreamPlayerPreferences.MIN_OVERLAY_BACKDROP_OPACITY..StreamPlayerPreferences.MAX_OVERLAY_BACKDROP_OPACITY
        ) { onEvent(StreamPlayerEvent.SetOverlayBackdropOpacity(it)) }
    }
    PercentSlider(
        stringResource(Res.string.player_overlay_scale),
        preferences.overlayScale,
        StreamPlayerPreferences.MIN_OVERLAY_SCALE..StreamPlayerPreferences.MAX_OVERLAY_SCALE,
        steps = 8
    ) { onEvent(StreamPlayerEvent.SetOverlayScale(it)) }
    SecondsSlider(
        label = stringResource(Res.string.player_overlay_hide_delay),
        seconds = preferences.overlayHideDelaySeconds,
        min = StreamPlayerPreferences.MIN_OVERLAY_HIDE_DELAY_SECONDS,
        max = StreamPlayerPreferences.MAX_OVERLAY_HIDE_DELAY_SECONDS
    ) { onEvent(StreamPlayerEvent.SetOverlayHideDelay(it)) }
    SettingsDivider()
    ToggleRow(
        Lucide.Lock,
        stringResource(Res.string.player_overlay_locked),
        stringResource(Res.string.player_overlay_locked_hint),
        preferences.overlayLocked
    ) { onEvent(StreamPlayerEvent.SetOverlayLocked(it)) }
    SecondsSlider(
        label = stringResource(Res.string.player_overlay_auto_lock),
        seconds = preferences.overlayAutoLockSeconds,
        min = StreamPlayerPreferences.MIN_OVERLAY_AUTO_LOCK_SECONDS,
        max = StreamPlayerPreferences.MAX_OVERLAY_AUTO_LOCK_SECONDS,
        step = StreamPlayerPreferences.OVERLAY_AUTO_LOCK_STEP_SECONDS,
        offLabel = stringResource(Res.string.player_chat_hidden),
        hint = stringResource(Res.string.player_overlay_auto_lock_hint)
    ) { onEvent(StreamPlayerEvent.SetOverlayAutoLock(it)) }
}

@Composable
private fun OverlayPreview(preferences: StreamPlayerPreferences) {
    val lines = listOf(
        stringResource(Res.string.player_overlay_preview_first),
        stringResource(Res.string.player_overlay_preview_second)
    )
    val backdrop = if (preferences.overlayBackdrop) preferences.overlayBackdropOpacity else 0f
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PreviewVideo)
            .background(Color.Black.copy(alpha = preferences.overlayOpacity))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.zip(PreviewNames).forEach { (text, author) ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = author.second, fontWeight = FontWeight.Bold)) { append(author.first) }
                    append(": ")
                    append(text)
                },
                color = Color.White,
                fontSize = (13 * preferences.overlayScale).sp,
                modifier = Modifier
                    .alpha(preferences.overlayMessageOpacity)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = backdrop))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun PercentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onCommit: (Float) -> Unit
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    SliderBlock(label, "${(local * 100).roundToInt()}%") {
        ChatoneSlider(
            value = local,
            onValueChange = { local = it },
            valueRange = range,
            steps = steps,
            onValueChangeFinished = { onCommit(local) }
        )
    }
}

@Composable
private fun SecondsSlider(
    label: String,
    seconds: Int,
    min: Int,
    max: Int,
    step: Int = 1,
    offLabel: String? = null,
    hint: String? = null,
    onCommit: (Int) -> Unit
) {
    var local by remember(seconds) { mutableFloatStateOf(seconds.toFloat()) }
    val rounded = local.roundToInt()
    val value = if (rounded == 0 && offLabel != null) offLabel else stringResource(Res.string.player_overlay_seconds, rounded)
    SliderBlock(label, value, hint) {
        ChatoneSlider(
            value = local,
            onValueChange = { local = it },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min) / step - 1,
            onValueChangeFinished = { onCommit(local.roundToInt()) }
        )
    }
}

@Composable
private fun SliderBlock(label: String, value: String, hint: String? = null, slider: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        slider()
    }
}
