package io.rudione.chatone.presentation.chat

import io.github.aakira.napier.Napier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import io.rudione.chatone.presentation.chat.rendering.bottomOverflow
import io.rudione.chatone.presentation.chat.rendering.selectionCursor
import io.rudione.chatone.presentation.chat.rendering.stickToBottomExact
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.presentation.chat.components.ChatTopBar
import io.rudione.chatone.presentation.chat.components.roomModeLabels
import io.rudione.chatone.presentation.chat.components.ModActionConfirmDialog
import io.rudione.chatone.presentation.chat.components.PendingModAction
import io.rudione.chatone.presentation.chat.components.ChatSearchBar
import io.rudione.chatone.presentation.chat.components.EmoteAutocompleteRow
import io.rudione.chatone.presentation.chat.components.HiddenEventsRestoreButton
import io.rudione.chatone.presentation.chat.components.InputCompletionCallbacks
import io.rudione.chatone.presentation.chat.components.InputCompletionState
import io.rudione.chatone.presentation.chat.components.MentionAutocompleteRow
import io.rudione.chatone.presentation.chat.components.MessageInput
import io.rudione.chatone.presentation.chat.components.MessageInputActions
import io.rudione.chatone.presentation.chat.components.MessageInputChrome
import io.rudione.chatone.presentation.chat.components.MessageInputTranslation
import io.rudione.chatone.presentation.chat.components.MessageInputUploadState
import io.rudione.chatone.presentation.chat.components.PinnedMessageBar
import io.rudione.chatone.presentation.chat.components.RaidBanner
import io.rudione.chatone.presentation.chat.components.ReplyBar
import io.rudione.chatone.presentation.chat.components.SlashCommandSuggestionsRow
import io.rudione.chatone.presentation.chat.components.rememberStaggeredMessages
import io.rudione.chatone.presentation.components.DockPanel
import io.rudione.chatone.presentation.components.evictedFrom
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.settings.PauseHotkeyMode
import io.rudione.chatone.presentation.settings.SettingsEvent
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.presentation.theme.chatPaneBackgroundColor
import io.rudione.chatone.presentation.theme.luminance
import io.rudione.chatone.util.EmoteAnimationCache
import io.rudione.chatone.util.system.GlobalKeyDispatcher
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.util.media.NotificationSoundPlayer
import io.rudione.chatone.util.media.externalFileDropTarget
import io.rudione.chatone.util.system.handleHover
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.chat.SlashCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val REPEAT_SIMILARITY_THRESHOLD = 0.85f
private const val REPEAT_SCAN_LIMIT = 300

