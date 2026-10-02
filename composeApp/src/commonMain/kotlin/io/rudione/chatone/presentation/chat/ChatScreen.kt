package io.rudione.chatone.presentation.chat

import io.github.aakira.napier.Napier
import io.rudione.chatone.presentation.stream.LocalStreamPlayerHost
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import io.rudione.chatone.presentation.chat.rendering.ChatFollowEffects
import io.rudione.chatone.presentation.chat.rendering.rememberChatFollowState
import io.rudione.chatone.presentation.chat.rendering.selectionCursor
import io.rudione.chatone.presentation.chat.rendering.stickToBottomExact
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.presentation.chat.components.NotableChatterItem
import io.rudione.chatone.presentation.chat.components.ChatTopBar
import io.rudione.chatone.presentation.chat.components.roomModeLabels
import io.rudione.chatone.presentation.chat.components.ModActionConfirmDialog
import io.rudione.chatone.presentation.chat.components.PendingModAction
import io.rudione.chatone.presentation.chat.components.ChatSearchBar
import io.rudione.chatone.presentation.chat.components.EmoteAutocompleteRow
import io.rudione.chatone.presentation.chat.components.InputCompletionCallbacks
import io.rudione.chatone.presentation.chat.components.InputCompletionState
import io.rudione.chatone.presentation.chat.components.MentionAutocompleteRow
import io.rudione.chatone.presentation.chat.components.MessageInput
import io.rudione.chatone.presentation.chat.components.MessageInputActions
import io.rudione.chatone.presentation.chat.components.MessageInputChrome
import io.rudione.chatone.presentation.chat.components.MessageInputTranslation
import io.rudione.chatone.presentation.chat.components.MessageInputUploadState
import io.rudione.chatone.presentation.chat.components.ReplyBar
import io.rudione.chatone.presentation.chat.components.SlashCommandSuggestionsRow
import io.rudione.chatone.presentation.chat.components.rememberStaggeredMessages
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.settings.PauseHotkeyMode
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.settings.SettingsDestination
import io.rudione.chatone.presentation.settings.SettingsNavigator
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import io.rudione.chatone.domain.model.ChatCommandKind
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.presentation.automod.DetachedAutomodWindow
import io.rudione.chatone.presentation.chat.components.ChannelPointsBitsSheet
import io.rudione.chatone.presentation.chat.components.chatInputBackdrop
import io.rudione.chatone.presentation.components.SwipeDirection
import io.rudione.chatone.presentation.components.directionalSwipe
import io.rudione.chatone.presentation.chat.components.rememberChatInputGlass
import io.rudione.chatone.presentation.chat.rendering.LocalScrollActivity
import io.rudione.chatone.presentation.chat.rendering.rememberDedupedMessages
import io.rudione.chatone.presentation.chat.rendering.rememberScrollActivity
import io.rudione.chatone.presentation.components.ChatoneIconButton
import io.rudione.chatone.presentation.components.ChatoneLazyScrollbar
import io.rudione.chatone.presentation.components.DockPanel
import io.rudione.chatone.presentation.components.LocalDockHost
import io.rudione.chatone.presentation.components.OverlayContentPadding
import io.rudione.chatone.presentation.components.ScrollbarTick
import io.rudione.chatone.presentation.components.backdropSource
import io.rudione.chatone.presentation.components.insetBottom
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.presentation.theme.chatPaneBackgroundColor
import io.rudione.chatone.presentation.theme.luminance
import io.rudione.chatone.util.EmoteAnimationCache
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import io.rudione.chatone.util.automod.DisplayedChatChannels
import io.rudione.chatone.presentation.chat.zoom.rememberChatMessageDensity
import io.rudione.chatone.util.chat.plainText
import io.rudione.chatone.util.media.NotificationSoundPlayer
import io.rudione.chatone.util.media.externalFileDropTarget
import io.rudione.chatone.util.system.handleHover
import io.rudione.chatone.util.system.isDesktopPlatform
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.startup.ReportLaunchStage
import io.rudione.chatone.presentation.startup.launchStage
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.util.chat.SlashCommand
import io.rudione.chatone.util.system.ChannelPanelRequestBus
import io.rudione.chatone.util.system.ChannelPanelRequestBus.openPointsBitsPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.Info
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X


