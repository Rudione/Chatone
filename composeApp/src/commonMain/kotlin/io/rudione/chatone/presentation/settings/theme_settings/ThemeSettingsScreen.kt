package io.rudione.chatone.presentation.settings.theme_settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.palette_fill_16
import io.rudione.chatone.domain.model.ChatoneColorTokens
import io.rudione.chatone.presentation.components.ChatoneSwitch
import io.rudione.chatone.presentation.settings.components.ColorTokensEditor
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.*
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeApplied: () -> Unit,
    settingsViewModel: SettingsViewModel,
    customThemeManager: CustomThemeManager,
    initialSeedColor: Int? = null
) {
    val settingsState by settingsViewModel.state.collectAsState()
    val themes by customThemeManager.savedThemes.collectAsState()
    val currentTheme by customThemeManager.currentTheme.collectAsState()

    val wallpaperController = LocalWallpaperController.current
    val wallpaper by remember { derivedStateOf { wallpaperController.state } }

    var selectedTheme by remember { mutableStateOf(currentTheme) }
    var showCreate by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showGenerator by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) }
    val s = LocalStrings.current

    val save: () -> Unit = {
        settingsViewModel.sendEvent(
            SettingsEvent.OnWallpaperDisplayConfigChanged(wallpaperController.state.displayConfig)
        )
    }

    fun applyTheme(theme: CustomThemeConfig) {
        customThemeManager.saveTheme(theme)
        customThemeManager.setTheme(theme)
        selectedTheme = theme
        onThemeApplied()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(s.themeTitle, fontWeight = FontWeight.Bold)
                        Text(s.themeSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                divider = {}
            ) {
                listOf(s.themeTabThemes to Icons.Outlined.Palette, s.themeTabBackground to Icons.Outlined.Wallpaper,
                    s.themeTabPanels to Icons.Outlined.Dashboard, s.themeTabChat to Icons.Outlined.Chat,
                    s.themeTabGlobal to Icons.Outlined.Tune
                ).forEachIndexed { i, (label, icon) ->
                    Tab(selected = activeTab == i, onClick = { activeTab = i },
                        text = { Text(label, fontWeight = if (activeTab == i) FontWeight.SemiBold else FontWeight.Normal) },
                        icon = { Icon(icon, null, Modifier.size(18.dp)) }
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            when (activeTab) {
                0 -> ThemesTab(themes, selectedTheme, showGenerator,
                    onToggleGenerator = { showGenerator = !showGenerator },
                    onApplyTheme = { applyTheme(it) },
                    onReset = { customThemeManager.resetToDefault(); selectedTheme = null; onThemeApplied() },
                    onDeleteTheme = { themeId ->
                        customThemeManager.deleteTheme(themeId)
                       
                        settingsViewModel.sendEvent(SettingsEvent.OnDeleteCustomTheme(themeId))
                        settingsViewModel.sendEvent(SettingsEvent.OnAccentColorChanged(0))
                    },
                    onCreateClick = { showCreate = true },
                    settingsState = settingsState, initialSeedColor = initialSeedColor
                )
                1 -> BackgroundTab(wallpaper,
                    onBlurType   = { wallpaperController.updateBlurType(it); save() },
                    onBlurRadius = { wallpaperController.updateBlurRadius(it); save() },
                    onDimming    = { wallpaperController.updateOverlayDimming(it); save() }
                )
                2 -> PanelsTab(
                    panelConfig = wallpaper.panelColorConfig,
                    onUpdate = { wallpaperController.updatePanelColors(it); save() },
                    onResetAll = { wallpaperController.updatePanelColors(PanelColorConfig()); save() }
                )
                3 -> ChatColorTab(
                    wallpaper = wallpaper,
                    onUpdate = { wallpaperController.updateChatColor(it); save() },
                    onReset  = { wallpaperController.updateChatColor(ChatColorConfig()); save() },
                    colorTokens = settingsState.colorTokens,
                    onColorTokensChange = { settingsViewModel.sendEvent(SettingsEvent.OnColorTokensChanged(it)) }
                )
                4 -> GlobalTab(
                    displayConfig = wallpaper.displayConfig,
                    onUpdate = { newCfg -> wallpaperController.setDisplayConfig(newCfg); save() },
                    onResetAll = { wallpaperController.resetToDefault(); save() }
                )
            }
        }
    }

    if (showCreate) {
        CreateThemeDialog(onDismiss = { showCreate = false }, onCreate = { n, s, d ->
            applyTheme(CustomThemeConfig(id = genId(), name = n, seedColor = s.toArgb(), isDark = d)); showCreate = false
        })
    }
    if (showEdit && selectedTheme != null) {
        EditThemeDialog(theme = selectedTheme!!, onDismiss = { showEdit = false },
            onSave = { applyTheme(it); showEdit = false },
            onResetOverrides = { applyTheme(selectedTheme!!.copy(customOverrides = emptyMap())) }
        )
    }
}


@Composable
private fun ThemesTab(
    themes: List<CustomThemeConfig>, selectedTheme: CustomThemeConfig?,
    showGenerator: Boolean, onToggleGenerator: () -> Unit,
    onApplyTheme: (CustomThemeConfig) -> Unit, onReset: () -> Unit,
    onDeleteTheme: (String) -> Unit = {},
    onCreateClick: () -> Unit,
    settingsState: io.rudione.chatone.presentation.settings.SettingsState, initialSeedColor: Int?
) {
    val s = LocalStrings.current
    var seed by remember { mutableStateOf(Color(initialSeedColor ?: 0xFF7C4DFF.toInt())) }
    var isDark by remember { mutableStateOf(settingsState.darkTheme) }
    var contrast by remember { mutableStateOf(0f) }
    val preview = remember(seed, isDark, contrast) { ColorSchemeGenerator.generateFromSeed(seed.toArgb(), isDark, contrast) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onToggleGenerator, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (showGenerator) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                ) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(s.themeAutoGenerate) }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(s.themeReset)
                }
            }
        }
        item {
            AnimatedVisibility(showGenerator, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                AutoGeneratorPanel(seed, { seed = it }, isDark, { isDark = it }, contrast, { contrast = it }, preview) {
                    onApplyTheme(CustomThemeConfig(id = genId(), name = "Auto", seedColor = seed.toArgb(), isDark = isDark, contrastLevel = contrast))
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.themeSavedThemes, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FilledTonalIconButton(onClick = onCreateClick, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, null, Modifier.size(20.dp)) }
            }
        }
        if (themes.isEmpty()) item { EmptyThemesPlaceholder(onCreateClick) }
        items(themes, key = { it.id }) { theme ->
            ThemeCard(theme, selectedTheme?.id == theme.id, onSelect = { onApplyTheme(it) }, onDelete = { onDeleteTheme(it.id) })
        }
    }
}


