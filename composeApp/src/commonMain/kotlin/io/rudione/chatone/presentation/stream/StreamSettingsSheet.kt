package io.rudione.chatone.presentation.stream

import chatone.composeapp.generated.resources.player_audio_only
import chatone.composeapp.generated.resources.player_auto_picture_in_picture
import chatone.composeapp.generated.resources.player_auto_picture_in_picture_description
import chatone.composeapp.generated.resources.player_background_audio
import chatone.composeapp.generated.resources.player_background_audio_description
import chatone.composeapp.generated.resources.player_close
import chatone.composeapp.generated.resources.player_catch_up
import chatone.composeapp.generated.resources.player_landscape_chat
import chatone.composeapp.generated.resources.player_latency_hint
import chatone.composeapp.generated.resources.player_latency_on_video
import chatone.composeapp.generated.resources.player_latency_on_video_hint
import chatone.composeapp.generated.resources.player_ambient_light
import chatone.composeapp.generated.resources.player_ambient_light_hint
import chatone.composeapp.generated.resources.player_latency_unknown
import chatone.composeapp.generated.resources.player_low_latency
import chatone.composeapp.generated.resources.player_low_latency_description
import chatone.composeapp.generated.resources.player_quality
import chatone.composeapp.generated.resources.player_quality_auto
import chatone.composeapp.generated.resources.player_quality_auto_active
import chatone.composeapp.generated.resources.player_scale_fill
import chatone.composeapp.generated.resources.player_scale_fit
import chatone.composeapp.generated.resources.player_settings
import chatone.composeapp.generated.resources.player_source
import chatone.composeapp.generated.resources.player_video
import chatone.composeapp.generated.resources.player_video_stats
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.player_overlay_look
import chatone.composeapp.generated.resources.player_overlay_look_hint
import chatone.composeapp.generated.resources.player_settings_back
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamLatencyLevel
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVideoScale
import io.rudione.chatone.icons.lucide.Activity
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Headphones
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Palette
import io.rudione.chatone.icons.lucide.PictureInPicture2
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.icons.lucide.Zap
import io.rudione.chatone.presentation.components.ChatoneChip
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.components.FitText
import io.rudione.chatone.presentation.components.chatoneGlassPanel
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillSegmented
import io.rudione.chatone.presentation.components.expressive.PillTone
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

private enum class SettingsPage { MAIN, OVERLAY_LOOK }