private fun buildScrollbarTicks(
    messages: List<DisplayMessage>,
    mentionColor: Color,
    firstMessageColor: Color,
    highlightedColor: Color
): List<ScrollbarTick> {
    val total = messages.size
    if (total == 0) return emptyList()
    return messages.mapIndexedNotNull { index, msg ->
        if (msg !is DisplayMessage.PrivMsg) return@mapIndexedNotNull null
        val color = when {
            msg.isMention -> msg.highlightColor?.let { Color(it) } ?: mentionColor
            msg.isHighlighted -> highlightedColor
            msg.isFirstMessage -> firstMessageColor
            else -> return@mapIndexedNotNull null
        }
        ScrollbarTick(
            fraction = index.toFloat() / total,
            color = color.copy(alpha = 0.85f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    channelLogin: String,
    identity: ChatSessionIdentity,
    callbacks: ChatScreenCallbacks,
    wallpaper: WallpaperState,
    modifier: Modifier = Modifier,
    options: ChatScreenOptions = ChatScreenOptions(),
    mentionMuteRepository: MentionMuteRepository? = null,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    val liveModButtons by SettingsViewModel.modButtonsLive.collectAsState()
    val liveMacros by SettingsViewModel.macrosLive.collectAsState()
    val effectivePinnedMacros = liveMacros?.let { Macro.pinnedFrom(it) } ?: settingsState.pinnedMacros
    val settingsNavigator: SettingsNavigator = koinInject()
    LaunchedEffect(Unit) {
        println("ModReorder[build-marker] direct-read mod-buttons fix ACTIVE — if you never see this line, you are running a STALE build")
    }
    LaunchedEffect(liveModButtons) {
        println(
            "ModReorder[chatRenderInput@${settingsViewModel.hashCode()}] LIVE=" +
                    (liveModButtons?.joinToString { "${it.id}(${it.sortOrder},en=${it.enabled})" } ?: "null")
        )
    }
    LaunchedEffect(settingsState.allModButtons, settingsState.modButtonsVersion) {
        Napier.d(
            tag = "ModReorder",
            message = "[ChatScreen@${settingsViewModel.hashCode()}] " +
                    "buttons=${settingsState.allModButtons.map { "${it.id}(${it.sortOrder})" }} " +
                    "ver=${settingsState.modButtonsVersion}"
        )
    }
    val s = LocalStrings.current
    val streamHost = LocalStreamPlayerHost.current
    DisposableEffect(channelLogin) {
        DisplayedChatChannels.acquire(channelLogin)
        onDispose { DisplayedChatChannels.release(channelLogin) }
    }
    val surfaceStyle = LocalChatSurfaceStyle.current
    val messageAlphaModifier = remember(surfaceStyle.messageAlpha) {
        if (surfaceStyle.messageAlpha < 1f) Modifier.alpha(surfaceStyle.messageAlpha) else Modifier
    }
    val clipboardManager = LocalClipboardManager.current
    val listState = remember(channelLogin) { LazyListState() }
    val chatScrollActivity =
        rememberScrollActivity(listState)

    val visibleMessages = rememberStaggeredMessages(
        source = state.messages,
        enabled = settingsState.smoothChatEnabled,
        stepMs = 90L
    )
    val dedupedMessages =
        rememberDedupedMessages(
            messages = visibleMessages,
            blockedUserIds = state.blockedUserIds,
            showBlockedMode = state.showBlockedMode
        )
    val renderedCount = dedupedMessages.size

    var showModPanel by remember { mutableStateOf(false) }
    var isFileDragOver by remember { mutableStateOf(false) }
    var showAutomodWindow by remember { mutableStateOf(false) }
    val dockHost = LocalDockHost.current
    LaunchedEffect(showAutomodWindow, dockHost, channelLogin) {
        if (dockHost != null && showAutomodWindow) dockHost.open(
            DockPanel.Automod,
            channelLogin
        )
        if (dockHost != null && !showAutomodWindow) dockHost.closeIf(
            DockPanel.Automod
        )
    }
    DockEvictionEffects(
        dockHost = dockHost,
        onModerationEvicted = { showModPanel = false },
        onAutomodEvicted = { showAutomodWindow = false },
        onEmotesEvicted = {
            if (viewModel.state.value.isEmotePickerVisible) viewModel.sendEvent(ChatEvent.OnToggleEmotePicker)
        },
        onPointsEvicted = {
            if (viewModel.state.value.showPointsBitsPanel) viewModel.sendEvent(ChatEvent.OnClosePointsBitsPanel)
        }
    )
    var messageInputFocused by remember { mutableStateOf(false) }
    val profileStack = rememberProfilePopupStack()
    var detachedProfileMessage by remember { mutableStateOf<DisplayMessage.PrivMsg?>(null) }
    var pendingModAction by remember { mutableStateOf<PendingModAction?>(null) }
    var isPausedByHotkey by remember { mutableStateOf(false) }
    var isHoveredOverChat by remember { mutableStateOf(false) }
    var chatErrorBanner by remember { mutableStateOf<String?>(null) }
    val inputFocusRequester = remember { FocusRequester() }
    var emoteTabIndex by remember { mutableStateOf(-1) }
    var mentionTabIndex by remember { mutableStateOf(-1) }
    var isHoveredOverEmoteTooltip by remember { mutableStateOf(false) }
    var hasNewMessagesWhilePaused by remember { mutableStateOf(false) }
    var chatSearchVisible by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var chatSearchMatchIndex by remember { mutableStateOf(0) }

    var unreadCount by remember { mutableStateOf(0) }
    val chatBoxFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val follow = rememberChatFollowState(listState)
    val modButtonsOnHover = settingsState.modButtonsOnHover && isDesktopPlatform
    LaunchedEffect(viewModel, modButtonsOnHover) {
        viewModel.sendEvent(ChatEvent.OnModModeDefaultChanged(modButtonsOnHover))
    }

    val effectivelyPaused = isPausedByHotkey ||
            (settingsState.pauseOnHover && isHoveredOverChat && !isHoveredOverEmoteTooltip) ||
            follow.isPausedByScroll

    var userScrolledSinceJoin by remember(channelLogin) { mutableStateOf(false) }

    LaunchedEffect(follow.isPausedByScroll) {
        if (follow.isPausedByScroll) {
            userScrolledSinceJoin = true
        } else if (!isPausedByHotkey) {
            hasNewMessagesWhilePaused = false
            unreadCount = 0
        }
    }

    val stickToBottom: suspend () -> Unit = remember(listState) {
        { listState.stickToBottomExact() }
    }

    LaunchedEffect(effectivelyPaused) {
        viewModel.sendEvent(ChatEvent.OnScrollbackPinned(effectivelyPaused))
    }
    val effectivelyPausedLatest by rememberUpdatedState(effectivelyPaused)

    ChatFollowEffects(
        listState = listState,
        newestItemKey = dedupedMessages.lastOrNull()?.id,
        paused = effectivelyPaused,
        onPinnedToBottom = {
            unreadCount = 0
            hasNewMessagesWhilePaused = false
        },
        holdBottomWhilePaused = settingsState.pauseOnHover && isHoveredOverChat &&
                !isPausedByHotkey && !follow.isPausedByScroll
    )

    val messagesSeq = state.messagesSeq
    LaunchedEffect(messagesSeq) {
        if (messagesSeq == 0L) return@LaunchedEffect
        if (effectivelyPaused && state.messages.isNotEmpty()) {
            hasNewMessagesWhilePaused = true
            unreadCount++
        }
    }

    val channelId = state.channelId
    LaunchedEffect(channelId) {
        if (channelId.isNotEmpty()) callbacks.onChannelIdResolved(channelId)
    }

    val historyStickHeldByUser by rememberUpdatedState(
        isPausedByHotkey ||
                (settingsState.pauseOnHover && isHoveredOverChat && !isHoveredOverEmoteTooltip) ||
                userScrolledSinceJoin
    )
    val renderedCountLatest by rememberUpdatedState(renderedCount)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatEffect.ShowError -> {
                    chatErrorBanner = effect.message
                }
                ChatEffect.ScrollToBottom -> {
                    if (!effectivelyPausedLatest && renderedCountLatest > 0) {
                        stickToBottom()
                    }
                }

                ChatEffect.HistoryMerged -> {
                    if (!historyStickHeldByUser) {
                        withFrameNanos { }
                        if (renderedCountLatest > 0) stickToBottom()
                    }
                }

                ChatEffect.FocusChatInput -> {
                    repeat(FOCUS_SETTLE_FRAMES) { withFrameNanos { } }
                    try {
                        inputFocusRequester.requestFocus()
                    } catch (_: Throwable) {
                    }
                }

                is ChatEffect.MentionDetected -> {
                    val mentionChannel = effect.channelLogin
                    val currentChannel = viewModel.state.value.channelLogin
                    val isActiveChannel = mentionChannel.equals(currentChannel, ignoreCase = true)

                    if (!isActiveChannel) {
                        if (effect.playSound && settingsState.mentionSoundEnabled) {
                            NotificationSoundPlayer.playMentionSound(
                                volume = settingsState.mentionSoundVolume,
                                customSoundPath = settingsState.customMentionSoundPath
                            )
                        }
                        callbacks.onMentionDetected(mentionChannel)
                    }

                    val mentionMsg = effect.message
                    if (mentionMsg != null) {
                        val entry = MentionEntry(
                            messageId = mentionMsg.id,
                            channelLogin = mentionChannel,
                            fromUsername = mentionMsg.username,
                            fromDisplayName = mentionMsg.displayName,
                            fromColor = mentionMsg.color,
                            text = mentionMsg.tokens.joinToString("") { token ->
                                token.plainText()
                            },
                            timestamp = mentionMsg.timestamp
                        )
                        callbacks.onMentionReceived(entry)
                    }
                }

                is ChatEffect.OpenUserProfile -> {
                    val syntheticMsg = DisplayMessage.PrivMsg(
                        id = "profile_${effect.userId.ifEmpty { effect.username }}",
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        channel = state.channelLogin,
                        userId = effect.userId,
                        username = effect.username,
                        displayName = effect.displayName,
                        tokens = emptyList(),
                        color = effect.color,
                        badges = emptyList(),
                        isModerator = false,
                        isSubscriber = false,
                        isVip = false,
                        isBroadcaster = false,
                        isMention = false,
                        isAction = false
                    )
                    profileStack.open(syntheticMsg)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        chatBoxFocusRequester.requestFocus()
    }

    val currentSettings by rememberUpdatedState(settingsState)
    val currentIsPausedByHotkey by rememberUpdatedState(isPausedByHotkey)
    val currentRenderedCount by rememberUpdatedState(renderedCount)
    val stickToBottomLatest by rememberUpdatedState(stickToBottom)
    val followLatest by rememberUpdatedState(follow)

    val pauseMouseButton = remember(settingsState.pauseHotkey) {
        hotkeyMouseButton(settingsState.pauseHotkey)
    }
    val resumeFromPause: () -> Unit = remember {
        {
            followLatest.resume()
            coroutineScope.launch {
                if (currentRenderedCount > 0) {
                    stickToBottomLatest()
                    hasNewMessagesWhilePaused = false
                    unreadCount = 0
                }
            }
            Unit
        }
    }

    val chatSearchMatches: List<Int> = remember(chatSearchQuery, dedupedMessages) {
        if (chatSearchQuery.isBlank()) emptyList()
        else {
            val q = chatSearchQuery.lowercase()
            dedupedMessages.mapIndexedNotNull { idx, msg ->
                val text = when (msg) {
                    is DisplayMessage.PrivMsg -> (msg.rawMessage?.message
                        ?: msg.tokens.joinToString("") {
                            it.plainText()
                        }).lowercase()

                    is DisplayMessage.SystemMsg -> msg.text.lowercase()
                    else -> null
                }
                if (text?.contains(q) == true) idx else null
            }
        }
    }
    val chatSearchMatchCount = chatSearchMatches.size
    val chatSearchCurrentIndex = if (chatSearchMatchCount == 0) 0
    else chatSearchMatchIndex.coerceIn(0, chatSearchMatchCount - 1)

    LaunchedEffect(chatSearchCurrentIndex, chatSearchMatches, listState) {
        if (chatSearchMatches.isNotEmpty()) {
            val targetIdx = chatSearchMatches[chatSearchCurrentIndex]
            follow.pause()
            listState.animateScrollToItem(targetIdx)
        }
    }

    LaunchedEffect(options.pendingScrollMessageId, listState) {
        val targetId = options.pendingScrollMessageId ?: return@LaunchedEffect
        if (!channelLogin.equals(state.channelLogin, ignoreCase = true)) return@LaunchedEffect
        repeat(20) {
            val idx = dedupedMessages.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                follow.pause()
                listState.animateScrollToItem(idx)
                callbacks.onScrollToMessageHandled()
                return@LaunchedEffect
            }
            delay(500)
        }
        callbacks.onScrollToMessageHandled()
    }
    val hotkeyHandler = remember<(KeyEvent) -> Boolean> {
        handler@{ event ->
            if (GlobalKeyDispatcher.isTextInputActive) return@handler false
            if (event.type == KeyEventType.KeyDown &&
                event.key == Key.F &&
                (event.isCtrlPressed || event.isMetaPressed)
            ) {
                chatSearchVisible = !chatSearchVisible
                if (!chatSearchVisible) chatSearchQuery = ""
                return@handler true
            }
            if (event.type == KeyEventType.KeyDown &&
                event.key == Key.A &&
                (event.isCtrlPressed || event.isMetaPressed) &&
                !messageInputFocused
            ) {
                showAutomodWindow = !showAutomodWindow
                return@handler true
            }
            if (event.type == KeyEventType.KeyDown) {
                val commands = currentSettings.chatCommands
                if (commands.isNotEmpty()) {
                    val matched = commands.firstOrNull { cmd ->
                        if (!cmd.enabled || cmd.hotkey.isBlank()) return@firstOrNull false
                        val lower = cmd.hotkey.lowercase()
                        val hasMod = "ctrl" in lower || "alt" in lower || "shift" in lower
                        if (!hasMod) return@firstOrNull false
                        pauseHotkeyMatches(event, cmd.hotkey)
                    }
                    if (matched != null) {
                        viewModel.triggerCommandHotkey(matched)
                        return@handler true
                    }
                }
            }
            val hotkey = currentSettings.pauseHotkey
            if (hotkey.isBlank()) return@handler false
            val isHoldMode = currentSettings.pauseHotkeyMode ==
                    PauseHotkeyMode.HOLD

            if (isHoldMode) {
                if (event.type == KeyEventType.KeyDown && pauseHotkeyMatches(event, hotkey)) {
                    if (!currentIsPausedByHotkey) isPausedByHotkey = true
                    return@handler true
                }
                if (event.type == KeyEventType.KeyUp &&
                    currentIsPausedByHotkey &&
                    pauseHotkeyMatchesRelease(event, hotkey)
                ) {
                    isPausedByHotkey = false
                    resumeFromPause()
                    return@handler true
                }
            } else {
                if (event.type == KeyEventType.KeyDown && pauseHotkeyMatches(event, hotkey)) {
                    val wasPaused = currentIsPausedByHotkey
                    isPausedByHotkey = !wasPaused

                    if (wasPaused) resumeFromPause()
                    return@handler true
                }
            }
            false
        }
    }

    DisposableEffect(Unit) {
        val unregister = GlobalKeyDispatcher.register(hotkeyHandler)
        onDispose { unregister() }
    }

    ReportLaunchStage(state.warmup.launchStage())

    LaunchedEffect(channelLogin, identity.accessToken) {
        EmoteAnimationCache.clearAll()
        viewModel.sendEvent(
            ChatEvent.OnInit(
                channelLogin,
                identity.accessToken,
                identity.userId,
                identity.userLogin,
                identity.displayName
            )
        )
    }

    LaunchedEffect(channelLogin) {
        openPointsBitsPanel.collect { requestedLogin ->
            if (requestedLogin.equals(channelLogin, ignoreCase = true)) {
                viewModel.sendEvent(ChatEvent.OnOpenPointsBitsPanel)
            }
        }
    }

    LaunchedEffect(channelLogin) {
        ChannelPanelRequestBus.toggleHiddenEvents.collect { requestedLogin ->
            if (requestedLogin.equals(channelLogin, ignoreCase = true)) {
                viewModel.sendEvent(ChatEvent.OnToggleHiddenEvents)
            }
        }
    }

    val modPanelDocked = dockHost != null && showModPanel && state.canModerate
    val slashSuggestions = remember(state.messageInput, state.isMod, state.isBroadcaster, settingsState.chatCommands) {
        val builtIn = SlashCommand.suggest(
            state.messageInput,
            isMod = state.isMod || state.canModerate,
            isBroadcaster = state.isBroadcaster
        )
        val customCmds = if (state.messageInput.startsWith("/")) {
            val typed = state.messageInput.removePrefix("/").lowercase()
            settingsState.chatCommands
                .filter { it.enabled && it.trigger.startsWith("/") }
                .filter { cmd ->
                    val triggerName = cmd.trigger.removePrefix("/").lowercase()
                    typed.isEmpty() || triggerName.startsWith(typed)
                }
                .map { cmd ->
                    SlashCommand.CommandInfo(
                        name = cmd.trigger.removePrefix("/"),
                        aliases = listOf(cmd.trigger.removePrefix("/")),
                        usage = cmd.trigger,
                        description = when (cmd.kind) {
                            ChatCommandKind.TEXT ->
                                if (cmd.sendImmediately) "→ sends: ${cmd.replacement.take(40)}"
                                else "→ fills: ${cmd.replacement.take(40)}"
                            ChatCommandKind.MACRO -> "→ macro"
                        }
                    )
                }
                .take(5)
        } else emptyList()
        (builtIn + customCmds).distinctBy { it.name }.take(10)
    }
    var slashTabIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(slashSuggestions) {
        slashTabIndex = if (slashSuggestions.isNotEmpty()) 0 else -1
    }
    val chatDensity = LocalDensity.current
    var bottomStackHeightPx by remember { mutableIntStateOf(0) }
    var inputHeightPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(surfaceStyle.showInput) {
        if (!surfaceStyle.showInput) {
            bottomStackHeightPx = 0
            inputHeightPx = 0
        }
    }
    val listContentPadding = remember(chatDensity) {
        OverlayContentPadding(horizontal = 4.dp, vertical = 2.dp, density = chatDensity) { bottomStackHeightPx }
    }
    val chatInputGlass = rememberChatInputGlass(listState = listState, followingLive = !effectivelyPaused)
    val messageDensity = rememberChatMessageDensity()
    val messageScale = messageDensity.density / chatDensity.density

    Box(
        modifier = modifier.fillMaxSize()
            .focusRequester(chatBoxFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event -> hotkeyHandler(event) }
            .externalFileDropTarget(
                enabled = settingsState.imageUploader.isUsable,
                onFilesDropped = { paths ->
                    viewModel.sendEvent(ChatEvent.OnFilesDropped(paths))
                },
                onDragStateChanged = { isFileDragOver = it }
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (surfaceStyle.transparentBackground) Color.Transparent else MaterialTheme.colorScheme.background)
        ) {

            AnimatedVisibility(
                visible = settingsState.showChatHeader && surfaceStyle.showHeader,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                GlowSurface(
                    dominantColor = wallpaper.dominantColor,
                    intensity = 1.1f,
                    centerX = 0.5f,
                    centerY = -0.3f
                ) {
                    ChatTopBar(
                        channelLogin = channelLogin,
                        channelDisplayName = if (state.channelLogin.equals(channelLogin, ignoreCase = true))
                            state.channelDisplayName else "",
                        liveStream = if (state.channelLogin.equals(channelLogin, ignoreCase = true))
                            state.liveStream else null,
                        connectionStatus = state.connectionStatus,
                        isConnected = state.isConnected,
                        roomState = state.roomState,
                        isMod = state.canModerate,
                        modModeEnabled = state.modModeEnabled,
                        modPanelOpen = showModPanel,
                        pinnedMacros = effectivePinnedMacros,
                        onBack = callbacks.onNavigateBack,
                        onToggleModMode = { viewModel.sendEvent(ChatEvent.OnToggleModMode) },
                        onOpenModPanel = { showModPanel = !showModPanel },
                        onExecuteMacro = { macro ->
                            viewModel.sendEvent(
                                ChatEvent.OnExecuteMacro(
                                    macro
                                )
                            )
                        },
                        onEditMacro = { macro ->
                            settingsNavigator.open(SettingsDestination.EditMacro(macro.id))
                        },
                        onRefresh = {
                            viewModel.sendEvent(ChatEvent.OnRefreshChannel)
                            streamHost?.reloadIfWatching(channelLogin)
                        },
                        onOpenPointsBits = { viewModel.sendEvent(ChatEvent.OnOpenPointsBitsPanel) },
                        isCompact = !options.isWideScreen,
                        showMenuButton = !options.isWideScreen && !options.isMultiChat,
                        menuBadgeCount = options.menuBadgeCount,
                        barHeight = settingsState.chatTopBarHeight.dp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .backdropSource(
                        state = chatInputGlass.backdrop,
                        blurRadius = chatInputGlass.style.blurRadius,
                        saturation = chatInputGlass.style.saturation,
                        underlay = if (surfaceStyle.transparentBackground) Color.Unspecified else MaterialTheme.colorScheme.background,
                        strength = chatInputGlass::progress
                    )
                    .then(
                        if (!options.isWideScreen && !options.isMultiChat) {
                            Modifier.directionalSwipe(SwipeDirection.Right, callbacks.onNavigateBack)
                        } else Modifier
                    )
            ) {
                val wallpaperCtrl = LocalWallpaperController.current
                val liveWallpaper by remember { derivedStateOf { wallpaperCtrl.state } }
                val isDarkChat = MaterialTheme.colorScheme.background.luminance() < 0.4f
                if (options.renderBackground && !surfaceStyle.transparentBackground) {
                    ChatBackgroundLayer(
                        wallpaper = liveWallpaper,
                        modifier = Modifier.fillMaxSize(),
                        darkTheme = isDarkChat
                    ) {}
                }

                val chatPaneDefaultColor = MaterialTheme.colorScheme.surfaceContainer
                if (!surfaceStyle.transparentBackground) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                remember(
                                    liveWallpaper.displayConfig,
                                    liveWallpaper.dominantColor,
                                    chatPaneDefaultColor
                                ) {
                                    chatPaneBackgroundColor(liveWallpaper, chatPaneDefaultColor)
                                }
                            )
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isLoading || state.messages.isEmpty()) {
                        ChatWaitingState(isLoading = state.isLoading)
                    } else {
                        val rowDecorations = rememberChatRowDecorations(
                            state = state,
                            dedupedMessages = dedupedMessages,
                            channelLogin = channelLogin,
                            alternateRows = settingsState.alternateRowBackground,
                            showRepeatCounter = settingsState.showRepeatedMessageCounter,
                            repeatWindowSeconds = settingsState.repeatedMessageWindow
                        )
                        key(channelLogin) {
                          CompositionLocalProvider(
                            LocalScrollActivity provides chatScrollActivity
                          ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                        .nestedScroll(follow.nestedScrollConnection)
                                        .selectionCursor().handleHover(
                                        onEnter = {
                                            if (settingsState.pauseOnHover) isHoveredOverChat = true
                                        },
                                        onExit = { isHoveredOverChat = false }
                                    )
                                        .then(
                                            if (pauseMouseButton != null) {
                                                Modifier.pointerInput(
                                                    pauseMouseButton,
                                                    settingsState.pauseHotkeyMode
                                                ) {
                                                    awaitPointerEventScope {
                                                        var buttonHeld = false
                                                        while (true) {
                                                            val event =
                                                                awaitPointerEvent(PointerEventPass.Initial)
                                                            val pressed = event.buttons
                                                                .isPauseButtonPressed(pauseMouseButton)
                                                            if (pressed == buttonHeld) continue
                                                            buttonHeld = pressed
                                                            if (settingsState.pauseHotkeyMode == PauseHotkeyMode.HOLD) {
                                                                isPausedByHotkey = pressed
                                                                if (!pressed) resumeFromPause()
                                                            } else if (pressed) {
                                                                val wasPaused = isPausedByHotkey
                                                                isPausedByHotkey = !wasPaused
                                                                if (wasPaused) resumeFromPause()
                                                            }
                                                        }
                                                    }
                                                }
                                            } else Modifier
                                        )
                                        .then(
                                            if (settingsState.disableScrollOnAlt && isPausedByHotkey) {
                                                Modifier.pointerInput(isPausedByHotkey) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            val event =
                                                                awaitPointerEvent(PointerEventPass.Initial)
                                                            if (event.type == PointerEventType.Scroll) {
                                                                event.changes.forEach { it.consume() }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else Modifier
                                        )
                                        .padding(end = 10.dp),
                                    contentPadding = listContentPadding,
                                    verticalArrangement = Arrangement.spacedBy(
                                        when (settingsState.messageSpacing) {
                                            SettingsState.MessageSpacing.NONE -> 0.dp
                                            SettingsState.MessageSpacing.LOW -> 1.dp
                                            SettingsState.MessageSpacing.MEDIUM -> 3.dp
                                            SettingsState.MessageSpacing.HIGH -> 5.dp
                                        } * messageScale
                                    )
                                ) {
                                    itemsIndexed(
                                        items = dedupedMessages,
                                        key = { _, it -> it.id },
                                        contentType = { _, it -> it::class }) { _, message ->
                                        val zebraTintColor =
                                            if (settingsState.alternateRowBackground &&
                                                rowDecorations.zebraParityById[message.id] == true
                                            ) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f)
                                            } else Color.Transparent
                                        CompositionLocalProvider(LocalDensity provides messageDensity) {
                                        ChatDayBreakFrame(rowDecorations.dayBreakById[message.id]) {
                                        when (message) {
                                            is DisplayMessage.PrivMsg -> PrivMsgItem(
                                                message = message,
                                                isCompact = !options.isWideScreen,
                                                zebraTint = zebraTintColor,
                                                showModActions = state.modModeEnabled,
                                                modActionsOnHover = modButtonsOnHover,
                                                timestampFormat = settingsState.timestampFormat,
                                                showBadges = settingsState.showBadges,
                                                repeatCount = rowDecorations.repeatCountByMsgId[message.id] ?: 1,
                                                isMod = state.canModerate || message.isBroadcaster,
                                                currentUserId = state.currentUserId,
                                                emoteSize = settingsState.emoteSize,
                                                customModButtons = settingsState.customModButtons,
                                                allModButtons = liveModButtons ?: settingsState.allModButtons,
                                                modButtonsVersion = settingsState.modButtonsVersion,
                                                extraVerticalPadding = when (settingsState.messageSpacing) {
                                                    SettingsState.MessageSpacing.NONE -> 0.dp
                                                    SettingsState.MessageSpacing.LOW -> 1.dp
                                                    SettingsState.MessageSpacing.MEDIUM -> 2.dp
                                                    SettingsState.MessageSpacing.HIGH -> 4.dp
                                                },
                                                showCustomModButtons = canActOnUser(
                                                    actorIsBroadcaster = state.isBroadcaster,
                                                    actorIsMod = state.isMod,
                                                    targetIsBroadcaster = message.isBroadcaster,
                                                    targetIsMod = message.isModerator,
                                                    targetIsVip = message.isVip,
                                                    targetIsSubscriber = message.isSubscriber,
                                                    actorIsGrandMod = state.isGrandMod,
                                                    targetIsGrandMod = message.isGrandMod
                                                ),
                                                actorCanModerate = state.canModerate,
                                                actorIsBroadcaster = state.isBroadcaster,
                                                showDefaultDeleteButton = settingsState.showDefaultDeleteButton,
                                                showDefaultTimeoutButton = settingsState.showDefaultTimeoutButton &&
                                                        canActOnUser(
                                                            actorIsBroadcaster = state.isBroadcaster,
                                                            actorIsMod = state.isMod,
                                                            targetIsBroadcaster = message.isBroadcaster,
                                                            targetIsMod = message.isModerator,
                                                            targetIsVip = message.isVip,
                                                            targetIsSubscriber = message.isSubscriber,
                                                            actorIsGrandMod = state.isGrandMod,
                                                            targetIsGrandMod = message.isGrandMod
                                                        ),
                                                showDefaultBanButton = settingsState.showDefaultBanButton &&
                                                        canActOnUser(
                                                            actorIsBroadcaster = state.isBroadcaster,
                                                            actorIsMod = state.isMod,
                                                            targetIsBroadcaster = message.isBroadcaster,
                                                            targetIsMod = message.isModerator,
                                                            targetIsVip = message.isVip,
                                                            targetIsSubscriber = message.isSubscriber,
                                                            actorIsGrandMod = state.isGrandMod,
                                                            targetIsGrandMod = message.isGrandMod
                                                        ),
                                                chatFontSizeSp = when (settingsState.fontSize) {
                                                    SettingsState.FontSize.SMALL -> 12f
                                                    SettingsState.FontSize.MEDIUM -> 15f
                                                    SettingsState.FontSize.LARGE -> 18f
                                                },
                                                onUsernameClick = {
                                                    profileStack.open(message)
                                                },
                                                onRightClickUsername = { displayName ->

                                                    viewModel.sendEvent(
                                                        ChatEvent.OnInsertMention(
                                                            displayName
                                                        )
                                                    )
                                                },
                                                onMentionClick = { mentionedUsername ->
                                                    val cleanLogin =
                                                        mentionedUsername.removePrefix("@")
                                                    val mentionedMsg = state.messages
                                                        .filterIsInstance<DisplayMessage.PrivMsg>()
                                                        .lastOrNull {
                                                            it.username.equals(
                                                                cleanLogin,
                                                                ignoreCase = true
                                                            )
                                                        }
                                                    if (mentionedMsg != null) {
                                                        profileStack.open(mentionedMsg)
                                                    } else {
                                                        profileStack.openLogin(
                                                            login = cleanLogin,
                                                            channel = state.channelLogin,
                                                            nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
                                                        )
                                                    }
                                                },
                                                onReply = {
                                                    viewModel.sendEvent(
                                                        ChatEvent.OnReplyToMessage(
                                                            message
                                                        )
                                                    )
                                                },
                                                onPin = {
                                                    viewModel.sendEvent(
                                                        ChatEvent.OnPinMessage(
                                                            message.id
                                                        )
                                                    )
                                                },
                                                onTimeout = {
                                                    if (settingsState.confirmModActions) {
                                                        pendingModAction = PendingModAction.Timeout(
                                                            message.userId,
                                                            message.displayName,
                                                            settingsState.defaultTimeoutDuration
                                                        )
                                                    } else {
                                                        viewModel.sendEvent(
                                                            ChatEvent.OnTimeoutUser(
                                                                message.userId,
                                                                settingsState.defaultTimeoutDuration
                                                            )
                                                        )
                                                    }
                                                },
                                                onCustomTimeout = { seconds ->
                                                    viewModel.sendEvent(
                                                        ChatEvent.OnTimeoutUser(
                                                            message.userId,
                                                            seconds
                                                        )
                                                    )
                                                },
                                                onBan = {
                                                    if (settingsState.confirmModActions) {
                                                        pendingModAction = PendingModAction.Ban(
                                                            message.userId,
                                                            message.displayName
                                                        )
                                                    } else {
                                                        viewModel.sendEvent(
                                                            ChatEvent.OnBanUser(
                                                                message.userId
                                                            )
                                                        )
                                                    }
                                                },
                                                onDelete = {
                                                    viewModel.sendEvent(
                                                        ChatEvent.OnDeleteMessage(
                                                            message.id
                                                        )
                                                    )
                                                },
                                                searchHighlightQuery = chatSearchQuery,
                                                highlightRules = settingsState.highlightRules,
                                                userColorByLogin = rowDecorations.userColorByLogin,
                                                accessToken = state.currentAccessToken,
                                                modifier = messageAlphaModifier
                                            )

                                            is DisplayMessage.SystemMsg -> SystemMsgItem(message)
                                            is DisplayMessage.NotableChatterMsg -> NotableChatterItem(
                                                message = message,
                                                chatFontSizeSp = when (settingsState.fontSize) {
                                                    SettingsState.FontSize.SMALL -> 12f
                                                    SettingsState.FontSize.MEDIUM -> 15f
                                                    SettingsState.FontSize.LARGE -> 18f
                                                },
                                                onClick = {
                                                    val found = state.messages
                                                        .filterIsInstance<DisplayMessage.PrivMsg>()
                                                        .lastOrNull { it.userId == message.userId }
                                                    profileStack.open(
                                                        found ?: DisplayMessage.PrivMsg(
                                                            id = "notable_profile_${message.userId}",
                                                            timestamp = message.timestamp,
                                                            channel = message.channel,
                                                            userId = message.userId,
                                                            username = message.login,
                                                            displayName = message.displayName,
                                                            color = message.color,
                                                            tokens = emptyList(),
                                                            badges = emptyList(),
                                                            isModerator = false,
                                                            isSubscriber = false,
                                                            isVip = false,
                                                            isBroadcaster = false,
                                                            isMention = false,
                                                            isAction = false
                                                        )
                                                    )
                                                }
                                            )
                                            is DisplayMessage.UserNoticeMsg -> UserNoticeMsgItem(
                                                message
                                            )

                                            is DisplayMessage.ModerationMsg -> ModerationMsgItem(
                                                message = message,
                                                chatFontSizeSp = when (settingsState.fontSize) {
                                                    SettingsState.FontSize.SMALL -> 12f
                                                    SettingsState.FontSize.MEDIUM -> 15f
                                                    SettingsState.FontSize.LARGE -> 18f
                                                },
                                                onUsernameClick = { targetUser ->
                                                    val found = state.messages
                                                        .filterIsInstance<DisplayMessage.PrivMsg>()
                                                        .lastOrNull {
                                                            it.username.equals(
                                                                targetUser,
                                                                ignoreCase = true
                                                            )
                                                        }
                                                    if (found != null) {
                                                        profileStack.open(found)
                                                    } else {
                                                        profileStack.openLogin(
                                                            login = targetUser,
                                                            channel = state.channelLogin,
                                                            nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
                                                        )
                                                    }
                                                },
                                                onUnbanByLogin = if (state.canModerate) { login ->
                                                    val userId = state.messages
                                                        .filterIsInstance<DisplayMessage.PrivMsg>()
                                                        .lastOrNull {
                                                            it.username.equals(
                                                                login,
                                                                ignoreCase = true
                                                            )
                                                        }
                                                        ?.userId ?: login
                                                    viewModel.sendEvent(ChatEvent.OnUnbanUser(userId))
                                                } else null
                                            )

                                            is DisplayMessage.AutoModMsg -> AutoModMsgItem(
                                                message = message,
                                                onAllow = {
                                                    viewModel.sendEvent(
                                                        ChatEvent.OnAllowAutoModMessage(
                                                            message.msgId
                                                        )
                                                    )
                                                },
                                                onDeny = {
                                                    viewModel.sendEvent(
                                                        ChatEvent.OnDenyAutoModMessage(
                                                            message.msgId
                                                        )
                                                    )
                                                },
                                                onUsernameClick = {
                                                    val syntheticMsg = DisplayMessage.PrivMsg(
                                                        id = "automod_profile_${message.userId}",
                                                        timestamp = message.timestamp,
                                                        channel = message.channel,
                                                        userId = message.userId,
                                                        username = message.username,
                                                        displayName = message.displayName,
                                                        color = message.color,
                                                        tokens = emptyList(),
                                                        badges = emptyList(),
                                                        isModerator = false,
                                                        isSubscriber = false,
                                                        isVip = false,
                                                        isBroadcaster = false,
                                                        isMention = false,
                                                        isAction = false
                                                    )
                                                    profileStack.open(syntheticMsg)
                                                }
                                            )
                                        }
                                        }
                                        }
                                    }
                                }
                                val rules = settingsState.highlightRules
                                fun ruleTint(id: String, fallback: Long) =
                                    Color(rules.firstOrNull { it.id == id }?.color ?: fallback)
                                val tickMentionColor = ruleTint("username", 0xFFFF6B6B)
                                val tickFirstMessageColor =
                                    ruleTint(
                                        "first_message",
                                        HighlightRule.FIRST_MESSAGE_RULE.color
                                    )
                                val tickHighlightedColor = ruleTint("channel_points", 0xFF9146FF)
                                val scrollbarTicks = remember(
                                    dedupedMessages,
                                    tickMentionColor,
                                    tickFirstMessageColor,
                                    tickHighlightedColor
                                ) {
                                    buildScrollbarTicks(
                                        messages = dedupedMessages,
                                        mentionColor = tickMentionColor,
                                        firstMessageColor = tickFirstMessageColor,
                                        highlightedColor = tickHighlightedColor
                                    )
                                }
                                if (surfaceStyle.showScrollbar) {
                                    ChatoneLazyScrollbar(
                                        listState = listState,
                                        itemCount = renderedCount,
                                        ticks = scrollbarTicks,
                                        onUserScroll = follow::onUserScrolled,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .insetBottom { bottomStackHeightPx }
                                            .width(settingsState.chatScrollbarWidth.dp)
                                    )
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = chatSearchVisible,
                                    enter = fadeIn() + androidx.compose.animation.slideInVertically { -it / 2 },
                                    exit = fadeOut() + androidx.compose.animation.slideOutVertically { -it / 2 },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 12.dp).zIndex(10f)
                                ) {
                                    ChatSearchBar(
                                        query = chatSearchQuery,
                                        matchCount = chatSearchMatchCount,
                                        currentMatchIndex = chatSearchCurrentIndex,
                                        onQueryChange = {
                                            chatSearchQuery = it; chatSearchMatchIndex = 0
                                        },
                                        onPrevious = {
                                            if (chatSearchMatchCount > 0)
                                                chatSearchMatchIndex =
                                                    (chatSearchCurrentIndex - 1 + chatSearchMatchCount) % chatSearchMatchCount
                                        },
                                        onNext = {
                                            if (chatSearchMatchCount > 0)
                                                chatSearchMatchIndex =
                                                    (chatSearchCurrentIndex + 1) % chatSearchMatchCount
                                        },
                                        onClose = {
                                            chatSearchVisible = false; chatSearchQuery = ""
                                        }
                                    )
                                }

                                if ((hasNewMessagesWhilePaused || follow.isPausedByScroll || isPausedByHotkey) && state.messages.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                            .offset { IntOffset(0, -bottomStackHeightPx) }
                                            .padding(12.dp)
                                    ) {
                                        SmallFloatingActionButton(
                                            onClick = {
                                                follow.resume()
                                                isPausedByHotkey = false
                                                isHoveredOverChat = false
                                                hasNewMessagesWhilePaused = false
                                                unreadCount = 0
                                                coroutineScope.launch {
                                                    if (renderedCount > 0) stickToBottom()
                                                }
                                            },
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Icon(
                                                Lucide.ChevronDown,
                                                contentDescription = s.chatScrollToBottom
                                            )
                                        }
                                        if (unreadCount > 0) {
                                            val badgeText =
                                                if (unreadCount > 99) "99+" else "$unreadCount"
                                            Surface(
                                                modifier = Modifier.align(Alignment.TopEnd)
                                                    .offset(x = 4.dp, y = (-4).dp)
                                                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.error,
                                                shadowElevation = 2.dp
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        color = MaterialTheme.colorScheme.onError,
                                                        fontSize = 10.sp,
                                                        lineHeight = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                          }
                        }
                    }
                }

                ChatEventOverlaysSection(
                    state = state,
                    viewModel = viewModel,
                    showPinnedMessage = surfaceStyle.showPinnedMessage,
                    scrollbarWidthDp = settingsState.chatScrollbarWidth,
                    dockHost = dockHost,
                    bottomInsetPx = { bottomStackHeightPx }
                )
            }

            if (modPanelDocked) {
                val panelBody = rememberUpdatedState<@Composable () -> Unit> {
                    ChatModerationPanelSection(
                        state = state,
                        viewModel = viewModel,
                        channelLogin = channelLogin,
                        pinnedMacros = effectivePinnedMacros,
                        onOpenLocalAutomod = { showAutomodWindow = true },
                        onClose = { showModPanel = false }
                    )
                }
                LaunchedEffect(modPanelDocked) {
                    dockHost?.openWith(
                        DockPanel.Moderation
                    ) { panelBody.value() }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        dockHost?.closeIf(
                            DockPanel.Moderation
                        )
                    }
                }
            }

            if (showAutomodWindow && dockHost == null) {
                DetachedAutomodWindow(
                    currentChannelLogin = channelLogin,
                    onClose = { showAutomodWindow = false }
                )
            }

            val pointsDocked = dockHost != null && state.showPointsBitsPanel
            if (pointsDocked) {
                val pointsBody = rememberUpdatedState<@Composable () -> Unit> {
                    ChannelPointsBitsSheet(
                        balance = state.pointsBalance,
                        pointsIconUrl = state.pointsIconUrl,
                        bitsCount = 0L,
                        rewards = state.channelRewards,
                        isLoading = state.pointsBitsLoading,
                        error = state.pointsBitsError,
                        onRedeem = { reward, text ->
                            viewModel.sendEvent(ChatEvent.OnRedeemReward(reward, text))
                        },
                        onClose = { viewModel.sendEvent(ChatEvent.OnClosePointsBitsPanel) },
                        docked = true
                    )
                }
                LaunchedEffect(pointsDocked) {
                    dockHost?.openWith(
                        DockPanel.Points
                    ) { pointsBody.value() }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        dockHost?.closeIf(
                            DockPanel.Points
                        )
                    }
                }
            }

        }

        val restingBarColor = topBarBackgroundColor(
            LocalWallpaperController.current.state,
            MaterialTheme.colorScheme.surfaceContainer
        )
        if (surfaceStyle.showInput) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomStackHeightPx = it.height }
                    .chatInputBackdrop(
                        glass = chatInputGlass,
                        restingBar = if (surfaceStyle.transparentBackground) Color.Transparent else restingBarColor,
                        scrim = if (surfaceStyle.transparentBackground) Color.Transparent else MaterialTheme.colorScheme.background
                    )
            ) {
                AnimatedVisibility(
                    visible = showModPanel && state.canModerate && !modPanelDocked,
                    enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
                ) {
                    ChatModerationPanelSection(
                        state = state,
                        viewModel = viewModel,
                        channelLogin = channelLogin,
                        pinnedMacros = effectivePinnedMacros,
                        onOpenLocalAutomod = { showAutomodWindow = true },
                        onClose = { showModPanel = false }
                    )
                }
                state.replyingTo?.let { replyMsg ->
                    ReplyBar(
                        displayName = replyMsg.displayName,
                        messagePreview = replyMsg.tokens.joinToString("") { token ->
                            token.plainText()
                        },
                        onCancel = { viewModel.sendEvent(ChatEvent.OnCancelReply) },
                        modifier = Modifier
                            .padding(horizontal = chatInputGlass.style.horizontalMargin)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                if (slashSuggestions.isNotEmpty()) {
                    SlashCommandSuggestionsRow(
                        commands = slashSuggestions,
                        selectedIndex = slashTabIndex,
                        onPick = { name ->
                            viewModel.sendEvent(ChatEvent.OnMessageInputChanged("/$name "))
                            slashTabIndex = -1
                            inputFocusRequester.requestFocus()
                        }
                    )
                }
                ChatErrorBanner(
                    message = chatErrorBanner,
                    onDismiss = { chatErrorBanner = null }
                )
                MessageInput(
                    modifier = Modifier
                        .onSizeChanged { inputHeightPx = it.height }
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom)
                        ),
                    value = state.messageInput,
                    onValueChange = {
                        emoteTabIndex = -1
                        mentionTabIndex = -1
                        viewModel.sendEvent(ChatEvent.OnMessageInputChanged(it))
                    },
                    enabled = state.isConnected && !state.isBanned,
                    focusRequester = inputFocusRequester,
                    actions = MessageInputActions(
                        onSend = { viewModel.sendEvent(ChatEvent.OnSendMessage) },
                        onSendKeepText = { viewModel.sendEvent(ChatEvent.OnSendMessageKeepText) },
                        onHistoryUp = { viewModel.sendEvent(ChatEvent.OnHistoryUp) },
                        onHistoryDown = { viewModel.sendEvent(ChatEvent.OnHistoryDown) },
                        onEmotePickerClick = {
                            viewModel.sendEvent(ChatEvent.OnToggleEmotePicker)
                        },
                        onTogglePause = { },
                        onFocusChanged = { messageInputFocused = it }
                    ),
                    chrome = MessageInputChrome(
                        isBanned = state.isBanned,
                        banReason = state.banReason,
                        hidePlaceholder = settingsState.hideChatInputPlaceholder,
                        hideEmojiButton = settingsState.hideEmojiButton,
                        placeholderText = when {
                            state.isBanned -> "You are banned" + (state.banReason?.let { " — $it" }
                                ?: "") + " in #${state.channelLogin}"

                            state.channelLogin.isNotEmpty() -> {
                                val strings = LocalStrings.current
                                val name = state.channelDisplayName.ifBlank { state.channelLogin }
                                val modes = roomModeLabels(state.roomState, strings)
                                strings.chatSendMessageIn.replace("{0}", name) +
                                    if (modes.isNotEmpty()) "  ·  ${modes.joinToString(" · ")}" else ""
                            }
                            else -> null
                        },
                        pauseHotkey = "",
                        glowIntensity = if (settingsState.chatInputEventGlow) state.inputGlowIntensity else 0f,
                        glowTriggerTs = state.inputGlowTriggerTs,
                        slowModeSeconds = if (!state.isMod && !state.isBroadcaster && !state.isGrandMod) state.roomState.slowMode else 0,
                        lastMessageSentAtMs = state.lastMessageSentAtMs
                    ),
                    completions = InputCompletionState(
                        showEmote = state.showEmoteCompletions && state.emoteCompletions.isNotEmpty(),
                        emoteCount = state.emoteCompletions.size,
                        emoteTabIndex = emoteTabIndex,
                        showMention = state.showMentionCompletions && state.mentionCompletions.isNotEmpty(),
                        mentionCount = state.mentionCompletions.size,
                        mentionTabIndex = mentionTabIndex,
                        showSlash = slashSuggestions.isNotEmpty(),
                        slashCount = slashSuggestions.size,
                        slashTabIndex = slashTabIndex
                    ),
                    completionCallbacks = InputCompletionCallbacks(
                        onTabEmote = { idx -> emoteTabIndex = idx },
                        onConfirmEmote = {
                            val idx = emoteTabIndex.coerceIn(0, state.emoteCompletions.lastIndex)
                            viewModel.sendEvent(ChatEvent.OnSelectEmoteCompletion(state.emoteCompletions[idx]))
                            emoteTabIndex = -1
                            inputFocusRequester.requestFocus()
                        },
                        onTabMention = { idx -> mentionTabIndex = idx },
                        onConfirmMention = {
                            val idx = mentionTabIndex.coerceIn(0, state.mentionCompletions.lastIndex)
                            viewModel.sendEvent(ChatEvent.OnSelectMentionCompletion(state.mentionCompletions[idx]))
                            mentionTabIndex = -1
                            inputFocusRequester.requestFocus()
                        },
                        onTabSlash = { slashTabIndex = it },
                        onConfirmSlash = {
                            val idx = slashTabIndex.coerceIn(0, slashSuggestions.lastIndex)
                            val name = slashSuggestions[idx].name
                            viewModel.sendEvent(ChatEvent.OnMessageInputChanged("/$name "))
                            slashTabIndex = -1
                            inputFocusRequester.requestFocus()
                        }
                    ),
                    upload = MessageInputUploadState(
                        progress = state.uploadProgress,
                        link = state.uploadedLink,
                        onCopyLink = {
                            state.uploadedLink?.let { link ->
                                clipboardManager.setText(AnnotatedString(link))
                            }
                        }
                    ),
                    translation = MessageInputTranslation(
                        targetLang = settingsState.translationTargetLang,
                        autoEnabled = settingsState.autoTranslateInput,
                        onToggleAuto = { settingsViewModel.sendEvent(SettingsEvent.OnAutoTranslateInputChanged(it)) }
                    ),
                    glass = chatInputGlass
                )
            }
        }

        if (state.showMentionCompletions && state.mentionCompletions.isNotEmpty()) {
            MentionAutocompleteRow(
                modifier = Modifier.insetBottom { inputHeightPx },
                usernames = state.mentionCompletions,
                selectedIndex = mentionTabIndex,
                onSelect = {
                    viewModel.sendEvent(ChatEvent.OnSelectMentionCompletion(it))
                    mentionTabIndex = -1
                    inputFocusRequester.requestFocus()
                },
                onDismiss = {
                    viewModel.sendEvent(ChatEvent.OnDismissMentionCompletions)
                    mentionTabIndex = -1
                }
            )
        }

        if (state.showEmoteCompletions && state.emoteCompletions.isNotEmpty()) {
            EmoteAutocompleteRow(
                modifier = Modifier.insetBottom { inputHeightPx },
                emotes = state.emoteCompletions,
                selectedIndex = emoteTabIndex,
                onSelect = {
                    viewModel.sendEvent(ChatEvent.OnSelectEmoteCompletion(it))
                    emoteTabIndex = -1
                    inputFocusRequester.requestFocus()
                },
                onDismiss = {
                    viewModel.sendEvent(ChatEvent.OnDismissCompletions)
                    emoteTabIndex = -1
                }
            )
        }

        if (isFileDragOver) {
            ChatFileDropHint()
        }

        detachedProfileMessage?.let { msg ->
            ChatDetachedProfileSection(
                msg = msg,
                state = state,
                viewModel = viewModel,
                channelLogin = channelLogin,
                onOpenWhisper = callbacks.onOpenWhisper,
                onClose = { detachedProfileMessage = null }
            )
        }
    }

    state.pendingUploadPath?.let { pendingPath ->
        val pendingName = pendingPath.substringAfterLast('/').substringAfterLast('\\')
        val pendingHost = state.pendingUploadHost.orEmpty()
        AlertDialog(
            onDismissRequest = { viewModel.sendEvent(ChatEvent.OnCancelPendingUpload) },
            title = { Text(s.uploaderConfirmTitle, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    s.uploaderConfirmText.replace("{0}", pendingName).replace("{1}", pendingHost) +
                        if (state.pendingUploadHostIsNew) "\n\n" + s.uploaderNewHostWarning else ""
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.sendEvent(ChatEvent.OnConfirmPendingUpload) }) {
                    Text(s.uploaderConfirmYes)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.sendEvent(ChatEvent.OnCancelPendingUpload) }) {
                    Text(s.uploaderConfirmNo)
                }
            }
        )
    }

    if (state.isEmotePickerVisible) {
        ChatEmotePickerSection(
            state = state,
            viewModel = viewModel,
            channelLogin = channelLogin,
            closeOnMouseLeave = settingsState.closeEmotePickerOnMouseLeave,
            dockHost = dockHost,
            inputHeightPx = { inputHeightPx }
        )
    }

    profileStack.slots.forEach { slot ->
        key(slot.key) {
            ChatProfilePopupSection(
                msg = slot.message,
                state = state,
                viewModel = viewModel,
                channelLogin = channelLogin,
                currentUserId = identity.userId,
                mentionMuteRepository = mentionMuteRepository,
                onOpenWhisper = callbacks.onOpenWhisper,
                onDetach = { detached ->
                    detachedProfileMessage = detached
                    profileStack.close(slot)
                },
                onPinnedChange = { pinned -> profileStack.setPinned(slot, pinned) },
                onDismiss = { profileStack.close(slot) }
            )
        }
    }

    pendingModAction?.let { action ->
        ModActionConfirmDialog(
            action = action,
            onConfirm = {
                when (action) {
                    is PendingModAction.Timeout -> viewModel.sendEvent(
                        ChatEvent.OnTimeoutUser(action.userId, action.duration)
                    )

                    is PendingModAction.Ban -> viewModel.sendEvent(ChatEvent.OnBanUser(action.userId))
                }
                pendingModAction = null
            },
            onDismiss = { pendingModAction = null }
        )
    }
}

@Composable
expect fun DetachedProfileWindow(
    msg: DisplayMessage.PrivMsg,
    channelMessages: List<DisplayMessage>,
    accessToken: String,
    channelId: String,
    showModActions: Boolean,
    currentUserIsBroadcaster: Boolean,
    isBlocked: Boolean = false,
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
    onTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onMod: () -> Unit,
    onUnmod: () -> Unit,
    onVip: () -> Unit,
    onUnvip: () -> Unit,
    onWhisper: () -> Unit,
    onClose: () -> Unit
)

@Composable
private fun ChatErrorBanner(message: String?, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(6000)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val text = remember(message) { message.orEmpty() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                    RoundedCornerShape(10.dp)
                )
                .padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Lucide.Info,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            ChatoneIconButton(onClick = onDismiss, modifier = Modifier.size(22.dp)) {
                Icon(
                    Lucide.X,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private const val FOCUS_SETTLE_FRAMES = 2