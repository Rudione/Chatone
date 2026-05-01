package io.rudione.chatone.presentation.settings

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import io.rudione.chatone.presentation.settings.components.ModerationSettingsSection
import io.rudione.chatone.presentation.settings.theme_settings.ThemeSettingsScreen
import io.rudione.chatone.presentation.settings.theme_settings.ThinSlider
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.ExpressivePalettes
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.util.BuildConfig
import io.rudione.chatone.util.NotificationSoundPlayer
import io.rudione.chatone.util.WallpaperLoader
import io.rudione.chatone.presentation.theme.i18n.AppLocale
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.pickAudioFile
import io.rudione.chatone.util.pickImageFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private enum class SettingsSection(
    val label: String,
    val icon: DrawableResource,
    val iconOutlined: DrawableResource? = null
) {
    APPEARANCE("Appearance", Res.drawable.palette_fill_16, Res.drawable.palette_stroke_12),
    CHAT("Chat", Res.drawable.chatbubbles, Res.drawable.chatbubbles_outline),
    NOTIFICATIONS("Notifications", Res.drawable.bell_filled, Res.drawable.bell_outlined),
    HIGHLIGHTS("Highlights", Res.drawable.star_filled, Res.drawable.star_outlined),
    BACKGROUND("Background", Res.drawable.wallpaper_filled, Res.drawable.wallpaper_outlined),
    HOTKEYS("Hotkeys", Res.drawable.keyboard_24_filled, Res.drawable.keyboard_24_regular),
    MODERATION("Moderation", Res.drawable.shield_filled, Res.drawable.shield_outlined),
    ACCOUNT("Account", Res.drawable.person_filled, Res.drawable.person_filled),
    ABOUT("About", Res.drawable.panel_left_key_16_regular, null);

    fun localizedLabel(s: AppStrings): String = when (this) {
        APPEARANCE -> s.settingsAppearance
        CHAT -> s.sectionChat
        NOTIFICATIONS -> s.settingsNotifications
        HIGHLIGHTS -> s.sectionHighlights
        BACKGROUND -> s.sectionBackground
        HOTKEYS -> s.sectionHotkeys
        MODERATION -> s.settingsModeration
        ACCOUNT -> s.settingsAccount
        ABOUT -> s.settingsAbout
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    isWideScreen: Boolean = false,
    modifier: Modifier = Modifier,
    wallpaperLoader: WallpaperLoader = koinInject(),
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {},
    onLogoutSuccess: (() -> Unit)? = null,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val customThemeManager: CustomThemeManager = LocalCustomThemeManager.current
    val s = LocalStrings.current
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is SettingsEffect.NavigateToAuth) {
                onNavigateBack()
            }
        }
    }

    if (isWideScreen) {
        Dialog(
            onDismissRequest = {
                onNavigateBack()
                if (state.showThemeCreator) {
                    viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            SettingsDialogContent(
                state = state,
                onNavigateBack = {
                    onNavigateBack()
                    if (state.showThemeCreator) {
                        viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                    }
                },
                onThemeChanged = onThemeChanged,
                viewModel = viewModel,
                onOpenThemeCreator = { seedColor ->
                    viewModel.sendEvent(SettingsEvent.OnOpenThemeCreator(seedColor))
                }
            )
        }
    } else {
        SettingsFullScreen(
            state = state,
            onNavigateBack = {
                onNavigateBack()
                if (state.showThemeCreator) {
                    viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                }
            },
            onThemeChanged = onThemeChanged,
            modifier = modifier,
            viewModel = viewModel,
            onOpenThemeCreator = { seedColor ->
                viewModel.sendEvent(SettingsEvent.OnOpenThemeCreator(seedColor))
            }
        )
    }

    if (state.showThemeCreator) {
        Dialog(
            onDismissRequest = {
                viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 16.dp,
                shadowElevation = 32.dp
            ) {
                ThemeSettingsScreen(
                    onNavigateBack = {
                        viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                    },
                    onThemeApplied = {
                        val active = customThemeManager.currentTheme.value
                        viewModel.sendEvent(SettingsEvent.OnCustomThemeApplied(active))
                    },
                    settingsViewModel = viewModel,
                    customThemeManager = customThemeManager,
                    initialSeedColor = state.themeCreatorSeedColor
                )
            }
        }
    }
}

@Composable
private fun SettingsDialogContent(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    var selectedSection by remember { mutableStateOf(SettingsSection.APPEARANCE) }
    val extra = ChatoneTheme.extraColors

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .fillMaxHeight(0.86f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 32.dp
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(216.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(extra.sidebarSurface, extra.sidebarSurface.copy(alpha = 0.95f))
                        )
                    )
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = extra.cardBorder,
                            topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                            size = Size(1.dp.toPx(), size.height)
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        s.settingsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Chatone",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = extra.cardBorder)
                Spacer(Modifier.height(8.dp))

                val navScrollState = rememberScrollState()
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(navScrollState)
                            .padding(horizontal = 8.dp)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SettingsSection.entries.forEach { section ->
                            SidebarNavItem(
                                section = section,
                                isSelected = selectedSection == section,
                                onClick = { selectedSection = section }
                            )
                        }
                    }
                    SettingsThinScrollbarScroll(
                        scrollState = navScrollState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = extra.cardBorder)
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.close)
                }
            }

            SectionContentLazy(
                section = selectedSection,
                state = state,
                onThemeChanged = onThemeChanged,
                viewModel = viewModel,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onOpenThemeCreator = onOpenThemeCreator
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsFullScreen(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    var expandedSections by remember { mutableStateOf(setOf<SettingsSection>()) }
    val extra = ChatoneTheme.extraColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->

        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 10.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                extra.sidebarSurface,
                                extra.sidebarSurface.copy(alpha = 0.95f)
                            )
                        )
                    )
            ) {
                SettingsSection.entries.forEach { section ->
                    val isExpanded = section in expandedSections

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedSections = if (isExpanded)
                                    expandedSections - section
                                else
                                    expandedSections + section
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(
                                if (isExpanded) section.icon
                                else (section.iconOutlined ?: section.icon)
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isExpanded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            section.localizedLabel(s),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isExpanded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (isExpanded) Icons.Filled.KeyboardArrowDown
                            else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = if (isExpanded) 0f else -90f },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(tween(200)) + fadeIn(tween(150)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                    ) {
                        SectionContentColumn(
                            section = section,
                            state = state,
                            onThemeChanged = onThemeChanged,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth(),
                            onOpenThemeCreator = onOpenThemeCreator
                        )
                    }

                    if (isExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            SettingsThinScrollbarScroll(
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp)
            )
        }
    }
}