@Composable
private fun BackgroundTab(
    wallpaper: WallpaperState,
    onBlurType: (BlurType) -> Unit, onBlurRadius: (Float) -> Unit, onDimming: (Float) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            InfoBanner("This blur is for the CHAT area when a wallpaper image is active. To blur panels (sidebar, bars), use the Panels tab.")
        }
        item {
            SectionCard("Chat Blur Style", icon = Icons.Outlined.BlurOn) {
                BlurTypeSelector(wallpaper.blurType, onBlurType)
            }
        }
        item {
            AnimatedVisibility(wallpaper.blurType != BlurType.NONE) {
                SectionCard("Blur Intensity", icon = Icons.Outlined.Tune) {
                    LabeledSlider("Radius", wallpaper.blurRadius, onBlurRadius, 0f..40f, "${wallpaper.blurRadius.toInt()}dp")
                }
            }
        }
        item {
            SectionCard("Overlay Dimming", icon = Icons.Outlined.BrightnessLow) {
                LabeledSlider("Dim", wallpaper.overlayDimming, onDimming, 0f..1f, "${(wallpaper.overlayDimming*100).toInt()}%")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.3f to "Subtle", 0.55f to "Normal", 0.75f to "Strong", 0.90f to "Max").forEach { (v, l) ->
                        SuggestionChip(onClick = { onDimming(v) }, label = { Text(l, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (kotlin.math.abs(wallpaper.overlayDimming - v) < 0.06f)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PanelsTab(
    panelConfig: PanelColorConfig,
    onUpdate: (PanelColorConfig) -> Unit,
    onResetAll: () -> Unit
) {
    val s = LocalStrings.current
    val hasCustomValues = panelConfig != PanelColorConfig()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            InfoBanner("Panel blur blurs the wallpaper behind each panel independently. Glass intensity controls LiquidGlass transparency.")
        }

       
        item {
            AnimatedVisibility(hasCustomValues) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onResetAll,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(6.dp))
                        Text(s.themeResetAllPanels, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        item {
            PanelSection(
                title = "Sidebar", subtitle = "Channel list • left rail", icon = Icons.Outlined.ViewSidebar,
                useCustom = panelConfig.useSidebarCustomColor,
                onUseCustomChanged = { onUpdate(panelConfig.copy(useSidebarCustomColor = it)) },
                colorArgb = panelConfig.sidebarCustomColor,
                onColorChanged = { onUpdate(panelConfig.copy(sidebarCustomColor = it)) },
                alpha = panelConfig.sidebarAlpha,
                onAlphaChanged = { onUpdate(panelConfig.copy(sidebarAlpha = it)) },
                brightness = panelConfig.sidebarBrightness,
                onBrightnessChanged = { onUpdate(panelConfig.copy(sidebarBrightness = it)) },
                blurRadius = panelConfig.sidebarBlurRadius,
                onBlurChanged = { onUpdate(panelConfig.copy(sidebarBlurRadius = it)) },
                glassIntensity = panelConfig.sidebarGlassIntensity,
                onGlassChanged = { onUpdate(panelConfig.copy(sidebarGlassIntensity = it)) },
                isCustomised = panelConfig.useSidebarCustomColor || panelConfig.sidebarAlpha != 0.92f
                        || panelConfig.sidebarBrightness != 0f || panelConfig.sidebarBlurRadius != 0f
                        || panelConfig.sidebarGlassIntensity != 0.72f,
                onResetSection = { onUpdate(panelConfig.copy(
                    useSidebarCustomColor = false, sidebarCustomColor = null,
                    sidebarAlpha = 0.92f, sidebarBrightness = 0f,
                    sidebarBlurRadius = 0f, sidebarGlassIntensity = 0.72f
                ))}
            )
        }

        item {
            PanelSection(
                title = "Top Bar", subtitle = "Mod buttons • channel tabs", icon = Icons.Outlined.Crop75,
                useCustom = panelConfig.useTopBarCustomColor,
                onUseCustomChanged = { onUpdate(panelConfig.copy(useTopBarCustomColor = it)) },
                colorArgb = panelConfig.topBarCustomColor,
                onColorChanged = { onUpdate(panelConfig.copy(topBarCustomColor = it)) },
                alpha = panelConfig.topBarAlpha,
                onAlphaChanged = { onUpdate(panelConfig.copy(topBarAlpha = it)) },
                brightness = panelConfig.topBarBrightness,
                onBrightnessChanged = { onUpdate(panelConfig.copy(topBarBrightness = it)) },
                blurRadius = panelConfig.topBarBlurRadius,
                onBlurChanged = { onUpdate(panelConfig.copy(topBarBlurRadius = it)) },
                glassIntensity = panelConfig.topBarGlassIntensity,
                onGlassChanged = { onUpdate(panelConfig.copy(topBarGlassIntensity = it)) },
                isCustomised = panelConfig.useTopBarCustomColor || panelConfig.topBarAlpha != 0.88f
                        || panelConfig.topBarBrightness != 0f || panelConfig.topBarBlurRadius != 0f
                        || panelConfig.topBarGlassIntensity != 0.80f,
                onResetSection = { onUpdate(panelConfig.copy(
                    useTopBarCustomColor = false, topBarCustomColor = null,
                    topBarAlpha = 0.88f, topBarBrightness = 0f,
                    topBarBlurRadius = 0f, topBarGlassIntensity = 0.80f
                ))}
            )
        }

        item {
            PanelSection(
                title = "Bottom Bar", subtitle = "Input field • emoji • send", icon = Icons.Outlined.TextFields,
                useCustom = panelConfig.useBottomBarCustomColor,
                onUseCustomChanged = { onUpdate(panelConfig.copy(useBottomBarCustomColor = it)) },
                colorArgb = panelConfig.bottomBarCustomColor,
                onColorChanged = { onUpdate(panelConfig.copy(bottomBarCustomColor = it)) },
                alpha = panelConfig.bottomBarAlpha,
                onAlphaChanged = { onUpdate(panelConfig.copy(bottomBarAlpha = it)) },
                brightness = panelConfig.bottomBarBrightness,
                onBrightnessChanged = { onUpdate(panelConfig.copy(bottomBarBrightness = it)) },
                blurRadius = panelConfig.bottomBarBlurRadius,
                onBlurChanged = { onUpdate(panelConfig.copy(bottomBarBlurRadius = it)) },
                glassIntensity = panelConfig.bottomBarGlassIntensity,
                onGlassChanged = { onUpdate(panelConfig.copy(bottomBarGlassIntensity = it)) },
                isCustomised = panelConfig.useBottomBarCustomColor || panelConfig.bottomBarAlpha != 0.90f
                        || panelConfig.bottomBarBrightness != 0f || panelConfig.bottomBarBlurRadius != 0f
                        || panelConfig.bottomBarGlassIntensity != 0.82f,
                onResetSection = { onUpdate(panelConfig.copy(
                    useBottomBarCustomColor = false, bottomBarCustomColor = null,
                    bottomBarAlpha = 0.90f, bottomBarBrightness = 0f,
                    bottomBarBlurRadius = 0f, bottomBarGlassIntensity = 0.82f
                ))}
            )
        }
    }
}


@Composable
private fun ChatColorTab(
    wallpaper: WallpaperState,
    onUpdate: (ChatColorConfig) -> Unit,
    onReset: () -> Unit,
    colorTokens: ChatoneColorTokens,
    onColorTokensChange: (ChatoneColorTokens) -> Unit
) {
    val s = LocalStrings.current
    val cfg = wallpaper.chatColorConfig
    val wallpaperActive = wallpaper.isActive

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        item {
            SectionCard(s.colorsSectionTitle, subtitle = s.colorsSectionSubtitle, icon = Icons.Outlined.Palette) {
                ColorTokensEditor(tokens = colorTokens, onChange = onColorTokensChange)
            }
        }

       
        if (wallpaperActive) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Wallpaper, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                        Text(
                            "Wallpaper is active — the dominant colour from the image is used as chat background. Custom colour is ignored while wallpaper is on.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        } else {
            item { InfoBanner("No wallpaper active — you can set a custom chat background colour below.") }
        }

        item {
            SectionCard("Chat Background", icon = Icons.Outlined.Chat) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(s.themeCustomColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (wallpaperActive) "Disabled while wallpaper is active"
                            else if (cfg.useCustomColor) "Using your colour" else "Using theme default",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (wallpaperActive) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ChatoneSwitch(
                        checked = cfg.useCustomColor && !wallpaperActive,
                        onCheckedChange = { if (!wallpaperActive) onUpdate(cfg.copy(useCustomColor = it)) },
                        enabled = !wallpaperActive
                    )
                }

                AnimatedVisibility(cfg.useCustomColor && !wallpaperActive) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        HexColorInput(cfg.customBackgroundColor) { onUpdate(cfg.copy(customBackgroundColor = it)) }
                        Spacer(Modifier.height(12.dp))
                        Text(s.themeQuickPresets, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        ChatColorPresets { onUpdate(cfg.copy(customBackgroundColor = it)) }
                    }
                }
            }
        }

        item {
            SectionCard("Fine-tuning", icon = Icons.Outlined.Tune) {
               
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.themeAdjustments, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (cfg != ChatColorConfig()) {
                        TextButton(onClick = onReset) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.themeReset, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                LabeledSlider("Opacity", cfg.backgroundAlpha, { onUpdate(cfg.copy(backgroundAlpha = it)) }, 0.1f..1f, "${(cfg.backgroundAlpha*100).toInt()}%")
                Spacer(Modifier.height(6.dp))
                LabeledSlider("Brightness", cfg.brightness, { onUpdate(cfg.copy(brightness = it)) }, -0.8f..0.8f, adjStr(cfg.brightness))
                Spacer(Modifier.height(6.dp))
                LabeledSlider("Saturation", cfg.saturation, { onUpdate(cfg.copy(saturation = it)) }, 0f..2f, "${(cfg.saturation*100).toInt()}%")
                Spacer(Modifier.height(6.dp))
                LabeledSlider("Contrast", cfg.contrast, { onUpdate(cfg.copy(contrast = it)) }, 0.5f..2f, "${(cfg.contrast*100).toInt()}%")
            }
        }

        item { ChatSimulationPreview(wallpaper) }
    }
}


@Composable
private fun PanelSection(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    useCustom: Boolean, onUseCustomChanged: (Boolean) -> Unit,
    colorArgb: Int?, onColorChanged: (Int) -> Unit,
    alpha: Float, onAlphaChanged: (Float) -> Unit,
    brightness: Float, onBrightnessChanged: (Float) -> Unit,
    blurRadius: Float, onBlurChanged: (Float) -> Unit,
    glassIntensity: Float, onGlassChanged: (Float) -> Unit,
    isCustomised: Boolean = false,
    onResetSection: () -> Unit = {}
) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    SectionCard(title, subtitle, icon) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
           
            if (isCustomised) {
                TextButton(
                    onClick = onResetSection,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(s.themeReset, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    "Default",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Less" else "More")
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp))
                }
                io.rudione.chatone.presentation.components.ChatoneSwitch(checked = useCustom, onCheckedChange = onUseCustomChanged)
            }
        }

        AnimatedVisibility(useCustom || expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(4.dp))
                AnimatedVisibility(useCustom) {
                    HexColorInput(argb = colorArgb, onColorPicked = onColorChanged)
                }
                LabeledSlider("Opacity", alpha, onAlphaChanged, 0.1f..1f, "${(alpha*100).toInt()}%")
                LabeledSlider("Brightness", brightness, onBrightnessChanged, -0.6f..0.6f, adjStr(brightness))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                LabeledSlider(
                    "Panel blur", blurRadius, onBlurChanged, 0f..30f,
                    if (blurRadius == 0f) "Off" else "${blurRadius.toInt()}dp"
                )
                LabeledSlider(
                    "Glass", glassIntensity, onGlassChanged, 0.1f..1f,
                    when {
                        glassIntensity < 0.30f -> "Crystal"
                        glassIntensity < 0.55f -> "Frosted"
                        glassIntensity < 0.80f -> "Tinted"
                        else -> "Solid"
                    }
                )
            }
        }
    }
}


