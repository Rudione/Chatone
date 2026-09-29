package io.rudione.chatone.presentation.stream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.rounded.Close
import io.rudione.chatone.icons.material.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVideoScale
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneSegmentedControl
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import kotlin.math.roundToInt

@Composable
fun StreamSettingsSheet(
    host: StreamPlayerHost,
    landscape: Boolean,
    pictureInPictureSupported: Boolean
) {
    val state by host.state.collectAsState()
    val dismiss = { host.onEvent(StreamPlayerEvent.SetSettingsVisible(false)) }
    StreamBackHandler(enabled = state.settingsVisible, onBack = dismiss)

    AnimatedVisibility(visible = state.settingsVisible, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss)
        )
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        AnimatedVisibility(
            visible = state.settingsVisible,
            enter = if (landscape) slideInHorizontally(tween(220)) { it } + fadeIn() else slideInVertically(tween(220)) { it } + fadeIn(),
            exit = if (landscape) slideOutHorizontally(tween(180)) { it } + fadeOut() else slideOutVertically(tween(180)) { it } + fadeOut(),
            modifier = Modifier.align(if (landscape) Alignment.CenterEnd else Alignment.BottomCenter)
        ) {
            val panelModifier = if (landscape) {
                Modifier.fillMaxHeight().width(360.dp)
            } else {
                Modifier.fillMaxWidth().heightIn(max = 560.dp)
            }
            Column(
                modifier = panelModifier
                    .padding(8.dp)
                    .chatoneGlassPanel(shape = RoundedCornerShape(20.dp), topAlpha = 1f, bottomAlpha = 1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                SheetHeader(title = LocalStrings.current.player.settings, onClose = dismiss)
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    QualitySection(state = state, onSelect = { host.onEvent(StreamPlayerEvent.SelectQuality(it)) })
                    PreferenceSections(
                        preferences = state.preferences,
                        statsVisible = state.statsVisible,
                        pictureInPictureSupported = pictureInPictureSupported,
                        onEvent = host::onEvent
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = LocalStrings.current.player.close,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualitySection(state: StreamPlayerUiState, onSelect: (StreamQualitySelection) -> Unit) {
    val appStrings = LocalStrings.current
    val strings = appStrings.player
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(strings.quality)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val autoLabel = state.activeVariant
                ?.takeIf { state.selection == StreamQualitySelection.Auto }
                ?.let { appStrings.format(strings.qualityAutoActive, it.displayName) }
                ?: strings.qualityAuto
            ChatoneChip(
                label = autoLabel,
                selected = state.selection == StreamQualitySelection.Auto,
                onClick = { onSelect(StreamQualitySelection.Auto) }
            )
            state.variants.filterNot { it.isAudioOnly }.forEach { variant ->
                val selection = StreamQualitySelection.Fixed(variant.groupId)
                ChatoneChip(
                    label = if (variant.isSource) "${variant.displayName} · ${strings.source}" else variant.displayName,
                    selected = state.selection == selection,
                    onClick = { onSelect(selection) }
                )
            }
            if (state.variants.any { it.isAudioOnly }) {
                ChatoneChip(
                    label = strings.audioOnly,
                    leadingIcon = Icons.Rounded.Headphones,
                    selected = state.selection == StreamQualitySelection.AudioOnly,
                    onClick = { onSelect(StreamQualitySelection.AudioOnly) }
                )
            }
        }
    }
}

@Composable
private fun PreferenceSections(
    preferences: StreamPlayerPreferences,
    statsVisible: Boolean,
    pictureInPictureSupported: Boolean,
    onEvent: (StreamPlayerEvent) -> Unit
) {
    val strings = LocalStrings.current.player
    ToggleRow(
        title = strings.lowLatency,
        description = strings.lowLatencyDescription,
        checked = preferences.lowLatency,
        onCheckedChange = { onEvent(StreamPlayerEvent.SetLowLatency(it)) }
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(strings.video)
        ChatoneSegmentedControl(
            options = listOf(StreamVideoScale.FIT to strings.scaleFit, StreamVideoScale.FILL to strings.scaleFill),
            selected = preferences.videoScale,
            onSelect = { onEvent(StreamPlayerEvent.SetVideoScale(it)) },
            fillWidth = true
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(strings.landscapeChat)
        ChatoneSegmentedControl(
            options = LandscapeChatMode.entries.map { it to chatModeLabel(it, strings) },
            selected = preferences.landscapeChatMode,
            onSelect = { onEvent(StreamPlayerEvent.SetLandscapeChatMode(it)) },
            fillWidth = true,
            iconOf = ::chatModeIcon
        )
        if (preferences.landscapeChatMode == LandscapeChatMode.OVERLAY) {
            PercentSliderRow(
                label = strings.chatBackground,
                value = preferences.overlayOpacity,
                range = StreamPlayerPreferences.MIN_OVERLAY_OPACITY..StreamPlayerPreferences.MAX_OVERLAY_OPACITY,
                onCommit = { onEvent(StreamPlayerEvent.SetOverlayOpacity(it)) }
            )
            PercentSliderRow(
                label = strings.chatMessagesOpacity,
                value = preferences.overlayMessageOpacity,
                range = StreamPlayerPreferences.MIN_OVERLAY_MESSAGE_OPACITY..StreamPlayerPreferences.MAX_OVERLAY_MESSAGE_OPACITY,
                onCommit = { onEvent(StreamPlayerEvent.SetOverlayMessageOpacity(it)) }
            )
        }
    }
    if (pictureInPictureSupported) {
        ToggleRow(
            title = strings.autoPictureInPicture,
            description = strings.autoPictureInPictureDescription,
            checked = preferences.autoPictureInPicture,
            onCheckedChange = { onEvent(StreamPlayerEvent.SetAutoPictureInPicture(it)) }
        )
    }
    ToggleRow(
        title = strings.videoStats,
        description = null,
        checked = statsVisible,
        onCheckedChange = { onEvent(StreamPlayerEvent.SetStatsVisible(it)) }
    )
}

@Composable
private fun PercentSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Float) -> Unit
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(local * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        ChatoneSlider(
            value = local,
            onValueChange = { local = it },
            valueRange = range,
            onValueChangeFinished = { onCommit(local) }
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
