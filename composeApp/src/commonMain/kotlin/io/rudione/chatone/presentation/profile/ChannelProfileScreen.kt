package io.rudione.chatone.presentation.profile

import chatone.composeapp.generated.resources.channel_page_close
import chatone.composeapp.generated.resources.channel_page_copy_link
import chatone.composeapp.generated.resources.channel_page_edit_profile
import chatone.composeapp.generated.resources.channel_page_link_copied
import chatone.composeapp.generated.resources.channel_page_load_failed
import chatone.composeapp.generated.resources.channel_page_more
import chatone.composeapp.generated.resources.channel_page_not_found
import chatone.composeapp.generated.resources.channel_page_open_twitch
import chatone.composeapp.generated.resources.channel_page_retry
import chatone.composeapp.generated.resources.channel_page_tab_about
import chatone.composeapp.generated.resources.channel_page_tab_clips
import chatone.composeapp.generated.resources.channel_page_tab_dossier
import chatone.composeapp.generated.resources.channel_page_tab_home
import chatone.composeapp.generated.resources.channel_page_tab_roles
import chatone.composeapp.generated.resources.channel_page_tab_videos
import chatone.composeapp.generated.resources.profile_editor_blocked_by_twitch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.data.repository.AiAssistantController
import chatone.composeapp.generated.resources.profile_ai_button
import chatone.composeapp.generated.resources.Res
import androidx.compose.runtime.CompositionLocalProvider
import io.rudione.chatone.util.link.InAppLinkRouter
import io.rudione.chatone.presentation.components.LinkConfirmDialog
import androidx.compose.material3.Surface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.icons.lucide.ArrowLeft
import io.rudione.chatone.icons.lucide.EllipsisVertical
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.chat.rememberSevenTvCosmetic
import io.rudione.chatone.presentation.chat.roles.rememberUserRoles
import io.rudione.chatone.presentation.components.ChatoneDropdownMenu
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.components.expressive.ExpressiveTabs
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import io.rudione.chatone.presentation.channelmenu.ChannelMenuItem
import io.rudione.chatone.presentation.stream.channel.FollowConfirmDialog
import io.rudione.chatone.util.platform.setText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Immutable
class ChannelProfileNavigation(
    val onClose: () -> Unit,
    val onOpenChat: (login: String, displayName: String, avatarUrl: String) -> Unit,
    val onWatch: (login: String) -> Unit,
    val onEditProfile: () -> Unit,
    val onOpenProfile: (login: String) -> Unit,
    val onOpenClip: (slug: String) -> Unit,
    val onOpenVideo: (id: String) -> Unit
)

