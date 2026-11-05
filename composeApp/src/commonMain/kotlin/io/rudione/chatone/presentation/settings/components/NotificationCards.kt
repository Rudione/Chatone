package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.musical_notes_outline
import chatone.composeapp.generated.resources.settings_browse_sound
import chatone.composeapp.generated.resources.settings_change_sound
import chatone.composeapp.generated.resources.settings_custom_sound
import chatone.composeapp.generated.resources.settings_enable_mention_sound
import chatone.composeapp.generated.resources.settings_enable_mention_sound_desc
import chatone.composeapp.generated.resources.settings_mention_sound
import chatone.composeapp.generated.resources.settings_sound_default
import chatone.composeapp.generated.resources.settings_sound_supported
import chatone.composeapp.generated.resources.settings_volume
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.filled.*
import io.rudione.chatone.icons.material.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chatone.composeapp.generated.resources.Res
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import io.rudione.chatone.presentation.components.rows.LocalSettingsSearch
import io.rudione.chatone.presentation.components.rows.RowDivider
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.components.rows.DropdownRow
import io.rudione.chatone.presentation.components.rows.SliderRow
import io.rudione.chatone.presentation.components.rows.HotkeyRow
import io.rudione.chatone.presentation.settings.theme_settings.ThemeSettingsScreen
import io.rudione.chatone.presentation.settings.theme_settings.ThinSlider
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.ExpressivePalettes
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import androidx.compose.runtime.CompositionLocalProvider
import io.rudione.chatone.util.BuildConfig
import io.rudione.chatone.util.system.HotkeyAction
import io.rudione.chatone.util.system.comboFor
import io.rudione.chatone.util.media.NotificationSoundPlayer
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.presentation.settings.TitleBarMode
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.media.pickAudioFile
import io.rudione.chatone.util.media.pickImageFile
import io.rudione.chatone.util.font.pickFontFile
import io.rudione.chatone.util.font.resolveFontFamily
import io.rudione.chatone.util.font.listAvailableFontNames
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
internal fun NotificationGroupCard(state: SettingsState, vm: SettingsViewModel) {
    SettingsSurface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.settings_mention_sound),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.settings_enable_mention_sound), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(Res.string.settings_enable_mention_sound_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                io.rudione.chatone.presentation.components.ChatoneSwitch(
                    checked = state.mentionSoundEnabled,
                    onCheckedChange = { vm.sendEvent(SettingsEvent.OnMentionSoundChanged(it)) })
            }
            if (state.mentionSoundEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(Res.string.settings_volume),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(state.mentionSoundVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ThinSlider(
                        value = state.mentionSoundVolume,
                        onValueChange = { vm.sendEvent(SettingsEvent.OnMentionSoundVolumeChanged(it)) },
                        valueRange = 0f..1f
                    )
                }
            }
        }
    }
}

@Composable
internal fun CustomSoundCard(state: SettingsState, vm: SettingsViewModel) {
    val scope = rememberCoroutineScope()
    SettingsSurface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.settings_custom_sound),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (state.customMentionSoundPath.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painterResource(Res.drawable.musical_notes_outline),
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            state.customMentionSoundPath.substringAfterLast('/')
                                .substringAfterLast('\\'),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        ChatoneIconButton(onClick = {
                            vm.sendEvent(
                                SettingsEvent.OnCustomMentionSoundPathChanged(
                                    ""
                                )
                            )
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = {
                        scope.launch {
                            val picked = pickAudioFile()
                            if (picked != null) vm.sendEvent(
                                SettingsEvent.OnCustomMentionSoundPathChanged(picked)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        if (state.customMentionSoundPath.isBlank()) stringResource(Res.string.settings_browse_sound) else stringResource(Res.string.settings_change_sound),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                FilledIconButton(
                    onClick = {
                        NotificationSoundPlayer.playMentionSound(
                            volume = state.mentionSoundVolume,
                            customSoundPath = state.customMentionSoundPath
                        )
                    },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) { Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp)) }
            }
            Text(
                text = if (state.customMentionSoundPath.isBlank()) stringResource(Res.string.settings_sound_default) else stringResource(Res.string.settings_sound_supported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
