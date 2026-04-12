package io.rudione.chatone.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import io.rudione.chatone.presentation.main.components.LiquidGlassDropdown
import io.rudione.chatone.presentation.settings.SettingsScreen
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.ChatoneColors
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperGlowEdge
import io.rudione.chatone.util.WallpaperLoader
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.hypot
import io.rudione.chatone.presentation.chat.components.ChattersPanel
import io.rudione.chatone.presentation.main.components.MentionsFeed

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wallpaperController = LocalWallpaperController.current
    var isWideScreenForSettings by remember { mutableStateOf(false) }
    val wallpaper by remember { derivedStateOf { wallpaperController.state } }
    val wallpaperLoader: WallpaperLoader = koinInject()
    var showWhisperOverlay by remember { mutableStateOf(false) }
    var showMentionsOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MainEffect.NavigateToAuth -> onNavigateToAuth()
                is MainEffect.ShowError -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is MainEffect.ShowEmoteUpdate -> scope.launch {
                    snackbarHostState.showSnackbar(
                        effect.text
                    )
                }
                is MainEffect.IncomingWhisper -> scope.launch { snackbarHostState.showSnackbar("💬 ${effect.fromDisplayName}: ${effect.text}") }
                is MainEffect.MentionToast -> scope.launch {
                    snackbarHostState.showSnackbar(
                        "🔔 @${effect.fromDisplayName} в #${effect.channelLogin}: ${effect.text}"
                    )
                }
            }
        }
    }

    if (state.showSettings && !isWideScreenForSettings) {
        SettingsScreen(
            onNavigateBack = { viewModel.sendEvent(MainEvent.HideSettings) },
            onThemeChanged = onThemeChanged,
            isWideScreen = false,
            wallpaperLoader = wallpaperLoader
        )
        return
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

            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box {
                        GlowSurface(
                            dominantColor = wallpaper.dominantColor,
                            intensity = 0.9f,
                            centerX = 1.2f,
                            centerY = 0.5f
                        ) {
                            ChannelSidebar(
                                state = state,
                                onEvent = { viewModel.sendEvent(it) },
                                isWideScreen = true
                            )
                        }
                        if (wallpaper.isActive) {
                            WallpaperGlowEdge(
                                dominantColor = wallpaper.dominantColor,
                                fromRight = true,
                                glowWidth = 100.dp,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val showTabBar =
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
                        if (activeChannel != null) {
                            ChatBackgroundLayer(
                                wallpaper = wallpaper,
                                darkTheme = settingsState.darkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                ChatScreen(
                                    channelLogin = activeChannel,
                                    onNavigateBack = {},
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
                                                userId,
                                                username,
                                                displayName,
                                                avatarUrl,
                                                color
                                            )
                                        )
                                    },
                                    onChannelIdResolved = { channelId ->
                                        viewModel.sendEvent(
                                            MainEvent.SetActiveChatChannelId(
                                                channelId
                                            )
                                        )
                                    },
                                    isWideScreen = true,
                                    wallpaper = wallpaper
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
                    val showTabBar =
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
                    if (activeChannel != null) {
                        ChatBackgroundLayer(
                            wallpaper = wallpaper,
                            darkTheme = settingsState.darkTheme,
                            modifier = Modifier.weight(1f)
                        ) {
                            ChatScreen(
                                channelLogin = activeChannel,
                                onNavigateBack = { viewModel.sendEvent(MainEvent.ToggleSidebar) },
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
                                            userId,
                                            username,
                                            displayName,
                                            avatarUrl,
                                            color
                                        )
                                    )
                                },
                                onChannelIdResolved = { channelId ->
                                    viewModel.sendEvent(MainEvent.SetActiveChatChannelId(channelId))
                                },
                                isWideScreen = false,
                                wallpaper = wallpaper
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
                        GlowSurface(
                            dominantColor = wallpaper.dominantColor,
                            intensity = 0.9f,
                            centerX = 1.2f,
                            centerY = 0.5f
                        ) {
                            ChannelSidebar(
                                state = state,
                                onEvent = { event: MainEvent -> viewModel.sendEvent(event) },
                                isWideScreen = true
                            )
                        }
                        if (wallpaper.isActive) {
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
                onChannelClick = { login -> viewModel.sendEvent(MainEvent.SelectChannel(login)) },
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

    LiquidGlassSurface(
        modifier = modifier.padding(8.dp).shadow(
            8.dp,
            RoundedCornerShape(16.dp),
            ambientColor = extra.shadowColor,
            spotColor = extra.elevatedShadow
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
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
                    contentDescription = "Open sidebar",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(
                    state.openChannels,
                    key = { channel: ChannelTab -> channel.login }) { channel ->
                    val isActive = channel.login == state.activeChannelLogin
                    var showTooltip by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.2f
                                    ) else Color.Transparent
                                )
                                .clickable { onEvent(MainEvent.SelectChannel(channel.login)) }
                                .then(
                                    if (isActive) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ) else Modifier
                                )
                                .pointerInput(channel.login) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Enter -> showTooltip = true
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
                    }
                    if (showTooltip) {
                        Popup(
                            alignment = Alignment.BottomCenter,
                            offset = IntOffset(0, 12),
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                        ) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        RoundedCornerShape(6.dp)
                                    ).padding(horizontal = 10.dp, vertical = 5.dp)
                                    .shadow(4.dp, RoundedCornerShape(6.dp))
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

            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Mentions",
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
                        contentDescription = "Whispers",
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
                    contentDescription = "Add channel",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ChannelSidebar(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    isWideScreen: Boolean = false
) {
    val extra = ChatoneTheme.extraColors
    val density = LocalDensity.current

    var draggedChannelLogin by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dropTargetFolderId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragOverIndex by remember { mutableStateOf<Int?>(null) }

    val folderBounds = remember { mutableStateListOf<ItemBounds>() }
    val channelBounds = remember { mutableStateListOf<ItemBounds>() }

    val folderedLogins = state.folders.flatMap { it.channels }.map { it.login }.toSet()
    val filteredUnfoldered = state.unfolderedChannels.filter { it.login !in folderedLogins }

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .shadow(
                12.dp,
                RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                ambientColor = extra.shadowColor,
                spotColor = extra.elevatedShadow
            )
            .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
            .background(extra.sidebarSurface)
            .border(1.dp, extra.glassBorder, RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
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
            if (!isWideScreen) {
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
                    Box {
                        var showChannelMenu by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showChannelMenu = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Channel options",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showChannelMenu,
                            onDismissRequest = { showChannelMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Viewers list") },
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
                    Text("Login to Twitch")
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
            contentPadding = PaddingValues(horizontal = 4.dp)
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
                        .onGloballyPositioned { coords ->
                            val rect = coords.boundsInRoot()
                            folderBounds.removeAll { it.id == folder.id }
                            folderBounds.add(ItemBounds(folder.id, rect))
                        },
                    isDropTarget = dropTargetFolderId == folder.id && draggedChannelLogin != null
                )

                if (folder.isExpanded) {
                    folder.channels.forEachIndexed { channelIndex, channel ->
                        ChannelItem(
                            channel = channel,
                            isActive = channel.login == state.activeChannelLogin,
                            folders = state.folders,
                            currentFolderId = folder.id,
                            onClick = { onEvent(MainEvent.SelectChannel(channel.login)) },
                            onClose = { onEvent(MainEvent.CloseChannel(channel.login)) },
                            onMoveToFolder = { folderId ->
                                onEvent(MainEvent.MoveChannelToFolder(channel.login, folderId))
                            }
                        )
                    }
                }
            }

            if (filteredUnfoldered.isNotEmpty()) {
                item(key = "unfoldered_header") {
                    Text(
                        "CHANNELS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                    index = index,
                    onDragStart = { startPos ->
                        draggedChannelLogin = channel.login
                        dragStartOffsetPx = startPos
                        dragOffsetPx = Offset.Zero
                        isDragging = true
                        dragStartIndex = index
                        dragOverIndex = null
                    },
                    onDrag = { delta ->
                        dragOffsetPx += delta
                        val currentPos = dragStartOffsetPx + dragOffsetPx


                        dropTargetFolderId = folderBounds.find { it.rect.contains(currentPos) }?.id


                        val targetChannel = channelBounds.find { it.rect.contains(currentPos) }
                        dragOverIndex = targetChannel?.id?.substringAfterLast("_")?.toIntOrNull()
                    },
                    onDragEnd = {
                        val currentPos = dragStartOffsetPx + dragOffsetPx


                        val hitFolder = folderBounds.find { it.rect.contains(currentPos) }
                        if (hitFolder != null && draggedChannelLogin != null) {
                            onEvent(MainEvent.DropChannelOnFolder(draggedChannelLogin!!, hitFolder.id))
                        }

                        else if (dragOverIndex != null && dragStartIndex != null && dragOverIndex != dragStartIndex) {
                            onEvent(MainEvent.ReorderUnfolderedChannels(dragStartIndex!!, dragOverIndex!!))
                        }


                        draggedChannelLogin = null
                        dropTargetFolderId = null
                        dragOffsetPx = Offset.Zero
                        dragStartIndex = null
                        dragOverIndex = null
                        isDragging = false
                    },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val rect = coords.boundsInRoot()
                        channelBounds.removeAll { it.id.startsWith("unfoldered_${channel.login}_") }
                        channelBounds.add(ItemBounds("unfoldered_${channel.login}_$index", rect))
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledTonalButton(
                onClick = { onEvent(MainEvent.ShowAddChannelDialog) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Channel", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { onEvent(MainEvent.ShowCreateFolderDialog) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Outlined.Create,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Folder", style = MaterialTheme.typography.labelMedium)
            }
        }

        TextButton(
            onClick = { onEvent(MainEvent.ToggleMentionsFeed) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
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
            Text("/mentions")
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
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
            Text("Whispers")
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Settings")
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
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.2f
                            ), RoundedCornerShape(8.dp)
                        ) else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            contentDescription = "Add channel to folder",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    DropdownMenu(
                        expanded = showAddToFolderMenu,
                        onDismissRequest = { showAddToFolderMenu = false }) {
                        if (unfolderedChannels.isEmpty()) {
                            DropdownMenuItem(text = {
                                Text(
                                    "No channels to add",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }, onClick = { showAddToFolderMenu = false }, enabled = false)
                        } else {
                            unfolderedChannels.forEach { ch: ChannelTab ->
                                DropdownMenuItem(
                                    text = { Text("#${ch.displayName}") },
                                    onClick = {
                                        showAddToFolderMenu = false; onMoveChannel(
                                        ch.login,
                                        folder.id
                                    )
                                    },
                                    leadingIcon = {
                                        Text(
                                            "#",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    })
                            }
                        }
                    }
                }
            }
            DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete folder") },
                    onClick = { showFolderMenu = false; onDelete() },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    })
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
                    onDragStart = { offset ->
                        onDragStart(offset)
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
                    onLongClick = { if (folders.isNotEmpty()) showContextMenu = true }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                try {
                                    uriHandler.openUri("https://www.twitch.tv/${channel.login}")
                                } catch (_: Exception) {}
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
                    color = Color.Red,
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
                    IconButton(onClick = { showContextMenu = true }, modifier = Modifier.size(20.dp)) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = "Move to folder",
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
                                backgroundAlphaHigh = 0.98f
                            ) {
                                Column {
                                    if (currentFolderId != null) {
                                        LiquidGlassDropdownItem(
                                            text = "Remove from folder",
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
                                        text = "Close channel",
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelItem(
    channel: ChannelTab,
    isActive: Boolean,
    folders: List<ChannelFolder> = emptyList(),
    currentFolderId: String? = null,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onMoveToFolder: (String?) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActive) ChatoneTheme.extraColors.sidebarSelected else Color.Transparent)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (folders.isNotEmpty()) showContextMenu = true }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                try {
                                    uriHandler.openUri("https://www.twitch.tv/${channel.login}")
                                } catch (_: Exception) {}
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
                    color = Color.Red,
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
                    IconButton(onClick = { showContextMenu = true }, modifier = Modifier.size(20.dp)) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = "Move to folder",
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
                                backgroundAlphaHigh = 0.98f
                            ) {
                                Column {
                                    if (currentFolderId != null) {
                                        LiquidGlassDropdownItem(
                                            text = "Remove from folder",
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
                                        text = "Close channel",
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
    onReorder: (Int, Int) -> Unit
) {
    val extra = ChatoneTheme.extraColors
    val uriHandler = LocalUriHandler.current

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

    val tabCenters = remember { mutableStateListOf<Pair<Int, Offset>>() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                channels.forEachIndexed { index: Int, channel: ChannelTab ->
                    val isActive = channel.login == activeLogin


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
                            .clickable { onSelect(channel.login) }
                            .padding(start = 8.dp, end = 2.dp, top = 8.dp, bottom = 8.dp)
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
                                        dropTargetIndex =
                                            tabCenters.minByOrNull { pair: Pair<Int, Offset> ->
                                                hypot(
                                                    currentDragPos.x - pair.second.x,
                                                    currentDragPos.y - pair.second.y
                                                )
                                            }?.first
                                    },
                                    onDragEnd = {
                                        val from = draggedIndex
                                        val to = dropTargetIndex
                                        if (from != null && to != null && from != to) {
                                            onReorder(from, to)
                                        }
                                        draggedIndex = null
                                        dragOffset = Offset.Zero
                                        dropTargetIndex = null
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffset = Offset.Zero
                                        dropTargetIndex = null
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
private fun EmptyState(isGuest: Boolean, onAddChannel: () -> Unit, onLogin: () -> Unit) {
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
                    text = "No channels open",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Add a channel to start chatting",
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
                    Text("Add Channel")
                }
                if (isGuest) {
                    OutlinedButton(onClick = onLogin) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Login to Twitch")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Channel") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Channel name...") },
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
            ) { Text("Join") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
private fun CreateFolderDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Folder name...") },
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
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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