@Composable
fun ChannelProfileScreen(
    login: String,
    navigation: ChannelProfileNavigation,
    modifier: Modifier = Modifier
) {
    val channelPageLoadFailedText = stringResource(Res.string.channel_page_load_failed)
    val channelPageLinkCopiedText = stringResource(Res.string.channel_page_link_copied)
    val viewModel: ChannelProfileViewModel =
        koinViewModel(key = "channel_profile_${login.lowercase()}") {
            parametersOf(login.lowercase())
        }
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmFollow by remember { mutableStateOf(false) }
    var clipFilters by remember { mutableStateOf(false) }
    var videoFilters by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    SystemBackHandler(onBack = navigation.onClose)
    val blockedText = stringResource(Res.string.profile_editor_blocked_by_twitch)
    LaunchedEffect(viewModel, blockedText) {
        viewModel.effect.collect { effect ->
            notice = when (effect) {
                ChannelProfileEffect.FollowBlocked -> blockedText
                ChannelProfileEffect.FollowFailed -> channelPageLoadFailedText
            }
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MS)
            notice = null
        }
    }
    var confirmUrl by remember { mutableStateOf<String?>(null) }
    val openUrl: (String) -> Unit = { url -> if (!InAppLinkRouter.dispatch(url)) confirmUrl = url }
    val grid = rememberLazyGridState()
    val scrollOffset by remember {
        derivedStateOf { if (grid.firstVisibleItemIndex == 0) grid.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE }
    }
    val profile = state.profile
    val stuck by remember { derivedStateOf { grid.firstVisibleItemIndex >= TABS_INDEX } }
    val statusTop =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    val tabsTop by animateDpAsState(
        if (stuck) statusTop + TopBarHeight else 0.dp,
        tween(180),
        label = "profileTabsTop"
    )
    val tabSwipe = rememberProfileTabSwipe()

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.load,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "profileLoad",
                modifier = Modifier.fillMaxSize()
            ) { load ->
                when {
                    load == ProfileLoad.READY && profile != null -> {
                        val sevenTv = rememberSevenTvCosmetic(profile.id)
                        val roles = rememberUserRoles(profile.id, profile.login)
                        val accent = profile.accentColor(MaterialTheme.colorScheme.primary)
                        val bottom =
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        val contentActions = remember(profile.login) {
                            ProfileContentActions(
                                onOpenUrl = openUrl,
                                onOpenClip = navigation.onOpenClip,
                                onOpenVideo = navigation.onOpenVideo,
                                onWatch = { navigation.onWatch(profile.login) },
                                onSelectTab = { viewModel.sendEvent(ChannelProfileEvent.SelectTab(it)) },
                                onOpenChannel = navigation.onOpenProfile
                            )
                        }
                        val headerActions = remember(profile.login) {
                            ProfileHeaderActions(
                                onOpenChat = {
                                    navigation.onOpenChat(
                                        profile.login,
                                        profile.displayName,
                                        profile.avatarUrl
                                    )
                                },
                                onWatch = { navigation.onWatch(profile.login) },
                                onFollowClick = { confirmFollow = true },
                                onEdit = navigation.onEditProfile,
                                onOpenLink = openUrl
                            )
                        }
                        val selectTab: (ProfileTab) -> Unit =
                            { viewModel.sendEvent(ChannelProfileEvent.SelectTab(it)) }
                        CompositionLocalProvider(LocalProfileTabSwipe provides tabSwipe) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                state = grid,
                                contentPadding = PaddingValues(
                                    start = GridGutter,
                                    end = GridGutter,
                                    bottom = 24.dp + bottom
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                                    .profileTabSwipe(tabSwipe, state.tab, selectTab)
                            ) {
                                headerSpan("banner") {
                                    Box(Modifier.bleed(GridGutter)) {
                                        ProfileBanner(
                                            profile,
                                            accent
                                        ) { scrollOffset }
                                    }
                                }
                                headerSpan("identity") {
                                    ProfileIdentity(
                                        profile,
                                        sevenTv,
                                        state.isOwn,
                                        state.follow,
                                        headerActions
                                    )
                                }
                                stickyHeader(key = "tabs", contentType = "tabs") {
                                    Box(
                                        Modifier.bleed(GridGutter).fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(top = tabsTop)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        ProfileTabs(state.tab) {
                                            viewModel.sendEvent(
                                                ChannelProfileEvent.SelectTab(it)
                                            )
                                        }
                                    }
                                }
                                when (state.tab) {
                                    ProfileTab.HOME -> homeSection(state, contentActions)
                                    ProfileTab.ABOUT -> aboutSection(state, contentActions)
                                    ProfileTab.CLIPS -> clipsSection(
                                        state,
                                        onOpenFilters = { clipFilters = true },
                                        onLoadMore = { viewModel.sendEvent(ChannelProfileEvent.LoadMoreClips) },
                                        onRetry = {
                                            viewModel.sendEvent(
                                                ChannelProfileEvent.ApplyClipQuery(
                                                    state.clipQuery
                                                )
                                            )
                                        },
                                        onOpenClip = navigation.onOpenClip
                                    )

                                    ProfileTab.VIDEOS -> videosSection(
                                        state,
                                        onOpenFilters = { videoFilters = true },
                                        onLoadMore = { viewModel.sendEvent(ChannelProfileEvent.LoadMoreVideos) },
                                        onRetry = {
                                            viewModel.sendEvent(
                                                ChannelProfileEvent.ApplyVideoQuery(
                                                    state.videoQuery
                                                )
                                            )
                                        },
                                        onOpenVideo = navigation.onOpenVideo
                                    )

                                    ProfileTab.DOSSIER -> dossierSection(
                                        state,
                                        sevenTv,
                                        contentActions
                                    )

                                    ProfileTab.ROLES -> rolesSection(roles, contentActions)
                                }
                            }
                        }
                    }

                    load == ProfileLoad.LOADING -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }

                    else -> Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (load == ProfileLoad.NOT_FOUND) stringResource(Res.string.channel_page_not_found) else stringResource(
                                Res.string.channel_page_load_failed
                            ), style = MaterialTheme.typography.bodyLarge
                        )
                        if (load == ProfileLoad.FAILED) TextButton(onClick = {
                            viewModel.sendEvent(
                                ChannelProfileEvent.Retry
                            )
                        }) { Text(stringResource(Res.string.channel_page_retry)) }
                    }
                }
            }

            val assistant: AiAssistantController = koinInject()
            val aiDraft = profile?.let { profileAiDraft(it) }
            TopBar(
                title = profile?.displayName.orEmpty(),
                titleAlpha = { if (stuck) 1f else ((scrollOffset - 260f) / 160f).coerceIn(0f, 1f) },
                onBack = navigation.onClose,
                onAskAi = aiDraft?.let { draft -> { assistant.openWithDraft(draft) } },
                onMenu = { menuOpen = true },
                menu = {
                    ChatoneDropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }) {
                        val link = "https://www.twitch.tv/${state.login}"
                        ChannelMenuItem(stringResource(Res.string.channel_page_open_twitch), null) {
                            menuOpen = false
                            confirmUrl = link
                        }
                        ChannelMenuItem(stringResource(Res.string.channel_page_copy_link), null) {
                            menuOpen = false
                            scope.launch { clipboard.setText(link) }
                            notice = channelPageLinkCopiedText
                        }
                        if (state.isOwn) {
                            ChannelMenuItem(
                                stringResource(Res.string.channel_page_edit_profile),
                                null
                            ) {
                                menuOpen = false
                                navigation.onEditProfile()
                            }
                        }
                    }
                }
            )

            AnimatedVisibility(
                visible = notice != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    notice.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.expressiveGlass(CircleShape)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
            if (clipFilters) {
                ClipFilterSheet(
                    initial = state.clipQuery,
                    onApply = { viewModel.sendEvent(ChannelProfileEvent.ApplyClipQuery(it)) },
                    onDismiss = { clipFilters = false }
                )
            }
            if (videoFilters) {
                VideoFilterSheet(
                    initial = state.videoQuery,
                    onApply = { viewModel.sendEvent(ChannelProfileEvent.ApplyVideoQuery(it)) },
                    onDismiss = { videoFilters = false }
                )
            }
        }
    }

    confirmUrl?.let { url ->
        LinkConfirmDialog(
            url = url,
            onOpen = { runCatching { uriHandler.openUri(url) } },
            onDismiss = { confirmUrl = null }
        )
    }
    if (confirmFollow && profile != null) {
        FollowConfirmDialog(
            channel = ChannelSnapshot(
                login = profile.login,
                channelId = profile.id,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                title = profile.live?.title.orEmpty(),
                category = profile.live?.category.orEmpty()
            ),
            following = state.follow == FollowStatus.FOLLOWING,
            onConfirm = { viewModel.sendEvent(ChannelProfileEvent.SetFollowing(state.follow != FollowStatus.FOLLOWING)) },
            onDismiss = { confirmFollow = false }
        )
    }
}

