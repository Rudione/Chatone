package io.rudione.chatone.presentation.settings.components

import chatone.composeapp.generated.resources.images
import chatone.composeapp.generated.resources.images_outline
import chatone.composeapp.generated.resources.settings_background_preview
import chatone.composeapp.generated.resources.settings_change_image
import chatone.composeapp.generated.resources.settings_chat_background_image
import chatone.composeapp.generated.resources.settings_choose_background_image
import chatone.composeapp.generated.resources.settings_opaque
import chatone.composeapp.generated.resources.settings_overlay_opacity
import chatone.composeapp.generated.resources.settings_supports_image_formats
import chatone.composeapp.generated.resources.settings_transparent
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
import io.rudione.chatone.presentation.theme.applyChatBlurOverlay
import kotlin.math.roundToInt
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
internal fun BackgroundCard(state: SettingsState, vm: SettingsViewModel) {
    val wallpaperController = LocalWallpaperController.current
    val wallpaper by remember { derivedStateOf { wallpaperController.state } }
    SettingsGroup(stringResource(Res.string.settings_chat_background_image)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val scope = rememberCoroutineScope()
            if (state.wallpaperPath.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = state.wallpaperPath,
                        contentDescription = stringResource(Res.string.settings_background_preview),
                        modifier = Modifier.fillMaxSize().applyChatBlurOverlay(
                            type = wallpaper.blurType,
                            radius = wallpaper.blurRadius,
                            dominantColor = wallpaper.dominantColor,
                            overlayDimming = wallpaper.overlayDimming
                        ),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (state.wallpaperPath.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(Res.drawable.images),
                            null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.wallpaperPath.substringAfterLast("/").substringAfterLast("\\"),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        ChatoneIconButton(
                            onClick = { vm.sendEvent(SettingsEvent.OnWallpaperPathChanged("")) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.settings_overlay_opacity), style = MaterialTheme.typography.bodyMedium)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "${(wallpaper.overlayDimming * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            ThinSlider(
                value = wallpaper.overlayDimming,
                onValueChange = {
                    wallpaperController.updateOverlayDimming(it)
                    vm.sendEvent(SettingsEvent.OnWallpaperDisplayConfigChanged(wallpaperController.state.displayConfig))
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(Res.string.settings_transparent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(Res.string.settings_opaque),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val picked = pickImageFile(); if (picked != null) vm.sendEvent(
                        SettingsEvent.OnWallpaperPathChanged(
                            picked
                        )
                    )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painterResource(Res.drawable.images_outline),
                    null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (state.wallpaperPath.isBlank()) stringResource(Res.string.settings_choose_background_image) else stringResource(Res.string.settings_change_image))
            }
            if (state.wallpaperPath.isBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(Res.string.settings_supports_image_formats),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
