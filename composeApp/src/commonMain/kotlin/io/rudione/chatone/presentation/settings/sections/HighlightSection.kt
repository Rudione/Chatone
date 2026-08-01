package io.rudione.chatone.presentation.settings.sections

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

import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.components.SettingsSurface
import io.rudione.chatone.presentation.settings.components.NotificationGroupCard
import io.rudione.chatone.presentation.settings.components.CustomSoundCard
import io.rudione.chatone.presentation.settings.components.BackgroundCard
import io.rudione.chatone.presentation.settings.components.AccentColorPaletteRow
import io.rudione.chatone.presentation.settings.components.HighlightRuleCardFor
import io.rudione.chatone.presentation.settings.components.HightlightRuleCard
import io.rudione.chatone.presentation.settings.components.FontSettingsCard
import io.rudione.chatone.presentation.components.ChatoneTextField

internal fun LazyListScope.highlightLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        val s = LocalStrings.current
        Text(
            s.settingsHighlightRulesHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
    val builtInHighlightRules = state.highlightRules.filter { !it.id.startsWith("custom_") }
    val customHighlightRules = state.highlightRules.filter { it.id.startsWith("custom_") }
    builtInHighlightRules.chunked(2).forEach { chunk ->
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chunk.forEach { rule ->
                    Box(modifier = Modifier.weight(1f)) {
                        HighlightRuleCardFor(rule = rule, vm = vm, compact = true)
                    }
                }
                if (chunk.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    items(customHighlightRules, key = { it.id }) { rule ->
        HighlightRuleCardFor(rule = rule, vm = vm)
        Spacer(Modifier.height(4.dp))
    }
    item {
        var newPattern by remember { mutableStateOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChatoneTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                modifier = Modifier.weight(1f),
                placeholder = LocalStrings.current.settingsAddHighlightPattern,
                singleLine = true
            )
            FilledIconButton(
                onClick = {
                    if (newPattern.isNotBlank()) {
                        vm.sendEvent(SettingsEvent.OnAddHighlightRule(newPattern.trim()))
                        newPattern = ""
                    }
                },
                enabled = newPattern.isNotBlank()
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun HighlightContent(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsHighlightRules) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                s.settingsHighlightRulesHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            val builtInHighlightRules = state.highlightRules.filter { !it.id.startsWith("custom_") }
            val customHighlightRules = state.highlightRules.filter { it.id.startsWith("custom_") }
            builtInHighlightRules.chunked(2).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chunk.forEach { rule ->
                        Box(modifier = Modifier.weight(1f)) {
                            HighlightRuleCardFor(rule = rule, vm = vm, compact = true)
                        }
                    }
                    if (chunk.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            customHighlightRules.forEach { rule ->
                HighlightRuleCardFor(rule = rule, vm = vm)
            }
        }
    }
    SettingsGroup(s.settingsAddRule) {
        var newPattern by remember { mutableStateOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChatoneTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                modifier = Modifier.weight(1f),
                placeholder = s.settingsAddHighlightPattern,
                singleLine = true
            )
            FilledIconButton(
                onClick = {
                    if (newPattern.isNotBlank()) {
                        vm.sendEvent(SettingsEvent.OnAddHighlightRule(newPattern.trim()))
                        newPattern = ""
                    }
                },
                enabled = newPattern.isNotBlank()
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
