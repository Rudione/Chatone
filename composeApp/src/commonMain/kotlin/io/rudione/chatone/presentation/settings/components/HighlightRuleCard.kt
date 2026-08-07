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
import io.rudione.chatone.presentation.components.ChatoneSlider
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
import io.rudione.chatone.presentation.components.ChatoneIconButton

@Composable
internal fun HighlightRuleCardFor(
    rule: HighlightRule,
    vm: SettingsViewModel,
    compact: Boolean = false
) {
    val isCustom = rule.id.startsWith("custom_")
    HightlightRuleCard(
        rule = rule,
        onToggle = { vm.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, it)) },
        onSoundToggle = { vm.sendEvent(SettingsEvent.OnHighlightRuleSoundToggled(rule.id, it)) },
        onColorChange = { color -> vm.sendEvent(SettingsEvent.OnHighlightRuleColorChanged(rule.id, color)) },
        onSubstringToggle = if (isCustom) {
            { vm.sendEvent(SettingsEvent.OnHighlightRuleSubstringToggled(rule.id, it)) }
        } else null,
        onRemove = if (isCustom) {
            { vm.sendEvent(SettingsEvent.OnRemoveHighlightRule(rule.id)) }
        } else null,
        compact = compact
    )
}

@Composable
internal fun HightlightRuleCard(
    rule: HighlightRule,
    onToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onColorChange: (Long) -> Unit = {},
    onSubstringToggle: ((Boolean) -> Unit)? = null,
    onRemove: (() -> Unit)?,
    compact: Boolean = false
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val ruleColor = Color(rule.color)

    Surface(
        shape = RoundedCornerShape(if (compact) 10.dp else 12.dp),
        color = ruleColor.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, ruleColor.copy(alpha = 0.25f)),
        modifier = if (compact) Modifier.fillMaxWidth() else Modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = if (compact) 8.dp else 12.dp,
                    vertical = if (compact) 6.dp else 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(if (compact) 11.dp else 14.dp)
                        .clip(CircleShape)
                        .background(ruleColor)
                        .clickable { showColorPicker = true }
                )
                Spacer(Modifier.width(if (compact) 7.dp else 10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rule.pattern.ifEmpty {
                            val s = LocalStrings.current
                            when (rule.id) {
                                "username" -> s.highlightRuleUsername
                                "whispers" -> s.highlightRuleWhispers
                                "subscriptions" -> s.highlightRuleSubscriptions
                                "first_message" -> s.highlightRuleFirstMessage
                                "search_match" -> s.highlightRuleSearchMatch
                                "mention_accent" -> s.highlightRuleMentionAccent
                                "channel_points" -> s.highlightRuleChannelPoints
                                else -> rule.id.replace("_", " ")
                                    .replaceFirstChar { it.uppercase() }
                            }
                        },
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (compact) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!compact && rule.isRegex) Text(
                        LocalStrings.current.settingsRegex,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ChatoneIconButton(
                    onClick = { onSoundToggle(!rule.playSound) },
                    modifier = Modifier.size(if (compact) 24.dp else 32.dp)
                ) {
                    Icon(
                        if (rule.playSound) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        null,
                        modifier = Modifier.size(if (compact) 13.dp else 16.dp),
                        tint = if (rule.playSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                io.rudione.chatone.presentation.components.ChatoneSwitch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                    modifier = if (compact) Modifier.scale(0.8f) else Modifier
                )

                if (onRemove != null) {
                    ChatoneIconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            if (!compact && onSubstringToggle != null && !rule.isRegex && rule.id.startsWith("custom_")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSubstringToggle(!rule.matchSubstring) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rule.matchSubstring,
                        onCheckedChange = { onSubstringToggle(it) },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            LocalStrings.current.highlightRuleSubstring,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            LocalStrings.current.highlightRuleSubstringDesc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        var pickedColor by remember { mutableStateOf(ruleColor) }
        val hue = remember(pickedColor) {
            val r = pickedColor.red;
            val g = pickedColor.green;
            val b = pickedColor.blue
            val mx = maxOf(r, g, b);
            val mn = minOf(r, g, b)
            if (mx == mn) 0f else when (mx) {
                r -> ((g - b) / (mx - mn) % 6f) / 6f * 360f
                g -> ((b - r) / (mx - mn) + 2f) / 6f * 360f
                else -> ((r - g) / (mx - mn) + 4f) / 6f * 360f
            }
        }
        val sd = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(sd.settingsHighlightColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pickedColor)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Color(0xFFFF6B6B), Color(0xFFFF9F43), Color(0xFFFFD700),
                            Color(0xFF2ECC71), Color(0xFF00BFFF), Color(0xFF9B59B6),
                            Color(0xFFFF69B4), Color(0xFFFF4500)
                        ).forEach { preset ->
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f)
                                    .clip(CircleShape).background(preset)
                                    .border(
                                        if (pickedColor == preset) 2.dp else 0.dp,
                                        MaterialTheme.colorScheme.onSurface, CircleShape
                                    )
                                    .clickable { pickedColor = preset }
                            )
                        }
                    }

                    Text(sd.settingsHue, style = MaterialTheme.typography.labelSmall)
                    ChatoneSlider(
                        value = hue,
                        onValueChange = { h ->
                            pickedColor = Color.hsl(h, 0.85f, 0.55f)
                        },
                        valueRange = 0f..360f,
                        trackHeight = 8.dp,
                        fullTrackBrush = Brush.horizontalGradient(
                            (0..12).map { Color.hsl(it * 30f % 360f, 0.85f, 0.55f) }
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {

                    val argb = pickedColor.copy(alpha = 1f)
                    val longColor = ((argb.red * 255).toLong() shl 16) or
                            ((argb.green * 255).toLong() shl 8) or
                            (argb.blue * 255).toLong() or 0xFF000000L
                    onColorChange(longColor)
                    showColorPicker = false
                }) { Text(sd.apply) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showColorPicker = false
                }) { Text(sd.cancel) }
            }
        )
    }
}