@Composable
private fun SidebarNavItem(section: SettingsSection, isSelected: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    val bg by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        tween(150), label = "nav_bg"
    )
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painterResource(
                if (isSelected) section.icon
                else (section.iconOutlined ?: section.icon)
            ),
            null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Text(
            section.localizedLabel(s),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                modifier = Modifier.size(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun SectionContentLazy(
    section: SettingsSection,
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 10.dp),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    section.localizedLabel(s),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
            }
            when (section) {
                SettingsSection.APPEARANCE -> appearanceLazyItems(
                    state,
                    onThemeChanged,
                    viewModel,
                    onOpenThemeCreator
                )
                SettingsSection.ACCOUNT -> accountLazyItems(viewModel)
                SettingsSection.CHAT -> chatLazyItems(state, viewModel)
                SettingsSection.NOTIFICATIONS -> notificationLazyItems(state, viewModel)
                SettingsSection.HIGHLIGHTS -> highlightLazyItems(state, viewModel)
                SettingsSection.BACKGROUND -> backgroundLazyItems(state, viewModel)
                SettingsSection.HOTKEYS -> hotkeyLazyItems(state, viewModel)
                SettingsSection.MODERATION -> moderationLazyItems(state, viewModel)
                SettingsSection.ABOUT -> aboutLazyItems()
            }
        }
        SettingsThinScrollbar(
            listState = listState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp),
            coroutineScope = coroutineScope
        )
    }
}

@Composable
private fun SectionContentColumn(
    section: SettingsSection,
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (section) {
                SettingsSection.APPEARANCE -> AppearanceContent(
                    state,
                    onThemeChanged,
                    viewModel,
                    onOpenThemeCreator
                )

                SettingsSection.CHAT -> ChatContent(state, viewModel)
                SettingsSection.NOTIFICATIONS -> NotificationContent(state, viewModel)
                SettingsSection.HIGHLIGHTS -> HighlightContent(state, viewModel)
                SettingsSection.BACKGROUND -> BackgroundContent(state, viewModel)
                SettingsSection.HOTKEYS -> HotkeyContent(state, viewModel)
                SettingsSection.MODERATION -> ModerationContent(state, viewModel)
                SettingsSection.ACCOUNT -> AccountContent(viewModel)
                SettingsSection.ABOUT -> AboutContent()
            }
        }
    }
}