@Composable
fun StreamSettingsSheet(
    host: StreamPlayerHost,
    landscape: Boolean,
    pictureInPictureSupported: Boolean
) {
    val state by host.state.collectAsState()
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    val dismiss = { host.onEvent(StreamPlayerEvent.SetSettingsVisible(false)) }
    LaunchedEffect(state.settingsVisible) { if (!state.settingsVisible) page = SettingsPage.MAIN }
    StreamBackHandler(enabled = state.settingsVisible) {
        if (page == SettingsPage.MAIN) dismiss() else page = SettingsPage.MAIN
    }

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
            val panelModifier = if (landscape) Modifier.fillMaxHeight().width(360.dp) else Modifier.fillMaxWidth().heightIn(max = 560.dp)
            Column(
                modifier = panelModifier
                    .padding(8.dp)
                    .chatoneGlassPanel(shape = RoundedCornerShape(24.dp), topAlpha = 1f, bottomAlpha = 1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                SheetHeader(
                    title = pageTitle(page),
                    onBack = if (page == SettingsPage.MAIN) null else ({ page = SettingsPage.MAIN }),
                    onClose = dismiss
                )
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val sign = if (forward) 1 else -1
                        (slideInHorizontally(tween(240)) { sign * it / 3 } + fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(tween(200)) { -sign * it / 3 } + fadeOut(tween(140)))
                    },
                    label = "playerSettingsPage"
                ) { current ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        when (current) {
                            SettingsPage.MAIN -> MainPage(state, host.latencySeconds, pictureInPictureSupported, host::onEvent) {
                                page = SettingsPage.OVERLAY_LOOK
                            }
                            SettingsPage.OVERLAY_LOOK -> OverlayLookPage(state.preferences, host::onEvent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun pageTitle(page: SettingsPage): String {
    return when (page) {
        SettingsPage.MAIN -> stringResource(Res.string.player_settings)
        SettingsPage.OVERLAY_LOOK -> stringResource(Res.string.player_overlay_look)
    }
}

@Composable
private fun MainPage(
    state: StreamPlayerUiState,
    latency: StateFlow<Float?>,
    pictureInPictureSupported: Boolean,
    onEvent: (StreamPlayerEvent) -> Unit,
    onOpenOverlayLook: () -> Unit
) {
    val preferences = state.preferences
    if (state.isPlaybackActive) {
        LatencyRow(latency) { onEvent(StreamPlayerEvent.Reload) }
        SettingsDivider()
    }
    SectionTitle(stringResource(Res.string.player_quality))
    QualityChips(state) { onEvent(StreamPlayerEvent.SelectQuality(it)) }
    SectionTitle(stringResource(Res.string.player_video))
    PillSegmented(
        options = StreamVideoScale.entries.map { it to videoScaleLabel(it) },
        selected = preferences.videoScale,
        onSelect = { onEvent(StreamPlayerEvent.SetVideoScale(it)) },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    SectionTitle(stringResource(Res.string.player_landscape_chat))
    PillSegmented(
        options = LandscapeChatMode.entries.map { it to chatModeLabel(it) },
        selected = preferences.landscapeChatMode,
        onSelect = { onEvent(StreamPlayerEvent.SetLandscapeChatMode(it)) },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    AnimatedVisibility(
        visible = preferences.landscapeChatMode == LandscapeChatMode.OVERLAY,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        NavigationRow(Lucide.Palette, stringResource(Res.string.player_overlay_look), stringResource(Res.string.player_overlay_look_hint), onOpenOverlayLook)
    }
    SettingsDivider()
    ToggleRow(Lucide.Zap, stringResource(Res.string.player_low_latency), stringResource(Res.string.player_low_latency_description), preferences.lowLatency) {
        onEvent(StreamPlayerEvent.SetLowLatency(it))
    }
    ToggleRow(Lucide.Timer, stringResource(Res.string.player_latency_on_video), stringResource(Res.string.player_latency_on_video_hint), preferences.latencyOnVideo) {
        onEvent(StreamPlayerEvent.SetLatencyOnVideo(it))
    }
    ToggleRow(Lucide.Sparkles, stringResource(Res.string.player_ambient_light), stringResource(Res.string.player_ambient_light_hint), preferences.ambientLight) {
        onEvent(StreamPlayerEvent.SetAmbientLight(it))
    }
    if (pictureInPictureSupported) {
        ToggleRow(
            Lucide.PictureInPicture2,
            stringResource(Res.string.player_auto_picture_in_picture),
            stringResource(Res.string.player_auto_picture_in_picture_description),
            preferences.autoPictureInPicture
        ) { onEvent(StreamPlayerEvent.SetAutoPictureInPicture(it)) }
    }
    ToggleRow(Lucide.Headphones, stringResource(Res.string.player_background_audio), stringResource(Res.string.player_background_audio_description), preferences.backgroundAudio) {
        onEvent(StreamPlayerEvent.SetBackgroundAudio(it))
    }
    ToggleRow(Lucide.Activity, stringResource(Res.string.player_video_stats), null, state.statsVisible) { onEvent(StreamPlayerEvent.SetStatsVisible(it)) }
}

@Composable
private fun LatencyRow(latency: StateFlow<Float?>, onCatchUp: () -> Unit) {
    val seconds by latency.collectAsState()
    val current = seconds
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsRowIcon(Lucide.Timer)
        Column(Modifier.weight(1f)) {
            Text(
                current?.let { streamLatencyText(it, compact = false) } ?: stringResource(Res.string.player_latency_unknown),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = current?.let { StreamLatencyLevel.of(it).color() } ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(Res.string.player_latency_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PillButton(stringResource(Res.string.player_catch_up), onClick = onCatchUp, tone = PillTone.Tonal, height = 38.dp)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualityChips(state: StreamPlayerUiState, onSelect: (StreamQualitySelection) -> Unit) {
    val autoLabel = state.activeVariant
        ?.takeIf { state.selection == StreamQualitySelection.Auto }
        ?.let { stringResource(Res.string.player_quality_auto_active, it.displayName) }
        ?: stringResource(Res.string.player_quality_auto)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QualityChip(autoLabel, state.selection == StreamQualitySelection.Auto) { onSelect(StreamQualitySelection.Auto) }
        state.variants.filterNot { it.isAudioOnly }.forEach { variant ->
            val selection = StreamQualitySelection.Fixed(variant.groupId)
            val label = if (variant.isSource) "${variant.displayName} · ${stringResource(Res.string.player_source)}" else variant.displayName
            QualityChip(label, state.selection == selection) { onSelect(selection) }
        }
        if (state.variants.any { it.isAudioOnly }) {
            QualityChip(
                stringResource(Res.string.player_audio_only),
                state.selection == StreamQualitySelection.AudioOnly,
                Lucide.Headphones
            ) { onSelect(StreamQualitySelection.AudioOnly) }
        }
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    ChatoneChip(label = label, onClick = onClick, selected = selected, leadingIcon = icon, height = 36.dp)
}

@Composable
private fun videoScaleLabel(scale: StreamVideoScale): String {
    return if (scale == StreamVideoScale.FIT) stringResource(Res.string.player_scale_fit) else stringResource(Res.string.player_scale_fill)
}

@Composable
private fun SheetHeader(title: String, onBack: (() -> Unit)?, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = onBack != null) {
            RoundIconButton(Lucide.ArrowLeft, stringResource(Res.string.player_settings_back)) { onBack?.invoke() }
        }
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "playerSettingsTitle",
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) { text ->
            FitText(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        RoundIconButton(Lucide.X, stringResource(Res.string.player_close), onClose)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun SettingsRowIcon(icon: ImageVector) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsRowIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun ToggleRow(
    icon: ImageVector?,
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Switch, onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) SettingsRowIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ChatoneSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun SettingsDivider() {
    Spacer(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}
