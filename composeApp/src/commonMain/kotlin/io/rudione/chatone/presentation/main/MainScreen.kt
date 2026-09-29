package io.rudione.chatone.presentation.main

import io.rudione.chatone.domain.model.ChannelTab
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.presentation.chat.ChatScreen
import io.rudione.chatone.presentation.chat.ChatScreenCallbacks
import io.rudione.chatone.presentation.chat.ChatScreenOptions
import io.rudione.chatone.presentation.chat.ChatSessionIdentity
import io.rudione.chatone.presentation.components.DockHost
import io.rudione.chatone.presentation.components.DockPanel
import io.rudione.chatone.presentation.components.LocalDockHost
import io.rudione.chatone.presentation.components.LocalWindowSize
import io.rudione.chatone.presentation.ai.AiAssistantPanel
import io.rudione.chatone.presentation.automod.AutomodScreen
import io.rudione.chatone.presentation.components.ChatoneSplitHandle
import io.rudione.chatone.presentation.window.DetachedDockWindow
import io.rudione.chatone.util.system.isDesktopPlatform
import io.rudione.chatone.presentation.components.ChatoneBreakpoints
import io.rudione.chatone.presentation.components.ChatoneWindowSize
import io.rudione.chatone.presentation.components.SwipeDirection
import io.rudione.chatone.presentation.components.directionalSwipe
import io.rudione.chatone.presentation.components.ChatoneTileDefaults
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.components.GradientButton
import io.rudione.chatone.presentation.components.TileTone
import io.rudione.chatone.presentation.components.chatoneTile
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.chat.pauseHotkeyMatches
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsNavigator
import io.rudione.chatone.presentation.settings.SettingsScreen
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import io.rudione.chatone.util.system.HotkeyAction
import io.rudione.chatone.util.system.comboFor
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.ChatoneColors
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperGlowEdge
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.util.media.WallpaperLoader
import io.rudione.chatone.util.system.handleHover
import io.rudione.chatone.util.system.NotificationLaunches
import kotlinx.coroutines.flow.filterNotNull
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.hypot
import io.rudione.chatone.presentation.chat.components.ChattersPanel
import io.rudione.chatone.presentation.main.components.MentionsFeed
import io.rudione.chatone.presentation.main.components.MentionTabsBar
import io.rudione.chatone.presentation.main.components.MentionToast
import io.rudione.chatone.presentation.main.components.EmptyState
import io.rudione.chatone.presentation.main.components.CreateFolderDialog
import io.rudione.chatone.presentation.main.components.EditFolderDialog
import io.rudione.chatone.presentation.main.components.sidebar.MiniRail
import io.rudione.chatone.presentation.main.components.sidebar.CompactSidebar
import io.rudione.chatone.presentation.main.components.sidebar.ChannelSidebar
import io.rudione.chatone.presentation.main.components.sidebar.ChannelTabBar
import io.rudione.chatone.presentation.automod.DetachedAutomodWindow
import io.rudione.chatone.presentation.settings.DetachedSettingsWindow
import io.rudione.chatone.presentation.settings.SettingsEffect
import io.rudione.chatone.presentation.chat.multichat.MultiChatRootSetup
import io.rudione.chatone.presentation.account.AccountAutoConnectEffect
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.MultiAccountConnectionRegistry
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.chat.multichat.MainScreenChatRouter
import io.rudione.chatone.presentation.stream.LandscapeStreamLayout
import io.rudione.chatone.presentation.stream.LocalStreamPlayerHost
import io.rudione.chatone.presentation.stream.PortraitStreamPlayer
import io.rudione.chatone.presentation.stream.StreamSettingsSheet
import io.rudione.chatone.presentation.stream.rememberMobileStreamUi
import io.rudione.chatone.presentation.stream.rememberStreamPlayerHost
import io.rudione.chatone.presentation.stream.StreamPlayerEvent
import io.rudione.chatone.presentation.browse.BrowseOverlay
import io.rudione.chatone.presentation.chat.zoom.ChatZoomIndicator
import io.rudione.chatone.presentation.chat.zoom.ChatZoomShortcuts
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import androidx.compose.ui.platform.LocalWindowInfo
import io.rudione.chatone.data.repository.AiAssistantController
import io.rudione.chatone.presentation.components.DockState
import io.rudione.chatone.icons.lucide.ChevronRight
import io.rudione.chatone.icons.lucide.Lucide

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToAuth: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    val s = LocalStrings.current
    val isPhone = remember { currentFormFactor() == DeviceFormFactor.PHONE }
    val isHandheld = remember { currentFormFactor().isHandheld }
    val openBrowse: (() -> Unit)? = if (isHandheld) {
        remember(viewModel) { { viewModel.sendEvent(MainEvent.ShowBrowse) } }
    } else {
        null
    }
    val streamHost = rememberStreamPlayerHost()
    val windowContainer = LocalWindowInfo.current.containerSize
    val streamChannel = state.activeChannelLogin
        ?.takeUnless { state.mentionsChannelActive || isMonitorLogin(it) }
    val streamUi = rememberMobileStreamUi(
        host = streamHost,
        channelLogin = streamChannel,
        landscapeWindow = windowContainer.width > windowContainer.height
    )

    MultiChatRootSetup()
    if (isDesktopPlatform) ChatZoomShortcuts()
    val accountManager: AccountManager = koinInject()
    val authRepository: AuthRepository = koinInject()
    val connectionRegistry: MultiAccountConnectionRegistry = koinInject()
    AccountAutoConnectEffect(
        authRepository = authRepository,
        registry = connectionRegistry,
        accountManager = accountManager
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wallpaperController = LocalWallpaperController.current
    var isWideScreenForSettings by remember { mutableStateOf(false) }
    val wallpaper by remember { derivedStateOf { wallpaperController.state } }
    val wallpaperLoader: WallpaperLoader = koinInject()
    var showWhisperOverlay by remember { mutableStateOf(false) }
    var showMentionsOverlay by remember { mutableStateOf(false) }
    var mentionToastData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val currentHotkeys by rememberUpdatedState(settingsState.hotkeys)
    val currentNavHidden by rememberUpdatedState(settingsState.navigationHidden)
    DisposableEffect(Unit) {
        val handler: (KeyEvent) -> Boolean = handler@{ event ->
            if (event.type != KeyEventType.KeyDown) return@handler false
            val hk = currentHotkeys
            when {
                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.TOGGLE_NAVIGATION)) -> {
                    settingsViewModel.sendEvent(SettingsEvent.OnNavigationHiddenChanged(!currentNavHidden)); true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.TOGGLE_SIDEBAR)) -> {
                    viewModel.sendEvent(MainEvent.ToggleSidebar); true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.TOGGLE_MENTIONS)) -> {
                    viewModel.sendEvent(MainEvent.ToggleMentionsFeed); true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.ADD_CHANNEL)) -> {
                    viewModel.sendEvent(MainEvent.ShowAddChannelDialog); true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.OPEN_SETTINGS)) -> {
                    viewModel.sendEvent(MainEvent.ShowSettings); true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.PREV_CHANNEL)) -> {
                    val st = viewModel.state.value
                    val logins = st.openChannels.map { it.login }
                    val idx = logins.indexOf(st.activeChannelLogin)
                    if (logins.isNotEmpty()) {
                        val prev = logins[(idx - 1 + logins.size) % logins.size]
                        viewModel.sendEvent(MainEvent.SelectChannel(prev))
                    }
                    true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.NEXT_CHANNEL)) -> {
                    val st = viewModel.state.value
                    val logins = st.openChannels.map { it.login }
                    val idx = logins.indexOf(st.activeChannelLogin)
                    if (logins.isNotEmpty()) {
                        val next = logins[(idx + 1) % logins.size]
                        viewModel.sendEvent(MainEvent.SelectChannel(next))
                    }
                    true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.CLOSE_CHANNEL)) -> {
                    viewModel.state.value.activeChannelLogin?.let {
                        viewModel.sendEvent(MainEvent.CloseChannel(it))
                    }
                    true
                }

                pauseHotkeyMatches(event, hk.comboFor(HotkeyAction.TOGGLE_WHISPERS)) -> {
                    viewModel.sendEvent(MainEvent.ToggleWhisperPanel); true
                }

                else -> false
            }
        }
        val unregister = GlobalKeyDispatcher.register(handler)
        onDispose { unregister() }
    }

    LaunchedEffect(Unit) {
        NotificationLaunches.pending.filterNotNull().collect { target ->
            viewModel.sendEvent(MainEvent.SelectChannel(target.channelLogin, target.messageId))
            NotificationLaunches.consume(target)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MainEffect.NavigateToAuth -> onNavigateToAuth()
                is MainEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is MainEffect.IncomingWhisper -> {
                    mentionToastData = Triple(
                        effect.fromDisplayName,
                        "💬 whisper",
                        effect.text
                    )
                }

                is MainEffect.MentionToast -> {
                    mentionToastData = Triple(
                        effect.fromDisplayName,
                        effect.channelLogin,
                        effect.text
                    )
                }
            }
            launch {
                settingsViewModel.effect.collect { effect ->
                    if (effect is SettingsEffect.NavigateToAuth) {
                        viewModel.sendEvent(MainEvent.HideSettings)
                        onNavigateToAuth()
                    }
                }
            }
        }
    }

    if (state.showSettings && !isWideScreenForSettings) {
        DetachedSettingsWindow(
            onClose = { viewModel.sendEvent(MainEvent.HideSettings) },
            onThemeChanged = onThemeChanged
        )
    }

    val statusBarTint = topBarBackgroundColor(wallpaper, MaterialTheme.colorScheme.surfaceContainer)
    CompositionLocalProvider(LocalStreamPlayerHost provides streamHost) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = if (streamUi?.playerVisible == true && (isPhone || streamUi.showLandscapeLayout)) {
                Color.Black
            } else {
                statusBarTint
            },
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        ) { scaffoldPadding ->
            if (state.showSettings && isWideScreenForSettings &&
                !DockState.large
            ) {
                SettingsScreen(
                    onNavigateBack = { viewModel.sendEvent(MainEvent.HideSettings) },
                    onThemeChanged = onThemeChanged,
                    isWideScreen = true,
                    wallpaperLoader = wallpaperLoader
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val availableWidth = maxWidth
                val dockHost = remember { DockHost() }
                var sidebarWidth by remember { mutableStateOf(state.sidebarWidthDp.dp) }
                var dockWidth by remember { mutableStateOf(ChatoneBreakpoints.dockWidth) }
                var tornSettings by remember { mutableStateOf(false) }
                val aiController: AiAssistantController = koinInject()
                val settingsNavigator: SettingsNavigator = koinInject()
                LaunchedEffect(settingsNavigator) {
                    settingsNavigator.openRequests.collect {
                        if (!tornSettings) viewModel.sendEvent(MainEvent.ShowSettings)
                    }
                }

                if (tornSettings) {
                    DetachedSettingsWindow(
                        onClose = { tornSettings = false },
                        onThemeChanged = onThemeChanged
                    )
                }
                if (dockHost.torn != DockPanel.None) {
                    DetachedDockWindow(
                        windowId = "dock_${dockHost.torn.name.lowercase()}",
                        title = "Chatone — ${dockHost.torn.name}",
                        onClose = { dockHost.closeTorn() }
                    ) {
                        when (dockHost.torn) {
                            DockPanel.Automod -> AutomodScreen(
                                currentChannelLogin = dockHost.channelLogin
                                    ?: state.activeChannelLogin,
                                onClose = { dockHost.closeTorn() },
                                onExport = { _, _ -> },
                                onImport = {},
                                modifier = Modifier.fillMaxSize()
                            )

                            DockPanel.Assistant -> AiAssistantPanel(
                                controller = aiController,
                                client = koinInject()
                            )

                            else -> dockHost.tornContent?.invoke()
                        }
                    }
                }
                val windowSize = ChatoneBreakpoints.of(availableWidth)
                val isLarge = windowSize == ChatoneWindowSize.Large
                val isWideScreen = windowSize != ChatoneWindowSize.Compact
                LaunchedEffect(isWideScreen, isLarge) {
                    isWideScreenForSettings = isWideScreen
                }
                SideEffect { io.rudione.chatone.presentation.components.DockState.large = isLarge }

                var sidebarDragging by remember { mutableStateOf(false) }
                LaunchedEffect(state.sidebarWidthDp) {
                    if (!sidebarDragging) sidebarWidth = state.sidebarWidthDp.dp
                }

                val sidebarRailMode = state.sidebarCollapsed
                val paneChrome = ChatoneTileDefaults.outerPadding * 2 + ChatoneTileDefaults.gap
                val maxSidebarWidth = (
                        availableWidth - paneChrome - if (isLarge) ChatoneBreakpoints.minPane * 2
                        else ChatoneBreakpoints.minChatPane
                        ).coerceAtLeast(ChatoneBreakpoints.minPane)
                val clampedSidebarWidth =
                    sidebarWidth.coerceIn(ChatoneBreakpoints.minPane, maxSidebarWidth)
                val effectiveSidebarWidth by animateDpAsState(
                    targetValue = if (sidebarRailMode) ChatoneBreakpoints.railWidth else clampedSidebarWidth,
                    animationSpec = if (sidebarDragging) snap() else tween(220),
                    label = "sidebar_width"
                )

                val chatIdentity by rememberUpdatedState(
                    ChatSessionIdentity(
                        accessToken = state.selectedAccount?.accessToken.orEmpty(),
                        userId = state.selectedAccount?.userId.orEmpty(),
                        userLogin = state.selectedAccount?.login.orEmpty(),
                        displayName = state.selectedAccount?.displayName.orEmpty()
                    )
                )
                val sharedChatCallbacks = remember(viewModel) {
                    ChatScreenCallbacks(
                        onMentionDetected = { login ->
                            viewModel.sendEvent(
                                MainEvent.IncrementMentionCount(
                                    login
                                )
                            )
                        },
                        onMentionReceived = { entry ->
                            viewModel.sendEvent(
                                MainEvent.AddMentionEntry(
                                    entry
                                )
                            )
                        },
                        onOpenWhisper = { userId, username, displayName, avatarUrl, color ->
                            viewModel.sendEvent(
                                MainEvent.OpenWhisperWith(
                                    userId,
                                    username,
                                    displayName,
                                    avatarUrl,
                                    color
                                )
                            )
                        },
                        onScrollToMessageHandled = { viewModel.sendEvent(MainEvent.ClearPendingScrollMessage) }
                    )
                }

                val chatContent = remember {
                    movableContentOf<String, Boolean> { activeChannel, wide ->
                        val callbacks = remember(wide, sharedChatCallbacks) {
                            ChatScreenCallbacks(
                                onNavigateBack = { if (!wide) viewModel.sendEvent(MainEvent.ToggleSidebar) },
                                onMentionDetected = sharedChatCallbacks.onMentionDetected,
                                onMentionReceived = sharedChatCallbacks.onMentionReceived,
                                onOpenWhisper = sharedChatCallbacks.onOpenWhisper,
                                onChannelIdResolved = { channelId ->
                                    viewModel.sendEvent(MainEvent.SetActiveChatChannelId(channelId))
                                },
                                onScrollToMessageHandled = sharedChatCallbacks.onScrollToMessageHandled
                            )
                        }
                        ChatScreen(
                            channelLogin = activeChannel,
                            identity = chatIdentity,
                            callbacks = callbacks,
                            wallpaper = wallpaper,
                            modifier = Modifier.fillMaxSize(),
                            options = ChatScreenOptions(
                                isWideScreen = wide,
                                menuBadgeCount = if (wide) 0 else state.unreadMentionsCount,
                                pendingScrollMessageId = state.pendingScrollMessageId
                            ),
                            mentionMuteRepository = viewModel.mentionMuteRepository
                        )
                    }
                }

                val multiPanelRenderer: @Composable (String, Boolean, Modifier) -> Unit =
                    { panelChannel, _, panelModifier ->
                        val perPanelViewModel: ChatViewModel =
                            koinViewModel(key = "panel-vm-${panelChannel.lowercase()}")
                        ChatScreen(
                            channelLogin = panelChannel,
                            identity = chatIdentity,
                            callbacks = sharedChatCallbacks,
                            wallpaper = wallpaper,
                            modifier = panelModifier,
                            options = ChatScreenOptions(
                                renderBackground = false,
                                isMultiChat = state.openChannels.size > 1,
                                pendingScrollMessageId = state.pendingScrollMessageId
                                    .takeIf {
                                        panelChannel.equals(
                                            state.activeChannelLogin,
                                            ignoreCase = true
                                        )
                                    }
                            ),
                            mentionMuteRepository = viewModel.mentionMuteRepository,
                            viewModel = perPanelViewModel
                        )
                    }

                val hasUnreadMentions = state.mentions.any { !it.isRead }
                val mentionStripInWide = !settingsState.navigationHidden && hasUnreadMentions
                val mentionsPanelShowing =
                    !settingsState.navigationHidden && settingsState.mentionTabsEnabled &&
                            hasUnreadMentions

                CompositionLocalProvider(
                    LocalWindowSize provides windowSize,
                    LocalDockHost provides if (isLarge) dockHost else null
                ) {
                    if (streamUi != null && streamUi.showLandscapeLayout && streamChannel != null) {
                        LandscapeStreamLayout(
                            ui = streamUi,
                            chat = { chatModifier ->
                                Box(chatModifier) {
                                    MainScreenChatRouter(
                                        activeChannel = streamChannel,
                                        isWideScreen = false,
                                        singleChatRenderer = { ch, _ -> chatContent(ch, false) },
                                        multiChatRenderer = multiPanelRenderer
                                    )
                                }
                            }
                        )
                        CompactChannelDrawer(
                            state = state,
                            viewModel = viewModel,
                            wallpaperActive = wallpaper.isActive,
                            glowEnabled = wallpaper.glowEffectsEnabled,
                            dominantColor = wallpaper.dominantColor,
                            onOpenBrowse = openBrowse
                        )
                    } else if (isWideScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars.only(
                                        WindowInsetsSides.Bottom
                                    )
                                )
                                .padding(ChatoneTileDefaults.outerPadding)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(effectiveSidebarWidth)
                                    .then(
                                        if (sidebarRailMode) Modifier
                                        else Modifier.chatoneTile(TileTone.Base)
                                    )
                            ) {
                                GlowSurface(
                                    dominantColor = wallpaper.dominantColor,
                                    intensity = 0.9f,
                                    centerX = 1.2f,
                                    centerY = 0.5f
                                ) {
                                    AnimatedContent(
                                        targetState = state.sidebarCollapsed,
                                        transitionSpec = {
                                            if (targetState) {
                                                (androidx.compose.animation.slideInHorizontally(
                                                    androidx.compose.animation.core.tween(
                                                        220
                                                    )
                                                ) { -it } +
                                                        androidx.compose.animation.fadeIn(
                                                            androidx.compose.animation.core.tween(
                                                                180
                                                            )
                                                        )).togetherWith(
                                                    androidx.compose.animation.slideOutHorizontally(
                                                        androidx.compose.animation.core.tween(
                                                            220
                                                        )
                                                    ) { -it } +
                                                            androidx.compose.animation.fadeOut(
                                                                androidx.compose.animation.core.tween(
                                                                    140
                                                                )
                                                            )
                                                )
                                            } else {
                                                (androidx.compose.animation.slideInHorizontally(
                                                    androidx.compose.animation.core.tween(
                                                        220
                                                    )
                                                ) { -it } +
                                                        androidx.compose.animation.fadeIn(
                                                            androidx.compose.animation.core.tween(
                                                                180
                                                            )
                                                        )).togetherWith(
                                                    androidx.compose.animation.slideOutHorizontally(
                                                        androidx.compose.animation.core.tween(
                                                            220
                                                        )
                                                    ) { -it } +
                                                            androidx.compose.animation.fadeOut(
                                                                androidx.compose.animation.core.tween(
                                                                    140
                                                                )
                                                            )
                                                )
                                            }
                                        },
                                        label = "sidebar_mode"
                                    ) { isCollapsed ->
                                        if (isCollapsed) {
                                            CompactSidebar(
                                                state = state,
                                                onEvent = { viewModel.sendEvent(it) }
                                            )
                                        } else {
                                            ChannelSidebar(
                                                state = state,
                                                onEvent = { viewModel.sendEvent(it) },
                                                isWideScreen = true,
                                                mentionMuteRepository = viewModel.mentionMuteRepository,
                                                onOpenBrowse = openBrowse
                                            )
                                        }
                                    }
                                }
                                if (wallpaper.isActive && wallpaper.glowEffectsEnabled) {
                                    WallpaperGlowEdge(
                                        dominantColor = wallpaper.dominantColor,
                                        fromRight = true,
                                        glowWidth = 100.dp,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                            }
                            if (sidebarRailMode) {
                                Spacer(Modifier.width(ChatoneTileDefaults.gap))
                            } else {
                                ChatoneSplitHandle(
                                    onDelta = { d ->
                                        sidebarDragging = true
                                        sidebarWidth = (clampedSidebarWidth + d)
                                            .coerceIn(ChatoneBreakpoints.minPane, maxSidebarWidth)
                                    },
                                    onDragEnd = {
                                        sidebarDragging = false
                                        viewModel.sendEvent(MainEvent.SetSidebarWidth(sidebarWidth.value))
                                    }
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .chatoneTile(TileTone.Sunken)
                            ) {
                                if (streamUi != null && streamUi.showPortraitPlayer) {
                                    PortraitStreamPlayer(ui = streamUi)
                                }
                                if (mentionStripInWide) {
                                    MentionTabsBar(
                                        mentions = state.mentions,
                                        activeLogin = state.activeChannelLogin,
                                        onSelect = { login, messageId ->
                                            viewModel.sendEvent(
                                                MainEvent.SelectChannel(
                                                    login,
                                                    messageId
                                                )
                                            )
                                            viewModel.sendEvent(
                                                MainEvent.MarkChannelMentionsRead(
                                                    login
                                                )
                                            )
                                        }
                                    )
                                }
                                val activeChannel = state.activeChannelLogin
                                if (state.mentionsChannelActive) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        MentionsFeed(
                                            state = state,
                                            onEvent = { viewModel.sendEvent(it) },
                                            onChannelClick = { login, messageId ->
                                                viewModel.sendEvent(
                                                    MainEvent.SelectChannel(
                                                        login,
                                                        messageId
                                                    )
                                                )
                                            },
                                            fillAvailableSpace = true,
                                            onCloseClick = { viewModel.sendEvent(MainEvent.CloseMentionsChannel) }
                                        )
                                    }
                                } else if (isMonitorLogin(activeChannel)) {
                                    ChatBackgroundLayer(
                                        wallpaper = wallpaper,
                                        darkTheme = settingsState.darkTheme,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MonitorFeedScreen(
                                            login = activeChannel!!,
                                            channelMeta = monitorChannelMeta(state)
                                        )
                                    }
                                } else if (activeChannel != null) {
                                    ChatBackgroundLayer(
                                        wallpaper = wallpaper,
                                        darkTheme = settingsState.darkTheme,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        MainScreenChatRouter(
                                            activeChannel = activeChannel,
                                            isWideScreen = true,
                                            singleChatRenderer = { ch, wide ->
                                                chatContent(
                                                    ch,
                                                    wide
                                                )
                                            },
                                            multiChatRenderer = multiPanelRenderer
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f)) {
                                        EmptyState(
                                            isGuest = state.isGuest,
                                            onAddChannel = { viewModel.sendEvent(MainEvent.ShowAddChannelDialog) },
                                            onLogin = { viewModel.sendEvent(MainEvent.NavigateToAuth) },
                                            onBrowse = openBrowse
                                        )
                                    }
                                }
                            }
                            val assistantOpen by aiController.isOpen.collectAsState()
                            val assistantFloating by aiController.floating.collectAsState()
                            val assistantDocked = assistantOpen && !assistantFloating
                            val dockActive = when {
                                dockHost.panel != DockPanel.None -> dockHost.panel
                                state.showSettings -> DockPanel.Settings
                                assistantDocked -> DockPanel.Assistant
                                else -> DockPanel.None
                            }
                            LaunchedEffect(dockHost.panel) {
                                if (dockHost.panel != DockPanel.None) {
                                    if (state.showSettings) viewModel.sendEvent(MainEvent.HideSettings)
                                    if (aiController.isOpen.value) aiController.close()
                                }
                            }
                            LaunchedEffect(state.showSettings) {
                                if (state.showSettings) {
                                    if (dockHost.panel != DockPanel.None) dockHost.close()
                                    if (aiController.isOpen.value) aiController.close()
                                }
                            }
                            LaunchedEffect(assistantDocked) {
                                if (assistantDocked) {
                                    if (dockHost.panel != DockPanel.None) dockHost.close()
                                    if (state.showSettings) viewModel.sendEvent(MainEvent.HideSettings)
                                }
                            }
                            if (isLarge && dockActive != DockPanel.None) {
                                ChatoneSplitHandle(
                                    onDelta = { d ->
                                        val maxDock =
                                            availableWidth - effectiveSidebarWidth - ChatoneBreakpoints.minPane
                                        dockWidth = (dockWidth - d)
                                            .coerceIn(
                                                ChatoneBreakpoints.minPane,
                                                maxDock.coerceAtLeast(ChatoneBreakpoints.minPane)
                                            )
                                    },
                                    onTearOut = if (isDesktopPlatform) {
                                        {
                                            if (dockActive == DockPanel.Settings) {
                                                viewModel.sendEvent(MainEvent.HideSettings)
                                                tornSettings = true
                                            } else {
                                                dockHost.tearOut()
                                            }
                                            dockWidth = ChatoneBreakpoints.dockWidth
                                        }
                                    } else null
                                )
                                Column(
                                    modifier = Modifier
                                        .width(dockWidth)
                                        .fillMaxHeight()
                                        .chatoneTile(TileTone.Base)
                                ) {
                                    val dockWindowSize =
                                        if (dockActive == DockPanel.Moderation) windowSize
                                        else ChatoneBreakpoints.of(dockWidth)
                                    CompositionLocalProvider(
                                        LocalWindowSize provides dockWindowSize
                                    ) {
                                        when (dockActive) {
                                            DockPanel.Settings -> SettingsScreen(
                                                onNavigateBack = { viewModel.sendEvent(MainEvent.HideSettings) },
                                                onThemeChanged = onThemeChanged,
                                                isWideScreen = dockWindowSize != ChatoneWindowSize.Compact,
                                                embedded = true,
                                                wallpaperLoader = wallpaperLoader
                                            )

                                            DockPanel.Assistant -> AiAssistantPanel(
                                                controller = aiController,
                                                client = koinInject()
                                            )

                                            DockPanel.Automod -> AutomodScreen(
                                                currentChannelLogin = dockHost.channelLogin
                                                    ?: state.activeChannelLogin,
                                                onClose = { dockHost.close() },
                                                onExport = { _, _ -> },
                                                onImport = {},
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            DockPanel.Moderation,
                                            DockPanel.Emotes,
                                            DockPanel.Points -> dockHost.content?.invoke()

                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (mentionsPanelShowing && !isPhone) {
                                MentionTabsBar(
                                    mentions = state.mentions,
                                    activeLogin = state.activeChannelLogin,
                                    onSelect = { login, messageId ->
                                        viewModel.sendEvent(
                                            MainEvent.SelectChannel(
                                                login,
                                                messageId
                                            )
                                        )
                                        viewModel.sendEvent(MainEvent.MarkChannelMentionsRead(login))
                                    }
                                )
                            }
                            val showTabBar = !isPhone &&
                                    !settingsState.navigationHidden && !mentionsPanelShowing &&
                                    settingsState.channelNavigation != SettingsState.ChannelNavigation.MINI_RAIL
                            if (showTabBar && (state.openChannels.size + state.monitorTabs.size) > 1) {
                                Box {
                                    ChannelTabBar(
                                        channels = state.openChannels + state.monitorTabs.map {
                                            ChannelTab(
                                                login = it,
                                                displayName = it
                                            )
                                        },
                                        activeLogin = state.activeChannelLogin,
                                        onSelect = { login: String ->
                                            viewModel.sendEvent(
                                                MainEvent.SelectChannel(
                                                    login
                                                )
                                            )
                                        },
                                        onClose = { login: String ->
                                            if (login.startsWith("/")) viewModel.sendEvent(
                                                MainEvent.CloseMonitorTab(
                                                    login
                                                )
                                            )
                                            else viewModel.sendEvent(MainEvent.CloseChannel(login))
                                        },
                                        onReorder = { from: Int, to: Int ->
                                            viewModel.sendEvent(MainEvent.ReorderChannels(from, to))
                                        },
                                        folders = state.folders,
                                        onToggleFolder = { folderId ->
                                            viewModel.sendEvent(MainEvent.ToggleFolder(folderId))
                                        },
                                        onMoveToFolder = { login, folderId ->
                                            viewModel.sendEvent(
                                                MainEvent.MoveChannelToFolder(
                                                    login,
                                                    folderId
                                                )
                                            )
                                        }
                                    )
                                    if (wallpaper.isActive) {
                                        WallpaperGlowEdge(
                                            dominantColor = wallpaper.dominantColor,
                                            fromRight = false,
                                            glowWidth = 60.dp,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }
                                }
                            }
                            val activeChannel = state.activeChannelLogin
                            if (streamUi != null && streamUi.showPortraitPlayer) {
                                PortraitStreamPlayer(ui = streamUi)
                            }
                            if (state.mentionsChannelActive) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MentionsFeed(
                                        state = state,
                                        onEvent = { viewModel.sendEvent(it) },
                                        onChannelClick = { login, messageId ->
                                            viewModel.sendEvent(
                                                MainEvent.SelectChannel(
                                                    login,
                                                    messageId
                                                )
                                            )
                                        },
                                        fillAvailableSpace = true,
                                        onCloseClick = { viewModel.sendEvent(MainEvent.CloseMentionsChannel) }
                                    )
                                }
                            } else if (isMonitorLogin(activeChannel)) {
                                ChatBackgroundLayer(
                                    wallpaper = wallpaper,
                                    darkTheme = settingsState.darkTheme,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        Modifier.windowInsetsPadding(
                                            WindowInsets.navigationBars.only(
                                                WindowInsetsSides.Bottom
                                            )
                                        )
                                    ) {
                                        MonitorFeedScreen(
                                            login = activeChannel!!,
                                            channelMeta = monitorChannelMeta(state),
                                            onMenuClick = { viewModel.sendEvent(MainEvent.ToggleSidebar) }
                                        )
                                    }
                                }
                            } else if (activeChannel != null) {
                                ChatBackgroundLayer(
                                    wallpaper = wallpaper,
                                    darkTheme = settingsState.darkTheme,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MainScreenChatRouter(
                                        activeChannel = activeChannel,
                                        isWideScreen = false,
                                        singleChatRenderer = { ch, wide -> chatContent(ch, wide) },
                                        multiChatRenderer = multiPanelRenderer
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .windowInsetsPadding(
                                            WindowInsets.navigationBars.only(
                                                WindowInsetsSides.Bottom
                                            )
                                        )
                                ) {
                                    EmptyState(
                                        isGuest = state.isGuest,
                                        onAddChannel = { viewModel.sendEvent(MainEvent.ShowAddChannelDialog) },
                                        onLogin = { viewModel.sendEvent(MainEvent.NavigateToAuth) },
                                        onBrowse = openBrowse
                                    )
                                }
                            }
                        }

                        val miniRailAllowed = !isPhone &&
                                !mentionsPanelShowing &&
                                settingsState.channelNavigation != SettingsState.ChannelNavigation.TAB_BAR
                        if (miniRailAllowed) {
                            val railVisible =
                                !settingsState.navigationHidden && !state.sidebarExpanded
                            AnimatedVisibility(
                                visible = railVisible && !state.miniRailCollapsed,
                                enter = slideInVertically(tween(260)) { -it } + fadeIn(tween(200)),
                                exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(160)),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Box {
                                    MiniRail(
                                        state = state,
                                        onEvent = { event: MainEvent -> viewModel.sendEvent(event) },
                                        onCollapseRail = {
                                            viewModel.sendEvent(
                                                MainEvent.SetMiniRailCollapsed(
                                                    true
                                                )
                                            )
                                        }
                                    )
                                    if (wallpaper.isActive) {
                                        WallpaperGlowEdge(
                                            dominantColor = wallpaper.dominantColor,
                                            fromRight = true,
                                            glowWidth = 80.dp,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = railVisible && state.miniRailCollapsed,
                                enter = fadeIn(tween(200)) + scaleIn(
                                    tween(200),
                                    initialScale = 0.7f
                                ),
                                exit = fadeOut(tween(150)) + scaleOut(
                                    tween(150),
                                    targetScale = 0.7f
                                ),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 14.dp, top = 28.dp)
                            ) {
                                LiquidGlassSurface(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                viewModel.sendEvent(
                                                    MainEvent.SetMiniRailCollapsed(
                                                        false
                                                    )
                                                )
                                            }
                                        ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    forceGlass = true,
                                    shimmer = true
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Lucide.ChevronRight,
                                            contentDescription = LocalStrings.current.mainExpandMiniRail,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        CompactChannelDrawer(
                            state = state,
                            viewModel = viewModel,
                            wallpaperActive = wallpaper.isActive,
                            glowEnabled = wallpaper.glowEffectsEnabled,
                            dominantColor = wallpaper.dominantColor,
                            onOpenBrowse = openBrowse
                        )
                    }
                }
            }
        }
        if (streamUi != null) {
            StreamSettingsSheet(
                host = streamUi.host,
                landscape = streamUi.landscape,
                pictureInPictureSupported = streamUi.controls.pictureInPicture.isSupported
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = mentionToastData != null,
            enter = slideInVertically { -it } + fadeIn(tween(200)),
            exit = slideOutVertically { -it } + fadeOut(tween(150)),
            modifier = Modifier.padding(top = 56.dp)
        ) {
            mentionToastData?.let { (from, channel, text) ->
                MentionToast(
                    fromDisplayName = from,
                    channelLogin = channel,
                    text = text,
                    onDismiss = { mentionToastData = null }
                )
            }
        }
    }

    if (isDesktopPlatform) {
        Box(modifier = Modifier.fillMaxSize().zIndex(100f), contentAlignment = Alignment.TopEnd) {
            ChatZoomIndicator(modifier = Modifier.padding(top = 10.dp, end = 14.dp))
        }
    }

    if (state.showWhisperPanel) {
        Box(modifier = Modifier.fillMaxSize().zIndex(50f), contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        viewModel.sendEvent(
                            MainEvent.HideWhisperPanel
                        )
                    }
            )
            WhisperPanel(
                state = state,
                onEvent = { viewModel.sendEvent(it) },
                modifier = Modifier.padding(end = 16.dp, bottom = 72.dp)
            )
        }
    }

    if (state.showMentionsFeed) {
        Box(modifier = Modifier.fillMaxSize().zIndex(50f), contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        viewModel.sendEvent(
                            MainEvent.HideMentionsFeed
                        )
                    }
            )
            MentionsFeed(
                state = state,
                onEvent = { viewModel.sendEvent(it) },
                onChannelClick = { login, messageId ->
                    viewModel.sendEvent(
                        MainEvent.SelectChannel(
                            login,
                            messageId
                        )
                    )
                },
                modifier = Modifier.padding(end = 16.dp, bottom = 72.dp)
            )
        }
    }

    if (state.showChattersPanel && state.activeChannelLogin != null) {
        val account = state.selectedAccount
        if (account != null) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(50f),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                            viewModel.sendEvent(MainEvent.HideChattersPanel)
                        }
                )
                ChattersPanel(
                    channelLogin = state.activeChannelLogin!!,
                    channelId = state.activeChatChannelId,
                    accessToken = account.accessToken,
                    currentUserId = account.userId,
                    onUserClick = { userId, username, displayName ->
                        viewModel.sendEvent(MainEvent.HideChattersPanel)

                    },
                    onDismiss = { viewModel.sendEvent(MainEvent.HideChattersPanel) },
                    modifier = Modifier.padding(top = 60.dp, end = 16.dp)
                )
            }
        }
    }

    if (state.isAddChannelDialogVisible) {
        OpenChannelDialog(
            query = state.addChannelQuery,
            searchResults = state.searchResults,
            isSearching = state.isSearching,
            onQueryChange = { query: String ->
                viewModel.sendEvent(MainEvent.UpdateAddChannelQuery(query))
            },
            onSearch = { viewModel.sendEvent(MainEvent.SearchChannels) },
            onChannelSelected = { login: String, profileImageUrl: String, displayName: String ->
                viewModel.sendEvent(MainEvent.AddChannel(login, profileImageUrl, displayName))
            },
            onOpenSpecialTab = { tab ->
                when (tab) {
                    OpenChannelTab.LIVE -> viewModel.sendEvent(MainEvent.OpenMonitorTab("/live"))
                    OpenChannelTab.AUTOMOD -> viewModel.sendEvent(MainEvent.OpenMonitorTab("/automod"))
                    else -> {}
                }
                viewModel.sendEvent(MainEvent.HideAddChannelDialog)
            },
            onDismiss = { viewModel.sendEvent(MainEvent.HideAddChannelDialog) }
        )
    }

    if (state.isCreateFolderDialogVisible) {
        CreateFolderDialog(
            name = state.newFolderName,
            colorHex = state.newFolderColor,
            onNameChange = { name: String -> viewModel.sendEvent(MainEvent.UpdateNewFolderName(name)) },
            onColorChange = { hex: String -> viewModel.sendEvent(MainEvent.UpdateNewFolderColor(hex)) },
            onCreate = { viewModel.sendEvent(MainEvent.CreateFolder) },
            onDismiss = { viewModel.sendEvent(MainEvent.HideCreateFolderDialog) },
            seedChannelLogin = state.newFolderSeedChannel
        )
    }

    if (state.editingFolderId != null) {
        EditFolderDialog(
            name = state.editFolderName,
            colorHex = state.editFolderColor,
            onNameChange = { name: String -> viewModel.sendEvent(MainEvent.UpdateEditFolderName(name)) },
            onColorChange = { hex: String -> viewModel.sendEvent(MainEvent.UpdateEditFolderColor(hex)) },
            onSave = { viewModel.sendEvent(MainEvent.SaveFolderEdit) },
            onDismiss = { viewModel.sendEvent(MainEvent.HideEditFolderDialog) }
        )
    }

    if (isHandheld) {
        BrowseOverlay(
            visible = state.showBrowse,
            onClose = { viewModel.sendEvent(MainEvent.HideBrowse) },
            onOpen = { request ->
                viewModel.sendEvent(
                    MainEvent.OpenRecentChannel(
                        request.login,
                        request.displayName,
                        request.avatarUrl
                    )
                )
                if (request.live) streamHost?.onEvent(StreamPlayerEvent.Open(request.login))
            }
        )
    }

    if (streamUi != null && streamUi.inPictureInPicture) {
        Box(Modifier.fillMaxSize().zIndex(1000f)) {
            streamUi.PictureInPicturePlayer()
        }
    }
}