private fun LazyListScope.appearanceLazyItems(
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    vm: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {},
) {
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsTheme) {
            RowDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.settingsAccentColor, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        ExpressivePalettes.getOrNull(state.accentColorIndex)?.name ?: "Violet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenThemeCreator(null) }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        s.settingsSetCustom,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painterResource(Res.drawable.palette_fill_16),
                        contentDescription = "Create custom theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                ExpressivePalettes.forEachIndexed { index, palette ->
                    val isSelected = index == state.accentColorIndex
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(palette.previewColor)
                            .border(
                                if (isSelected) 2.dp else 0.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                CircleShape
                            )
                            .clickable { vm.sendEvent(SettingsEvent.OnAccentColorChanged(index)) }
                    )
                }
            }
        }
    }

    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsDisplay) {
            ListRow(
                s.settingsFontSize, state.fontSize.name.lowercase().replaceFirstChar { it.uppercase() },
                SettingsState.FontSize.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
            UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
            RowDivider()
            ListRow(
                s.settingsEmoteSize, state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
                SettingsState.EmoteSize.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
            RowDivider()
            ListRow(
                s.settingsChannelNavigation,
                when (state.channelNavigation) {
                    SettingsState.ChannelNavigation.TAB_BAR -> s.settingsTabBar
                    SettingsState.ChannelNavigation.MINI_RAIL -> s.settingsMiniRail
                    SettingsState.ChannelNavigation.BOTH -> s.settingsBoth
                },
                listOf(s.settingsTabBar, s.settingsMiniRail, s.settingsBoth)
            ) { vm.sendEvent(SettingsEvent.OnChannelNavigationChanged(SettingsState.ChannelNavigation.entries[it])) }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsLinks) {
            ListRow(
                s.settingsOpenLinks,
                when (state.linkOpenMode) {
                    SettingsState.LinkOpenMode.DEFAULT -> s.settingsDefaultBrowser
                    SettingsState.LinkOpenMode.INCOGNITO -> s.settingsIncognitoMode
                },
                listOf(s.settingsDefaultBrowser, s.settingsIncognitoMode)
            ) {
                vm.sendEvent(SettingsEvent.OnLinkOpenModeChanged(SettingsState.LinkOpenMode.entries[it]))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsWindow) {
            SwitchRow(s.settingsAlwaysOnTop, s.settingsAlwaysOnTopDesc, state.alwaysOnTop) {
                vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
            }
            RowDivider()
            run {
                val locales = AppLocale.all
                val selectedIdx = locales.indexOfFirst { it.code == state.language }.coerceAtLeast(0)
                DropdownRow(
                    label = s.settingsLanguage,
                    description = s.settingsLanguageDesc,
                    options = locales.map { it.displayName },
                    selected = selectedIdx,
                    onSelected = { vm.sendEvent(SettingsEvent.OnLanguageChanged(locales[it].code)) }
                )
            }
            RowDivider()
            SwitchRow(
                s.settingsBlockScroll,
                s.settingsBlockScrollDesc,
                state.disableScrollOnAlt
            ) {
                vm.sendEvent(SettingsEvent.OnDisableScrollOnAltChanged(it))
            }
        }
    }
}

private fun LazyListScope.chatLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsMessages) {
            SwitchRow(
                s.settingsTimestamps, s.settingsShowTimestampsDesc,
                state.timestampFormat != SettingsState.TimestampFormat.OFF
            ) { enabled ->
                vm.sendEvent(
                    SettingsEvent.OnTimestampFormatChanged(
                        if (enabled) SettingsState.TimestampFormat.H24 else SettingsState.TimestampFormat.OFF
                    )
                )
            }
            if (state.timestampFormat != SettingsState.TimestampFormat.OFF) {
                RowDivider()
                ListRow(
                    s.settingsTimestampFormat,
                    when (state.timestampFormat) {
                        SettingsState.TimestampFormat.H12 -> s.settingsTimestamp12h
                        SettingsState.TimestampFormat.H24 -> s.settingsTimestamp24h
                        else -> s.settingsTimestampOff
                    },
                    listOf(s.settingsTimestamp12h, s.settingsTimestamp24h)
                ) {
                    vm.sendEvent(
                        SettingsEvent.OnTimestampFormatChanged(
                            if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
                        )
                    )
                }
            }
            RowDivider()
            SwitchRow(
                s.settingsShowChatHeader,
                s.settingsShowChatHeaderDesc,
                state.showChatHeader
            ) {
                vm.sendEvent(SettingsEvent.OnShowChatHeaderChanged(it))
            }
            RowDivider()
            SwitchRow(s.chatShowBadges, s.chatShowBadgesDesc, state.showBadges) {
                vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
            }
            RowDivider()
            SwitchRow(
                s.settingsShowDeletedMessages,
                s.settingsShowDeletedMessagesDesc,
                state.showDeletedMessages
            ) {
                vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
            }
            RowDivider()
            SwitchRow(
                s.settingsSmoothChat,
                s.settingsSmoothChatDesc,
                state.smoothChatEnabled
            ) {
                vm.sendEvent(SettingsEvent.OnSmoothChatEnabledChanged(it))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsAutoScroll) {
            SwitchRow(
                s.settingsPauseOnHover,
                s.settingsPauseOnHoverDesc,
                state.pauseOnHover
            ) {
                vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsEmotePicker) {
            SwitchRow(
                s.settingsCloseOnMouseLeave,
                s.settingsCloseOnMouseLeaveDesc,
                state.closeEmotePickerOnMouseLeave
            ) {
                vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsHistoryGroup) {
            SliderRow(
                s.settingsMessageHistoryLimit, state.scrollbackLimit, 100f..2000f, 18,
                s.settingsMessagesUnit.replace("{0}", state.scrollbackLimit.toString())
            ) {
                vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt()))
            }
        }
    }
}

private fun LazyListScope.notificationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { NotificationGroupCard(state, vm) }
    if (state.mentionSoundEnabled) {
        item { CustomSoundCard(state, vm) }
    }
}

