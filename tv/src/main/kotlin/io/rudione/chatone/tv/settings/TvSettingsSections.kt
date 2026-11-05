package io.rudione.chatone.tv.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import io.rudione.chatone.presentation.theme.ExpressivePalettes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.player_chat_hidden
import chatone.composeapp.generated.resources.player_chat_overlay
import chatone.composeapp.generated.resources.player_chat_side
import chatone.composeapp.generated.resources.player_latency_on_video
import chatone.composeapp.generated.resources.player_latency_on_video_hint
import chatone.composeapp.generated.resources.player_low_latency
import chatone.composeapp.generated.resources.player_low_latency_description
import chatone.composeapp.generated.resources.player_quality_auto
import chatone.composeapp.generated.resources.player_source
import chatone.composeapp.generated.resources.settings_accent_color
import chatone.composeapp.generated.resources.settings_language
import chatone.composeapp.generated.resources.settings_logout
import chatone.composeapp.generated.resources.settings_logout_desc
import chatone.composeapp.generated.resources.tv_settings_chat_mode
import chatone.composeapp.generated.resources.tv_settings_default_quality
import chatone.composeapp.generated.resources.tv_settings_guest
import chatone.composeapp.generated.resources.tv_settings_rights
import chatone.composeapp.generated.resources.tv_settings_rights_desc
import chatone.composeapp.generated.resources.tv_settings_rights_on
import chatone.composeapp.generated.resources.tv_settings_sign_in
import chatone.composeapp.generated.resources.tv_settings_version
import chatone.composeapp.generated.resources.player_ambient_light
import chatone.composeapp.generated.resources.player_ambient_light_hint
import chatone.composeapp.generated.resources.player_scale_fill
import chatone.composeapp.generated.resources.player_scale_fit
import chatone.composeapp.generated.resources.player_video
import chatone.composeapp.generated.resources.settings_clear_cache
import chatone.composeapp.generated.resources.settings_clear_cache_desc
import chatone.composeapp.generated.resources.settings_ui_scale
import chatone.composeapp.generated.resources.tv_settings_clear_confirm
import io.rudione.chatone.domain.stream.StreamVideoScale
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.stream.StreamPlayerEvent
import io.rudione.chatone.presentation.stream.StreamPlayerHost
import io.rudione.chatone.util.BuildConfig
import org.jetbrains.compose.resources.stringResource

private val QualityOptions =
    listOf(StreamPlayerPreferences.QUALITY_AUTO, StreamVariant.SOURCE_GROUP, "720p60", "480p")
private val LanguageOptions = listOf("en" to "English", "ru" to "Русский")
private val UiScaleOptions = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)

@Composable
internal fun TvPlayerSettings(host: StreamPlayerHost?) {
    if (host == null) return
    val state by host.state.collectAsState()
    val preferences = state.preferences
    TvSettingsList {
        item(key = "quality") {
            val value = when (preferences.preferredQuality) {
                StreamPlayerPreferences.QUALITY_AUTO -> stringResource(Res.string.player_quality_auto)
                StreamVariant.SOURCE_GROUP -> stringResource(Res.string.player_source)
                else -> preferences.preferredQuality
            }
            TvCycleRow(
                title = stringResource(Res.string.tv_settings_default_quality),
                value = value,
                options = QualityOptions,
                current = preferences.preferredQuality,
                onChange = { host.onEvent(StreamPlayerEvent.SetPreferredQuality(it)) }
            )
        }
        item(key = "latency") {
            TvSwitchRow(
                stringResource(Res.string.player_low_latency),
                stringResource(Res.string.player_low_latency_description),
                preferences.lowLatency
            ) { host.onEvent(StreamPlayerEvent.SetLowLatency(it)) }
        }
        item(key = "latencyBadge") {
            TvSwitchRow(
                stringResource(Res.string.player_latency_on_video),
                stringResource(Res.string.player_latency_on_video_hint),
                preferences.latencyOnVideo
            ) { host.onEvent(StreamPlayerEvent.SetLatencyOnVideo(it)) }
        }
        item(key = "ambient") {
            TvSwitchRow(
                stringResource(Res.string.player_ambient_light),
                stringResource(Res.string.player_ambient_light_hint),
                preferences.ambientLight
            ) { host.onEvent(StreamPlayerEvent.SetAmbientLight(it)) }
        }
        item(key = "scale") {
            TvCycleRow(
                title = stringResource(Res.string.player_video),
                value = stringResource(
                    when (preferences.videoScale) {
                        StreamVideoScale.FIT -> Res.string.player_scale_fit
                        StreamVideoScale.FILL -> Res.string.player_scale_fill
                    }
                ),
                options = StreamVideoScale.entries,
                current = preferences.videoScale,
                onChange = { host.onEvent(StreamPlayerEvent.SetVideoScale(it)) }
            )
        }
        item(key = "chatMode") {
            TvCycleRow(
                title = stringResource(Res.string.tv_settings_chat_mode),
                value = stringResource(
                    when (preferences.landscapeChatMode) {
                        LandscapeChatMode.SIDE -> Res.string.player_chat_side
                        LandscapeChatMode.OVERLAY -> Res.string.player_chat_overlay
                        LandscapeChatMode.HIDDEN -> Res.string.player_chat_hidden
                    }
                ),
                options = LandscapeChatMode.entries,
                current = preferences.landscapeChatMode,
                onChange = { host.onEvent(StreamPlayerEvent.SetLandscapeChatMode(it)) }
            )
        }
    }
}