@Composable
private fun BoxScope.CompactChannelDrawer(
    state: MainState,
    viewModel: MainViewModel,
    wallpaperActive: Boolean,
    glowEnabled: Boolean,
    dominantColor: Color,
    onOpenBrowse: (() -> Unit)?
) {
    AnimatedVisibility(
        visible = state.sidebarExpanded,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .directionalSwipe(SwipeDirection.Left) { viewModel.sendEvent(MainEvent.CloseSidebar) }
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { viewModel.sendEvent(MainEvent.CloseSidebar) }
        )
    }

    AnimatedVisibility(
        visible = state.sidebarExpanded,
        enter = slideInHorizontally(tween(250)) + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(250)) + fadeOut(tween(200)),
        modifier = Modifier.align(Alignment.TopStart)
    ) {
        Box(Modifier.directionalSwipe(SwipeDirection.Left) { viewModel.sendEvent(MainEvent.CloseSidebar) }) {
            ChannelSidebar(
                state = state,
                onEvent = { event: MainEvent -> viewModel.sendEvent(event) },
                isWideScreen = false,
                mentionMuteRepository = viewModel.mentionMuteRepository,
                onOpenBrowse = onOpenBrowse
            )
            if (wallpaperActive && glowEnabled) {
                WallpaperGlowEdge(
                    dominantColor = dominantColor,
                    fromRight = true,
                    glowWidth = 100.dp,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}