private fun LazyListScope.highlightLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        val s = LocalStrings.current
        Text(
            s.settingsHighlightRulesHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
    items(state.highlightRules, key = { it.id }) { rule ->
        HightlightRuleCard(
            rule = rule,
            onToggle = { vm.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, it)) },
            onSoundToggle = { vm.sendEvent(SettingsEvent.OnHighlightRuleSoundToggled(rule.id, it)) },
            onColorChange = { color -> vm.sendEvent(SettingsEvent.OnHighlightRuleColorChanged(rule.id, color)) },
            onRemove = if (!rule.id.startsWith("custom_")) null else {
                { vm.sendEvent(SettingsEvent.OnRemoveHighlightRule(rule.id)) }
            }
        )
        Spacer(Modifier.height(4.dp))
    }
    item {
        var newPattern by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    val sp = LocalStrings.current
                    Text(sp.settingsAddHighlightPattern)
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            FilledTonalButton(
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

private fun LazyListScope.backgroundLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { BackgroundCard(state, vm) }
}

private fun LazyListScope.hotkeyLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsChatControls) {
            HotkeyRow(s.settingsPauseAutoScroll, s.settingsPauseAutoScrollDesc, state.pauseHotkey) {
                vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it))
            }
            DropdownRow(
                label = s.settingsPauseMode,
                description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) s.settingsPauseModeHoldDesc else s.settingsPauseModeToggleDesc,
                options = PauseHotkeyMode.entries.map { it.name },
                selected = state.pauseHotkeyMode.ordinal
            ) { idx ->
                vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx]))
            }
        }
    }
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsImageLinks) {
            DropdownRow(
                label = s.settingsShowInlineImages,
                description = s.settingsShowInlineImagesDesc,
                options = listOf(s.on, s.off, s.blur),
                selected = state.showInlineImages.ordinal
            ) { idx ->
                vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx]))
            }
            if (state.showInlineImages != InlineImageMode.OFF) {
                SliderRow(
                    label = s.settingsImageMaxHeight,
                    value = state.inlineImageMaxHeight.toFloat(),
                    valueRange = 50f..500f,
                    steps = 8,
                    valueLabel = s.settingsImageMaxHeightUnit.replace("{0}", state.inlineImageMaxHeight.toString())
                ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
            }
        }
    }
}

private fun LazyListScope.moderationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        ModerationSettingsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
}

private fun LazyListScope.aboutLazyItems() {
    item { AboutCard() }
}

private fun LazyListScope.accountLazyItems(vm: SettingsViewModel) {
    item {
        val s = LocalStrings.current
        SettingsGroup(s.settingsAccount) {
            AccountSectionBody(
                onLogout = { vm.sendEvent(SettingsEvent.OnLogoutClicked) },
                onClearCache = { vm.sendEvent(SettingsEvent.OnClearCacheClicked) }
            )
        }
    }
}

@Composable
private fun AccountSectionBody(
    onLogout: () -> Unit,
    onClearCache: () -> Unit
) {
    val s = LocalStrings.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            s.settingsLogoutDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                Icons.Default.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(s.settingsLogout)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            s.settingsClearCacheDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(s.settingsClearCache)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(s.settingsClearCache) },
            text = { Text(s.settingsConfirmClearCache) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearCache()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(s.settingsClearCache) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(s.settingsCancel)
                }
            }
        )
    }
}

@Composable
private fun AccountContent(viewModel: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsAccount) {
        AccountSectionBody(
            onLogout = { viewModel.sendEvent(SettingsEvent.OnLogoutClicked) },
            onClearCache = { viewModel.sendEvent(SettingsEvent.OnClearCacheClicked) }
        )
    }
}

