package io.rudione.chatone.tv.home

import android.os.Build
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.zIndex
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.data.repository.AccountTokenKeeper
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.data.repository.MultiAccountConnectionRegistry
import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.domain.model.ChannelTab
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.domain.media.MediaKind
import io.rudione.chatone.presentation.account.AccountAutoConnectEffect
import io.rudione.chatone.presentation.browse.BrowseViewModel
import io.rudione.chatone.presentation.chat.ChatScreenCallbacks
import io.rudione.chatone.presentation.chat.ChatSessionIdentity
import io.rudione.chatone.presentation.chat.multichat.MultiChatRootSetup
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.main.MainEffect
import io.rudione.chatone.presentation.main.MainEvent
import io.rudione.chatone.presentation.main.MainState
import io.rudione.chatone.presentation.main.MainViewModel
import io.rudione.chatone.domain.media.ClipOrigin
import io.rudione.chatone.presentation.media.ClipEditorNavigation
import io.rudione.chatone.presentation.media.ClipEditorScreen
import io.rudione.chatone.presentation.settings.SettingsEffect
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.startup.LaunchStage
import io.rudione.chatone.presentation.startup.ReportLaunchStage
import io.rudione.chatone.presentation.stream.LocalStreamPlayerHost
import io.rudione.chatone.presentation.stream.StreamPlayerEvent
import io.rudione.chatone.presentation.stream.rememberStreamPlayerHost
import io.rudione.chatone.tv.auth.TvRightsDialog
import io.rudione.chatone.tv.channel.TvChannelActions
import io.rudione.chatone.tv.components.claim
import io.rudione.chatone.tv.channel.TvChannelScreen
import io.rudione.chatone.tv.navigation.TvNavigator
import io.rudione.chatone.tv.navigation.TvRoute
import io.rudione.chatone.tv.player.TvMediaScreen
import io.rudione.chatone.tv.player.TvPlayerActions
import io.rudione.chatone.tv.player.TvPlayerScreen
import io.rudione.chatone.tv.settings.TvSettingsScreen
import io.rudione.chatone.tv.theme.TvTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val OVERLAY_Z_INDEX = 10f
private val SYSTEM_DELIVERS_BACK = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA

@Composable
private fun Modifier.tvBackKey(): Modifier {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return onPreviewKeyEvent { event ->
        if (event.key != Key.Back) return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyUp && dispatcher != null) {
            val deliveredBySystem = SYSTEM_DELIVERS_BACK && dispatcher.hasEnabledCallbacks()
            if (!deliveredBySystem) dispatcher.onBackPressed()
        }
        true
    }
}

