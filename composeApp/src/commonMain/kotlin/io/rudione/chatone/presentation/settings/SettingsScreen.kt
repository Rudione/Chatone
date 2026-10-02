package io.rudione.chatone.presentation.settings

import io.rudione.chatone.presentation.components.expressive.ChatoneMark
import io.rudione.chatone.presentation.components.expressive.ExpressiveSearchField
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ScallopBadge
import io.rudione.chatone.presentation.components.expressive.ScallopShape
import io.rudione.chatone.presentation.components.expressive.ambientHaze
import io.rudione.chatone.presentation.components.rows.HighlightedSettingsText
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chatone.composeapp.generated.resources.icon
import io.rudione.chatone.presentation.components.ChatoneSlider
import io.rudione.chatone.presentation.components.DismissOnOutsideClick
import io.rudione.chatone.presentation.window.windowDragArea
import io.rudione.chatone.presentation.components.rows.LocalSettingsSearch
import io.rudione.chatone.presentation.components.rows.SwitchRow
import io.rudione.chatone.presentation.components.rows.ListRow
import io.rudione.chatone.presentation.settings.components.SettingsPair
import io.rudione.chatone.presentation.settings.components.ModerationSettingsSection
import io.rudione.chatone.presentation.settings.components.NotificationGroupCard
import io.rudione.chatone.presentation.settings.components.CustomSoundCard
import io.rudione.chatone.presentation.settings.components.MentionAlertsCard
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.presentation.settings.components.BackgroundCard
import io.rudione.chatone.presentation.settings.components.AboutCard
import io.rudione.chatone.presentation.settings.components.BackupCard
import io.rudione.chatone.presentation.settings.components.SettingsGroup
import io.rudione.chatone.presentation.settings.sections.appearanceLazyItems
import io.rudione.chatone.presentation.settings.sections.chatLazyItems
import io.rudione.chatone.presentation.settings.sections.highlightLazyItems
import io.rudione.chatone.presentation.settings.sections.hotkeyLazyItems
import io.rudione.chatone.presentation.settings.sections.AppearanceContent
import io.rudione.chatone.presentation.settings.sections.ChatContent
import io.rudione.chatone.presentation.settings.sections.HighlightContent
import io.rudione.chatone.presentation.settings.sections.HotkeyContent
import io.rudione.chatone.presentation.settings.theme_settings.ThemeSettingsScreen
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.CustomThemeManager
import io.rudione.chatone.presentation.theme.LocalCustomThemeManager
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import androidx.compose.runtime.CompositionLocalProvider
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.presentation.account.AccountActions
import io.rudione.chatone.presentation.account.AccountAddDialog
import io.rudione.chatone.presentation.account.AccountListLoader
import io.rudione.chatone.presentation.account.AccountsSettingsSectionCompact
import io.rudione.chatone.presentation.account.rememberAccountUiState
import io.rudione.chatone.presentation.chat.TranslationLanguages
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import io.rudione.chatone.presentation.components.ChatoneLazyScrollbar
import io.rudione.chatone.presentation.components.ChatoneScrollbar
import io.rudione.chatone.presentation.settings.components.ActionsSection
import io.rudione.chatone.presentation.settings.components.AiAssistantConfigCard
import io.rudione.chatone.presentation.settings.components.AiAutoModCard
import io.rudione.chatone.presentation.settings.components.BlockedUsersSection
import io.rudione.chatone.presentation.settings.components.ChatCommandsSection
import io.rudione.chatone.presentation.settings.components.FirstPartyTokenCard
import io.rudione.chatone.presentation.settings.components.SettingsPaneHeader
import io.rudione.chatone.presentation.settings.components.StreamerModeCard
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.LogOut
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Trash2

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
            onThemeChanged = onThemeChanged,
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
                    onThemeChanged = onThemeChanged,
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
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    isDetached: Boolean = false,
    isPinned: Boolean? = null,
    onTogglePin: () -> Unit = {},
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    val s = LocalStrings.current
    var selectedSection by remember { mutableStateOf(SettingsSection.APPEARANCE) }
    var searchQuery by remember { mutableStateOf("") }
    val extra = ChatoneTheme.extraColors
    val settingsNavigator: SettingsNavigator = koinInject()
    val pendingDestination by settingsNavigator.pending.collectAsState()
    LaunchedEffect(pendingDestination) {
        pendingDestination?.let { destination ->
            searchQuery = ""
            selectedSection = destination.section()
        }
    }

    val filteredSections = remember(searchQuery, s) { settingsSectionsMatching(searchQuery, s) }

    LaunchedEffect(filteredSections) {
        if (filteredSections.isNotEmpty() && selectedSection !in filteredSections) {
            selectedSection = filteredSections.first()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(if (isDetached) 1f else 0.88f)
            .fillMaxHeight(if (isDetached) 1f else 0.86f),
        shape = if (isDetached) RectangleShape else RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 32.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .ambientHaze(base = MaterialTheme.colorScheme.surface, strength = DESKTOP_HAZE, animated = false)
        ) {
            Column(
                modifier = Modifier
                    .width(224.dp)
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
                        .padding(start = 18.dp, end = 12.dp, top = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScallopBadge(
                        icon = ChatoneMark,
                        size = 34.dp,
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
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = s.settingsSearchPlaceholder,
                    clearLabel = s.clear,
                    height = 42.dp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Spacer(Modifier.height(10.dp))

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
                        if (filteredSections.isEmpty()) {
                            Text(
                                s.settingsSearchNoResults,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                        } else {
                            filteredSections.forEach { section ->
                                SidebarNavItem(
                                    section = section,
                                    isSelected = selectedSection == section,
                                    onClick = { selectedSection = section },
                                    highlightQuery = searchQuery
                                )
                            }
                        }
                    }
                    SettingsThinScrollbarScroll(
                        scrollState = navScrollState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(6.dp)
                    )
                }

                PillButton(
                    text = s.close,
                    onClick = onNavigateBack,
                    icon = Lucide.ArrowLeft,
                    tone = PillTone.Ghost,
                    height = 40.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (isDetached) {
                    SettingsPaneHeader(
                        isPinned = isPinned,
                        onTogglePin = onTogglePin,
                        onClose = onNavigateBack
                    )
                }
                CompositionLocalProvider(LocalSettingsSearch provides searchQuery) {
                    SectionContentLazy(
                        section = selectedSection,
                        state = state,
                        onThemeChanged = onThemeChanged,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        onOpenThemeCreator = onOpenThemeCreator
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    section: SettingsSection,
    isSelected: Boolean,
    onClick: () -> Unit,
    highlightQuery: String = ""
) {
    val s = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (isSelected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        tween(160), label = "nav_bg"
    )
    val contentColor = if (isSelected) scheme.primary else scheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(section.icon, null, modifier = Modifier.size(17.dp), tint = contentColor)
        CompositionLocalProvider(LocalSettingsSearch provides highlightQuery) {
            HighlightedSettingsText(
                section.localizedLabel(s),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(SectionChipShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            section.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        section.localizedLabel(s),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                SettingsSection.COMMANDS -> commandsLazyItems(state, viewModel)
                SettingsSection.ACTIONS -> actionsLazyItems(state, viewModel)
                SettingsSection.MODERATION -> moderationLazyItems(state, viewModel)
                SettingsSection.AI -> aiLazyItems()
                SettingsSection.ABOUT -> aboutLazyItems(viewModel)
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
internal fun SectionContentColumn(
    section: SettingsSection,
    state: SettingsState,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenThemeCreator: (seedColor: Int?) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
            SettingsSection.ACTIONS -> ActionsSection(
                state = state,
                onEvent = { viewModel.sendEvent(it) }
            )

            SettingsSection.COMMANDS -> ChatCommandsSection(
                state = state,
                onEvent = { viewModel.sendEvent(it) }
            )

            SettingsSection.MODERATION -> ModerationContent(state, viewModel)
            SettingsSection.AI -> {
                AiAssistantConfigCard()
                AiAutoModCard()
            }

            SettingsSection.ACCOUNT -> AccountContent(viewModel)
            SettingsSection.ABOUT -> AboutContent(viewModel)
        }
    }
}

private fun LazyListScope.notificationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    if (currentFormFactor() == DeviceFormFactor.PHONE) {
        item { MentionAlertsCard() }
    }
    item {
        SettingsPair(
            first = { NotificationGroupCard(state, vm) },
            second = if (state.mentionSoundEnabled) {
                { CustomSoundCard(state, vm) }
            } else null
        )
    }
}

private fun LazyListScope.backgroundLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item { BackgroundCard(state, vm) }
}

private fun LazyListScope.moderationLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        ModerationSettingsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
    item { StreamerModeCard() }
}

private fun LazyListScope.aiLazyItems() {
    item {
        SettingsPair(
            first = { AiAssistantConfigCard() },
            second = { AiAutoModCard() }
        )
    }
}

private fun LazyListScope.actionsLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        ActionsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
}

private fun LazyListScope.commandsLazyItems(state: SettingsState, vm: SettingsViewModel) {
    item {
        ChatCommandsSection(
            state = state,
            onEvent = { vm.sendEvent(it) }
        )
    }
}

private fun LazyListScope.aboutLazyItems(vm: SettingsViewModel) {
    item {
        SettingsPair(
            first = { BackupCard(vm) },
            second = { AboutCard() }
        )
    }
}

private fun LazyListScope.accountLazyItems(vm: SettingsViewModel) {
    item {
        AccountContent(vm)
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
                Lucide.LogOut,
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
                Lucide.Trash2,
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
    val state by viewModel.state.collectAsState()
    val accountManager =
        koinInject<AccountManager>()
    val accountActions =
        koinInject<AccountActions>()
    val accountLoader =
        koinInject<AccountListLoader>()
    val accountUi = rememberAccountUiState(
        accountLoader,
        accountManager
    )
    var showAddAccountDialog by remember { mutableStateOf(false) }

    SettingsGroup(s.accountsTitle) {
        AccountsSettingsSectionCompact(
            accounts = accountUi.accounts,
            accountManager = accountManager,
            onAddAccount = { showAddAccountDialog = true },
            onRemoveAccount = { accountActions.remove(it) },
            onSetPrimary = { accountActions.setPrimary(it) }
        )
    }
    SettingsGroup(s.settingsAccount) {
        AccountSectionBody(
            onLogout = { viewModel.sendEvent(SettingsEvent.OnLogoutClicked) },
            onClearCache = { viewModel.sendEvent(SettingsEvent.OnClearCacheClicked) }
        )
    }
    FirstPartyTokenCard()
    LaunchedEffect(Unit) {
        if (state.blockedUsernames.isEmpty() && !state.isLoadingBlockedUsers) {
            viewModel.sendEvent(SettingsEvent.OnLoadBlockedUsers)
        }
    }
    BlockedUsersSection(
        blockedUsernames = state.blockedUsernames,
        showBlockedMode = state.showBlockedMode,
        isLoadingBlockedUsers = state.isLoadingBlockedUsers,
        loadError = state.blockedLoadError,
        onShowBlockedModeChange = { viewModel.sendEvent(SettingsEvent.OnShowBlockedModeChanged(it)) },
        onUnblockUser = { username ->
            viewModel.sendEvent(SettingsEvent.OnUnblockUserFromSettings(username, ""))
        },
        onRefresh = { viewModel.sendEvent(SettingsEvent.OnLoadBlockedUsers) }
    )

    if (showAddAccountDialog) {
        AccountAddDialog(
            onDismiss = { showAddAccountDialog = false },
            onAccountAdded = { accountActions.setPrimary(it) }
        )
    }
}

@Composable
private fun NotificationContent(state: SettingsState, vm: SettingsViewModel) {
    if (currentFormFactor() == DeviceFormFactor.PHONE) {
        MentionAlertsCard()
        Spacer(Modifier.height(8.dp))
    }
    NotificationGroupCard(state, vm)
    if (state.mentionSoundEnabled) {
        Spacer(Modifier.height(8.dp))
        CustomSoundCard(state, vm)
    }
}

@Composable
private fun BackgroundContent(state: SettingsState, vm: SettingsViewModel) {
    BackgroundCard(state, vm)
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
private fun AboutContent(vm: SettingsViewModel) {
    BackupCard(vm)
    AboutCard()
}

@Composable
internal fun MentionTabsSettingsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    SettingsGroup(s.settingsMentionTabs) {
        SwitchRow(s.settingsMentionTabs, s.settingsMentionTabsDesc, state.mentionTabsEnabled) {
            vm.sendEvent(SettingsEvent.OnMentionTabsChanged(it))
        }
    }
}

@Composable
internal fun TranslationSettingsGroup(state: SettingsState, vm: SettingsViewModel) {
    val s = LocalStrings.current
    val langs = TranslationLanguages
    val currentName = langs.firstOrNull { it.first == state.translationTargetLang }?.second
        ?: state.translationTargetLang
    SettingsGroup(s.settingsTranslationLang) {
        ListRow(
            s.settingsTranslationLangDesc,
            currentName,
            langs.map { it.second }
        ) { idx -> vm.sendEvent(SettingsEvent.OnTranslationLangChanged(langs[idx].first)) }
        SwitchRow(
            s.settingsAutoTranslateInput,
            s.settingsAutoTranslateInputDesc,
            state.autoTranslateInput
        ) {
            vm.sendEvent(SettingsEvent.OnAutoTranslateInputChanged(it))
        }
    }
}

@Composable
internal fun UiScaleRow(currentScale: Float, onScaleChanged: (Float) -> Unit) {
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
        ChatoneSlider(
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
    ChatoneLazyScrollbar(
        listState = listState,
        itemCount = listState.layoutInfo.totalItemsCount,
        modifier = modifier
    )
}

@Composable
private fun SettingsThinScrollbarScroll(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    ChatoneScrollbar(
        scrollState = scrollState,
        modifier = modifier
    )
}

private val SectionChipShape = ScallopShape(lobes = 8, depth = 0.09f)
private const val DESKTOP_HAZE = 0.45f