@Composable
private fun AppearanceContent(
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    vm: SettingsViewModel,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsTheme) {
        RowDivider()
        AccentColorPaletteRow(
            selectedIndex = state.accentColorIndex,
            onSelect = { vm.sendEvent(SettingsEvent.OnAccentColorChanged(it)) },
            state = state,
            onOpenThemeCreator = onOpenThemeCreator,
            onReset = {
                vm.sendEvent(SettingsEvent.OnAccentColorChanged(0))
                vm.sendEvent(SettingsEvent.OnCustomThemeApplied(null))
            }
        )
    }

    SettingsGroup(s.settingsDisplay) {
        ListRow(
            s.settingsFontSize, state.fontSize.name.lowercase().replaceFirstChar { it.uppercase() },
            SettingsState.FontSize.entries.map {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        ) { vm.sendEvent(SettingsEvent.OnFontSizeChanged(SettingsState.FontSize.entries[it])) }
        UiScaleRow(state.uiScale) { vm.sendEvent(SettingsEvent.OnUiScaleChanged(it)) }
        RowDivider()
        ListRow(
            s.settingsEmoteSize, state.emoteSize.name.lowercase().replaceFirstChar { it.uppercase() },
            SettingsState.EmoteSize.entries.map {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        ) { vm.sendEvent(SettingsEvent.OnEmoteSizeChanged(SettingsState.EmoteSize.entries[it])) }
        RowDivider()
        ListRow(
            s.settingsChannelNavigation,
            when (state.channelNavigation) {
                SettingsState.ChannelNavigation.TAB_BAR -> s.settingsTabBar
                SettingsState.ChannelNavigation.MINI_RAIL -> s.settingsMiniRail
                SettingsState.ChannelNavigation.BOTH -> s.settingsBoth
            },
            listOf(s.settingsTabBar, s.settingsMiniRail, s.settingsBoth)
        ) { vm.sendEvent(SettingsEvent.OnChannelNavigationChanged(SettingsState.ChannelNavigation.entries[it])) }
    }
    SettingsGroup(s.settingsLinks) {
        ListRow(
            s.settingsOpenLinks,
            when (state.linkOpenMode) {
                SettingsState.LinkOpenMode.DEFAULT -> s.settingsDefaultBrowser
                SettingsState.LinkOpenMode.INCOGNITO -> s.settingsIncognitoMode
            },
            listOf(s.settingsDefaultBrowser, s.settingsIncognitoMode)
        ) { vm.sendEvent(SettingsEvent.OnLinkOpenModeChanged(SettingsState.LinkOpenMode.entries[it])) }
    }
    SettingsGroup(s.settingsWindow) {
        SwitchRow(s.settingsAlwaysOnTop, s.settingsAlwaysOnTopDesc, state.alwaysOnTop) {
            vm.sendEvent(SettingsEvent.OnAlwaysOnTopChanged(it))
        }
        RowDivider()
        run {
            val locales = AppLocale.all
            val selectedIdx = locales.indexOfFirst { it.code == state.language }.coerceAtLeast(0)
            DropdownRow(
                label = s.settingsLanguage,
                description = s.settingsLanguageDesc,
                options = locales.map { it.displayName },
                selected = selectedIdx,
                onSelected = { vm.sendEvent(SettingsEvent.OnLanguageChanged(locales[it].code)) }
            )
        }
        RowDivider()
        SwitchRow(
            s.settingsBlockScroll,
            s.settingsBlockScrollDesc,
            state.disableScrollOnAlt
        ) {
            vm.sendEvent(SettingsEvent.OnDisableScrollOnAltChanged(it))
        }
    }
}

@Composable
private fun ChatContent(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsMessages) {
        SwitchRow(
            s.settingsTimestamps, s.settingsShowTimestampsDesc,
            state.timestampFormat != SettingsState.TimestampFormat.OFF
        ) { enabled ->
            vm.sendEvent(
                SettingsEvent.OnTimestampFormatChanged(
                    if (enabled) SettingsState.TimestampFormat.H24 else SettingsState.TimestampFormat.OFF
                )
            )
        }
        if (state.timestampFormat != SettingsState.TimestampFormat.OFF) {
            RowDivider()
            ListRow(
                s.settingsTimestampFormat,
                when (state.timestampFormat) {
                    SettingsState.TimestampFormat.H12 -> s.settingsTimestamp12h
                    SettingsState.TimestampFormat.H24 -> s.settingsTimestamp24h
                    else -> s.settingsTimestampOff
                },
                listOf(s.settingsTimestamp12h, s.settingsTimestamp24h)
            ) {
                vm.sendEvent(
                    SettingsEvent.OnTimestampFormatChanged(
                        if (it == 0) SettingsState.TimestampFormat.H12 else SettingsState.TimestampFormat.H24
                    )
                )
            }
        }
        RowDivider()
        SwitchRow(
            s.settingsShowChatHeader,
            s.settingsShowChatHeaderDesc,
            state.showChatHeader
        ) {
            vm.sendEvent(SettingsEvent.OnShowChatHeaderChanged(it))
        }
        RowDivider()
        SwitchRow(s.chatShowBadges, s.chatShowBadgesDesc, state.showBadges) {
            vm.sendEvent(SettingsEvent.OnShowBadgesChanged(it))
        }
        RowDivider()
        SwitchRow(
            s.settingsShowDeletedMessages,
            s.settingsShowDeletedMessagesDesc,
            state.showDeletedMessages
        ) {
            vm.sendEvent(SettingsEvent.OnShowDeletedChanged(it))
        }
        RowDivider()
        SwitchRow(
            s.settingsSmoothChat,
            s.settingsSmoothChatDesc,
            state.smoothChatEnabled
        ) {
            vm.sendEvent(SettingsEvent.OnSmoothChatEnabledChanged(it))
        }
    }
    SettingsGroup(s.settingsAutoScroll) {
        SwitchRow(
            s.settingsPauseOnHover,
            s.settingsPauseOnHoverDesc,
            state.pauseOnHover
        ) {
            vm.sendEvent(SettingsEvent.OnPauseOnHoverChanged(it))
        }
    }
    SettingsGroup(s.settingsEmotePicker) {
        SwitchRow(
            s.settingsCloseOnMouseLeave,
            s.settingsCloseOnMouseLeaveDesc,
            state.closeEmotePickerOnMouseLeave
        ) {
            vm.sendEvent(SettingsEvent.OnCloseEmotePickerOnMouseLeaveChanged(it))
        }
    }
    SettingsGroup(s.settingsHistoryGroup) {
        SliderRow(
            s.settingsMessageHistoryLimit, state.scrollbackLimit, 100f..2000f, 18,
            s.settingsMessagesUnit.replace("{0}", state.scrollbackLimit.toString())
        ) {
            vm.sendEvent(SettingsEvent.OnScrollbackLimitChanged(it.toInt()))
        }
    }
}