@Composable
fun TvMainScreen(onSignedOut: () -> Unit) {
    TvTheme {
        val mainViewModel: MainViewModel = koinViewModel()
        val mainState by mainViewModel.state.collectAsState()
        val settingsViewModel: SettingsViewModel = koinViewModel()
        val browseViewModel: BrowseViewModel = koinViewModel()
        val browseState by browseViewModel.state.collectAsState()
        val followed = browseViewModel.followedStreams.collectAsLazyPagingItems()
        val host = rememberStreamPlayerHost()
        val navigator = remember { TvNavigator() }
        val accountManager: AccountManager = koinInject()
        val authRepository: AuthRepository = koinInject()
        val registry: MultiAccountConnectionRegistry = koinInject()
        val tokenKeeper: AccountTokenKeeper = koinInject()
        val moderationAuth: ModerationAuthStore = koinInject()
        val identityRights by moderationAuth.identity.collectAsState()
        var confirmingRights by remember { mutableStateOf(false) }
        var menuTarget by remember { mutableStateOf<TvMenuTarget?>(null) }
        var restoreDrawerFocus by remember { mutableStateOf(false) }
        val drawerFocus = remember { FocusRequester() }
        LaunchedEffect(menuTarget) {
            if (menuTarget == null && restoreDrawerFocus) {
                restoreDrawerFocus = false
                drawerFocus.claim()
            }
        }
        val browseFocus = remember { FocusRequester() }
        val signedOut by rememberUpdatedState(onSignedOut)
        val selected = mainState.selectedAccount

        ReportLaunchStage(LaunchStage.Ready)
        MultiChatRootSetup()
        AccountAutoConnectEffect(authRepository, registry, accountManager)
        LaunchedEffect(tokenKeeper) { tokenKeeper.start() }
        LaunchedEffect(mainViewModel) {
            mainViewModel.effect.collect { effect ->
                if (effect == MainEffect.NavigateToAuth) signedOut()
            }
        }
        LaunchedEffect(settingsViewModel) {
            settingsViewModel.effect.collect { effect ->
                if (effect is SettingsEffect.NavigateToAuth) signedOut()
            }
        }

        val identity = ChatSessionIdentity(
            accessToken = selected?.accessToken.orEmpty(),
            userId = selected?.userId.orEmpty(),
            userLogin = selected?.login.orEmpty(),
            displayName = selected?.displayName.orEmpty()
        )
        val chatCallbacks = remember(mainViewModel) {
            ChatScreenCallbacks(
                onMentionDetected = { mainViewModel.sendEvent(MainEvent.IncrementMentionCount(it)) },
                onMentionReceived = { mainViewModel.sendEvent(MainEvent.AddMentionEntry(it)) },
                onChannelIdResolved = { mainViewModel.sendEvent(MainEvent.SetActiveChatChannelId(it)) }
            )
        }
        val openStream: (BrowseStream) -> Unit = { stream ->
            navigator.open(TvRoute.Player(stream.login, stream.displayName, stream.avatarUrl))
        }
        val browseActions = remember(navigator) {
            TvBrowseActions(
                onOpenStream = openStream,
                onOpenCategory = { navigator.open(TvRoute.Category(it)) },
                onSignIn = { signedOut() }
            )
        }
        val openChannel: (ChannelTab) -> Unit = { tab ->
            if (tab.isLive) navigator.open(
                TvRoute.Player(
                    tab.login,
                    tab.displayName,
                    tab.profileImageUrl
                )
            )
            else navigator.open(TvRoute.Channel(tab.login))
        }
        val drawerChannels =
            remember(mainState.recentChannels, mainState.unfolderedChannels, mainState.folders) {
                val foldered =
                    mainState.folders.flatMapTo(HashSet()) { folder -> folder.channels.map { it.login } }
                TvDrawerChannels(
                    recent = mainState.recentChannels,
                    channels = mainState.unfolderedChannels.filter { it.login !in foldered },
                    folders = mainState.folders
                )
            }
        val drawerActions = remember(mainViewModel, navigator) {
            TvDrawerActions(
                onOpenChannel = { openChannel(it) },
                onChannelMenu = {
                    restoreDrawerFocus = true
                    menuTarget = TvMenuTarget.Channel(it)
                },
                onToggleFolder = { mainViewModel.sendEvent(MainEvent.ToggleFolder(it)) },
                onFolderMenu = {
                    restoreDrawerFocus = true
                    menuTarget = TvMenuTarget.Folder(it)
                },
                onAddChannel = {
                    restoreDrawerFocus = true
                    menuTarget = TvMenuTarget.AddChannel(
                        mainViewModel.state.value.openChannels.mapTo(HashSet()) { it.login }
                    )
                }
            )
        }
        val menuActions = remember(mainViewModel, navigator) {
            TvChannelMenuActions(
                onWatch = { tab ->
                    navigator.open(
                        TvRoute.Player(
                            tab.login,
                            tab.displayName,
                            tab.profileImageUrl
                        )
                    )
                },
                onOpenProfile = { navigator.open(TvRoute.Channel(it)) },
                send = mainViewModel::sendEvent
            )
        }
        val route = navigator.top
        val player = navigator.stack.lastOrNull { it is TvRoute.Player } as? TvRoute.Player
        val browseVisible = route == TvRoute.Browse

        LaunchedEffect(player?.login) {
            val current = player ?: return@LaunchedEffect
            mainViewModel.sendEvent(
                MainEvent.OpenRecentChannel(
                    current.login,
                    current.displayName,
                    current.avatarUrl
                )
            )
        }
        LaunchedEffect(browseVisible) {
            if (browseVisible) runCatching { browseFocus.requestFocus() }
        }
        LaunchedEffect(route is TvRoute.ClipEditor) {
            if (host != null && (route is TvRoute.ClipEditor || player != null)) {
                val state = host.state.value
                if (route is TvRoute.ClipEditor && state.isPlaybackActive) {
                    host.onEvent(StreamPlayerEvent.TogglePlayback)
                } else if (route !is TvRoute.ClipEditor && !state.isPlaybackActive && state.isOpen) {
                    host.onEvent(StreamPlayerEvent.TogglePlayback)
                }
            }
        }
        SystemBackHandler(enabled = route is TvRoute.Category || route is TvRoute.Channel) { navigator.back() }

        CompositionLocalProvider(LocalStreamPlayerHost provides host) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .tvBackKey()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = if (browseVisible) 1f else 0f }
                        .focusProperties { onEnter = { if (!browseVisible) cancelFocusChange() } }
                        .focusGroup()
                ) {
                    TvBrowseShell(
                        account = selected,
                        contentFocus = browseFocus,
                        active = browseVisible && menuTarget == null,
                        channels = drawerChannels,
                        drawerActions = drawerActions,
                        drawerFocus = drawerFocus
                    ) { section ->
                        when (section) {
                            TvSection.HOME -> TvHomeScreen(
                                browseViewModel,
                                browseActions,
                                browseFocus
                            )

                            TvSection.CATEGORIES -> TvCategoriesScreen(
                                browseViewModel,
                                browseActions,
                                browseFocus
                            )

                            TvSection.SEARCH -> TvSearchScreen(
                                viewModel = browseViewModel,
                                actions = browseActions,
                                onOpenChannel = { channel ->
                                    if (channel.isLive) {
                                        navigator.open(
                                            TvRoute.Player(
                                                channel.login,
                                                channel.displayName,
                                                channel.avatarUrl
                                            )
                                        )
                                    } else {
                                        navigator.open(TvRoute.Channel(channel.login))
                                    }
                                }
                            )

                            TvSection.SETTINGS -> TvSettingsScreen(
                                host = host,
                                account = selected,
                                hasExtendedRights = identityRights != null,
                                onConfirmRights = { confirmingRights = true },
                                onSignIn = onSignedOut
                            )
                        }
                    }
                }
                if (player != null && host != null) {
                    TvPlayerScreen(
                        login = player.login,
                        active = route is TvRoute.Player,
                        host = host,
                        identity = identity,
                        chatCallbacks = chatCallbacks,
                        followed = followed,
                        needsSignIn = browseState.followsNeedSignIn,
                        pinned = mainState.openChannels.any {
                            it.login.equals(
                                player.login,
                                ignoreCase = true
                            )
                        },
                        actions = remember(navigator, host) {
                            TvPlayerActions(
                                onClose = {
                                    host.onEvent(StreamPlayerEvent.Close)
                                    navigator.back()
                                },
                                onSwitchStream = openStream,
                                onOpenChannel = { navigator.open(TvRoute.Channel(it)) },
                                onCreateClip = { login, delay ->
                                    navigator.open(
                                        TvRoute.ClipEditor(
                                            login,
                                            delay
                                        )
                                    )
                                },
                                onTogglePin = { channel ->
                                    mainViewModel.sendEvent(
                                        pinEvent(
                                            mainViewModel.state.value,
                                            channel
                                        )
                                    )
                                }
                            )
                        },
                        modifier = Modifier.zIndex(OVERLAY_Z_INDEX)
                    )
                }
                when (route) {
                    is TvRoute.Category -> TvCategoryStreamsScreen(
                        category = route.category,
                        viewModel = browseViewModel,
                        onOpenStream = openStream,
                        modifier = Modifier
                            .zIndex(OVERLAY_Z_INDEX)
                            .background(MaterialTheme.colorScheme.background)
                    )

                    is TvRoute.Channel -> TvChannelScreen(
                        login = route.login,
                        actions = TvChannelActions(
                            onWatch = { profile ->
                                navigator.open(
                                    TvRoute.Player(
                                        profile.login,
                                        profile.displayName,
                                        profile.avatarUrl
                                    )
                                )
                            },
                            onOpenClip = { navigator.open(TvRoute.Media(MediaKind.CLIP, it.slug)) },
                            onOpenVideo = { navigator.open(TvRoute.Media(MediaKind.VIDEO, it.id)) }
                        ),
                        modifier = Modifier.zIndex(OVERLAY_Z_INDEX)
                    )

                    is TvRoute.Media -> TvMediaScreen(
                        kind = route.kind,
                        id = route.id,
                        onClose = { navigator.back() },
                        modifier = Modifier.zIndex(OVERLAY_Z_INDEX)
                    )

                    is TvRoute.ClipEditor -> ClipEditorScreen(
                        login = route.login,
                        origin = ClipOrigin.Live(route.delaySeconds),
                        navigation = remember(navigator) {
                            ClipEditorNavigation(
                                onClose = { navigator.back() },
                                onWatch = { slug ->
                                    navigator.replaceTop(
                                        TvRoute.Media(
                                            MediaKind.CLIP,
                                            slug
                                        )
                                    )
                                }
                            )
                        },
                        modifier = Modifier.zIndex(OVERLAY_Z_INDEX)
                    )

                    TvRoute.Browse, is TvRoute.Player -> Unit
                }
                menuTarget?.let { target ->
                    Box(Modifier.zIndex(OVERLAY_Z_INDEX + 1f)) {
                        TvChannelMenu(
                            target = target,
                            folders = mainState.folders,
                            actions = menuActions,
                            onDismiss = { menuTarget = null }
                        )
                    }
                }
                AnimatedVisibility(
                    visible = confirmingRights,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.zIndex(OVERLAY_Z_INDEX + 1f)
                ) {
                    TvRightsDialog(onDone = { confirmingRights = false })
                }
            }
        }
    }
}

private fun pinEvent(state: MainState, channel: ChannelSnapshot): MainEvent {
    val login = channel.login.lowercase()
    return when {
        state.openChannels.any { it.login == login } -> MainEvent.CloseChannel(login)
        state.recentChannels.any { it.login == login } -> MainEvent.KeepRecentChannel(login)
        else -> MainEvent.AddChannel(login, channel.avatarUrl, channel.displayName)
    }
}