@Composable
private fun ProfileTabs(selected: ProfileTab, onSelect: (ProfileTab) -> Unit) {
    val channelPageTabHomeText = stringResource(Res.string.channel_page_tab_home)
    val channelPageTabAboutText = stringResource(Res.string.channel_page_tab_about)
    val channelPageTabClipsText = stringResource(Res.string.channel_page_tab_clips)
    val channelPageTabVideosText = stringResource(Res.string.channel_page_tab_videos)
    val channelPageTabDossierText = stringResource(Res.string.channel_page_tab_dossier)
    val channelPageTabRolesText = stringResource(Res.string.channel_page_tab_roles)
    ExpressiveTabs(
        tabs = ProfileTab.entries,
        selected = selected,
        onSelect = onSelect,
        fill = false,
        label = { tab ->
            when (tab) {
                ProfileTab.HOME -> channelPageTabHomeText
                ProfileTab.ABOUT -> channelPageTabAboutText
                ProfileTab.CLIPS -> channelPageTabClipsText
                ProfileTab.VIDEOS -> channelPageTabVideosText
                ProfileTab.DOSSIER -> channelPageTabDossierText
                ProfileTab.ROLES -> channelPageTabRolesText
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TopBar(
    title: String,
    titleAlpha: () -> Float,
    onBack: () -> Unit,
    onAskAi: (() -> Unit)?,
    onMenu: () -> Unit,
    menu: @Composable () -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    Row(
        Modifier.fillMaxWidth()
            .drawBehind { drawRect(background.copy(alpha = titleAlpha())) }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(TopBarHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            Lucide.ArrowLeft,
            stringResource(Res.string.channel_page_close),
            onBack,
            Modifier.expressiveGlass(CircleShape, elevation = 4.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                .graphicsLayer { alpha = titleAlpha() }
        )
        if (onAskAi != null) {
            GlassIconButton(
                Lucide.Sparkles,
                stringResource(Res.string.profile_ai_button),
                onAskAi,
                Modifier.padding(end = 8.dp).expressiveGlass(CircleShape, elevation = 4.dp)
            )
        }
        Box {
            GlassIconButton(
                Lucide.EllipsisVertical,
                stringResource(Res.string.channel_page_more),
                onMenu,
                Modifier.expressiveGlass(CircleShape, elevation = 4.dp)
            )
            menu()
        }
    }
}

private const val NOTICE_MS = 2_600L
private const val TABS_INDEX = 2
private val TopBarHeight = 56.dp