@Composable
internal fun TvAppearanceSettings(settings: SettingsState, send: (SettingsEvent) -> Unit) {
    val customThemes = LocalCustomThemeManager.current
    TvSettingsList {
        item(key = "accent") {
            TvPaletteRow(
                title = stringResource(Res.string.settings_accent_color),
                selected = settings.accentColorIndex,
                onSelect = { index ->
                    customThemes.resetToDefault()
                    send(SettingsEvent.OnAccentColorChanged(index))
                }
            )
        }
        item(key = "uiScale") {
            val current = UiScaleOptions.minBy { kotlin.math.abs(it - settings.uiScale) }
            TvCycleRow(
                title = stringResource(Res.string.settings_ui_scale),
                value = "${(current * 100).toInt()}%",
                options = UiScaleOptions,
                current = current,
                onChange = { send(SettingsEvent.OnUiScaleChanged(it)) }
            )
        }
        item(key = "language") {
            TvCycleRow(
                title = stringResource(Res.string.settings_language),
                value = LanguageOptions.firstOrNull { it.first == settings.language }?.second
                    ?: settings.language,
                options = LanguageOptions.map { it.first },
                current = settings.language,
                onChange = { send(SettingsEvent.OnLanguageChanged(it)) }
            )
        }
    }
}

@Composable
internal fun TvAccountSettings(
    account: TwitchAccount?,
    hasExtendedRights: Boolean,
    onConfirmRights: () -> Unit,
    onSignIn: () -> Unit,
    onLogout: () -> Unit
) {
    TvSettingsList {
        if (account == null) {
            item(key = "guest") {
                ListItem(
                    selected = false,
                    onClick = onSignIn,
                    headlineContent = { Text(stringResource(Res.string.tv_settings_sign_in)) },
                    supportingContent = { Text(stringResource(Res.string.tv_settings_guest)) }
                )
            }
            return@TvSettingsList
        }
        item(key = "name") {
            ListItem(
                selected = false,
                onClick = {},
                headlineContent = { Text(account.displayName, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(account.login) }
            )
        }
        item(key = "rights") {
            ListItem(
                selected = false,
                onClick = onConfirmRights,
                headlineContent = { Text(stringResource(Res.string.tv_settings_rights)) },
                supportingContent = { Text(stringResource(Res.string.tv_settings_rights_desc)) },
                trailingContent = if (hasExtendedRights) {
                    { Text(stringResource(Res.string.tv_settings_rights_on)) }
                } else {
                    null
                }
            )
        }
        item(key = "logout") {
            ListItem(
                selected = false,
                onClick = onLogout,
                headlineContent = { Text(stringResource(Res.string.settings_logout)) },
                supportingContent = { Text(stringResource(Res.string.settings_logout_desc)) }
            )
        }
    }
}

@Composable
internal fun TvAboutSettings(onClearCache: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    TvSettingsList {
        item(key = "clearCache") {
            ListItem(
                selected = false,
                onClick = { if (confirmClear) onClearCache() else confirmClear = true },
                headlineContent = {
                    Text(
                        stringResource(if (confirmClear) Res.string.tv_settings_clear_confirm else Res.string.settings_clear_cache),
                        color = if (confirmClear) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                },
                supportingContent = { Text(stringResource(Res.string.settings_clear_cache_desc), maxLines = 2) },
                modifier = Modifier.onFocusChanged { if (!it.isFocused) confirmClear = false }
            )
        }
        item(key = "version") {
            ListItem(
                selected = false,
                onClick = {},
                headlineContent = { Text("Chatone") },
                supportingContent = {
                    Text(
                        stringResource(
                            Res.string.tv_settings_version,
                            BuildConfig.VERSION
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun TvPaletteRow(title: String, selected: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            modifier = Modifier.focusRestorer(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(ExpressivePalettes, key = { _, palette -> palette.name }) { index, palette ->
                val active = index == selected
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier.size(52.dp),
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.18f, pressedScale = 0.94f),
                    border = ClickableSurfaceDefaults.border(
                        border = if (active) Border(BorderStroke(3.dp, scheme.onSurface), shape = CircleShape) else Border.None,
                        focusedBorder = Border(BorderStroke(3.dp, scheme.onSurface), shape = CircleShape)
                    ),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = palette.accent,
                        focusedContainerColor = palette.accent
                    )
                ) {
                    Box(Modifier.fillMaxSize().padding(14.dp).clip(CircleShape).background(palette.base))
                }
            }
        }
        Text(
            ExpressivePalettes.getOrNull(selected)?.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