@Composable
private fun NotificationContent(state: SettingsState, vm: SettingsViewModel) {
    NotificationGroupCard(state, vm)
    if (state.mentionSoundEnabled) {
        Spacer(Modifier.height(8.dp))
        CustomSoundCard(state, vm)
    }
}

@Composable
private fun HighlightContent(state: SettingsState, vm: SettingsViewModel) {
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
            state.highlightRules.forEach { rule ->
                HightlightRuleCard(
                    rule = rule,
                    onToggle = { vm.sendEvent(SettingsEvent.OnHighlightRuleToggled(rule.id, it)) },
                    onSoundToggle = {
                        vm.sendEvent(SettingsEvent.OnHighlightRuleSoundToggled(rule.id, it))
                    },
                    onColorChange = { color ->
                        vm.sendEvent(SettingsEvent.OnHighlightRuleColorChanged(rule.id, color))
                    },
                    onRemove = if (!rule.id.startsWith("custom_")) null else {
                        { vm.sendEvent(SettingsEvent.OnRemoveHighlightRule(rule.id)) }
                    }
                )
            }
        }
    }
    SettingsGroup(s.settingsAddRule) {
        var newPattern by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(s.settingsAddHighlightPattern) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            FilledTonalButton(
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
private fun BackgroundContent(state: SettingsState, vm: SettingsViewModel) {
    BackgroundCard(state, vm)
}

@Composable
private fun HotkeyContent(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsChatControls) {
        HotkeyRow(s.settingsPauseAutoScroll, s.settingsPauseAutoScrollDesc, state.pauseHotkey) {
            vm.sendEvent(SettingsEvent.OnPauseHotkeyChanged(it))
        }
        DropdownRow(
            label = s.settingsPauseMode,
            description = if (state.pauseHotkeyMode == PauseHotkeyMode.HOLD) s.settingsPauseModeHoldDesc else s.settingsPauseModeToggleDesc,
            options = PauseHotkeyMode.entries.map { it.name },
            selected = state.pauseHotkeyMode.ordinal
        ) { idx ->
            vm.sendEvent(SettingsEvent.OnPauseHotkeyModeChanged(PauseHotkeyMode.entries[idx]))
        }
    }
    SettingsGroup(s.settingsImageLinks) {
        DropdownRow(
            label = s.settingsShowInlineImages,
            description = s.settingsShowInlineImagesDesc,
            options = listOf(s.on, s.off, s.blur),
            selected = state.showInlineImages.ordinal
        ) { idx ->
            vm.sendEvent(SettingsEvent.OnShowInlineImagesChanged(InlineImageMode.entries[idx]))
        }
        if (state.showInlineImages != InlineImageMode.OFF) {
            SliderRow(
                label = s.settingsImageMaxHeight,
                value = state.inlineImageMaxHeight.toFloat(),
                valueRange = 50f..500f,
                steps = 8,
                valueLabel = s.settingsImageMaxHeightUnit.replace("{0}", state.inlineImageMaxHeight.toString())
            ) { vm.sendEvent(SettingsEvent.OnInlineImageMaxHeightChanged(it.toInt())) }
        }
    }
}

@Composable
private fun ModerationContent(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsModeration) {
        Column(modifier = Modifier.padding(12.dp)) {
            ModerationSettingsSection(
                state = state,
                onEvent = { vm.sendEvent(it) }
            )
        }
    }
}

@Composable
private fun AboutContent() {
    AboutCard()
}


