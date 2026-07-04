package io.rudione.chatone.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.*
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
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.folder_outline
import coil3.compose.AsyncImage
import io.rudione.chatone.domain.model.Channel
import io.rudione.chatone.presentation.chat.ChatScreen
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.chat.pauseHotkeyMatches
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsScreen
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.GlobalKeyDispatcher
import io.rudione.chatone.util.HotkeyAction
import io.rudione.chatone.util.comboFor
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.ChatoneColors
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperGlowEdge
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.util.WallpaperLoader
import io.rudione.chatone.util.handleHover
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.hypot
import io.rudione.chatone.presentation.chat.components.ChattersPanel
import io.rudione.chatone.presentation.main.components.MentionsFeed
import io.rudione.chatone.presentation.main.components.MentionTabsBar
import io.rudione.chatone.presentation.main.components.MentionToast
import io.rudione.chatone.presentation.settings.DetachedSettingsWindow
import io.rudione.chatone.presentation.settings.SettingsEffect
import io.rudione.chatone.presentation.chat.multichat.MultiChatRootSetup
import io.rudione.chatone.presentation.account.AccountAutoConnectEffect
import io.rudione.chatone.presentation.account.AccountManager
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.MultiAccountConnectionRegistry
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.chat.multichat.MainScreenChatRouter

