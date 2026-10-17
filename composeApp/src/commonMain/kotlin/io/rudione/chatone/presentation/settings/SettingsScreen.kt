package io.rudione.chatone.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.components.ChatoneScrollbar
import io.rudione.chatone.presentation.components.DismissOnOutsideClick
import io.rudione.chatone.presentation.components.expressive.ChatoneMark
import io.rudione.chatone.presentation.components.expressive.ExpressiveSearchField
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ScallopBadge
import io.rudione.chatone.presentation.components.expressive.ambientHaze
import io.rudione.chatone.presentation.components.rows.LocalSettingsSearch
import io.rudione.chatone.presentation.settings.components.SettingsPaneHeader
import io.rudione.chatone.presentation.screenchat.screenChatFloatsOverFullScreen
import io.rudione.chatone.presentation.settings.search.SettingsCatalogContext
import io.rudione.chatone.presentation.settings.search.SettingsSearchEmpty
import io.rudione.chatone.presentation.settings.search.SettingsSearchHit
import io.rudione.chatone.presentation.settings.search.SettingsSearchResultsPane
import io.rudione.chatone.presentation.settings.search.SettingsSearchState
import io.rudione.chatone.presentation.settings.search.SettingsSubsection
import io.rudione.chatone.presentation.settings.search.rememberSettingsCatalog
import io.rudione.chatone.presentation.settings.search.rememberSettingsSearchModel
import io.rudione.chatone.presentation.settings.search.settingsSubsections
import io.rudione.chatone.presentation.settings.search.reveal
import io.rudione.chatone.presentation.settings.search.settingsSearchKeys
import io.rudione.chatone.presentation.settings.theme_settings.ThemeSettingsScreen
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.window.windowDragArea
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.util.platform.rememberInputCapabilities
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    isWideScreen: Boolean = false,
    isDetached: Boolean = false,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
    wallpaperLoader: WallpaperLoader = koinInject(),
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {},
    onLogoutSuccess: (() -> Unit)? = null,
    isPinned: Boolean? = null,
    onTogglePin: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val customThemeManager: CustomThemeManager = LocalCustomThemeManager.current
    val wallpaperController = LocalWallpaperController.current
    val s = LocalStrings.current
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is SettingsEffect.NavigateToAuth) {
                onNavigateBack()
            }
        }
    }

    if (isWideScreen && embedded) {
        SettingsDialogContent(
            state = state,
            onNavigateBack = {
                onNavigateBack()
                if (state.showThemeCreator) {
                    viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                }
            },
            viewModel = viewModel,
            isDetached = true,
            isPinned = isPinned,
            onTogglePin = onTogglePin,
            onOpenThemeCreator = { seedColor ->
                viewModel.sendEvent(SettingsEvent.OnOpenThemeCreator(seedColor))
            }
        )
    } else if (isWideScreen) {
        val dismissSettings = {
            onNavigateBack()
            if (state.showThemeCreator) {
                viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
            }
        }
        Dialog(
            onDismissRequest = dismissSettings,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            DismissOnOutsideClick(onDismiss = dismissSettings) {
                SettingsDialogContent(
                    state = state,
                    onNavigateBack = dismissSettings,
                    viewModel = viewModel,
                    isDetached = isDetached,
                    onOpenThemeCreator = { seedColor ->
                        viewModel.sendEvent(SettingsEvent.OnOpenThemeCreator(seedColor))
                    }
                )
            }
        }
    } else {
        SettingsCompactScreen(
            state = state,
            onNavigateBack = {
                onNavigateBack()
                if (state.showThemeCreator) {
                    viewModel.sendEvent(SettingsEvent.OnCloseThemeCreator)
                }
            },
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
                dismissOnClickOutside = false,
            )
        ) {
            CompositionLocalProvider(
                LocalWallpaperController provides wallpaperController,
                LocalCustomThemeManager provides customThemeManager,
                LocalStrings provides s
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
                        initialSeedColor = state.themeCreatorSeedColor,
                        initialSection = state.themeCreatorSection
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDialogContent(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    isDetached: Boolean = false,
    isPinned: Boolean? = null,
    onTogglePin: () -> Unit = {},
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    val extra = ChatoneTheme.extraColors
    val page = remember { SettingsPageState() }
    val search = remember { SettingsSearchState() }
    var selectedSection by remember { mutableStateOf(SettingsSection.APPEARANCE) }
    val scope = rememberCoroutineScope()
    val searchFocus = remember { FocusRequester() }
    val input = rememberInputCapabilities()
    val context = remember(input) {
        SettingsCatalogContext(
            input,
            desktop = isDesktopPlatform,
            phone = currentFormFactor() == DeviceFormFactor.PHONE,
            macOs = screenChatFloatsOverFullScreen
        )
    }
    val catalog = rememberSettingsCatalog(s, context)
    val model = rememberSettingsSearchModel(search.query, s, catalog)
    val subsections = remember(catalog, s) { settingsSubsections(catalog, s) }
    var activeSubsection by remember { mutableStateOf<SettingsSubsection?>(null) }
    val available = remember(input) { SettingsSection.entries.filter { it.isAvailable(input) } }
    val sidebarSections = if (search.isSearching) {
        available.filter(model::shows)
    } else available

    val settingsNavigator: SettingsNavigator = koinInject()
    val pendingDestination by settingsNavigator.pending.collectAsState()
    SideEffect(pendingDestination) {
        pendingDestination?.let { destination ->
            search.clear()
            selectedSection = destination.section()
            activeSubsection = null
            destination.moderationTab()?.let { page.moderationTab = it }
        }
    }

    val openHit: (SettingsSearchHit) -> Unit = { hit ->
        selectedSection = hit.entry.section
        activeSubsection = null
        search.showResults = false
        scope.launch { page.reveal(hit.entry, settleMs = 0) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(if (isDetached) 1f else 0.88f)
            .fillMaxHeight(if (isDetached) 1f else 0.86f)
            .onPreviewKeyEvent { event ->
                val find = event.type == KeyEventType.KeyDown && event.key == Key.F &&
                    (event.isCtrlPressed || event.isMetaPressed)
                if (find) runCatching { searchFocus.requestFocus() }
                find
            },
        shape = if (isDetached) RectangleShape else RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 32.dp
    ) {
        CompositionLocalProvider(LocalSettingsPage provides page) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .ambientHaze(base = MaterialTheme.colorScheme.surface, strength = DESKTOP_HAZE, animated = false)
            ) {
                Column(
                    modifier = Modifier
                        .width(SIDEBAR_WIDTH)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f))
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                color = extra.cardBorder,
                                topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                                size = Size(1.dp.toPx(), size.height)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowDragArea()
                            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScallopBadge(
                            icon = ChatoneMark,
                            size = 32.dp,
                            animated = false,
                            iconFraction = 0.5f,
                            content = Color.Unspecified
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            s.settingsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    ExpressiveSearchField(
                        value = search.query,
                        onValueChange = search::change,
                        placeholder = s.settingsSearchPlaceholder,
                        clearLabel = s.clear,
                        height = 40.dp,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .focusRequester(searchFocus)
                            .settingsSearchKeys(search, { model.flat.size }) {
                                model.flat.getOrNull(search.activeIndex)?.let(openHit)
                            }
                    )
                    if (search.isSearching && model.flat.isNotEmpty()) {
                        Text(
                            s.settingsHub.searchKeyboardHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 18.dp, top = 6.dp)
                        )
                    }

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
                            if (sidebarSections.isEmpty()) {
                                Text(
                                    s.settingsSearchNoResults,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                                )
                            }
                            CompositionLocalProvider(LocalSettingsSearch provides search.query) {
                                sidebarSections.forEach { section ->
                                    SidebarNavItem(
                                        section = section,
                                        isSelected = selectedSection == section && !(search.isSearching && search.showResults),
                                        count = if (search.isSearching) model.countBySection[section] ?: 0 else 0,
                                        onClick = {
                                            selectedSection = section
                                            activeSubsection = null
                                            search.showResults = false
                                        }
                                    )
                                    SidebarSubsections(
                                        visible = !search.isSearching && selectedSection == section,
                                        items = subsections[section].orEmpty(),
                                        active = activeSubsection,
                                        onOpen = { subsection ->
                                            activeSubsection = subsection
                                            scope.launch { page.reveal(subsection) }
                                        }
                                    )
                                }
                            }
                        }
                        ChatoneScrollbar(
                            scrollState = navScrollState,
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp)
                        )
                    }

                    PillButton(
                        text = s.close,
                        onClick = onNavigateBack,
                        icon = Lucide.ArrowLeft,
                        tone = PillTone.Ghost,
                        height = 38.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (isDetached) {
                        SettingsPaneHeader(isPinned = isPinned, onTogglePin = onTogglePin, onClose = onNavigateBack)
                    }
                    CompositionLocalProvider(LocalSettingsSearch provides search.query) {
                        if (search.isSearching && search.showResults) {
                            if (model.flat.isEmpty()) {
                                SettingsSearchEmpty(modifier = Modifier.padding(top = 40.dp))
                            } else {
                                SettingsSearchResultsPane(
                                    groups = model.groups,
                                    activeIndex = search.activeIndex,
                                    onOpen = openHit,
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            }
                        } else {
                            SectionPane(
                                section = selectedSection,
                                state = state,
                                viewModel = viewModel,
                                onOpenThemeCreator = onOpenThemeCreator,
                                resultCount = if (search.isSearching) model.flat.size else null,
                                onBackToResults = { search.showResults = true },
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private val SIDEBAR_WIDTH = 224.dp
private const val DESKTOP_HAZE = 0.45f