@Composable
private fun ChatSimulationPreview(wallpaper: WallpaperState) {
    val s = LocalStrings.current
    val chatPaneDefaultColor = MaterialTheme.colorScheme.surfaceContainer
    val chatBg = remember(wallpaper.displayConfig, wallpaper.dominantColor, wallpaper.isActive, chatPaneDefaultColor) {
        chatPaneBackgroundColor(wallpaper, chatPaneDefaultColor)
    }
    val textOnBg = if (chatBg.luminance() > 0.4f) Color(0xFF0A0A14) else Color(0xFFF0F0F5)
    val textSecondary = textOnBg.copy(alpha = 0.5f)

    data class Msg(val user: String, val color: Color, val text: String, val badge: String? = null,
                   val highlight: Color? = null, val isSub: Boolean = false, val isFirst: Boolean = false)

    val msgs = remember { listOf(
        Msg("skillz0r1337", Color(0xFF9B6DFF), "PogChamp WOW insane play!!!", "⭐"),
        Msg("ModeratorMike", Color(0xFF00E5FF), "Keep chat civil everyone monkaS", "⚔️", Color(0xFF00E5FF).copy(0.07f)),
        Msg("YourName", Color(0xFFFF6B8A), "@skillz0r1337 totally agree bro!", null, Color(0xFF7C4DFF).copy(0.12f)),
        Msg("SubGiftUser", Color(0xFF34D399), "Thanks for the gift sub! KEKW", "⭐", null, isSub = true),
        Msg("NewViewer123", Color(0xFFFFD54F), "Hello! First time watching PauseChamp", null, null, isFirst = true),
    ) }

    SectionCard("Live Chat Preview", icon = Icons.Outlined.Visibility) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
            Text(s.themeLivePreview, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            if (wallpaper.isActive) {
                Spacer(Modifier.width(4.dp))
                Text("• wallpaper dominant colour", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Surface(color = chatBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                msgs.forEach { msg ->
                    val rowBg = when {
                        msg.isFirst -> Color(0xFF7B2FBE).copy(0.12f)
                        msg.isSub   -> Color(0xFF34D399).copy(0.07f)
                        msg.highlight != null -> msg.highlight
                        else -> Color.Transparent
                    }
                    Row(Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        msg.badge?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 4.dp, top = 1.dp)) }
                        Text("12:34 ", style = MaterialTheme.typography.labelSmall, color = textSecondary, fontSize = 10.sp)
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(color = msg.color, fontWeight = FontWeight.SemiBold)) { append(msg.user) }
                            withStyle(SpanStyle(color = textSecondary)) { append(": ") }
                            val t = msg.text
                            if (t.startsWith("@")) {
                                withStyle(SpanStyle(color = Color(0xFF9B6DFF), fontWeight = FontWeight.Medium)) { append(t.substringBefore(" ")) }
                                withStyle(SpanStyle(color = textOnBg)) { append(t.substringAfter(" ", "")) }
                            } else {
                                withStyle(SpanStyle(color = textOnBg)) { append(t) }
                            }
                            if (msg.isFirst) withStyle(SpanStyle(color = Color(0xFF9B6DFF).copy(0.6f), fontSize = 10.sp)) { append("  ★ First") }
                            if (msg.isSub) withStyle(SpanStyle(color = Color(0xFF34D399).copy(0.7f), fontSize = 10.sp)) { append("  ★ Sub") }
                        }, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Divider(color = textOnBg.copy(0.08f))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(Modifier.weight(1f).height(28.dp), color = textOnBg.copy(0.06f), shape = RoundedCornerShape(8.dp)) {
                        Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                            Text(s.themeSendMessagePlaceholder, style = MaterialTheme.typography.bodySmall, color = textSecondary, fontSize = 11.sp)
                        }
                    }
                    Surface(shape = CircleShape, color = textOnBg.copy(0.08f), modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("😊", fontSize = 12.sp) }
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(width = 42.dp, height = 28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }
    }
}