@Composable
private fun NotificationGroupCard(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(16.dp),
        backgroundAlphaHigh = 0.80f,
        backgroundAlphaLow = 0.65f,
        borderAlphaHigh = 0f,
        borderAlphaLow = 0f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                s.settingsMentionSound,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.settingsEnableMentionSound, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        s.settingsEnableMentionSoundDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
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
                            s.settingsVolume,
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
private fun CustomSoundCard(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(16.dp),
        backgroundAlphaHigh = 0.80f,
        backgroundAlphaLow = 0.65f,
        borderAlphaHigh = 0f,
        borderAlphaLow = 0f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                s.settingsCustomSound,
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
                        IconButton(onClick = {
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
                        val picked = pickAudioFile(); if (picked != null) vm.sendEvent(
                        SettingsEvent.OnCustomMentionSoundPathChanged(picked)
                    )
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        if (state.customMentionSoundPath.isBlank()) s.settingsBrowseSound else s.settingsChangeSound,
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
                text = if (state.customMentionSoundPath.isBlank()) s.settingsSoundDefault else s.settingsSoundSupported,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun BackgroundCard(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsChatBackgroundImage) {
        Column(modifier = Modifier.padding(16.dp)) {
            val scope = rememberCoroutineScope()
            if (state.wallpaperPath.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = state.wallpaperPath, contentDescription = s.settingsBackgroundPreview,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = state.wallpaperBlur / 40f))
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
                        IconButton(
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
                Text(s.settingsOverlayOpacity, style = MaterialTheme.typography.bodyMedium)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "${state.wallpaperBlur.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            ThinSlider(
                value = state.wallpaperBlur,
                onValueChange = { vm.sendEvent(SettingsEvent.OnWallpaperBlurChanged(it)) },
                valueRange = 0f..40f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    s.settingsTransparent,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.settingsOpaque,
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
                Text(if (state.wallpaperPath.isBlank()) s.settingsChooseBackgroundImage else s.settingsChangeImage)
            }
            if (state.wallpaperPath.isBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    s.settingsSupportsImageFormats,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val s = LocalStrings.current
    SettingsGroup(s.settingsAppInfo) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = s.appName,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
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
                "https://t.me/rudionee",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.60f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Column(content = content)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AccentColorPaletteRow(
    selectedIndex: Int,
    state: SettingsState,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {},
    onReset: () -> Unit = {},
    onSelect: (Int) -> Unit
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(s.themeAccentColor, style = MaterialTheme.typography.bodyMedium)
                Text(
                    ExpressivePalettes.getOrNull(state.accentColorIndex)?.name ?: "Violet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
               
                val customThemeManager = LocalCustomThemeManager.current
                val activeTheme by customThemeManager.currentTheme.collectAsState()
                if (activeTheme != null || state.accentColorIndex != 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReset() }
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset theme",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenThemeCreator(null) }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Set custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painterResource(Res.drawable.palette_fill_16),
                        contentDescription = "Create custom theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ExpressivePalettes.forEachIndexed { index, palette ->
                val isSelected = index == selectedIndex
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .clip(CircleShape)
                        .background(palette.previewColor)
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ListRow(
    title: String,
    value: String,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painterResource(Res.drawable.unfold_more),
                null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelected(i); expanded = false })
            }
        }
    }
}

@Composable
private fun DropdownRow(
    label: String,
    description: String,
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                options.getOrElse(selected) { "" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelected(i); expanded = false })
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        ThinSlider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    isFloat: Boolean = false,
    onFloatChange: ((Float) -> Unit)? = null,
    onValueChange: ((Float) -> Unit)? = null
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (isFloat && onFloatChange != null) {
            Slider(
                value = value / 100f,
                onValueChange = onFloatChange,
                valueRange = valueRange,
                steps = steps
            )
        } else if (onValueChange != null) {
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
private fun HightlightRuleCard(
    rule: HighlightRule,
    onToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onColorChange: (Long) -> Unit = {},
    onRemove: (() -> Unit)?
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val ruleColor = Color(rule.color)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ruleColor.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, ruleColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
               
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(ruleColor)
                        .clickable { showColorPicker = true }
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rule.pattern.ifEmpty {
                            rule.id.replace("_", " ").replaceFirstChar { it.uppercase() }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (rule.isRegex) Text(
                        LocalStrings.current.settingsRegex,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
               
                IconButton(onClick = { onSoundToggle(!rule.playSound) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (rule.playSound) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (rule.playSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
               
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
               
                if (onRemove != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

   
    if (showColorPicker) {
        var pickedColor by remember { mutableStateOf(ruleColor) }
        val hue = remember(pickedColor) {
            val r = pickedColor.red; val g = pickedColor.green; val b = pickedColor.blue
            val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
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
                    Slider(
                        value = hue,
                        onValueChange = { h ->
                            pickedColor = Color.hsl(h, 0.85f, 0.55f)
                        },
                        valueRange = 0f..360f
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
            dismissButton = { TextButton(onClick = { showColorPicker = false }) { Text(sd.cancel) } }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HotkeyRow(
    title: String,
    subtitle: String,
    currentHotkey: String,
    onHotkeyChanged: (String) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isRecording) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.widthIn(min = 110.dp)
                    .onKeyEvent { event ->
                        if (!isRecording) return@onKeyEvent false
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent true

                        if (event.key in listOf(
                                Key.CtrlLeft,
                                Key.CtrlRight,
                                Key.MetaLeft,
                                Key.MetaRight
                            )
                        ) {
                            if (!event.isAltPressed && !event.isShiftPressed) {
                                onHotkeyChanged("ctrl"); isRecording = false; return@onKeyEvent true
                            }
                        }
                        if (event.key in listOf(Key.AltLeft, Key.AltRight)) {
                            val parts2 = mutableListOf<String>()
                            if (event.isCtrlPressed || event.isMetaPressed) parts2.add("ctrl")
                            parts2.add("alt")
                            if (event.isShiftPressed) parts2.add("shift")
                            onHotkeyChanged(parts2.joinToString("+")); isRecording =
                                false; return@onKeyEvent true
                        }
                        if (event.key in listOf(Key.ShiftLeft, Key.ShiftRight)) {
                            if (!event.isAltPressed && !(event.isCtrlPressed || event.isMetaPressed)) {
                                onHotkeyChanged("shift"); isRecording =
                                    false; return@onKeyEvent true
                            }
                        }
                        if (event.key == Key.Escape) {
                            onHotkeyChanged(""); isRecording = false; return@onKeyEvent true
                        }
                        val parts = mutableListOf<String>()
                        if (event.isCtrlPressed || event.isMetaPressed) parts.add("ctrl")
                        if (event.isAltPressed) parts.add("alt")
                        if (event.isShiftPressed) parts.add("shift")
                        val keyName = hotkeyKeyToName(event.key)
                        if (keyName.isNotEmpty()) parts.add(keyName)
                        onHotkeyChanged(parts.joinToString("+"))
                        isRecording = false; true
                    }
                    .clickable { isRecording = true }
            ) {
                val sh = LocalStrings.current
                Text(
                    when {
                        isRecording -> sh.settingsRecording; currentHotkey.isBlank() -> sh.settingsHotkeyNotSet; else -> currentHotkey.uppercase()
                        .replace("+", " + ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        isRecording -> MaterialTheme.colorScheme.onPrimaryContainer; currentHotkey.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.45f
                        ); else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            if (currentHotkey.isNotBlank() || isRecording) {
                IconButton(
                    onClick = { onHotkeyChanged(""); isRecording = false },
                    modifier = Modifier.size(28.dp)
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
    }
}

private fun hotkeyKeyToName(key: Key): String = when (key) {
    Key.Spacebar -> "space"; Key.Enter -> "enter"; Key.Tab -> "tab"
    Key.Backspace -> "backspace"; Key.Delete -> "delete"
    Key.MoveHome -> "home"; Key.MoveEnd -> "end"
    Key.PageUp -> "pageup"; Key.PageDown -> "pagedown"
    Key.DirectionUp -> "up"; Key.DirectionDown -> "down"
    Key.DirectionLeft -> "left"; Key.DirectionRight -> "right"
    Key.F1 -> "f1"; Key.F2 -> "f2"; Key.F3 -> "f3"; Key.F4 -> "f4"
    Key.F5 -> "f5"; Key.F6 -> "f6"; Key.F7 -> "f7"; Key.F8 -> "f8"
    Key.F9 -> "f9"; Key.F10 -> "f10"; Key.F11 -> "f11"; Key.F12 -> "f12"
    else -> {
        val code = key.keyCode
        when {
            code in 65L..90L -> ('A' + (code - 65).toInt()).lowercaseChar().toString()
            code in 48L..57L -> (code - 48).toString()
            else -> ""
        }
    }
}

@Composable
private fun UiScaleRow(currentScale: Float, onScaleChanged: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(LocalStrings.current.settingsUiScale, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${(currentScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Slider(
            value = currentScale,
            onValueChange = onScaleChanged,
            valueRange = 0.7f..2.0f,
            steps = 12,
            modifier = Modifier.width(180.dp)
        )
    }
}

private fun formatDuration(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds < 3600 -> "${seconds / 60} minute${if (seconds / 60 > 1) "s" else ""}"
    seconds < 86400 -> "${seconds / 3600} hour${if (seconds / 3600 > 1) "s" else ""}"
    else -> "${seconds / 86400} day${if (seconds / 86400 > 1) "s" else ""}"
}

@Composable
private fun SettingsThinScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) {
    val thumbFraction by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount.takeIf { it > 0 } ?: return@derivedStateOf 0f to 0f
            val visible = info.visibleItemsInfo.size.toFloat()
            val thumbSz = (visible / total).coerceIn(0.05f, 1f)
            val firstIdx = listState.firstVisibleItemIndex.toFloat()
            val firstOff = listState.firstVisibleItemScrollOffset
            val avgH = info.visibleItemsInfo.map { it.size }.average().takeIf { it > 0 } ?: 1.0
            val pos = firstIdx + (firstOff / avgH).toFloat()
            val maxScroll = (total - visible).coerceAtLeast(0f)
            val off = if (maxScroll > 0f)
                (pos / maxScroll) * (1f - thumbSz) else 1f - thumbSz
            off.coerceIn(0f, 1f - thumbSz) to thumbSz
        }
    }
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val frac = (offset.y / size.height).coerceIn(0f, 1f)
                val target = (frac * (listState.layoutInfo.totalItemsCount - 1))
                    .toInt().coerceAtLeast(0)
                coroutineScope.launch { listState.scrollToItem(target) }
            }
        }
    ) {
        drawRect(color = Color.White.copy(alpha = 0.05f))
        val (off, sz) = thumbFraction
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(1.dp.toPx(), off * size.height),
            size = Size(size.width - 2.dp.toPx(), (sz * size.height).coerceAtLeast(14.dp.toPx())),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}

@Composable
private fun SettingsThinScrollbarScroll(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(color = Color.White.copy(alpha = 0.05f))
        val maxVal = scrollState.maxValue.toFloat().takeIf { it > 0f } ?: return@Canvas
        val viewFrac = size.height / (size.height + maxVal)
        val thumbH = (viewFrac * size.height).coerceAtLeast(14.dp.toPx())
        val thumbTop = (scrollState.value / maxVal) * (size.height - thumbH)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(1.dp.toPx(), thumbTop),
            size = Size(size.width - 2.dp.toPx(), thumbH),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}