private data class ItemBounds(val id: String, val rect: Rect)

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


    MultiChatRootSetup()
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        if (state.showSettings && isWideScreenForSettings) {
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
            val isWideScreen = maxWidth >= 725.dp
            LaunchedEffect(isWideScreen) { isWideScreenForSettings = isWideScreen }

            val chatContent = remember {
                movableContentOf<String, Boolean> { activeChannel, wide ->
                    ChatScreen(
                        channelLogin = activeChannel,
                        onNavigateBack = {
                            if (!wide) viewModel.sendEvent(MainEvent.ToggleSidebar)
                        },
                        modifier = Modifier.fillMaxSize(),
                        accessToken = state.selectedAccount?.accessToken ?: "",
                        currentUserId = state.selectedAccount?.userId ?: "",
                        currentUserLogin = state.selectedAccount?.login ?: "",
                        currentDisplayName = state.selectedAccount?.displayName ?: "",
                        onMentionDetected = { login: String ->
                            viewModel.sendEvent(MainEvent.IncrementMentionCount(login))
                        },
                        onMentionReceived = { entry ->
                            viewModel.sendEvent(MainEvent.AddMentionEntry(entry))
                        },
                        onOpenWhisper = { userId, username, displayName, avatarUrl, color ->
                            viewModel.sendEvent(
                                MainEvent.OpenWhisperWith(
                                    userId, username, displayName, avatarUrl, color
                                )
                            )
                        },
                        onChannelIdResolved = { channelId ->
                            viewModel.sendEvent(MainEvent.SetActiveChatChannelId(channelId))
                        },
                        isWideScreen = wide,
                        wallpaper = wallpaper,
                        mentionMuteRepository = viewModel.mentionMuteRepository,
                        pendingScrollMessageId = state.pendingScrollMessageId,
                        onScrollToMessageHandled = { viewModel.sendEvent(MainEvent.ClearPendingScrollMessage) }
                    )
                }
            }


            val multiPanelRenderer: @Composable (String, Boolean, Modifier) -> Unit =
                { panelChannel, isCompact, panelModifier ->

                    val perPanelViewModel: ChatViewModel =
                        koinViewModel(key = "panel-vm-${panelChannel.lowercase()}")
                    ChatScreen(
                        channelLogin = panelChannel,
                        onNavigateBack = {},
                        modifier = panelModifier,
                        accessToken = state.selectedAccount?.accessToken ?: "",
                        currentUserId = state.selectedAccount?.userId ?: "",
                        currentUserLogin = state.selectedAccount?.login ?: "",
                        currentDisplayName = state.selectedAccount?.displayName ?: "",
                        onMentionDetected = { login: String ->
                            viewModel.sendEvent(MainEvent.IncrementMentionCount(login))
                        },
                        onMentionReceived = { entry ->
                            viewModel.sendEvent(MainEvent.AddMentionEntry(entry))
                        },
                        onOpenWhisper = { userId, username, displayName, avatarUrl, color ->
                            viewModel.sendEvent(
                                MainEvent.OpenWhisperWith(userId, username, displayName, avatarUrl, color)
                            )
                        },
                        onChannelIdResolved = { _ -> },
                        isWideScreen = false,
                        wallpaper = wallpaper,
                        mentionMuteRepository = viewModel.mentionMuteRepository,
                        renderBackground = false,
                        isMultiChat = state.openChannels.size > 1,
                        viewModel = perPanelViewModel,
                        pendingScrollMessageId = state.pendingScrollMessageId
                            .takeIf { panelChannel.equals(state.activeChannelLogin, ignoreCase = true) },
                        onScrollToMessageHandled = { viewModel.sendEvent(MainEvent.ClearPendingScrollMessage) }
                    )
                }

            val mentionsPanelShowing = !settingsState.navigationHidden && settingsState.mentionTabsEnabled &&
                state.mentions.any { !it.isRead }

            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box {
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
                                        mentionMuteRepository = viewModel.mentionMuteRepository
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
                    Column(modifier = Modifier.weight(1f)) {
                        if (mentionsPanelShowing) {
                            MentionTabsBar(
                                mentions = state.mentions,
                                activeLogin = state.activeChannelLogin,
                                onSelect = { login, messageId ->
                                    viewModel.sendEvent(MainEvent.SelectChannel(login, messageId))
                                    viewModel.sendEvent(MainEvent.MarkChannelMentionsRead(login))
                                }
                            )
                        }
                        val showTabBar =
                            !settingsState.navigationHidden && !mentionsPanelShowing &&
                                    settingsState.channelNavigation != SettingsState.ChannelNavigation.MINI_RAIL
                        if (showTabBar && state.openChannels.size > 1) {
                            Box {
                                ChannelTabBar(
                                    channels = state.openChannels,
                                    activeLogin = state.activeChannelLogin,
                                    onSelect = { login: String ->
                                        viewModel.sendEvent(
                                            MainEvent.SelectChannel(
                                                login
                                            )
                                        )
                                    },
                                    onClose = { login: String ->
                                        viewModel.sendEvent(
                                            MainEvent.CloseChannel(
                                                login
                                            )
                                        )
                                    },
                                    onReorder = { from: Int, to: Int ->
                                        viewModel.sendEvent(MainEvent.ReorderChannels(from, to))
                                    },
                                    folders = state.folders,
                                    onMoveToFolder = { login, folderId ->
                                        viewModel.sendEvent(MainEvent.MoveChannelToFolder(login, folderId))
                                    }
                                )
                                if (wallpaper.isActive && wallpaper.glowEffectsEnabled) {
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
                        if (state.mentionsChannelActive) {
                            Box(modifier = Modifier.weight(1f)) {
                                MentionsFeed(
                                    state = state,
                                    onEvent = { viewModel.sendEvent(it) },
                                    onChannelClick = { login, messageId ->
                                        viewModel.sendEvent(MainEvent.SelectChannel(login, messageId))
                                    },
                                    fillAvailableSpace = true,
                                    onCloseClick = { viewModel.sendEvent(MainEvent.CloseMentionsChannel) }
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
                                    singleChatRenderer = { ch, wide -> chatContent(ch, wide) },
                                    multiChatRenderer = multiPanelRenderer
                                )
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                EmptyState(
                                    isGuest = state.isGuest,
                                    onAddChannel = { viewModel.sendEvent(MainEvent.ShowAddChannelDialog) },
                                    onLogin = { viewModel.sendEvent(MainEvent.NavigateToAuth) }
                                )
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (mentionsPanelShowing) {
                        MentionTabsBar(
                            mentions = state.mentions,
                            activeLogin = state.activeChannelLogin,
                            onSelect = { login, messageId ->
                                viewModel.sendEvent(MainEvent.SelectChannel(login, messageId))
                                viewModel.sendEvent(MainEvent.MarkChannelMentionsRead(login))
                            }
                        )
                    }
                    val showTabBar =
                        !settingsState.navigationHidden && !mentionsPanelShowing &&
                                settingsState.channelNavigation != SettingsState.ChannelNavigation.MINI_RAIL
                    if (showTabBar && state.openChannels.size > 1) {
                        Box {
                            ChannelTabBar(
                                channels = state.openChannels,
                                activeLogin = state.activeChannelLogin,
                                onSelect = { login: String ->
                                    viewModel.sendEvent(
                                        MainEvent.SelectChannel(
                                            login
                                        )
                                    )
                                },
                                onClose = { login: String ->
                                    viewModel.sendEvent(
                                        MainEvent.CloseChannel(
                                            login
                                        )
                                    )
                                },
                                onReorder = { from: Int, to: Int ->
                                    viewModel.sendEvent(MainEvent.ReorderChannels(from, to))
                                },
                                folders = state.folders,
                                onMoveToFolder = { login, folderId ->
                                    viewModel.sendEvent(MainEvent.MoveChannelToFolder(login, folderId))
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
                    if (state.mentionsChannelActive) {
                        Box(modifier = Modifier.weight(1f)) {
                            MentionsFeed(
                                state = state,
                                onEvent = { viewModel.sendEvent(it) },
                                onChannelClick = { login, messageId ->
                                    viewModel.sendEvent(MainEvent.SelectChannel(login, messageId))
                                },
                                fillAvailableSpace = true,
                                onCloseClick = { viewModel.sendEvent(MainEvent.CloseMentionsChannel) }
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
                                isWideScreen = false,
                                singleChatRenderer = { ch, wide -> chatContent(ch, wide) },
                                multiChatRenderer = multiPanelRenderer
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            EmptyState(
                                isGuest = state.isGuest,
                                onAddChannel = { viewModel.sendEvent(MainEvent.ShowAddChannelDialog) },
                                onLogin = { viewModel.sendEvent(MainEvent.NavigateToAuth) }
                            )
                        }
                    }
                }

                val showMiniRail =
                    !mentionsPanelShowing &&
                        settingsState.channelNavigation != SettingsState.ChannelNavigation.TAB_BAR
                if (showMiniRail && !state.sidebarExpanded) {
                    Box {
                        MiniRail(
                            state = state,
                            onEvent = { event: MainEvent -> viewModel.sendEvent(event) },
                            modifier = Modifier.align(Alignment.TopStart)
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
                    visible = state.sidebarExpanded,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
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
                    Box {
                        ChannelSidebar(
                            state = state,
                            onEvent = { event: MainEvent -> viewModel.sendEvent(event) },
                            isWideScreen = false,
                            mentionMuteRepository = viewModel.mentionMuteRepository
                        )
                        if (wallpaper.isActive && wallpaper.glowEffectsEnabled) {
                            WallpaperGlowEdge(
                                dominantColor = wallpaper.dominantColor,
                                fromRight = true,
                                glowWidth = 100.dp,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
            }
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
                onChannelClick = { login, messageId -> viewModel.sendEvent(MainEvent.SelectChannel(login, messageId)) },
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
        AddChannelDialog(
            query = state.addChannelQuery,
            searchResults = state.searchResults,
            isSearching = state.isSearching,
            onQueryChange = { query: String ->
                viewModel.sendEvent(
                    MainEvent.UpdateAddChannelQuery(
                        query
                    )
                )
            },
            onSearch = { viewModel.sendEvent(MainEvent.SearchChannels) },
            onChannelSelected = { login: String, profileImageUrl: String, displayName: String ->
                viewModel.sendEvent(MainEvent.AddChannel(login, profileImageUrl, displayName))
            },
            onDismiss = { viewModel.sendEvent(MainEvent.HideAddChannelDialog) }
        )
    }

    if (state.isCreateFolderDialogVisible) {
        CreateFolderDialog(
            name = state.newFolderName,
            onNameChange = { name: String -> viewModel.sendEvent(MainEvent.UpdateNewFolderName(name)) },
            onCreate = { viewModel.sendEvent(MainEvent.CreateFolder) },
            onDismiss = { viewModel.sendEvent(MainEvent.HideCreateFolderDialog) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiniRail(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = ChatoneTheme.extraColors
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val railScrollState = rememberLazyListState()
    val density = LocalDensity.current

    var railDragLogin by remember { mutableStateOf<String?>(null) }
    var railDragFromIndex by remember { mutableStateOf<Int?>(null) }
    var railDragOffset by remember { mutableStateOf(Offset.Zero) }
    var railDragStartCenterX by remember { mutableStateOf(0f) }
    var railDropTargetLogin by remember { mutableStateOf<String?>(null) }
    val s = LocalStrings.current
    val railItemCenters = remember { mutableStateMapOf<String, Float>() }
    val miniRailCollapsedFolders = remember { mutableStateListOf<String>() }
    val tooltipOffsetYPx = with(density) { 28.dp.roundToPx() }
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled

    LiquidGlassSurface(
        modifier = modifier.padding(8.dp).then(
            if (glowEnabled) Modifier.shadow(
                8.dp,
                RoundedCornerShape(16.dp),
                ambientColor = extra.shadowColor,
                spotColor = extra.elevatedShadow
            ) else Modifier
        ),
        contentPadding = PaddingValues(2.dp),
        backgroundAlphaHigh = 0.95f,
        backgroundAlphaLow = 0.88f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = { onEvent(MainEvent.ToggleSidebar) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = LocalStrings.current.mainOpenSidebar,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            LazyRow(
                state = railScrollState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                    if (delta != 0f) {
                                        coroutineScope.launch {
                                            railScrollState.scrollBy(delta * 5f)
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val railNodes: List<Any> = run {
                    val out = mutableListOf<Any>()
                    val openByKey = state.openChannels.associateBy { it.login.lowercase().removePrefix("#") }
                    val emitted = mutableSetOf<String>()
                    state.folders.forEach { f ->
                        out.add(f)
                        if (f.id !in miniRailCollapsedFolders) {
                            f.channels.forEach { fc ->
                                val key = fc.login.lowercase().removePrefix("#")
                                openByKey[key]?.let { out.add(it) }
                                emitted.add(key)
                            }
                        } else {
                            f.channels.forEach { emitted.add(it.login.lowercase().removePrefix("#")) }
                        }
                    }
                    state.openChannels.forEach { ch ->
                        val key = ch.login.lowercase().removePrefix("#")
                        if (key !in emitted) out.add(ch)
                    }
                    out
                }
                val channelFolderTint: Map<String, Color> = run {
                    val map = mutableMapOf<String, Color>()
                    state.folders.forEach { f ->
                        val tint = runCatching {
                            Color(f.color.removePrefix("#").toLong(16) or 0xFF000000L)
                        }.getOrDefault(Color(0xFF9146FF))
                        f.channels.forEach { fc ->
                            map[fc.login.lowercase().removePrefix("#")] = tint
                        }
                    }
                    map
                }
                items(
                    railNodes,
                    key = { node ->
                        when (node) {
                            is ChannelFolder -> "rail_folder_${node.id}"
                            is ChannelTab -> "rail_ch_${node.login}"
                            else -> node.hashCode().toString()
                        }
                    }
                ) { node ->
                    if (node is ChannelFolder) {
                        val folder = node
                        val isCollapsed = folder.id in miniRailCollapsedFolders
                        val parsedColor = runCatching {
                            Color(folder.color.removePrefix("#").toLong(16) or 0xFF000000L)
                        }.getOrDefault(Color(0xFF9146FF))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isCollapsed) miniRailCollapsedFolders.remove(folder.id)
                                    else miniRailCollapsedFolders.add(folder.id)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor.copy(alpha = 0.18f))
                                    .border(1.dp, parsedColor.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        Res.drawable.folder_outline
                                    ),
                                    contentDescription = folder.name,
                                    tint = parsedColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = folder.name.take(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = parsedColor,
                                fontSize = 8.sp,
                                maxLines = 1
                            )
                        }
                        return@items
                    }
                    val channel = node as ChannelTab
                    val isActive = channel.login == state.activeChannelLogin
                    var showTooltip by remember { mutableStateOf(false) }
                    var tooltipOffset by remember { mutableStateOf(IntOffset.Zero) }
                    val isDraggedItem = railDragLogin == channel.login
                    val isDragTarget = railDropTargetLogin == channel.login && !isDraggedItem
                    val itemCenterX = remember { mutableStateOf(0f) }
                    val folderTint = channelFolderTint[channel.login.lowercase().removePrefix("#")]

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .then(
                                if (folderTint != null) Modifier.drawBehind {
                                    val lineHeight = 2.dp.toPx()
                                    drawRect(
                                        color = folderTint,
                                        topLeft = Offset(0f, size.height - lineHeight),
                                        size = androidx.compose.ui.geometry.Size(size.width, lineHeight)
                                    )
                                } else Modifier
                            )
                            .onGloballyPositioned { coords ->
                                val bounds = coords.boundsInRoot()
                                val cx = bounds.left + bounds.width / 2f
                                railItemCenters[channel.login] = cx
                                itemCenterX.value = cx
                            }
                            .pointerInput(channel.login) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        railDragLogin = channel.login
                                        railDragOffset = Offset.Zero
                                        railDragStartCenterX = itemCenterX.value
                                        railDropTargetLogin = null
                                        railDragFromIndex =
                                            state.openChannels.indexOfFirst { it.login == channel.login }
                                    },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        railDragOffset += delta
                                        val currentX = railDragStartCenterX + railDragOffset.x
                                        val itemHalfWidthPx = with(density) { 19.dp.toPx() }
                                        val closest = railItemCenters.entries
                                            .filter { (login, _) -> login != channel.login }
                                            .minByOrNull { (_, cx) -> kotlin.math.abs(cx - currentX) }?.key
                                        val nearestCx = closest?.let { railItemCenters[it] }
                                        railDropTargetLogin =
                                            if (closest != null && nearestCx != null &&
                                                kotlin.math.abs(nearestCx - currentX) < itemHalfWidthPx
                                            ) closest else null
                                    },
                                    onDragEnd = {
                                        val fromIdx = railDragFromIndex
                                        val toLogin = railDropTargetLogin
                                        val toIdx = if (toLogin != null)
                                            state.openChannels.indexOfFirst { it.login == toLogin }
                                                .takeIf { it >= 0 }
                                        else null
                                        if (fromIdx != null && toIdx != null && toIdx != fromIdx) {
                                            onEvent(MainEvent.ReorderChannels(fromIdx, toIdx))
                                        }
                                        railDragLogin = null
                                        railDragOffset = Offset.Zero
                                        railDragStartCenterX = 0f
                                        railDropTargetLogin = null
                                        railDragFromIndex = null
                                    },
                                    onDragCancel = {
                                        railDragLogin = null
                                        railDragOffset = Offset.Zero
                                        railDragStartCenterX = 0f
                                        railDropTargetLogin = null
                                        railDragFromIndex = null
                                    }
                                )
                            }
                            .pointerInput(channel.login) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Enter -> {
                                                val pos = event.changes.firstOrNull()?.position
                                                    ?: Offset.Zero
                                                tooltipOffset = IntOffset(
                                                    pos.x.toInt() - 35,
                                                    pos.y.toInt() - tooltipOffsetYPx
                                                )
                                                showTooltip = true
                                            }

                                            PointerEventType.Move -> {
                                                val pos = event.changes.firstOrNull()?.position
                                                    ?: Offset.Zero
                                                tooltipOffset = IntOffset(
                                                    pos.x.toInt() - 35,
                                                    pos.y.toInt() - tooltipOffsetYPx
                                                )
                                            }

                                            PointerEventType.Exit -> showTooltip = false
                                            PointerEventType.Press -> if (event.buttons.isSecondaryPressed) {
                                                try {
                                                    uriHandler.openUri("https://www.twitch.tv/${channel.login}")
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDraggedItem -> MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.35f
                                        )

                                        isDragTarget -> MaterialTheme.colorScheme.secondary.copy(
                                            alpha = 0.25f
                                        )

                                        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onEvent(MainEvent.SelectChannel(channel.login)) }
                                .then(
                                    if (isActive || isDraggedItem) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.profileImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = channel.profileImageUrl,
                                    contentDescription = channel.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(26.dp).clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = channel.displayName.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (channel.isLive) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp).size(10.dp).clip(CircleShape)
                                    .background(ChatoneTheme.extraColors.live)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .zIndex(2f)
                            )
                        }
                        if (channel.unreadCount > 0) {
                            Box(
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp).size(12.dp).clip(CircleShape)
                                    .background(Color.Red)
                                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .zIndex(2f), contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (channel.unreadCount > 9) "9+" else "${channel.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                    color = Color.White
                                )
                            }
                        }
                        if (showTooltip) {
                            Popup(
                                alignment = Alignment.TopStart,
                                offset = tooltipOffset,
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = false,
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .shadow(4.dp, RoundedCornerShape(6.dp))
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    event.changes.forEach { it.consumeAllChanges() }
                                                }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = channel.displayName.ifBlank { channel.login.replaceFirstChar { char -> char.uppercaseChar() } },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = s.chatMentionsTab,
                        tint = if (state.unreadMentionsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (state.unreadMentionsCount > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                            .size(14.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .zIndex(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.unreadMentionsCount > 9) "9+" else "${state.unreadMentionsCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = Color.White
                        )
                    }
                }
            }

            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { onEvent(MainEvent.ToggleWhisperPanel) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.MailOutline,
                        contentDescription = s.chatWhisperTab,
                        tint = if (state.totalUnreadWhispers > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (state.totalUnreadWhispers > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                            .size(14.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .zIndex(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.totalUnreadWhispers > 9) "9+" else "${state.totalUnreadWhispers}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = Color.White
                        )
                    }
                }
            }
            IconButton(
                onClick = { onEvent(MainEvent.ShowAddChannelDialog) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = s.addChannel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit
) {
    val extra = ChatoneTheme.extraColors
    val expandedFolders = remember { mutableStateListOf<String>() }
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled
    val compactShape =
        if (glowEnabled) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        else RoundedCornerShape(0.dp)
    val compactDivider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .then(
                if (glowEnabled) Modifier.shadow(
                    12.dp,
                    compactShape,
                    ambientColor = extra.shadowColor,
                    spotColor = extra.elevatedShadow
                ) else Modifier
            )
            .clip(compactShape)
            .background(
                if (glowEnabled) extra.sidebarSurface
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .then(
                if (glowEnabled) Modifier.border(1.dp, extra.glassBorder, compactShape)
                else Modifier.drawBehind {
                    drawRect(
                        color = compactDivider,
                        topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                        size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height)
                    )
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        IconButton(
            onClick = { onEvent(MainEvent.ToggleSidebarCollapsed) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = LocalStrings.current.mainExpandSidebar,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        val folderedLogins = state.folders.flatMap { it.channels }.map { it.login }.toSet()
        val filteredUnfoldered = state.unfolderedChannels.filter { it.login !in folderedLogins }
        val s = LocalStrings.current

        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(state.folders, key = { _, f -> "cf_${f.id}" }) { _, folder ->
                val isExpanded = folder.id in expandedFolders
                val folderColor = try {
                    val c = folder.color.removePrefix("#").toLong(16)
                    Color(
                        red = ((c shr 16) and 0xFF) / 255f,
                        green = ((c shr 8) and 0xFF) / 255f,
                        blue = (c and 0xFF) / 255f
                    )
                } catch (_: Exception) {
                    ChatoneColors.Violet400
                }

                val hasFolderActive = folder.channels.any { it.login == state.activeChannelLogin }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(52.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (hasFolderActive) folderColor.copy(alpha = 0.2f)
                                    else if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                    else Color.Transparent
                                )
                                .then(
                                    if (hasFolderActive) Modifier.border(
                                        2.dp,
                                        folderColor,
                                        RoundedCornerShape(10.dp)
                                    )
                                    else Modifier
                                )
                                .clickable {
                                    if (isExpanded) expandedFolders.remove(folder.id)
                                    else expandedFolders.add(folder.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(Res.drawable.folder_outline),
                                contentDescription = folder.name,
                                modifier = Modifier.size(20.dp),
                                tint = folderColor
                            )
                        }
                        if (folder.channels.count { it.isLive } > 0) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(10.dp).clip(CircleShape)
                                    .background(ChatoneTheme.extraColors.live)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }

                    Text(
                        text = folder.name.take(8),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = if (hasFolderActive) folderColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                    )
                }

                if (isExpanded) {
                    folder.channels.forEach { channel ->
                        val isActive = channel.login == state.activeChannelLogin
                        CompactChannelAvatar(
                            channel = channel,
                            isActive = isActive,
                            onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                            indented = true
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    Spacer(Modifier.height(2.dp))
                }
            }

            items(filteredUnfoldered, key = { "cu_${it.login}" }) { channel ->
                val isActive = channel.login == state.activeChannelLogin
                CompactChannelAvatar(
                    channel = channel,
                    isActive = isActive,
                    onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                    indented = false
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = s.chatMentionsTab,
                    tint = if (state.unreadMentionsCount > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (state.unreadMentionsCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                        .size(14.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state.unreadMentionsCount > 9) "9+" else "${state.unreadMentionsCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = Color.White
                    )
                }
            }
        }
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = { onEvent(MainEvent.ToggleWhisperPanel) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.MailOutline,
                    contentDescription = s.chatWhisperTab,
                    tint = if (state.totalUnreadWhispers > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (state.totalUnreadWhispers > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp)
                        .size(14.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state.totalUnreadWhispers > 9) "9+" else "${state.totalUnreadWhispers}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = Color.White
                    )
                }
            }
        }
        IconButton(
            onClick = { onEvent(MainEvent.ShowSettings) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = s.settingsTitle,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CompactChannelAvatar(
    channel: ChannelTab,
    isActive: Boolean,
    onClick: () -> Unit,
    indented: Boolean = false
) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth().padding(start = if (indented) 8.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .then(
                        if (isActive) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        else Modifier
                    )
                    .clickable { onClick() }
                    .pointerInput(channel.login) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> showTooltip = true
                                    PointerEventType.Exit -> showTooltip = false
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (channel.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = channel.profileImageUrl,
                        contentDescription = channel.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(30.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = channel.displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (channel.isLive) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(10.dp).clip(CircleShape)
                        .background(ChatoneTheme.extraColors.live)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
            if (channel.unreadCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(14.dp).clip(CircleShape)
                        .background(Color.Red)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (channel.unreadCount > 9) "9+" else "${channel.unreadCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = Color.White
                    )
                }
            }
        }
        if (showTooltip) {
            Popup(
                alignment = Alignment.CenterEnd,
                offset = IntOffset(28, 0),
                properties = androidx.compose.ui.window.PopupProperties(focusable = false)
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = channel.displayName.ifBlank { channel.login.replaceFirstChar { it.uppercaseChar() } },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChannelSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    isWideScreen: Boolean = false,
    mentionMuteRepository: MentionMuteRepository? = null
) {
    val extra = ChatoneTheme.extraColors
    val density = LocalDensity.current

    var draggedChannelLogin by remember { mutableStateOf<String?>(null) }
    var draggedChannelSourceFolderId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dropTargetFolderId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragOverIndex by remember { mutableStateOf<Int?>(null) }
    var sidebarTopLeftInRoot by remember { mutableStateOf(Offset.Zero) }

    val folderBounds = remember { mutableStateListOf<ItemBounds>() }
    val channelRectMap = remember { mutableStateMapOf<String, Rect>() }
    val channelBounds = remember { mutableStateListOf<ItemBounds>() }
    val s = LocalStrings.current
    val folderedLogins = state.folders.flatMap { it.channels }.map { it.login }.toSet()
    val filteredUnfoldered = state.unfolderedChannels.filter { it.login !in folderedLogins }
    val glowEnabled = LocalWallpaperController.current.state.glowEffectsEnabled
    val floating = glowEnabled && isWideScreen
    val sidebarShape = when {
        floating -> RoundedCornerShape(18.dp)
        glowEnabled -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(0.dp)
    }
    val sidebarDivider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(
        modifier = Modifier
            .width(256.dp)
            .fillMaxHeight()
            .then(if (floating) Modifier.padding(8.dp) else Modifier)
            .then(
                if (floating) Modifier.shadow(
                    16.dp,
                    sidebarShape,
                    ambientColor = extra.shadowColor,
                    spotColor = extra.elevatedShadow
                ) else Modifier
            )
            .clip(sidebarShape)
            .background(
                if (glowEnabled) extra.sidebarSurface
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .then(
                if (glowEnabled) Modifier.border(1.dp, extra.glassBorder, sidebarShape)
                else Modifier.drawBehind {
                    drawRect(
                        color = sidebarDivider,
                        topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                        size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height)
                    )
                }
            )
            .onGloballyPositioned { sidebarTopLeftInRoot = it.positionInRoot() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chatone",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isWideScreen) {
                IconButton(
                    onClick = { onEvent(MainEvent.ToggleSidebarCollapsed) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (state.sidebarCollapsed)
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft
                        else Icons.AutoMirrored.Default.KeyboardArrowRight,
                        contentDescription = LocalStrings.current.mainCollapseSidebar,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { onEvent(MainEvent.CloseSidebar) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        state.selectedAccount?.let { account ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (account.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = account.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.activeChannelLogin != null) {
                        Text(
                            text = "#${state.activeChannelLogin}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                if (state.activeChannelLogin != null) {
                    val isBroadcaster = state.activeChannelLogin.equals(account.login, ignoreCase = true)
                    val isModForActive = state.activeChatChannelId.isNotEmpty() &&
                            state.activeChatChannelId in state.moderatedChannelIds
                    Box {
                        var showChannelMenu by remember { mutableStateOf(false) }
                        var showBadgesBitsStub by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showChannelMenu = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = LocalStrings.current.mainChannelOptions,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (showBadgesBitsStub) {
                            AlertDialog(
                                onDismissRequest = { showBadgesBitsStub = false },
                                title = { Text(LocalStrings.current.mainChannelBadgesBits) },
                                text = { Text(LocalStrings.current.mainChannelBadgesBitsSoon) },
                                confirmButton = {
                                    TextButton(onClick = { showBadgesBitsStub = false }) { Text("OK") }
                                }
                            )
                        }
                        val activeLogin = state.activeChannelLogin ?: ""
                        val isChannelMuted = remember(activeLogin) {
                            mentionMuteRepository?.isChannelMuted(activeLogin) ?: false
                        }
                        var channelMuted by remember(activeLogin) { mutableStateOf(isChannelMuted) }
                        DropdownMenu(
                            expanded = showChannelMenu,
                            onDismissRequest = { showChannelMenu = false }) {
                            if (isBroadcaster || isModForActive) {
                                DropdownMenuItem(
                                    text = { Text(LocalStrings.current.mainViewersList) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Person,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        showChannelMenu = false; onEvent(MainEvent.ShowChattersPanel)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (channelMuted)
                                            LocalStrings.current.unmuteMentionsForChannel
                                        else
                                            LocalStrings.current.muteMentionsForChannel
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (channelMuted) Icons.Filled.Notifications
                                        else Icons.Outlined.NotificationsOff,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showChannelMenu = false
                                    if (channelMuted) {
                                        onEvent(MainEvent.UnmuteMentionsChannel(activeLogin))
                                        channelMuted = false
                                    } else {
                                        onEvent(MainEvent.MuteMentionsChannel(activeLogin))
                                        channelMuted = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(LocalStrings.current.mainChannelBadgesBits) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showChannelMenu = false
                                    showBadgesBitsStub = true
                                }
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (state.isConnected) ChatoneTheme.extraColors.connected else MaterialTheme.colorScheme.error)
                )
            }
        } ?: run {
            if (state.isGuest) {
                TextButton(
                    onClick = { onEvent(MainEvent.NavigateToAuth) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(LocalStrings.current.mainLoginToTwitch)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .pointerInput(isDragging) {
                    if (isDragging) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.Top
        ) {
            itemsIndexed(
                state.folders,
                key = { _: Int, f: ChannelFolder -> "folder_${f.id}" }
            ) { folderIndex: Int, folder: ChannelFolder ->
                FolderItem(
                    folder = folder,
                    activeChannelLogin = state.activeChannelLogin,
                    allFolders = state.folders,
                    unfolderedChannels = filteredUnfoldered,
                    onToggle = { onEvent(MainEvent.ToggleFolder(folder.id)) },
                    onChannelSelect = { login -> onEvent(MainEvent.SelectChannel(login)) },
                    onChannelClose = { login -> onEvent(MainEvent.CloseChannel(login)) },
                    onMoveChannel = { login, targetFolderId ->
                        onEvent(MainEvent.MoveChannelToFolder(login, targetFolderId))
                    },
                    onDelete = { onEvent(MainEvent.DeleteFolder(folder.id)) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coords ->
                            val rect = coords.boundsInRoot()
                            folderBounds.removeAll { it.id == folder.id }
                            folderBounds.add(ItemBounds(folder.id, rect))
                        },
                    isDropTarget = dropTargetFolderId == folder.id && draggedChannelLogin != null
                )

                if (folder.isExpanded) {
                    folder.channels.forEachIndexed { channelIndex, channel ->
                        ChannelItemWithDrag(
                            channel = channel,
                            isActive = channel.login == state.activeChannelLogin,
                            folders = state.folders,
                            currentFolderId = folder.id,
                            onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                            onClose = { onEvent(MainEvent.CloseChannel(channel.login)) },
                            onMoveToFolder = { folderId ->
                                onEvent(MainEvent.MoveChannelToFolder(channel.login, folderId))
                            },
                            liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                            onToggleLiveNotify = { onEvent(MainEvent.ToggleLiveNotify(channel.login)) },
                            index = channelIndex,
                            onDragStart = { rootPos ->
                                draggedChannelLogin = channel.login
                                draggedChannelSourceFolderId = folder.id
                                dragStartOffsetPx = rootPos - sidebarTopLeftInRoot
                                dragOffsetPx = Offset.Zero
                                isDragging = true
                                dragStartIndex = channelIndex
                                dragOverIndex = null
                            },
                            onDrag = { delta ->
                                dragOffsetPx += delta
                                val currentPos = dragStartOffsetPx + dragOffsetPx

                                dropTargetFolderId = folderBounds.find { it.rect.contains(currentPos) }?.id

                                val hoveredLogin = channelRectMap.entries
                                    .firstOrNull { (_, rect) -> rect.contains(currentPos) }?.key
                                dragOverIndex = if (hoveredLogin != null) {
                                    folder.channels.indexOfFirst { it.login == hoveredLogin }
                                        .takeIf { it >= 0 }
                                } else null
                            },
                            onDragEnd = {
                                val currentPos = dragStartOffsetPx + dragOffsetPx
                                val hitFolder = folderBounds.find { it.rect.contains(currentPos) }
                                when {
                                    hitFolder != null && hitFolder.id != folder.id && draggedChannelLogin != null -> {
                                        onEvent(MainEvent.MoveChannelToFolder(draggedChannelLogin!!, hitFolder.id))
                                    }
                                    dragOverIndex != null && dragStartIndex != null && dragOverIndex != dragStartIndex -> {
                                        onEvent(MainEvent.ReorderFolderChannels(folder.id, dragStartIndex!!, dragOverIndex!!))
                                    }
                                    hitFolder == null && dragOverIndex == null && draggedChannelLogin != null -> {
                                        onEvent(MainEvent.MoveChannelToFolder(draggedChannelLogin!!, null))
                                    }
                                }
                                draggedChannelLogin = null
                                draggedChannelSourceFolderId = null
                                dropTargetFolderId = null
                                dragOffsetPx = Offset.Zero
                                dragStartIndex = null
                                dragOverIndex = null
                                isDragging = false
                            },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .onGloballyPositioned { coords ->
                                    channelRectMap[channel.login] = coords.boundsInRoot()
                                }
                        )
                    }
                }
            }

            if (filteredUnfoldered.isNotEmpty()) {
                item(key = "unfoldered_header") {
                    Text(
                        LocalStrings.current.mainChannelsHeader,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            itemsIndexed(
                filteredUnfoldered,
                key = { _: Int, c: ChannelTab -> "channel_${c.login}" }
            ) { index: Int, channel: ChannelTab ->
                ChannelItemWithDrag(
                    channel = channel,
                    isActive = channel.login == state.activeChannelLogin,
                    folders = state.folders,
                    currentFolderId = null,
                    onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                    onClose = { onEvent(MainEvent.CloseChannel(channel.login)) },
                    onMoveToFolder = { folderId ->
                        onEvent(MainEvent.MoveChannelToFolder(channel.login, folderId))
                    },
                    liveNotifyEnabled = channel.login.lowercase() in state.liveNotifyChannels,
                    onToggleLiveNotify = { onEvent(MainEvent.ToggleLiveNotify(channel.login)) },
                    index = index,
                    onDragStart = { rootPos ->
                        draggedChannelLogin = channel.login
                        dragStartOffsetPx = rootPos - sidebarTopLeftInRoot
                        dragOffsetPx = Offset.Zero
                        isDragging = true
                        dragStartIndex =
                            filteredUnfoldered.indexOfFirst { it.login == channel.login }
                        dragOverIndex = null
                    },
                    onDrag = { delta ->
                        dragOffsetPx += delta
                        val currentPos = dragStartOffsetPx + dragOffsetPx

                        dropTargetFolderId = folderBounds.find { it.rect.contains(currentPos) }?.id


                        val hoveredLogin = channelRectMap.entries
                            .firstOrNull { (_, rect) -> rect.contains(currentPos) }?.key
                        dragOverIndex = if (hoveredLogin != null) {
                            filteredUnfoldered.indexOfFirst { it.login == hoveredLogin }
                                .takeIf { it >= 0 }
                        } else null
                    },
                    onDragEnd = {
                        val currentPos = dragStartOffsetPx + dragOffsetPx
                        val hitFolder = folderBounds.find { it.rect.contains(currentPos) }
                        if (hitFolder != null && draggedChannelLogin != null) {
                            onEvent(
                                MainEvent.DropChannelOnFolder(
                                    draggedChannelLogin!!,
                                    hitFolder.id
                                )
                            )
                        } else if (dragOverIndex != null && dragStartIndex != null && dragOverIndex != dragStartIndex) {
                            onEvent(
                                MainEvent.ReorderUnfolderedChannels(
                                    dragStartIndex!!,
                                    dragOverIndex!!
                                )
                            )
                        }
                        draggedChannelLogin = null
                        dropTargetFolderId = null
                        dragOffsetPx = Offset.Zero
                        dragStartIndex = null
                        dragOverIndex = null
                        isDragging = false
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coords ->
                            val rect = coords.boundsInRoot()
                            channelRectMap[channel.login] = rect
                            channelBounds.removeAll { it.id == "unfoldered_${channel.login}" }
                            channelBounds.add(ItemBounds("unfoldered_${channel.login}", rect))
                        }
                )
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(12.dp))
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            GradientButton(
                text = LocalStrings.current.sidebarAddChannelButton,
                onClick = { onEvent(MainEvent.ShowAddChannelDialog) },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                )
            )
            Spacer(Modifier.width(8.dp))

            GradientButton(
                text = LocalStrings.current.sidebarAddFolderButton,
                onClick = { onEvent(MainEvent.ShowCreateFolderDialog) },
                modifier = Modifier.weight(1f),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            )
        }

        TextButton(
            onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(38.dp)
        ) {
            Box {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                if (state.unreadMentionsCount > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                            .size(10.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.unreadMentionsCount > 9) "9+" else "${state.unreadMentionsCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(s.panelMentionsTitle)
            Spacer(Modifier.weight(1f))
            if (state.unreadMentionsCount > 0) {
                Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape) {
                    Text(
                        "${state.unreadMentionsCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
        TextButton(
            onClick = { onEvent(MainEvent.ToggleWhisperPanel) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(38.dp)
        ) {
            Box {
                Icon(
                    Icons.Filled.MailOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                if (state.totalUnreadWhispers > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                            .size(10.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.totalUnreadWhispers > 9) "9+" else "${state.totalUnreadWhispers}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(s.chatWhisperTab)
            Spacer(Modifier.weight(1f))
            if (state.totalUnreadWhispers > 0) {
                Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape) {
                    Text(
                        "${state.totalUnreadWhispers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
        TextButton(
            onClick = { onEvent(MainEvent.ShowSettings) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(38.dp)
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(s.settingsTitle)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
    }

    if (isDragging && draggedChannelLogin != null) {
        Box(
            modifier = Modifier
                .offset {
                    with(density) {
                        IntOffset(
                            (dragStartOffsetPx.x + dragOffsetPx.x).toInt(),
                            (dragStartOffsetPx.y + dragOffsetPx.y).toInt()
                        )
                    }
                }
                .zIndex(1000f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(16.dp, RoundedCornerShape(8.dp))
        ) {
            val ch = filteredUnfoldered.find { it.login == draggedChannelLogin }
                ?: state.folders.flatMap { it.channels }.find { it.login == draggedChannelLogin }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ch?.profileImageUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = ch.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = ch?.displayName ?: draggedChannelLogin!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderItem(
    folder: ChannelFolder,
    activeChannelLogin: String?,
    allFolders: List<ChannelFolder>,
    unfolderedChannels: List<ChannelTab> = emptyList(),
    onToggle: () -> Unit,
    onChannelSelect: (String) -> Unit,
    onChannelClose: (String) -> Unit,
    onMoveChannel: (String, String?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isDropTarget: Boolean = false
) {
    var showFolderMenu by remember { mutableStateOf(false) }
    var showAddToFolderMenu by remember { mutableStateOf(false) }

    Column {
        Box {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onToggle, onLongClick = { showFolderMenu = true })
                    .then(
                        if (isDropTarget) Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        ) else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                    showFolderMenu = true
                                }
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (folder.isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painterResource(if (folder.isExpanded) Res.drawable.folder_outline else Res.drawable.folder_outline),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = parseFolderColor(folder.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${folder.channels.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(
                        onClick = { showAddToFolderMenu = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = LocalStrings.current.mainAddChannelToFolder,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    DropdownMenu(
                        expanded = showAddToFolderMenu,
                        onDismissRequest = { showAddToFolderMenu = false }) {
                        if (unfolderedChannels.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        LocalStrings.current.mainNoChannelsToAdd,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = { showAddToFolderMenu = false },
                                enabled = false
                            )
                        } else {
                            unfolderedChannels.forEach { ch: ChannelTab ->
                                DropdownMenuItem(
                                    text = { Text("#${ch.displayName}") },
                                    onClick = {
                                        showAddToFolderMenu = false
                                        onMoveChannel(ch.login, folder.id)
                                    },
                                    leadingIcon = {
                                        Text(
                                            "#",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            DropdownMenu(
                expanded = showFolderMenu,
                onDismissRequest = { showFolderMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(LocalStrings.current.mainDeleteFolder) },
                    onClick = { showFolderMenu = false; onDelete() },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun ChannelItemWithDrag(
    modifier: Modifier,
    channel: ChannelTab,
    isActive: Boolean,
    folders: List<ChannelFolder> = emptyList(),
    currentFolderId: String? = null,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onMoveToFolder: (String?) -> Unit = {},
    liveNotifyEnabled: Boolean = false,
    onToggleLiveNotify: () -> Unit = {},
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    index: Int = -1
) {
    val uriHandler = LocalUriHandler.current
    var showContextMenu by remember { mutableStateOf(false) }
    var itemBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .pointerInput(channel.login) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        val itemTopLeft = itemBounds?.topLeft ?: Offset.Zero
                        onDragStart(itemTopLeft + localOffset)
                    },
                    onDrag = { change, amount ->
                        change.consumeAllChanges()
                        onDrag(amount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .onGloballyPositioned { coords ->
                itemBounds = coords.boundsInRoot()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isActive) ChatoneTheme.extraColors.sidebarSelected else Color.Transparent
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                try {
                                    uriHandler.openUri("https://www.twitch.tv/${channel.login}")
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel.isLive) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(ChatoneTheme.extraColors.live)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
            }
            if (channel.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = channel.profileImageUrl,
                    contentDescription = channel.displayName,
                    modifier = Modifier.size(22.dp).clip(CircleShape)
                )
            } else {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = channel.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (channel.unreadCount > 0) {
                Surface(
                    color = ChatoneTheme.extraColors.live,
                    shape = CircleShape,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = if (channel.unreadCount > 99) "99+" else "${channel.unreadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (folders.isNotEmpty()) {
                Box {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = LocalStrings.current.mainMoveToFolder,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    if (showContextMenu) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(0, 24),
                            properties = androidx.compose.ui.window.PopupProperties(
                                focusable = true,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            ),
                            onDismissRequest = { showContextMenu = false }
                        ) {
                            LiquidGlassSurface(
                                modifier = Modifier
                                    .width(200.dp)
                                    .zIndex(100f)
                                    .shadow(16.dp, RoundedCornerShape(12.dp)),
                                contentPadding = PaddingValues(4.dp),
                                backgroundAlphaHigh = 0.99f
                            ) {
                                Column {
                                    if (currentFolderId != null) {
                                        LiquidGlassDropdownItem(
                                            text = LocalStrings.current.mainRemoveFromFolder,
                                            icon = Icons.Outlined.Close,
                                            onClick = {
                                                showContextMenu = false
                                                onMoveToFolder(null)
                                            }
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    }
                                    folders.filter { it.id != currentFolderId }.forEach { folder ->
                                        LiquidGlassDropdownItem(
                                            text = folder.name,
                                            icon = Icons.AutoMirrored.Outlined.List,
                                            onClick = {
                                                showContextMenu = false
                                                onMoveToFolder(folder.id)
                                            }
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                    LiquidGlassDropdownItem(
                                        text = if (liveNotifyEnabled)
                                            LocalStrings.current.mainLiveNotifyOff
                                        else LocalStrings.current.mainLiveNotifyOn,
                                        icon = if (liveNotifyEnabled)
                                            Icons.Filled.Notifications
                                        else Icons.Outlined.Notifications,
                                        onClick = {
                                            showContextMenu = false
                                            onToggleLiveNotify()
                                        }
                                    )
                                    var showQualityMenu by remember { mutableStateOf(false) }
                                    if (!showQualityMenu) {
                                        LiquidGlassDropdownItem(
                                            text = LocalStrings.current.mainOpenInPlayer,
                                            icon = Icons.Outlined.PlayArrow,
                                            onClick = { showQualityMenu = true }
                                        )
                                    } else {
                                        listOf("best", "720p60", "480p", "audio_only").forEach { q ->
                                            LiquidGlassDropdownItem(
                                                text = "▶ $q",
                                                icon = Icons.Outlined.PlayArrow,
                                                onClick = {
                                                    showContextMenu = false
                                                    showQualityMenu = false
                                                    io.rudione.chatone.util.openInStreamlink(channel.login, q)
                                                }
                                            )
                                        }
                                    }
                                    LiquidGlassDropdownItem(
                                        text = LocalStrings.current.mainCloseChannel,
                                        icon = Icons.Outlined.Close,
                                        onClick = {
                                            showContextMenu = false
                                            onClose()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(2.dp))
            }
            if (isActive) {
                IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ChannelTabBar(
    channels: List<ChannelTab>,
    activeLogin: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    folders: List<ChannelFolder> = emptyList(),
    onMoveToFolder: (channelLogin: String, folderId: String) -> Unit = { _, _ -> }
) {
    data class FolderRef(val id: String, val name: String, val color: Color)
    val folderByLogin: Map<String, FolderRef> = remember(folders) {
        buildMap {
            folders.forEach { f ->
                val parsed = runCatching {
                    Color(f.color.removePrefix("#").toLong(16) or 0xFF000000L)
                }.getOrDefault(Color(0xFF9146FF))
                val ref = FolderRef(f.id, f.name, parsed)
                f.channels.forEach { ch ->
                    put(ch.login.lowercase().removePrefix("#"), ref)
                }
            }
        }
    }
    val collapsedFolders = remember { mutableStateListOf<String>() }
    LaunchedEffect(folders, channels) {
        io.github.aakira.napier.Napier.d(
            "ChannelTabBar: folders=${folders.size} (${folders.joinToString { it.name + "[${it.channels.size}]" }}), channels=${channels.size}, mapped=${folderByLogin.size}",
            tag = "ChannelTabBar"
        )
    }
    val extra = ChatoneTheme.extraColors
    val uriHandler = LocalUriHandler.current

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

    val tabCenters = remember { mutableStateListOf<Pair<Int, Offset>>() }
    val folderChipBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var folderDropTargetId by remember { mutableStateOf<String?>(null) }


    val tabBarWallpaper = LocalWallpaperController.current.state
    val tabBarBlur =
        if (tabBarWallpaper.glowEffectsEnabled) tabBarWallpaper.panelColorConfig.topBarBlurRadius else 0f
    val tabBarColor =
        topBarBackgroundColor(tabBarWallpaper, MaterialTheme.colorScheme.surfaceContainer)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, extra.cardBorder)
    ) {
        if (tabBarBlur > 0f) {
            Box(modifier = Modifier.matchParentSize().background(tabBarColor).panelBlur(tabBarBlur))
        } else {
            Box(modifier = Modifier.matchParentSize().background(tabBarColor))
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            val displayChannels: List<ChannelTab> = remember(channels, folders) {
                val key = { ch: ChannelTab -> ch.login.lowercase().removePrefix("#") }
                val foldered = folders.flatMap { f ->
                    val ids = f.channels.map { it.login.lowercase().removePrefix("#") }.toSet()
                    channels.filter { key(it) in ids }
                }
                val seen = foldered.map { key(it) }.toHashSet()
                val unfoldered = channels.filter { key(it) !in seen }
                foldered + unfoldered
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val folderRefByIndex: List<FolderRef?> = displayChannels.map { ch ->
                    folderByLogin[ch.login.lowercase().removePrefix("#")]
                }
                val groupStarts: Map<Int, Boolean> = run {
                    val starts = mutableMapOf<Int, Boolean>()
                    var prev: String? = "__SENTINEL__"
                    folderRefByIndex.forEachIndexed { idx, ref ->
                        val cur = ref?.id
                        if (cur != prev) {
                            starts[idx] = true
                            prev = cur
                        }
                    }
                    starts
                }
                val groupSizes: Map<Int, Int> = run {
                    val sizes = mutableMapOf<Int, Int>()
                    var runStart = -1
                    var runId: String? = "__SENTINEL__"
                    var runCount = 0
                    fun flush() {
                        if (runStart >= 0) sizes[runStart] = runCount
                    }
                    folderRefByIndex.forEachIndexed { idx, ref ->
                        val cur = ref?.id
                        if (cur != runId) {
                            flush()
                            runStart = idx
                            runId = cur
                            runCount = 1
                        } else {
                            runCount += 1
                        }
                    }
                    flush()
                    sizes
                }

                displayChannels.forEachIndexed { index: Int, channel: ChannelTab ->
                    val isActive = channel.login == activeLogin
                    val folderInfo = folderRefByIndex[index]
                    val isGroupStart = groupStarts[index] == true

                    if (isGroupStart && folderInfo != null) {
                        val isCollapsed = folderInfo.id in collapsedFolders
                        val count = groupSizes[index] ?: 0
                        TabBarFolderChip(
                            name = folderInfo.name,
                            color = folderInfo.color,
                            count = count,
                            isCollapsed = isCollapsed,
                            onClick = {
                                if (isCollapsed) collapsedFolders.remove(folderInfo.id)
                                else collapsedFolders.add(folderInfo.id)
                            },
                            isDropTarget = folderDropTargetId == folderInfo.id,
                            onPositioned = { rect -> folderChipBounds[folderInfo.id] = rect }
                        )
                    } else if (isGroupStart && folderInfo == null && index > 0) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .width(1.dp)
                                .height(14.dp)
                                .background(extra.cardBorder)
                        )
                    }

                    if (folderInfo != null && folderInfo.id in collapsedFolders) {
                        return@forEachIndexed
                    }

                    val tabCenter = remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .then(
                                if (dropTargetIndex == index && draggedIndex != null) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(6.dp)
                                ) else Modifier
                            )
                            .then(
                                if (folderInfo != null) Modifier.drawBehind {
                                    val stripeHeight = 2.dp.toPx()
                                    drawRect(
                                        color = folderInfo.color,
                                        topLeft = Offset(0f, size.height - stripeHeight),
                                        size = androidx.compose.ui.geometry.Size(size.width, stripeHeight)
                                    )
                                } else Modifier
                            )
                            .clickable { onSelect(channel.login) }
                            .padding(start = 6.dp, end = 2.dp, top = 3.dp, bottom = 3.dp)
                            .onGloballyPositioned { coords ->
                                val bounds = coords.boundsInRoot()
                                tabCenter.value = Offset(
                                    bounds.left + bounds.width / 2,
                                    bounds.top + bounds.height / 2
                                )

                                tabCenters.removeAll { pair: Pair<Int, Offset> -> pair.first == index }
                                tabCenters.add(index to tabCenter.value)
                            }
                            .pointerInput(channel.login, index) {
                                detectDragGestures(
                                    onDragStart = { draggedIndex = index },
                                    onDrag = { change, delta ->
                                        change.consumeAllChanges()
                                        dragOffset += delta

                                        val currentDragPos = tabCenter.value + dragOffset

                                        val hitFolderId = folderChipBounds.entries.firstOrNull { (_, rect) ->
                                            rect.contains(currentDragPos)
                                        }?.key
                                        if (hitFolderId != null) {
                                            folderDropTargetId = hitFolderId
                                            dropTargetIndex = null
                                        } else {
                                            folderDropTargetId = null
                                            dropTargetIndex =
                                                tabCenters.minByOrNull { pair: Pair<Int, Offset> ->
                                                    hypot(
                                                        currentDragPos.x - pair.second.x,
                                                        currentDragPos.y - pair.second.y
                                                    )
                                                }?.first
                                        }
                                    },
                                    onDragEnd = {
                                        val fromDisp = draggedIndex
                                        val targetFolder = folderDropTargetId
                                        val toDisp = dropTargetIndex
                                        val fromLogin = fromDisp?.let { displayChannels.getOrNull(it)?.login }
                                        val toLogin = toDisp?.let { displayChannels.getOrNull(it)?.login }
                                        val fromOrig = fromLogin?.let { l -> channels.indexOfFirst { it.login == l } }
                                            ?.takeIf { it >= 0 }
                                        val toOrig = toLogin?.let { l -> channels.indexOfFirst { it.login == l } }
                                            ?.takeIf { it >= 0 }
                                        when {
                                            fromLogin != null && targetFolder != null -> {
                                                onMoveToFolder(fromLogin, targetFolder)
                                            }
                                            fromOrig != null && toOrig != null && fromOrig != toOrig -> {
                                                onReorder(fromOrig, toOrig)
                                            }
                                        }
                                        draggedIndex = null
                                        dragOffset = Offset.Zero
                                        dropTargetIndex = null
                                        folderDropTargetId = null
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffset = Offset.Zero
                                        dropTargetIndex = null
                                        folderDropTargetId = null
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                            try {
                                                uriHandler.openUri("https://www.twitch.tv/${channel.login}")
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (channel.isLive) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape)
                                        .background(ChatoneTheme.extraColors.live).border(
                                            1.dp,
                                            MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = "#${channel.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            if (channel.unreadCount > 0) {
                                Spacer(Modifier.width(3.dp))
                                Surface(color = Color.Red, shape = CircleShape) {
                                    Text(
                                        text = if (channel.unreadCount > 99) "99+" else "${channel.unreadCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White,
                                        modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 1.dp
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.width(2.dp))
                            Box(
                                modifier = Modifier.size(16.dp).clip(CircleShape)
                                    .clickable { onClose(channel.login) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close ${channel.displayName}",
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                }

                if (draggedIndex != null) {
                    val openFolderIds = folderRefByIndex.mapNotNull { it?.id }.toSet()
                    folders.forEach { f ->
                        if (f.id in openFolderIds) return@forEach
                        val parsed = runCatching {
                            Color(f.color.removePrefix("#").toLong(16) or 0xFF000000L)
                        }.getOrDefault(Color(0xFF9146FF))
                        TabBarFolderChip(
                            name = f.name,
                            color = parsed,
                            count = 0,
                            isCollapsed = true,
                            onClick = { /* no-op */ },
                            isDropTarget = folderDropTargetId == f.id,
                            onPositioned = { rect -> folderChipBounds[f.id] = rect }
                        )
                    }
                }
            }


            draggedIndex?.let { idx: Int ->
                channels.getOrNull(idx)?.let { ch: ChannelTab ->
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    tabCenters.getOrNull(idx)?.second?.x?.toInt()
                                        ?: 0 + dragOffset.x.toInt(),
                                    tabCenters.getOrNull(idx)?.second?.y?.toInt()
                                        ?: 0 + dragOffset.y.toInt()
                                )
                            }
                            .zIndex(10f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .shadow(8.dp, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = "#${ch.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color>
) {
    var hovered by remember { mutableStateOf(false) }
    val elevation by animateFloatAsState(
        targetValue = if (hovered) 8f else 2f,
        animationSpec = tween(150),
        label = "btn_elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.03f else 1f,
        animationSpec = tween(150),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .height(34.dp)
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                shadowElevation = elevation
                shape = RoundedCornerShape(10.dp)
                clip = true
            }
            .shadow(elevation.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = if (hovered)
                        gradientColors.map { it.copy(alpha = (it.alpha + 0.1f).coerceAtMost(1f)) }
                    else gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(
                        Float.POSITIVE_INFINITY,
                        Float.POSITIVE_INFINITY
                    )
                )
            )
            .handleHover(onEnter = { hovered = true }, onExit = { hovered = false })
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        if (hovered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    )
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyState(
    isGuest: Boolean,
    onAddChannel: () -> Unit,
    onLogin: () -> Unit
) {
    val s = LocalStrings.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LiquidGlassSurface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(24.dp),
            backgroundAlphaHigh = 0.92f,
            backgroundAlphaLow = 0.80f
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Outlined.MailOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = s.noChannelsOpen,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = LocalStrings.current.mainNoChannelsHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                FilledTonalButton(onClick = onAddChannel) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(LocalStrings.current.mainAddChannelTitle)
                }
                if (isGuest) {
                    OutlinedButton(onClick = onLogin) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(LocalStrings.current.mainLoginToTwitch)
                    }
                }
            }
        }
    }
}


@Composable
private fun AddChannelDialog(
    query: String,
    searchResults: List<Channel>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onChannelSelected: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.mainAddChannelTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(s.mainChannelNamePlaceholder) },
                    leadingIcon = {
                        Text(
                            "#",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (isSearching) CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (query.isNotBlank()) onChannelSelected(
                            query.trim().lowercase().removePrefix("#"), "", ""
                        )
                    })
                )
                if (searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { channel: Channel ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onChannelSelected(
                                            channel.login,
                                            channel.profileImageUrl,
                                            channel.displayName
                                        )
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (channel.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = channel.profileImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (channel.gameName.isNotEmpty()) {
                                        Text(
                                            text = channel.gameName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (channel.isLive) {
                                    Surface(
                                        color = ChatoneTheme.extraColors.live,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (query.isNotBlank()) onChannelSelected(
                        query.trim().lowercase().removePrefix("#"), "", ""
                    )
                },
                enabled = query.isNotBlank()
            ) { Text(s.mainJoin) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}


@Composable
private fun CreateFolderDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.mainCreateFolderTitle) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.mainFolderNamePlaceholder) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onCreate() })
            )
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = name.isNotBlank()
            ) { Text(s.mainCreate) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}


private fun parseFolderColor(hex: String): Color {
    return try {
        val colorInt = hex.removePrefix("#").toLong(16)
        Color(
            red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f
        )
    } catch (e: Exception) {
        ChatoneColors.Violet400
    }
}

@Composable
private fun TabBarFolderChip(
    name: String,
    color: Color,
    count: Int,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    isDropTarget: Boolean = false,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(start = 3.dp, end = 1.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isDropTarget) color.copy(alpha = 0.4f)
                else color.copy(alpha = 0.14f)
            )
            .then(
                if (isDropTarget) Modifier.border(
                    1.2.dp, color, RoundedCornerShape(6.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .onGloballyPositioned { coords -> onPositioned(coords.boundsInRoot()) }
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            painter = painterResource(chatone.composeapp.generated.resources.Res.drawable.folder_outline),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp)
        )
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            fontSize = 10.sp
        )
        if (count > 0) {
            Text(
                "·$count",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
        Icon(
            if (isCollapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight
            else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = color.copy(alpha = 0.7f),
            modifier = Modifier.size(10.dp)
        )
    }
}