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
import io.rudione.chatone.util.font.resolveFontFamilyWithBundled
import io.rudione.chatone.util.font.BUNDLED_FONT_INTER
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
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu

@Composable
fun FontSettingsCard(
    state: SettingsState,
    vm: SettingsViewModel
) {
    val fontNames = remember(state.customFontPaths) {
        listAvailableFontNames(state.customFontPaths)
    }
    val currentFontIndex = remember(state.fontFamilyName, fontNames) {
        fontNames.indexOfFirst { it == state.fontFamilyName }.coerceAtLeast(0)
    }
    val resolvedFamily = resolveFontFamilyWithBundled(state.fontFamilyName, state.customFontPaths)
    val previewDecoration = when {
        state.fontUnderline && state.fontStrikethrough -> TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
        )

        state.fontUnderline -> TextDecoration.Underline
        state.fontStrikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }

    var fontDropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsGroup("Typography") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "Font Family",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box {
                    OutlinedButton(
                        onClick = { fontDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            state.fontFamilyName,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = resolvedFamily,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    ChatoneDropdownMenu(
                        expanded = fontDropdownExpanded,
                        onDismissRequest = { fontDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        fontNames.forEachIndexed { idx, name ->
                            val isCustom = state.customFontPaths.any { path ->
                                path.substringAfterLast('/').substringAfterLast('\\')
                                    .substringBeforeLast('.') == name
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        name,
                                        fontFamily = resolveFontFamilyWithBundled(name, state.customFontPaths),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (idx == currentFontIndex)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    vm.sendEvent(SettingsEvent.OnFontFamilyChanged(name))
                                    fontDropdownExpanded = false
                                },
                                trailingIcon = if (isCustom) {
                                    {
                                        val matchPath = state.customFontPaths.firstOrNull { p ->
                                            p.substringAfterLast('/').substringAfterLast('\\')
                                                .substringBeforeLast('.') == name
                                        }
                                        ChatoneIconButton(
                                            onClick = {
                                                if (matchPath != null) {
                                                    vm.sendEvent(
                                                        SettingsEvent.OnRemoveCustomFontPath(
                                                            matchPath
                                                        )
                                                    )
                                                }
                                                if (state.fontFamilyName == name) {
                                                    vm.sendEvent(SettingsEvent.OnFontFamilyChanged(BUNDLED_FONT_INTER))
                                                }
                                                fontDropdownExpanded = false
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove custom font",
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Add font file (.ttf / .otf)…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                fontDropdownExpanded = false
                                scope.launch {
                                    val path = pickFontFile()
                                    if (!path.isNullOrBlank()) {
                                        vm.sendEvent(SettingsEvent.OnAddCustomFontPath(path))
                                    }
                                }
                            }
                        )
                    }
                }

                Text(
                    "Style & Effects",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FontStyleChip(
                        label = "I",
                        active = state.fontStyleItalic,
                        fontStyle = FontStyle.Italic,
                        tooltip = "Italic",
                        onClick = { vm.sendEvent(SettingsEvent.OnFontItalicChanged(!state.fontStyleItalic)) }
                    )
                    FontStyleChip(
                        label = "U",
                        active = state.fontUnderline,
                        textDecoration = TextDecoration.Underline,
                        tooltip = "Underline",
                        onClick = { vm.sendEvent(SettingsEvent.OnFontUnderlineChanged(!state.fontUnderline)) }
                    )
                    FontStyleChip(
                        label = "S",
                        active = state.fontStrikethrough,
                        textDecoration = TextDecoration.LineThrough,
                        tooltip = "Strikethrough",
                        onClick = { vm.sendEvent(SettingsEvent.OnFontStrikethroughChanged(!state.fontStrikethrough)) }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val fs = if (state.fontStyleItalic) FontStyle.Italic else FontStyle.Normal
                        Text(
                            "Aa — The quick brown fox",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = resolvedFamily,
                                fontStyle = fs,
                                textDecoration = previewDecoration
                            )
                        )
                        Text(
                            "0123456789 #!@$%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = resolvedFamily,
                                fontStyle = fs,
                                textDecoration = previewDecoration
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            state.fontFamilyName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = resolvedFamily
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        vm.sendEvent(SettingsEvent.OnFontFamilyChanged(BUNDLED_FONT_INTER))
                        vm.sendEvent(SettingsEvent.OnFontItalicChanged(false))
                        vm.sendEvent(SettingsEvent.OnFontUnderlineChanged(false))
                        vm.sendEvent(SettingsEvent.OnFontStrikethroughChanged(false))
                    },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        "Reset to default",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun FontStyleChip(
    label: String,
    active: Boolean,
    tooltip: String = "",
    fontStyle: FontStyle = FontStyle.Normal,
    textDecoration: TextDecoration? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (active)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        border = if (active)
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        else null,
        modifier = Modifier.size(width = 34.dp, height = 30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontStyle = fontStyle,
                    textDecoration = textDecoration,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