private fun buildScrollbarTicks(
    messages: List<DisplayMessage>,
    mentionColor: Color,
    firstMessageColor: Color,
    highlightedColor: Color
): List<io.rudione.chatone.presentation.components.ScrollbarTick> {
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
        io.rudione.chatone.presentation.components.ScrollbarTick(
            fraction = index.toFloat() / total,
            color = color.copy(alpha = 0.85f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    channelLogin: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    accessToken: String = "",
    currentUserId: String = "",
    currentUserLogin: String = "",
    currentDisplayName: String = "",
    isWideScreen: Boolean = false,
    onMentionDetected: (String) -> Unit = {},
    onMentionReceived: (MentionEntry) -> Unit = {},
    onOpenWhisper: (userId: String, username: String, displayName: String, avatarUrl: String, color: String?) -> Unit = { _, _, _, _, _ -> },
    onChannelIdResolved: (String) -> Unit = {},
    wallpaper: WallpaperState,
    mentionMuteRepository: MentionMuteRepository? = null,
    renderBackground: Boolean = true,
    isMultiChat: Boolean = false,
    pendingScrollMessageId: String? = null,
    onScrollToMessageHandled: () -> Unit = {},
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    val liveModButtons by SettingsViewModel.modButtonsLive.collectAsState()
    val liveMacros by SettingsViewModel.macrosLive.collectAsState()
    val effectivePinnedMacros = liveMacros?.let { Macro.pinnedFrom(it) } ?: settingsState.pinnedMacros
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
    val clipboardManager = LocalClipboardManager.current
    val listState = remember(channelLogin) { LazyListState() }
    val chatScrollActivity =
        io.rudione.chatone.presentation.chat.rendering.rememberScrollActivity(listState)

    val visibleMessages = rememberStaggeredMessages(
        source = state.messages,
        enabled = settingsState.smoothChatEnabled,
        stepMs = 90L
    )
    val dedupedMessages =
        io.rudione.chatone.presentation.chat.rendering.rememberDedupedMessages(
            messages = visibleMessages,
            blockedUserIds = state.blockedUserIds,
            showBlockedMode = state.showBlockedMode
        )
    val renderedCount = dedupedMessages.size

    var showModPanel by remember { mutableStateOf(false) }
    var isFileDragOver by remember { mutableStateOf(false) }
    var showAutomodWindow by remember { mutableStateOf(false) }
    val dockHost = io.rudione.chatone.presentation.components.LocalDockHost.current
    LaunchedEffect(showAutomodWindow, dockHost, channelLogin) {
        if (dockHost != null && showAutomodWindow) dockHost.open(
            io.rudione.chatone.presentation.components.DockPanel.Automod,
            channelLogin
        )
        if (dockHost != null && !showAutomodWindow) dockHost.closeIf(
            io.rudione.chatone.presentation.components.DockPanel.Automod
        )
    }
    var modPanelWasDocked by remember { mutableStateOf(false) }
    LaunchedEffect(dockHost?.panel) {
        if (dockHost.evictedFrom(DockPanel.Moderation, modPanelWasDocked)) {
            modPanelWasDocked = false
            showModPanel = false
        } else if (dockHost?.panel == DockPanel.Moderation) {
            modPanelWasDocked = true
        }
    }
    var automodWasDocked by remember { mutableStateOf(false) }
    LaunchedEffect(dockHost?.panel) {
        if (dockHost.evictedFrom(DockPanel.Automod, automodWasDocked)) {
            automodWasDocked = false
            showAutomodWindow = false
        } else if (dockHost?.panel == DockPanel.Automod) {
            automodWasDocked = true
        }
    }
    var emotesWereDocked by remember { mutableStateOf(false) }
    LaunchedEffect(dockHost?.panel) {
        if (dockHost.evictedFrom(DockPanel.Emotes, emotesWereDocked)) {
            emotesWereDocked = false
            if (state.isEmotePickerVisible) viewModel.sendEvent(ChatEvent.OnToggleEmotePicker)
        } else if (dockHost?.panel == DockPanel.Emotes) {
            emotesWereDocked = true
        }
    }
    var pointsWereDocked by remember { mutableStateOf(false) }
    LaunchedEffect(dockHost?.panel) {
        if (dockHost.evictedFrom(DockPanel.Points, pointsWereDocked)) {
            pointsWereDocked = false
            if (state.showPointsBitsPanel) viewModel.sendEvent(ChatEvent.OnClosePointsBitsPanel)
        } else if (dockHost?.panel == DockPanel.Points) {
            pointsWereDocked = true
        }
    }
    var messageInputFocused by remember { mutableStateOf(false) }
    var profilePopupUserId by remember { mutableStateOf<String?>(null) }
    var profilePopupMessage by remember { mutableStateOf<DisplayMessage.PrivMsg?>(null) }
    var detachedProfileMessage by remember { mutableStateOf<DisplayMessage.PrivMsg?>(null) }
    var predictionResolveDetached by remember { mutableStateOf(false) }
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

    var isAutoScrolling by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableStateOf(0) }
    val chatBoxFocusRequester = remember { FocusRequester() }
    val emoteRepository: EmoteRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val isAtBottom = remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= totalItems - 3
        }
    }

    val isScrolledAway = remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex < totalItems - 5
        }
    }

    val effectivelyPaused = isPausedByHotkey ||
            (settingsState.pauseOnHover && isHoveredOverChat && !isHoveredOverEmoteTooltip) ||
            isScrolledAway.value

    var userScrolledSinceJoin by remember(channelLogin) { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAtBottom.value && !isAutoScrolling) {
            hasNewMessagesWhilePaused = true
            userScrolledSinceJoin = true
        }
    }

    LaunchedEffect(isAtBottom.value) {
        if (isAtBottom.value && !isPausedByHotkey) {
            hasNewMessagesWhilePaused = false
            unreadCount = 0
        }
    }

    val stickToBottom: suspend () -> Unit = remember(listState) {
        {
            isAutoScrolling = true
            try {
                listState.stickToBottomExact()
            } finally {
                isAutoScrolling = false
            }
        }
    }

    LaunchedEffect(effectivelyPaused) {
        viewModel.sendEvent(ChatEvent.OnScrollbackPinned(effectivelyPaused))
    }

    val effectivelyPausedLatest by rememberUpdatedState(effectivelyPaused)
    LaunchedEffect(listState) {
        snapshotFlow { listState.bottomOverflow() ?: 0f }
            .collect { overflow ->
                if (overflow > 0f && !effectivelyPausedLatest && !listState.isScrollInProgress) {
                    isAutoScrolling = true
                    listState.scrollBy(overflow)
                    isAutoScrolling = false
                }
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
        stickToBottom()
        unreadCount = 0
        hasNewMessagesWhilePaused = false
    }

    val newestRenderedId = dedupedMessages.lastOrNull()?.id
    LaunchedEffect(newestRenderedId, effectivelyPaused) {
        if (newestRenderedId == null) return@LaunchedEffect
        if (!effectivelyPaused) {
            stickToBottom()
            unreadCount = 0
            hasNewMessagesWhilePaused = false
        }
    }

    val messagesSeq = state.messagesSeq
    LaunchedEffect(messagesSeq) {
        if (messagesSeq == 0L) return@LaunchedEffect
        if (effectivelyPaused && state.messages.isNotEmpty()) {
            hasNewMessagesWhilePaused = true
            unreadCount++
        }
    }

    LaunchedEffect(channelLogin, accessToken) {
        viewModel.sendEvent(
            ChatEvent.OnInit(
                channelLogin,
                accessToken,
                currentUserId,
                currentUserLogin,
                currentDisplayName
            )
        )
    }

    val channelId = state.channelId
    LaunchedEffect(channelId) {
        if (channelId.isNotEmpty()) onChannelIdResolved(channelId)
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
                    if (!effectivelyPaused && renderedCount > 0) {
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
                        onMentionDetected(mentionChannel)
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
                                when (token) {
                                    is MessageToken.Text -> token.text
                                    is MessageToken.TwitchEmoteToken -> token.name
                                    is MessageToken.ThirdPartyEmoteToken -> token.emote.code
                                    is MessageToken.Link -> token.displayText
                                    is MessageToken.Mention -> token.username
                                    is MessageToken.Cheer -> "${token.prefix}${token.amount}"
                                }
                            },
                            timestamp = mentionMsg.timestamp
                        )
                        onMentionReceived(entry)
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
                    profilePopupMessage = syntheticMsg
                    profilePopupUserId = effect.userId
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

    val pauseMouseButton = remember(settingsState.pauseHotkey) {
        hotkeyMouseButton(settingsState.pauseHotkey)
    }
    val resumeFromPause: () -> Unit = {
        coroutineScope.launch {
            if (currentRenderedCount > 0) {
                stickToBottomLatest()
                hasNewMessagesWhilePaused = false
                unreadCount = 0
            }
        }
        Unit
    }

    val chatSearchMatches: List<Int> = remember(chatSearchQuery, dedupedMessages) {
        if (chatSearchQuery.isBlank()) emptyList()
        else {
            val q = chatSearchQuery.lowercase()
            dedupedMessages.mapIndexedNotNull { idx, msg ->
                val text = when (msg) {
                    is DisplayMessage.PrivMsg -> (msg.rawMessage?.message
                        ?: msg.tokens.joinToString("") {
                            when (it) {
                                is MessageToken.Text -> it.text
                                is MessageToken.TwitchEmoteToken -> it.name
                                is MessageToken.ThirdPartyEmoteToken -> it.emote.code
                                is MessageToken.Link -> it.displayText
                                is MessageToken.Mention -> it.username
                                is MessageToken.Cheer -> "${it.prefix}${it.amount}"
                            }
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
            listState.animateScrollToItem(targetIdx)
        }
    }

    LaunchedEffect(pendingScrollMessageId, listState) {
        val targetId = pendingScrollMessageId ?: return@LaunchedEffect
        if (!channelLogin.equals(state.channelLogin, ignoreCase = true)) return@LaunchedEffect
        repeat(20) {
            val idx = dedupedMessages.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
                onScrollToMessageHandled()
                return@LaunchedEffect
            }
            delay(500)
        }
        onScrollToMessageHandled()
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

                    coroutineScope.launch {
                        if (currentRenderedCount > 0) {
                            stickToBottomLatest()
                            hasNewMessagesWhilePaused = false
                            unreadCount = 0
                        }
                    }
                    return@handler true
                }
            } else {
                if (event.type == KeyEventType.KeyDown && pauseHotkeyMatches(event, hotkey)) {
                    val wasPaused = currentIsPausedByHotkey
                    isPausedByHotkey = !wasPaused

                    if (wasPaused) {
                        coroutineScope.launch {
                            if (currentRenderedCount > 0) {
                                stickToBottomLatest()
                                hasNewMessagesWhilePaused = false
                                unreadCount = 0
                            }
                        }
                    }
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

    LaunchedEffect(channelLogin) {
        EmoteAnimationCache.clearAll()
        viewModel.sendEvent(
            ChatEvent.OnInit(
                channelLogin,
                accessToken,
                currentUserId,
                currentUserLogin,
                currentDisplayName
            )
        )
    }

    LaunchedEffect(channelLogin) {
        io.rudione.chatone.util.system.ChannelPanelRequestBus.openPointsBitsPanel.collect { requestedLogin ->
            if (requestedLogin.equals(channelLogin, ignoreCase = true)) {
                viewModel.sendEvent(ChatEvent.OnOpenPointsBitsPanel)
            }
        }
    }

    LaunchedEffect(channelLogin) {
        io.rudione.chatone.util.system.ChannelPanelRequestBus.toggleHidePin.collect { requestedLogin ->
            if (requestedLogin.equals(channelLogin, ignoreCase = true)) {
                viewModel.sendEvent(ChatEvent.OnToggleHidePin)
            }
        }
    }

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
                .background(MaterialTheme.colorScheme.background)
        ) {

            AnimatedVisibility(
                visible = settingsState.showChatHeader,
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
                        onBack = onNavigateBack,
                        onToggleModMode = { viewModel.sendEvent(ChatEvent.OnToggleModMode) },
                        onOpenModPanel = { showModPanel = !showModPanel },
                        onExecuteMacro = { macro ->
                            viewModel.sendEvent(
                                ChatEvent.OnExecuteMacro(
                                    macro
                                )
                            )
                        },
                        onRefresh = { viewModel.sendEvent(ChatEvent.OnRefreshChannel) },
                        onOpenPointsBits = { viewModel.sendEvent(ChatEvent.OnOpenPointsBitsPanel) },
                        isCompact = !isWideScreen,
                        showMenuButton = !isWideScreen && !isMultiChat
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val wallpaperCtrl = LocalWallpaperController.current
                val liveWallpaper by remember { derivedStateOf { wallpaperCtrl.state } }
                val isDarkChat = MaterialTheme.colorScheme.background.luminance() < 0.4f
                if (renderBackground) {
                    ChatBackgroundLayer(
                        wallpaper = liveWallpaper,
                        modifier = Modifier.fillMaxSize(),
                        darkTheme = isDarkChat
                    ) {}
                }

                val chatPaneDefaultColor = MaterialTheme.colorScheme.surfaceContainer
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

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (state.messages.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.MailOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                LocalStrings.current.chatWaitingForMessages,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        val showRepeatCounter = settingsState.showRepeatedMessageCounter
                        val repeatWindowMs =
                            (settingsState.repeatedMessageWindow.coerceAtLeast(1)) * 1000L

                        val zebraParityById: Map<String, Boolean> =
                            remember(state.messagesSeq, state.messagesStartOrdinal) {
                                if (!settingsState.alternateRowBackground) emptyMap()
                                else buildMap {
                                    state.messages.forEachIndexed { i, m ->
                                        put(m.id, (state.messagesStartOrdinal + i) and 1L == 1L)
                                    }
                                }
                            }
                        val userColorByLogin: Map<String, Color> = remember(state.messagesSeq) {
                            buildMap {
                                state.messages.asReversed()
                                    .filterIsInstance<DisplayMessage.PrivMsg>()
                                    .forEach { m ->
                                        val key = m.username.lowercase()
                                        if (key.isNotEmpty() && !containsKey(key)) {
                                            val c = m.color?.let { hex ->
                                                runCatching {
                                                    val v = hex.removePrefix("#").toLong(16)
                                                    Color(
                                                        ((v shr 16) and 0xFF) / 255f,
                                                        ((v shr 8) and 0xFF) / 255f,
                                                        (v and 0xFF) / 255f
                                                    )
                                                }.getOrNull()
                                            }
                                            if (c != null) put(key, c)
                                        }
                                    }
                            }
                        }
                        val repeatCountByMsgId: Map<String, Int> =
                            remember(dedupedMessages, showRepeatCounter, repeatWindowMs) {
                                if (!showRepeatCounter) emptyMap()
                                else buildMap {
                                    val scanned = if (dedupedMessages.size > REPEAT_SCAN_LIMIT) {
                                        dedupedMessages.subList(
                                            dedupedMessages.size - REPEAT_SCAN_LIMIT,
                                            dedupedMessages.size
                                        )
                                    } else dedupedMessages
                                    val recent = ArrayDeque<Pair<String, Long>>()
                                    for (m in scanned) {
                                        if (m !is DisplayMessage.PrivMsg) continue
                                        val ts = m.timestamp
                                        val norm = m.tokens.joinToString("") { tok ->
                                            when (tok) {
                                                is MessageToken.Text -> tok.text
                                                is MessageToken.TwitchEmoteToken -> tok.name
                                                is MessageToken.ThirdPartyEmoteToken -> tok.emote.code
                                                is MessageToken.Link -> tok.displayText
                                                is MessageToken.Mention -> "@${tok.username}"
                                                is MessageToken.Cheer -> "${tok.prefix}${tok.amount}"
                                            }
                                        }.trim().lowercase()
                                        if (norm.isEmpty()) continue
                                        while (recent.isNotEmpty() && ts - recent.first().second > repeatWindowMs) {
                                            recent.removeFirst()
                                        }
                                        val n = recent.count {
                                            it.first == norm ||
                                                io.rudione.chatone.util.automod.relativeSimilarity(
                                                    it.first, norm, atLeast = REPEAT_SIMILARITY_THRESHOLD
                                                ) >= REPEAT_SIMILARITY_THRESHOLD
                                        } + 1
                                        if (n > 1) put(m.id, n)
                                        recent.addLast(norm to ts)
                                    }
                                }
                            }
                        key(channelLogin) {
                          CompositionLocalProvider(
                            io.rudione.chatone.presentation.chat.rendering.LocalScrollActivity provides chatScrollActivity
                          ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().selectionCursor().handleHover(
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
                                    contentPadding = PaddingValues(
                                        horizontal = 4.dp,
                                        vertical = 2.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(
                                        when (settingsState.messageSpacing) {
                                            SettingsState.MessageSpacing.NONE -> 0.dp
                                            SettingsState.MessageSpacing.LOW -> 1.dp
                                            SettingsState.MessageSpacing.MEDIUM -> 3.dp
                                            SettingsState.MessageSpacing.HIGH -> 5.dp
                                        }
                                    )
                                ) {
                                    itemsIndexed(
                                        items = dedupedMessages,
                                        key = { _, it -> it.id },
                                        contentType = { _, it -> it::class }) { _, message ->
                                        val zebraTintColor =
                                            if (settingsState.alternateRowBackground &&
                                                zebraParityById[message.id] == true
                                            ) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f)
                                            } else Color.Transparent
                                        when (message) {
                                            is DisplayMessage.PrivMsg -> PrivMsgItem(
                                                message = message,
                                                isCompact = !isWideScreen,
                                                zebraTint = zebraTintColor,
                                                showModActions = state.modModeEnabled,
                                                timestampFormat = settingsState.timestampFormat,
                                                showBadges = settingsState.showBadges,
                                                repeatCount = repeatCountByMsgId[message.id] ?: 1,
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
                                                    profilePopupMessage = message
                                                    profilePopupUserId = message.userId
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
                                                        profilePopupMessage = mentionedMsg
                                                        profilePopupUserId = mentionedMsg.userId
                                                    } else {
                                                        val placeholder = DisplayMessage.PrivMsg(
                                                            id = "synthetic_${cleanLogin}",
                                                            timestamp = kotlin.time.Clock.System.now()
                                                                .toEpochMilliseconds(),
                                                            channel = state.channelLogin,
                                                            userId = "",
                                                            username = cleanLogin.lowercase(),
                                                            displayName = cleanLogin,
                                                            tokens = emptyList(),
                                                            color = null,
                                                            badges = emptyList(),
                                                            isModerator = false,
                                                            isSubscriber = false,
                                                            isVip = false,
                                                            isBroadcaster = false,
                                                            isMention = false,
                                                            isAction = false
                                                        )
                                                        profilePopupMessage = placeholder
                                                        profilePopupUserId = ""
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
                                                userColorByLogin = userColorByLogin,
                                                accessToken = state.currentAccessToken
                                            )

                                            is DisplayMessage.SystemMsg -> SystemMsgItem(message)
                                            is DisplayMessage.UserNoticeMsg -> UserNoticeMsgItem(
                                                message
                                            )

                                            is DisplayMessage.ModerationMsg -> ModerationMsgItem(
                                                message = message,
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
                                                        profilePopupMessage = found
                                                        profilePopupUserId = found.userId
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
                                                    profilePopupMessage = syntheticMsg
                                                    profilePopupUserId = message.userId
                                                }
                                            )
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
                                        io.rudione.chatone.domain.model.HighlightRule.FIRST_MESSAGE_RULE.color
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
                                io.rudione.chatone.presentation.components.ChatoneLazyScrollbar(
                                    listState = listState,
                                    itemCount = renderedCount,
                                    ticks = scrollbarTicks,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(settingsState.chatScrollbarWidth.dp)
                                )

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

                                if ((hasNewMessagesWhilePaused || !isAtBottom.value || isPausedByHotkey) && state.messages.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                            .padding(12.dp)
                                    ) {
                                        SmallFloatingActionButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    if (renderedCount > 0) stickToBottom()
                                                    isPausedByHotkey = false
                                                    isHoveredOverChat = false
                                                    hasNewMessagesWhilePaused = false
                                                    unreadCount = 0
                                                }
                                            },
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Icon(
                                                Icons.Filled.KeyboardArrowDown,
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

                run {
                    fun isoToMs(iso: String?): Long? =
                        iso?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
                    val pollStillRunning = state.livePoll?.status.equals("ACTIVE", ignoreCase = true)
                    val predictionStillRunning =
                        state.livePrediction?.status == "ACTIVE" || state.livePrediction?.status == "LOCKED"
                    val pollHidden = state.livePoll?.id in state.hiddenEventIds
                    val predictionHidden = state.livePrediction?.id in state.hiddenEventIds
                    val hiddenRestorableCount =
                        (if (state.pinnedMessage != null && state.pinLocallyHidden) 1 else 0) +
                                (if (pollHidden && pollStillRunning) 1 else 0) +
                                (if (predictionHidden && predictionStillRunning) 1 else 0)

                    val bannerItems = buildList {
                        state.pinnedMessage?.takeIf { !state.pinLocallyHidden }?.let { pinned ->
                            add(
                                io.rudione.chatone.presentation.chat.components.EventBannerItem(
                                    key = "pin",
                                    label = "📌 Pinned",
                                    endsAtMs = state.pinEndsAtMs,
                                    totalDurationMs = null
                                ) {
                                    PinnedMessageBar(
                                        message = pinned,
                                        canUnpin = state.canModerate,
                                        endsAtMs = state.pinEndsAtMs,
                                        pinnedByName = state.pinnedByName,
                                        pinnedByBadges = state.pinnedByBadges,
                                        onUnpin = { viewModel.sendEvent(ChatEvent.OnUnpinMessage) },
                                        onHide = { viewModel.sendEvent(ChatEvent.OnToggleHidePin) })
                                }
                            )
                        }
                        state.livePoll?.takeIf { !pollHidden }?.let { poll ->
                            val ends = isoToMs(poll.startedAt)?.plus(poll.duration * 1000L)
                            add(
                                io.rudione.chatone.presentation.chat.components.EventBannerItem(
                                    key = "poll",
                                    label = "Poll",
                                    endsAtMs = ends?.takeIf { poll.status == "ACTIVE" },
                                    totalDurationMs = poll.duration * 1000L
                                ) {
                                    io.rudione.chatone.presentation.chat.components.PollBanner(
                                        poll = poll,
                                        onVote = { choiceId ->
                                            viewModel.sendEvent(ChatEvent.OnVotePoll(poll.id, choiceId))
                                        },
                                        onHide = { viewModel.sendEvent(ChatEvent.OnHideEventBanner("poll")) }
                                    )
                                }
                            )
                        }
                        state.livePrediction?.takeIf { !predictionHidden }?.let { pred ->
                            val ends = isoToMs(pred.createdAt)?.plus(pred.predictionWindow * 1000L)
                            add(
                                io.rudione.chatone.presentation.chat.components.EventBannerItem(
                                    key = "prediction",
                                    label = "Prediction",
                                    endsAtMs = ends?.takeIf { pred.status == "ACTIVE" },
                                    totalDurationMs = pred.predictionWindow * 1000L
                                ) {
                                    io.rudione.chatone.presentation.chat.components.PredictionBanner(
                                        prediction = pred,
                                        pointsBalance = state.pointsBalance,
                                        onPredict = { outcomeId, points ->
                                            viewModel.sendEvent(ChatEvent.OnPlacePrediction(pred.id, outcomeId, points))
                                        },
                                        onHide = { viewModel.sendEvent(ChatEvent.OnHideEventBanner("prediction")) }
                                    )
                                }
                            )
                        }
                        state.pendingRaidTarget?.let { target ->
                            add(
                                io.rudione.chatone.presentation.chat.components.EventBannerItem(
                                    key = "raid",
                                    label = "Raid",
                                    endsAtMs = state.pendingRaidStartedAt + 90_000L,
                                    totalDurationMs = 90_000L
                                ) {
                                    RaidBanner(
                                        targetLogin = target,
                                        startedAtMs = state.pendingRaidStartedAt,
                                        onCancel = { viewModel.sendEvent(ChatEvent.OnCancelRaid) },
                                        onRaidNow = { viewModel.sendEvent(ChatEvent.OnRaidNow) },
                                        accessToken = state.currentAccessToken
                                    )
                                }
                            )
                        }
                    }
                    val bannerEndInset = (settingsState.chatScrollbarWidth + 10).dp
                    if (bannerItems.isNotEmpty() || hiddenRestorableCount > 0) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .zIndex(5f)
                                .padding(top = 6.dp)
                        ) {
                            if (hiddenRestorableCount > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = bannerEndInset, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    HiddenEventsRestoreButton(
                                        count = hiddenRestorableCount,
                                        onClick = {
                                            viewModel.sendEvent(ChatEvent.OnRestoreHiddenBanners)
                                        }
                                    )
                                }
                            }
                            if (bannerItems.isNotEmpty()) {
                                io.rudione.chatone.presentation.chat.components.UnifiedEventBanner(
                                    items = bannerItems,
                                    endInset = bannerEndInset
                                )
                            }
                        }
                    }

                    if (state.showPollCreation) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .zIndex(6f)
                                .padding(top = 6.dp)
                        ) {
                            PollCreationPanel(
                                history = state.recentPolls.map { Triple(it.title, it.choices, it.durationSeconds) },
                                onSubmit = { title, choices, durationSeconds ->
                                    viewModel.sendEvent(ChatEvent.OnCreatePoll(title, choices, durationSeconds))
                                },
                                onClose = { viewModel.sendEvent(ChatEvent.OnClosePollCreation) }
                            )
                        }
                    }

                    val resolvablePrediction = state.livePrediction
                        ?.takeIf { it.status == "ACTIVE" || it.status == "LOCKED" }

                    if (state.showPredictionCreation) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .zIndex(6f)
                                .padding(top = 6.dp)
                        ) {
                            if (resolvablePrediction != null) {
                                if (!predictionResolveDetached) {
                                    PredictionResolvePanel(
                                        prediction = resolvablePrediction,
                                        canDetach = true,
                                        onDetach = { predictionResolveDetached = true },
                                        onLock = {
                                            viewModel.sendEvent(ChatEvent.OnLockPrediction(resolvablePrediction.id))
                                        },
                                        onResolve = { outcomeId ->
                                            viewModel.sendEvent(
                                                ChatEvent.OnResolvePrediction(resolvablePrediction.id, outcomeId)
                                            )
                                            viewModel.sendEvent(ChatEvent.OnClosePredictionCreation)
                                        },
                                        onClose = { viewModel.sendEvent(ChatEvent.OnClosePredictionCreation) }
                                    )
                                }
                            } else {
                                PredictionCreationPanel(
                                    history = state.recentPredictions.map { Triple(it.title, it.outcomes, it.windowSeconds) },
                                    onSubmit = { title, outcomes, windowSeconds ->
                                        viewModel.sendEvent(ChatEvent.OnCreatePrediction(title, outcomes, windowSeconds))
                                    },
                                    onClose = { viewModel.sendEvent(ChatEvent.OnClosePredictionCreation) }
                                )
                            }
                        }
                    }

                    val resolvePanelAvailable = state.showPredictionCreation && resolvablePrediction != null
                    LaunchedEffect(resolvePanelAvailable) {
                        if (!resolvePanelAvailable) predictionResolveDetached = false
                    }
                    if (predictionResolveDetached && resolvablePrediction != null) {
                        io.rudione.chatone.presentation.window.DetachedToolWindow(
                            windowId = "prediction-resolve",
                            title = "Chatone — ${resolvablePrediction.title}",
                            defaultWidth = 460.dp,
                            defaultHeight = 400.dp,
                            onClose = { predictionResolveDetached = false }
                        ) {
                            PredictionResolvePanel(
                                prediction = resolvablePrediction,
                                canDetach = false,
                                onDetach = {},
                                onLock = {
                                    viewModel.sendEvent(ChatEvent.OnLockPrediction(resolvablePrediction.id))
                                },
                                onResolve = { outcomeId ->
                                    viewModel.sendEvent(
                                        ChatEvent.OnResolvePrediction(resolvablePrediction.id, outcomeId)
                                    )
                                    predictionResolveDetached = false
                                    viewModel.sendEvent(ChatEvent.OnClosePredictionCreation)
                                },
                                onClose = {
                                    predictionResolveDetached = false
                                    viewModel.sendEvent(ChatEvent.OnClosePredictionCreation)
                                }
                            )
                        }
                    }

                    if (state.showPointsBitsPanel && dockHost == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(7f)
                        ) {
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
                                onClose = { viewModel.sendEvent(ChatEvent.OnClosePointsBitsPanel) }
                            )
                        }
                    }
                }
            }

            val modPanelDocked = dockHost != null && showModPanel && state.canModerate
            AnimatedVisibility(
                visible = showModPanel && state.canModerate && !modPanelDocked,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
            ) {
                ModerationPanel(
                    roomState = state.roomState,
                    channelLogin = channelLogin,
                    isMod = state.canModerate,
                    pinnedMacros = effectivePinnedMacros,
                    onUpdateChatSettings = { settings ->
                        viewModel.sendEvent(
                            ChatEvent.OnUpdateChatSettings(
                                settings
                            )
                        )
                    },
                    onClearChat = { viewModel.sendEvent(ChatEvent.OnClearChat) },
                    onSendAnnouncement = { message, color ->
                        viewModel.sendEvent(
                            ChatEvent.OnSendAnnouncement(
                                message,
                                color
                            )
                        )
                    },
                    onStartRaid = { targetLogin ->
                        viewModel.sendEvent(
                            ChatEvent.OnStartRaid(
                                targetLogin
                            )
                        )
                    },
                    onCancelRaid = { viewModel.sendEvent(ChatEvent.OnCancelRaid) },
                    onExecuteMacro = { macro -> viewModel.sendEvent(ChatEvent.OnExecuteMacro(macro)) },
                    onSendPinMessage = { msg -> viewModel.sendEvent(ChatEvent.OnSendMessageText("/pin $msg")) },
                    onShoutout = { target -> viewModel.sendEvent(ChatEvent.OnSendMessageText("/shoutout $target")) },
                    onSendRawCommand = { cmd -> viewModel.sendEvent(ChatEvent.OnSendMessageText(cmd)) },
                    onOpenLocalAutomod = { showAutomodWindow = true },
                    onClose = { showModPanel = false },
                    accessToken = state.currentAccessToken,
                    channelId = state.channelId,
                    isBroadcaster = state.isBroadcaster
                )
            }

            if (modPanelDocked) {
                val panelBody = rememberUpdatedState<@Composable () -> Unit> {
        ModerationPanel(
                            roomState = state.roomState,
                            channelLogin = channelLogin,
                            isMod = state.canModerate,
                            pinnedMacros = effectivePinnedMacros,
                            onUpdateChatSettings = { settings ->
                                viewModel.sendEvent(
                                    ChatEvent.OnUpdateChatSettings(
                                        settings
                                    )
                                )
                            },
                            onClearChat = { viewModel.sendEvent(ChatEvent.OnClearChat) },
                            onSendAnnouncement = { message, color ->
                                viewModel.sendEvent(
                                    ChatEvent.OnSendAnnouncement(
                                        message,
                                        color
                                    )
                                )
                            },
                            onStartRaid = { targetLogin ->
                                viewModel.sendEvent(
                                    ChatEvent.OnStartRaid(
                                        targetLogin
                                    )
                                )
                            },
                            onCancelRaid = { viewModel.sendEvent(ChatEvent.OnCancelRaid) },
                            onExecuteMacro = { macro -> viewModel.sendEvent(ChatEvent.OnExecuteMacro(macro)) },
                            onSendPinMessage = { msg -> viewModel.sendEvent(ChatEvent.OnSendMessageText("/pin $msg")) },
                            onShoutout = { target -> viewModel.sendEvent(ChatEvent.OnSendMessageText("/shoutout $target")) },
                            onSendRawCommand = { cmd -> viewModel.sendEvent(ChatEvent.OnSendMessageText(cmd)) },
                            onOpenLocalAutomod = { showAutomodWindow = true },
                            onClose = { showModPanel = false },
                            accessToken = state.currentAccessToken,
                            channelId = state.channelId,
                            isBroadcaster = state.isBroadcaster
                        )
                }
                LaunchedEffect(modPanelDocked) {
                    dockHost?.openWith(
                        io.rudione.chatone.presentation.components.DockPanel.Moderation
                    ) { panelBody.value() }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        dockHost?.closeIf(
                            io.rudione.chatone.presentation.components.DockPanel.Moderation
                        )
                    }
                }
            }

            if (showAutomodWindow && dockHost == null) {
                io.rudione.chatone.presentation.automod.DetachedAutomodWindow(
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
                        io.rudione.chatone.presentation.components.DockPanel.Points
                    ) { pointsBody.value() }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        dockHost?.closeIf(
                            io.rudione.chatone.presentation.components.DockPanel.Points
                        )
                    }
                }
            }

            state.replyingTo?.let { replyMsg ->
                ReplyBar(
                    displayName = replyMsg.displayName,
                    messagePreview = replyMsg.tokens.joinToString("") { token ->
                        when (token) {
                            is MessageToken.Text -> token.text
                            is MessageToken.TwitchEmoteToken -> token.name
                            is MessageToken.ThirdPartyEmoteToken -> token.emote.code
                            is MessageToken.Link -> token.displayText
                            is MessageToken.Mention -> token.username
                            is MessageToken.Cheer -> "${token.prefix}${token.amount}"
                        }
                    },
                    onCancel = { viewModel.sendEvent(ChatEvent.OnCancelReply) }
                )
            }

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
                                    io.rudione.chatone.domain.model.ChatCommandKind.TEXT ->
                                        if (cmd.sendImmediately) "→ sends: ${cmd.replacement.take(40)}"
                                        else "→ fills: ${cmd.replacement.take(40)}"
                                    io.rudione.chatone.domain.model.ChatCommandKind.MACRO -> "→ macro"
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
                )
            )
        }

        if (state.showMentionCompletions && state.mentionCompletions.isNotEmpty()) {
            MentionAutocompleteRow(
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.uploaderDropHint,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    state.pendingUploadPath?.let { pendingPath ->
        val pendingName = pendingPath.substringAfterLast('/').substringAfterLast('\\')
        AlertDialog(
            onDismissRequest = { viewModel.sendEvent(ChatEvent.OnCancelPendingUpload) },
            title = { Text(s.uploaderConfirmTitle, fontWeight = FontWeight.SemiBold) },
            text = { Text(s.uploaderConfirmText.replace("{0}", pendingName)) },
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
        val resolvedEmotes = emoteRepository.getResolvedEmotes(channelLogin).copy(
            twitchEmotes = state.twitchChannelEmotes,
            twitchGlobal = state.twitchGlobalEmotes
        )
        val pickerPersonalEmotes = remember(state.currentUserId, state.twitchSubscriberEmotes) {
            val sevenTv = if (state.currentUserId.isNotEmpty())
                emoteRepository.getCachedPersonalEmotes(state.currentUserId)
            else emptyList()
            (sevenTv + state.twitchSubscriberEmotes).distinctBy { "${it.provider.name}_${it.id}" }
        }

        val onEmotePicked: (io.rudione.chatone.domain.model.GenericEmote) -> Unit = { emote ->
            val current = state.messageInput
            val newInput =
                if (current.isEmpty() || current.endsWith(" ")) "$current${emote.code} " else "$current ${emote.code} "
            viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
        }
        val onEmojiPicked: (String) -> Unit = { emoji ->
            val current = state.messageInput
            val newInput =
                if (current.isEmpty() || current.endsWith(" ")) "$current$emoji" else "$current $emoji"
            viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
        }

        if (dockHost == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 42.dp)
            ) {
                EmotePickerSheet(
                    channelEmotes = resolvedEmotes,
                    personalEmotes = pickerPersonalEmotes,
                    closeOnMouseLeave = settingsState.closeEmotePickerOnMouseLeave,
                    onEmoteSelected = onEmotePicked,
                    onEmojiSelected = onEmojiPicked,
                    onDismiss = { viewModel.sendEvent(ChatEvent.OnToggleEmotePicker) }
                )
            }
        } else {
            val emoteBody = rememberUpdatedState<@Composable () -> Unit> {
                EmotePickerSheet(
                    channelEmotes = resolvedEmotes,
                    personalEmotes = pickerPersonalEmotes,
                    closeOnMouseLeave = false,
                    onEmoteSelected = onEmotePicked,
                    onEmojiSelected = onEmojiPicked,
                    onDismiss = { viewModel.sendEvent(ChatEvent.OnToggleEmotePicker) },
                    docked = true
                )
            }
            LaunchedEffect(Unit) {
                dockHost.openWith(
                    io.rudione.chatone.presentation.components.DockPanel.Emotes
                ) { emoteBody.value() }
            }
            DisposableEffect(Unit) {
                onDispose {
                    dockHost.closeIf(
                        io.rudione.chatone.presentation.components.DockPanel.Emotes
                    )
                }
            }
        }
    }

    profilePopupMessage?.let { msg ->
        key(currentUserId) {
            UserProfilePopup(
                userId = msg.userId,
                username = msg.username,
                displayName = msg.displayName,
                color = msg.color,
                channelMessages = state.messages,
                accessToken = state.currentAccessToken,
                channelId = state.channelId,
                isModerator = msg.isModerator,
                isSubscriber = msg.isSubscriber,
                isVip = msg.isVip,
                isBroadcaster = msg.isBroadcaster,
                badges = msg.badges,
                sevenTvBadge = msg.sevenTvBadge,
                showModActions = state.modModeEnabled || state.canModerate,
                currentUserIsBroadcaster = state.currentUserLogin.equals(
                    channelLogin,
                    ignoreCase = true
                ),
                isBlocked = msg.userId in state.blockedUserIds,
                onBlock = {
                    viewModel.sendEvent(ChatEvent.OnBlockUser(msg.userId, msg.username))
                },
                onUnblock = {
                    viewModel.sendEvent(ChatEvent.OnUnblockUser(msg.userId, msg.username))
                },
                onTimeout = { seconds ->
                    viewModel.sendEvent(ChatEvent.OnTimeoutUser(msg.userId, seconds))
                },
                onBan = { viewModel.sendEvent(ChatEvent.OnBanUser(msg.userId)) },
                onBanWithReason = { r -> viewModel.sendEvent(ChatEvent.OnBanUser(msg.userId, r)) },
                onUnban = { viewModel.sendEvent(ChatEvent.OnUnbanUser(msg.userId)) },
                onMod = { viewModel.sendEvent(ChatEvent.OnModUser(msg.userId)) },
                onUnmod = { viewModel.sendEvent(ChatEvent.OnUnmodUser(msg.userId)) },
                onVip = { viewModel.sendEvent(ChatEvent.OnVipUser(msg.userId)) },
                onUnvip = { viewModel.sendEvent(ChatEvent.OnUnvipUser(msg.userId)) },
                onWhisper = {
                    onOpenWhisper(msg.userId, msg.username, msg.displayName, "", msg.color)
                    profilePopupMessage = null
                    profilePopupUserId = null
                },
                onDetach = {
                    detachedProfileMessage = msg
                    profilePopupMessage = null
                    profilePopupUserId = null
                },
                channelLogin = channelLogin,
                mentionMuteRepository = mentionMuteRepository,
                onDismiss = { profilePopupMessage = null; profilePopupUserId = null }
            )
        }
    }

    detachedProfileMessage?.let { msg ->
        DetachedProfileWindow(
            msg = msg,
            channelMessages = state.messages,
            accessToken = state.currentAccessToken,
            channelId = state.channelId,
            showModActions = state.modModeEnabled || state.canModerate,
            currentUserIsBroadcaster = state.currentUserLogin.equals(
                channelLogin,
                ignoreCase = true
            ),
            isBlocked = msg.userId in state.blockedUserIds,
            onBlock = { viewModel.sendEvent(ChatEvent.OnBlockUser(msg.userId, msg.username)) },
            onUnblock = { viewModel.sendEvent(ChatEvent.OnUnblockUser(msg.userId, msg.username)) },
            onTimeout = { seconds ->
                viewModel.sendEvent(
                    ChatEvent.OnTimeoutUser(
                        msg.userId,
                        seconds
                    )
                )
            },
            onBan = { viewModel.sendEvent(ChatEvent.OnBanUser(msg.userId)) },
            onUnban = { viewModel.sendEvent(ChatEvent.OnUnbanUser(msg.userId)) },
            onMod = { viewModel.sendEvent(ChatEvent.OnModUser(msg.userId)) },
            onUnmod = { viewModel.sendEvent(ChatEvent.OnUnmodUser(msg.userId)) },
            onVip = { viewModel.sendEvent(ChatEvent.OnVipUser(msg.userId)) },
            onUnvip = { viewModel.sendEvent(ChatEvent.OnUnvipUser(msg.userId)) },
            onWhisper = {
                onOpenWhisper(msg.userId, msg.username, msg.displayName, "", msg.color)
                detachedProfileMessage = null
            },
            onClose = { detachedProfileMessage = null }
        )
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
                Icons.Outlined.Info,
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
            io.rudione.chatone.presentation.components.ChatoneIconButton(onClick = onDismiss, modifier = Modifier.size(22.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
