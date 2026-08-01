package io.rudione.chatone.presentation.settings.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import chatone.composeapp.generated.resources.bell_filled
import chatone.composeapp.generated.resources.bell_outlined
import chatone.composeapp.generated.resources.chatbubbles
import chatone.composeapp.generated.resources.chatbubbles_outline
import chatone.composeapp.generated.resources.icon
import chatone.composeapp.generated.resources.images
import chatone.composeapp.generated.resources.images_outline
import chatone.composeapp.generated.resources.key_outline
import chatone.composeapp.generated.resources.ic_sword
import chatone.composeapp.generated.resources.sparkle_filled
import chatone.composeapp.generated.resources.keyboard_24_filled
import chatone.composeapp.generated.resources.keyboard_24_regular
import chatone.composeapp.generated.resources.musical_notes_outline
import chatone.composeapp.generated.resources.palette_fill_16
import chatone.composeapp.generated.resources.palette_stroke_12
import chatone.composeapp.generated.resources.panel_left_key_16_regular
import chatone.composeapp.generated.resources.person_filled
import chatone.composeapp.generated.resources.shield_filled
import chatone.composeapp.generated.resources.shield_outlined
import chatone.composeapp.generated.resources.star_filled
import chatone.composeapp.generated.resources.star_outlined
import chatone.composeapp.generated.resources.unfold_more
import chatone.composeapp.generated.resources.wallpaper_filled
import chatone.composeapp.generated.resources.wallpaper_outlined
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
import io.rudione.chatone.presentation.settings.components.ModerationSettingsSection
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
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
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
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

@Composable
internal fun AboutCard() {

    val s = LocalStrings.current
    val uriHandler = LocalUriHandler.current

    SettingsGroup(s.settingsAppInfo) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = s.appName,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(Modifier.width(14.dp))

                Column {

                    Text(
                        s.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${s.settingsVersion} ${BuildConfig.VERSION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                s.settingsTelegram,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "https://t.me/rudionee",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        uriHandler.openUri("https://t.me/rudionee")
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun BackupCard(vm: SettingsViewModel) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    SettingsGroup(s.settingsBackupTitle) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                s.settingsBackupDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { menuOpen = true }) {
                        Text(s.automodExport)
                    }
                    ChatoneDropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(s.settingsBackupExportJson) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val backup =
                                        io.rudione.chatone.util.settings.SettingsImportExport.snapshot(
                                            SettingsViewModel.settings
                                        )
                                    val text =
                                        io.rudione.chatone.util.settings.SettingsImportExport.toJson(backup)
                                    io.rudione.chatone.util.automod.saveAutomodText(
                                        "chatone-settings.json",
                                        text
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.settingsBackupExportCsv) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val backup =
                                        io.rudione.chatone.util.settings.SettingsImportExport.snapshot(
                                            SettingsViewModel.settings
                                        )
                                    val text =
                                        io.rudione.chatone.util.settings.SettingsImportExport.toCsv(backup)
                                    io.rudione.chatone.util.automod.saveAutomodText(
                                        "chatone-settings.csv",
                                        text
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.settingsBackupExportXlsx) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val backup =
                                        io.rudione.chatone.util.settings.SettingsImportExport.snapshot(
                                            SettingsViewModel.settings
                                        )
                                    val text =
                                        io.rudione.chatone.util.settings.SettingsImportExport.toXlsx(backup)
                                    io.rudione.chatone.util.automod.saveAutomodText(
                                        "chatone-settings.xlsx",
                                        text
                                    )
                                }
                            }
                        )
                    }
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        val text = io.rudione.chatone.util.automod.readAutomodText() ?: return@launch
                        vm.sendEvent(SettingsEvent.OnImportSettingsText(text))
                    }
                }) {
                    Text(s.settingsBackupImport)
                }
            }
        }
    }
}