@Composable
private fun GlobalTab(
    displayConfig: WallpaperDisplayConfig,
    onUpdate: (WallpaperDisplayConfig) -> Unit,
    onResetAll: () -> Unit
) {
    val s = LocalStrings.current
    val isNonDefault = displayConfig != WallpaperDisplayConfig()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        item {
           
            AnimatedVisibility(isNonDefault) {
                FilledTonalButton(
                    onClick = onResetAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(s.themeResetAllAppearance, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            SectionCard("Apply One Color to Everything", icon = Icons.Outlined.Palette) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(s.themeMasterColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (displayConfig.useMasterColor) "Overrides all panel defaults"
                            else "Off — each panel uses its own color",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    io.rudione.chatone.presentation.components.ChatoneSwitch(
                        checked = displayConfig.useMasterColor,
                        onCheckedChange = { onUpdate(displayConfig.copy(useMasterColor = it)) }
                    )
                }

                AnimatedVisibility(displayConfig.useMasterColor) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        HexColorInput(displayConfig.masterColor) {
                            onUpdate(displayConfig.copy(masterColor = it, useMasterColor = true))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(s.themePresets, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(
                                Color(0xFF0A0A0F) to "Black", Color(0xFF18182A) to "Navy",
                                Color(0xFF12092A) to "Violet", Color(0xFF0D1B2A) to "Ocean",
                                Color(0xFF071A0E) to "Forest", Color(0xFF1A080A) to "Ember",
                                Color(0xFF1A1000) to "Gold", Color(0xFF101020) to "Midnight"
                            )) { (color, name) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(color)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable { onUpdate(displayConfig.copy(masterColor = color.toArgb(), useMasterColor = true)) })
                                    Spacer(Modifier.height(3.dp))
                                    Text(name, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(s.themeGlowEffects, icon = Icons.Outlined.AutoAwesome) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(s.themeGlowEffects, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            s.themeGlowEffectsDesc,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    io.rudione.chatone.presentation.components.ChatoneSwitch(
                        checked = displayConfig.glowEffectsEnabled,
                        onCheckedChange = { onUpdate(displayConfig.copy(glowEffectsEnabled = it)) }
                    )
                }
            }
        }

        item {
            SectionCard("Global Adjustments", icon = Icons.Outlined.Tune) {
                Text(s.themeAppliedOnTop, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                LabeledSlider("Brightness", displayConfig.globalBrightness, { onUpdate(displayConfig.copy(globalBrightness = it)) }, -0.8f..0.8f, adjStr(displayConfig.globalBrightness))
                Spacer(Modifier.height(6.dp))
                LabeledSlider("Saturation", displayConfig.globalSaturation, { onUpdate(displayConfig.copy(globalSaturation = it)) }, 0f..2f, "${(displayConfig.globalSaturation * 100).toInt()}%")
                Spacer(Modifier.height(6.dp))
                LabeledSlider("Contrast", displayConfig.globalContrast, { onUpdate(displayConfig.copy(globalContrast = it)) }, 0.5f..2f, "${(displayConfig.globalContrast * 100).toInt()}%")
                Spacer(Modifier.height(4.dp))
                val isDefaultAdj = displayConfig.globalBrightness == 0f && displayConfig.globalSaturation == 1f && displayConfig.globalContrast == 1f
                AnimatedVisibility(!isDefaultAdj) {
                    TextButton(onClick = { onUpdate(displayConfig.copy(globalBrightness = 0f, globalSaturation = 1f, globalContrast = 1f)) },
                        modifier = Modifier.align(Alignment.End)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(s.themeResetAdjustments, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}


@Composable
private fun BlurTypeSelector(selected: BlurType, onSelect: (BlurType) -> Unit) {
    val items = listOf(
        BlurType.NONE to ("Off" to Icons.Outlined.BlurOff),
        BlurType.UNIFORM to ("Uniform" to Icons.Outlined.BlurOn),
        BlurType.FROSTED to ("Frosted" to Icons.Outlined.AcUnit),
        BlurType.RADIAL to ("Radial" to Icons.Outlined.RadioButtonChecked),
        BlurType.VIGNETTE to ("Vignette" to Icons.Outlined.Vignette),
        BlurType.LINEAR_H to ("Linear H" to Icons.Outlined.MoreHoriz),
        BlurType.LINEAR_V to ("Linear V" to Icons.Outlined.MoreVert),
        BlurType.SUNSHINE to ("Sunshine" to Icons.Outlined.WbSunny)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, pair) ->
                    val (label, icon) = pair
                    val isSel = selected == type
                    val bg by animateColorAsState(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, tween(200), label = "bb")
                    Surface(onClick = { onSelect(type) }, modifier = Modifier.weight(1f).height(68.dp), shape = RoundedCornerShape(12.dp), color = bg,
                        border = if (isSel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(icon, label, Modifier.size(22.dp), tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AutoGeneratorPanel(
    seedColor: Color, onSeedChanged: (Color) -> Unit,
    isDark: Boolean, onDarkChanged: (Boolean) -> Unit,
    contrast: Float, onContrastChanged: (Float) -> Unit,
    preview: ColorScheme, onApply: () -> Unit
) {
    val s = LocalStrings.current
    var showPicker by remember { mutableStateOf(false) }
    SectionCard("Auto Generator", icon = Icons.Default.AutoAwesome) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(seedColor)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .pointerInput(Unit) { detectDragGestures { ch, d -> ch.consume(); val h = seedColor.toHsl(); onSeedChanged(Color.hsl(((h.hue+d.x*0.5f)%360f+360f)%360f, (h.saturation-d.y*0.008f).coerceIn(0f,1f), h.lightness)) } }
                .clickable { showPicker = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ColorLens, null, tint = if (seedColor.luminance() > 0.4f) Color.Black.copy(0.6f) else Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("#${seedColor.toArgb().toUInt().toString(16).padStart(8,'0').substring(2).uppercase()}", style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(s.themeDragTapHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = { showPicker = true }) { Text(s.themePick) }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(Color(0xFF7C4DFF), Color(0xFF00BFA5), Color(0xFFFF6E6E), Color(0xFF4FC3F7), Color(0xFF81C784), Color(0xFFF48FB1), Color(0xFFFFD54F), Color(0xFF9FA8DA))) { p ->
                Box(Modifier.size(32.dp).clip(CircleShape).background(p).border(if (seedColor == p) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { onSeedChanged(p) })
            }
        }
        Spacer(Modifier.height(14.dp)); Divider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.themeMode, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(50.dp))
            M3SegBtn(listOf("Dark", "Light"), if (isDark) 0 else 1, { onDarkChanged(it == 0) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        LabeledSlider("Contrast", contrast, onContrastChanged, -1f..1f, adjStr(contrast))
        Spacer(Modifier.height(14.dp)); Divider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(12.dp))
        Text(s.themePreview, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Primary" to (preview.primary to preview.onPrimary), "Surface" to (preview.surface to preview.onSurface), "Error" to (preview.error to preview.onError)).forEach { (l,c) ->
                PreviewChip(l, c.first, c.second, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(onClick = onApply, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(s.themeApplyTheme, fontWeight = FontWeight.SemiBold)
        }
    }
    if (showPicker) ColorPickerDialog(seedColor, { onSeedChanged(it); showPicker = false }, { showPicker = false })
}


@Composable
private fun ThemeCard(theme: CustomThemeConfig, isSelected: Boolean, onSelect: (CustomThemeConfig) -> Unit, onDelete: (CustomThemeConfig) -> Unit = {}) {
    val s = remember(theme) { ColorSchemeGenerator.generateFromSeed(theme.seedColor, theme.isDark, theme.contrastLevel) }
    Card(Modifier.fillMaxWidth().clickable { onSelect(theme) }
        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(8.dp))) {
                listOf(s.primary, s.secondary, s.tertiary, s.surface, s.error).forEach { c -> Box(Modifier.weight(1f).fillMaxHeight().background(c)) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(theme.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("#${theme.seedColor.toUInt().toString(16).padStart(8,'0').substring(2).uppercase()} · ${if (theme.isDark) "Dark" else "Light"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isSelected) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(4.dp).size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { onDelete(theme) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}


@Composable
private fun CreateThemeDialog(onDismiss: () -> Unit, onCreate: (String, Color, Boolean) -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var seed by remember { mutableStateOf(Color(0xFF7C4DFF.toInt())) }
    var dark by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(s.themeNewTheme, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(s.themeName) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(seed).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)).clickable { showPicker = true })
                    Column { Text(s.themeSeed, style = MaterialTheme.typography.labelSmall); Text("#${seed.toArgb().toUInt().toString(16).padStart(8,'0').substring(2).uppercase()}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.weight(1f)); FilledTonalButton(onClick = { showPicker = true }) { Text(s.themePick) }
                }
                M3SegBtn(listOf("Dark", "Light"), if (dark) 0 else 1, { dark = it == 0 })
            }
        },
        confirmButton = { Button(onClick = { onCreate(name.ifBlank { "My Theme" }, seed, dark) }, enabled = name.isNotBlank()) { Text(s.themeCreate) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.themeCancel) } }
    )
    if (showPicker) ColorPickerDialog(seed, { seed = it; showPicker = false }, { showPicker = false })
}

@Composable
private fun EditThemeDialog(theme: CustomThemeConfig, onDismiss: () -> Unit, onSave: (CustomThemeConfig) -> Unit, onResetOverrides: () -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf(theme.name) }
    var overrides by remember { mutableStateOf(theme.customOverrides.toMutableMap()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(s.themeEditTheme, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(s.themeName) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Text(s.themeOverrideColors, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(ColorSchemeGenerator.getAvailableRoles()) { role ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ColorSchemeGenerator.roleToDisplayName(role), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            val cur = overrides[role]?.let { Color(it) } ?: run { val s = ColorSchemeGenerator.generateFromSeed(theme.seedColor, theme.isDark); when(role) { "primary" -> s.primary; "secondary" -> s.secondary; "tertiary" -> s.tertiary; "error" -> s.error; "background" -> s.background; "surface" -> s.surface; else -> s.outline } }
                            var showP by remember { mutableStateOf(false) }
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(cur).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable { showP = true })
                            if (showP) ColorPickerDialog(cur, { overrides[role] = it.toArgb(); showP = false }, { showP = false })
                            AnimatedVisibility(overrides.containsKey(role)) { IconButton(onClick = { overrides.remove(role) }, Modifier.size(24.dp)) { Icon(Icons.Filled.Close, null, Modifier.size(14.dp)) } }
                        }
                    }
                }
                TextButton(onClick = onResetOverrides, Modifier.align(Alignment.End)) { Text(s.themeResetOverrides, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(onClick = { onSave(theme.copy(name = name.ifBlank { "My Theme" }, customOverrides = overrides)) }) { Text(s.themeSave) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.themeCancel) } }
    )
}

@Composable
private fun ColorPickerDialog(initial: Color, onSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var color by remember { mutableStateOf(initial) }
    var hex by remember(color) { mutableStateOf(color.toArgb().toUInt().toString(16).padStart(8,'0').substring(2).uppercase()) }
    val hsl = color.toHsl(); val fm = LocalFocusManager.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text(s.themeColorPicker, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(14.dp)).background(color).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)), Alignment.Center) {
                    Text("#$hex", style = MaterialTheme.typography.titleMedium, color = if (color.luminance() > 0.5f) Color.Black else Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(hex, { raw -> val c = raw.trimStart('#').uppercase().filter { it.isLetterOrDigit() }.take(6); hex = c; if (c.length == 6) try { color = Color(("FF$c").toLong(16).toInt()) } catch (_: Exception) {} },
                    label = { Text(s.themeHex) }, prefix = { Text("#") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(Color(0xFF7C4DFF), Color(0xFF00BFA5), Color(0xFFFF6E6E), Color(0xFF4FC3F7), Color(0xFF81C784), Color(0xFFF48FB1), Color(0xFFFFD54F), Color(0xFF9FA8DA), Color(0xFF0A0A0F), Color(0xFFFFFFFF))) { p ->
                        Box(Modifier.size(36.dp).clip(CircleShape).background(p).border(if (color==p) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { color=p; hex=p.toArgb().toUInt().toString(16).padStart(8,'0').substring(2).uppercase() })
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                HslRow("Hue", hsl.hue, 0f..360f, "${hsl.hue.toInt()}°") { color = Color.hsl(it, hsl.saturation, hsl.lightness) }
                HslRow("Sat", hsl.saturation, 0f..1f, "${(hsl.saturation*100).toInt()}%") { color = Color.hsl(hsl.hue, it, hsl.lightness) }
                HslRow("Light", hsl.lightness, 0f..1f, "${(hsl.lightness*100).toInt()}%") { color = Color.hsl(hsl.hue, hsl.saturation, it) }
            }
        },
        confirmButton = { Button(onClick = { onSelected(color) }) { Text(s.themeApply) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.themeCancel) } }
    )
}


@Composable
private fun HexColorInput(argb: Int?, onColorPicked: (Int) -> Unit) {
    val s = LocalStrings.current
    var hex by remember(argb) { mutableStateOf(argb?.let { Color(it).toArgb().toUInt().toString(16).padStart(8,'0').substring(2).uppercase() } ?: "") }
    val fm = LocalFocusManager.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(
            if (hex.length == 6) try { Color(("FF$hex").toLong(16).toInt()) } catch (_: Exception) { Color.Gray.copy(0.3f) } else Color.Gray.copy(0.3f)
        ).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)))
        OutlinedTextField(hex, { raw -> val c = raw.trimStart('#').uppercase().filter { it.isLetterOrDigit() }.take(6); hex = c; if (c.length == 6) try { onColorPicked(Color(("FF$c").toLong(16).toInt()).toArgb()) } catch (_: Exception) {} },
            label = { Text(s.themeHexColor) }, prefix = { Text("#") }, placeholder = { Text("7C4DFF") }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }),
            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            trailingIcon = if (hex.length == 6) {{ Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }} else null)
    }
}

@Composable
private fun ChatColorPresets(onPick: (Int) -> Unit) {
    val presets = listOf("Black" to Color(0xFF0A0A0F), "Navy" to Color(0xFF0D1117), "Violet" to Color(0xFF12092A),
        "Slate" to Color(0xFF0F1621), "Teal" to Color(0xFF071A1A), "Ember" to Color(0xFF1A0A08),
        "White" to Color(0xFFFFFFFF), "Light" to Color(0xFFF2F2F8))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(presets) { (name, c) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(c).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable { onPick(c.toArgb()) })
                Spacer(Modifier.height(3.dp))
                Text(name, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}


@Composable
private fun SectionCard(title: String, subtitle: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp)) }
                }
                Column { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(14.dp)); content()
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, display: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ThinSlider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(display, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp), textAlign = TextAlign.End, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun HslRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ThinSlider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(display, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(42.dp), textAlign = TextAlign.End, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .shadow(3.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        },
        track = { sliderState ->
            val fraction = (sliderState.value - sliderState.valueRange.start) /
                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
        }
    )
}

@Composable
private fun PreviewChip(label: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Surface(color = bg, shape = RoundedCornerShape(10.dp), modifier = modifier.height(52.dp)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(s.themeFontPreview, style = MaterialTheme.typography.bodySmall, color = fg, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = fg.copy(0.7f))
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun EmptyThemesPlaceholder(onCreate: () -> Unit) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) }
        }
        Text(s.themeNoCustomThemes, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(s.themeUseAutoGenerator, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(onClick = onCreate) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(s.themeCreateTheme) }
    }
}

@Composable
private fun M3SegBtn(options: List<String>, selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, lbl ->
            val bg by animateColorAsState(if (selected == i) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, tween(200), label = "seg")
            Surface(onClick = { onSelected(i) }, color = bg, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text(lbl, style = MaterialTheme.typography.labelMedium, color = if (selected == i) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected == i) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)
            }
        }
    }
}


private fun Color.luminance() = 0.299f*red + 0.587f*green + 0.114f*blue

private data class HslColor(val hue: Float, val saturation: Float, val lightness: Float)
private fun Color.toHsl(): HslColor {
    val mx = maxOf(red,green,blue); val mn = minOf(red,green,blue); val l = (mx+mn)/2f
    if (mx == mn) return HslColor(0f,0f,l)
    val d = mx-mn; val s = if (l>0.5f) d/(2f-mx-mn) else d/(mx+mn)
    val h = when(mx) { red -> (green-blue)/d + if (green<blue) 6f else 0f; green -> (blue-red)/d+2f; else -> (red-green)/d+4f } / 6f * 360f
    return HslColor(h,s,l)
}

private fun adjStr(v: Float) = if (v >= 0f) "+${(v*100).toInt()}%" else "${(v*100).toInt()}%"
private fun genId() = "theme_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"