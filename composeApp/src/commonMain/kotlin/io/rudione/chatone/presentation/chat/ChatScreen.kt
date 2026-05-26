package io.rudione.chatone.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.emoji_icon
import chatone.composeapp.generated.resources.ic_sword
import coil3.compose.AsyncImage
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.presentation.chat.components.LinkHoverPopup
import io.rudione.chatone.presentation.chat.components.ChatSearchBar
import io.rudione.chatone.presentation.chat.components.MessageInput
import io.rudione.chatone.presentation.chat.components.SlashCommandSuggestionsRow
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.presentation.settings.PauseHotkeyMode
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatBackgroundLayer
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.FirstMessageColor
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.presentation.theme.chatPaneBackgroundColor
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.luminance
import io.rudione.chatone.presentation.theme.panelBlur
import io.rudione.chatone.presentation.theme.topBarBackgroundColor
import io.rudione.chatone.util.EmoteAnimationCache
import io.rudione.chatone.util.EmoteImageWithTooltip
import io.rudione.chatone.util.GlobalKeyDispatcher
import io.rudione.chatone.util.MessageToken
import io.rudione.chatone.util.NotificationSoundPlayer
import io.rudione.chatone.util.handleHover
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.openUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

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
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    var showModPanel by remember { mutableStateOf(false) }
    var showAutomodWindow by remember { mutableStateOf(false) }
    var profilePopupUserId by remember { mutableStateOf<String?>(null) }
    var profilePopupMessage by remember { mutableStateOf<DisplayMessage.PrivMsg?>(null) }
    var detachedProfileMessage by remember { mutableStateOf<DisplayMessage.PrivMsg?>(null) }
    var pendingModAction by remember { mutableStateOf<PendingModAction?>(null) }
    var isPausedByHotkey by remember { mutableStateOf(false) }
    var isHoveredOverChat by remember { mutableStateOf(false) }
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

    val isAtBottom = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= totalItems - 3
        }
    }


    val isScrolledAway = remember {
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


    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAtBottom.value && !isAutoScrolling) {
            hasNewMessagesWhilePaused = true
        }
    }


    LaunchedEffect(isAtBottom.value) {
        if (isAtBottom.value && !isPausedByHotkey) {
            hasNewMessagesWhilePaused = false
            unreadCount = 0
        }
    }


    LaunchedEffect(effectivelyPaused) {
        if (!effectivelyPaused && state.messages.isNotEmpty()) {
            isAutoScrolling = true
            listState.animateScrollToItem(state.messages.size - 1)
            isAutoScrolling = false
            hasNewMessagesWhilePaused = false
            unreadCount = 0
        }
    }


    val messagesSeq = state.messagesSeq
    LaunchedEffect(messagesSeq) {
        if (messagesSeq == 0L) return@LaunchedEffect
        val size = state.messages.size
        if (size == 0) return@LaunchedEffect
        if (!effectivelyPaused) {
            isAutoScrolling = true


            listState.scrollToItem(size - 1)
            isAutoScrolling = false
            unreadCount = 0
            hasNewMessagesWhilePaused = false
        } else {
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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatEffect.ShowError -> {}
                ChatEffect.ScrollToBottom -> {
                    if (!effectivelyPaused && state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
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
                    val senderLogin = effect.message?.username ?: ""
                    val isMuted =
                        mentionMuteRepository?.isMuted(senderLogin, mentionChannel) == true

                    if (!isActiveChannel && !isMuted) {
                        if (settingsState.mentionSoundEnabled) {
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

    val chatSearchMatches: List<Int> = remember(chatSearchQuery, state.messages) {
        if (chatSearchQuery.isBlank()) emptyList()
        else {
            val q = chatSearchQuery.lowercase()
            state.messages.mapIndexedNotNull { idx, msg ->
                val text = when (msg) {
                    is DisplayMessage.PrivMsg -> (msg.rawMessage?.message
                        ?: msg.tokens.joinToString("") {
                            when (it) {
                                is MessageToken.Text -> it.text
                                is MessageToken.TwitchEmoteToken -> it.name
                                is MessageToken.ThirdPartyEmoteToken -> it.emote.code
                                is MessageToken.Link -> it.displayText
                                is MessageToken.Mention -> it.username
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

    LaunchedEffect(chatSearchCurrentIndex, chatSearchMatches) {
        if (chatSearchMatches.isNotEmpty()) {
            val targetIdx = chatSearchMatches[chatSearchCurrentIndex]
            listState.animateScrollToItem(targetIdx)
        }
    }
    val hotkeyHandler = remember<(KeyEvent) -> Boolean> {
        handler@{ event ->
            if (event.type == KeyEventType.KeyDown &&
                event.key == Key.F &&
                (event.isCtrlPressed || event.isMetaPressed)
            ) {
                chatSearchVisible = !chatSearchVisible
                if (!chatSearchVisible) chatSearchQuery = ""
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
                        val size = state.messages.size
                        if (size > 0) {
                            isAutoScrolling = true
                            listState.scrollToItem(size - 1)
                            isAutoScrolling = false
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
                            val size = state.messages.size
                            if (size > 0) {
                                isAutoScrolling = true
                                listState.scrollToItem(size - 1)
                                isAutoScrolling = false
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

    Box(
        modifier = modifier.fillMaxSize()
            .focusRequester(chatBoxFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event -> hotkeyHandler(event) }) {

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
                        connectionStatus = state.connectionStatus,
                        isConnected = state.isConnected,
                        roomState = state.roomState,
                        isMod = state.canModerate,
                        modModeEnabled = state.modModeEnabled,
                        modPanelOpen = showModPanel,
                        pinnedMacros = settingsState.pinnedMacros,
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
                        isCompact = !isWideScreen,
                        showMenuButton = !isWideScreen && !isMultiChat
                    )
                }
            }

            state.pinnedMessage?.let { pinned ->
                PinnedMessageBar(
                    message = pinned,
                    canUnpin = true,
                    onUnpin = { viewModel.sendEvent(ChatEvent.OnUnpinMessage) })
            }

            state.livePoll?.let { poll ->
                io.rudione.chatone.presentation.chat.components.PollBanner(poll = poll)
            }
            state.livePrediction?.let { pred ->
                io.rudione.chatone.presentation.chat.components.PredictionBanner(prediction = pred)
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

                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            remember(
                                liveWallpaper.displayConfig,
                                liveWallpaper.dominantColor,
                                isDarkChat
                            ) {
                                chatPaneBackgroundColor(liveWallpaper, isDarkChat)
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
                        val visibleMessages = rememberStaggeredMessages(
                            source = state.messages,
                            enabled = settingsState.smoothChatEnabled,
                            stepMs = 90L
                        )
                        val showRepeatCounter = settingsState.showRepeatedMessageCounter
                        val repeatWindowMs =
                            (settingsState.repeatedMessageWindow.coerceAtLeast(1)) * 1000L
                        val dedupedMessages =
                            io.rudione.chatone.presentation.chat.rendering.rememberDedupedMessages(
                                messages = visibleMessages,
                                blockedUserIds = state.blockedUserIds,
                                showBlockedMode = state.showBlockedMode
                            )
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
                                    val recent = ArrayDeque<Triple<String, Long, String>>()
                                    for (m in dedupedMessages) {
                                        if (m !is DisplayMessage.PrivMsg) continue
                                        val ts = m.timestamp
                                        val norm = m.tokens.joinToString("") { tok ->
                                            when (tok) {
                                                is MessageToken.Text -> tok.text
                                                is MessageToken.TwitchEmoteToken -> tok.name
                                                is MessageToken.ThirdPartyEmoteToken -> tok.emote.code
                                                is MessageToken.Link -> tok.displayText
                                                is MessageToken.Mention -> "@${tok.username}"
                                            }
                                        }.trim().lowercase()
                                        if (norm.isEmpty()) continue
                                        while (recent.isNotEmpty() && ts - recent.first().second > repeatWindowMs) {
                                            recent.removeFirst()
                                        }
                                        val n = recent.count { it.first == norm } + 1
                                        if (n > 1) put(m.id, n)
                                        recent.addLast(Triple(norm, ts, m.id))
                                    }
                                }
                            }
                        key(channelLogin) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().handleHover(
                                        onEnter = {
                                            if (settingsState.pauseOnHover) isHoveredOverChat = true
                                        },
                                        onExit = { isHoveredOverChat = false }
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
                                        key = { _, it -> it.id }) { index, message ->
                                        val zebraTintColor =
                                            if (settingsState.alternateRowBackground && index % 2 == 1) {
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
                                                allModButtons = settingsState.allModButtons,
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
                                                            timestamp = kotlinx.datetime.Clock.System.now()
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
                                                userColorByLogin = userColorByLogin
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
                                HighlightScrollbar(
                                    listState = listState,
                                    messages = state.messages,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(10.dp)
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
                                                    isAutoScrolling = true
                                                    val lastIndex = state.messages.lastIndex
                                                    if (lastIndex >= 0) listState.scrollToItem(
                                                        index = lastIndex,
                                                        scrollOffset = -48
                                                    )
                                                    isAutoScrolling = false
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
                                                    .offset(x = 2.dp, y = (-2).dp).size(16.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.error,
                                                shadowElevation = 2.dp
                                            ) {
                                                Text(
                                                    text = badgeText,
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontSize = 8.sp, lineHeight = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    modifier = Modifier.wrapContentSize(Alignment.Center)
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

            AnimatedVisibility(
                visible = showModPanel && state.canModerate,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
            ) {
                ModerationPanel(
                    roomState = state.roomState,
                    channelLogin = channelLogin,
                    isMod = state.canModerate,
                    pinnedMacros = settingsState.pinnedMacros,
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
                    onClose = { showModPanel = false }
                )
            }

            if (showAutomodWindow) {
                io.rudione.chatone.presentation.automod.DetachedAutomodWindow(
                    currentChannelLogin = channelLogin,
                    onClose = { showAutomodWindow = false }
                )
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
                        }
                    },
                    onCancel = { viewModel.sendEvent(ChatEvent.OnCancelReply) }
                )
            }

            val slashSuggestions = remember(state.messageInput, state.isMod, state.isBroadcaster) {
                io.rudione.chatone.util.SlashCommand.suggest(
                    state.messageInput,
                    isMod = state.isMod || state.canModerate,
                    isBroadcaster = state.isBroadcaster
                )
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
            MessageInput(
                value = state.messageInput,
                onValueChange = {
                    emoteTabIndex = -1
                    mentionTabIndex = -1
                    viewModel.sendEvent(ChatEvent.OnMessageInputChanged(it))
                },
                onSend = { viewModel.sendEvent(ChatEvent.OnSendMessage) },
                onSendKeepText = { viewModel.sendEvent(ChatEvent.OnSendMessageKeepText) },
                onHistoryUp = { viewModel.sendEvent(ChatEvent.OnHistoryUp) },
                onHistoryDown = { viewModel.sendEvent(ChatEvent.OnHistoryDown) },
                onEmotePickerClick = {
                    viewModel.sendEvent(ChatEvent.OnToggleEmotePicker)
                },
                enabled = state.isConnected && !state.isBanned,
                isBanned = state.isBanned,
                banReason = state.banReason,
                pauseHotkey = "",
                onTogglePause = { },
                focusRequester = inputFocusRequester,
                showEmoteCompletions = state.showEmoteCompletions && state.emoteCompletions.isNotEmpty(),
                showMentionCompletions = state.showMentionCompletions && state.mentionCompletions.isNotEmpty(),
                emoteCount = state.emoteCompletions.size,
                mentionCount = state.mentionCompletions.size,
                emoteTabIndex = emoteTabIndex,
                mentionTabIndex = mentionTabIndex,
                onTabEmote = { idx -> emoteTabIndex = idx },
                onTabMention = { idx -> mentionTabIndex = idx },
                onConfirmEmoteTab = {
                    val idx = emoteTabIndex.coerceIn(0, state.emoteCompletions.lastIndex)
                    viewModel.sendEvent(ChatEvent.OnSelectEmoteCompletion(state.emoteCompletions[idx]))
                    emoteTabIndex = -1
                    inputFocusRequester.requestFocus()
                },
                onConfirmMentionTab = {
                    val idx = mentionTabIndex.coerceIn(0, state.mentionCompletions.lastIndex)
                    viewModel.sendEvent(ChatEvent.OnSelectMentionCompletion(state.mentionCompletions[idx]))
                    mentionTabIndex = -1
                    inputFocusRequester.requestFocus()
                },
                hidePlaceholder = settingsState.hideChatInputPlaceholder,
                hideEmojiButton = settingsState.hideEmojiButton,
                placeholderText = when {
                    state.isBanned -> "You are banned" + (state.banReason?.let { " — $it" }
                        ?: "") + " in #${state.channelLogin}"

                    state.channelLogin.isNotEmpty() -> "Send a message in #${state.channelLogin}"
                    else -> null
                },
                showSlashCompletions = slashSuggestions.isNotEmpty(),
                slashCount = slashSuggestions.size,
                slashTabIndex = slashTabIndex,
                onTabSlash = { slashTabIndex = it },
                onConfirmSlashTab = {
                    val idx = slashTabIndex.coerceIn(0, slashSuggestions.lastIndex)
                    val name = slashSuggestions[idx].name
                    viewModel.sendEvent(ChatEvent.OnMessageInputChanged("/$name "))
                    slashTabIndex = -1
                    inputFocusRequester.requestFocus()
                },
                glowIntensity = if (settingsState.chatInputEventGlow) state.inputGlowIntensity else 0f,
                glowTriggerTs = state.inputGlowTriggerTs
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
    }

    if (state.isEmotePickerVisible) {
        val resolvedEmotes = emoteRepository.getResolvedEmotes(channelLogin)
        val pickerPersonalEmotes = remember(state.currentUserId, state.twitchSubscriberEmotes) {
            val sevenTv = if (state.currentUserId.isNotEmpty())
                emoteRepository.getCachedPersonalEmotes(state.currentUserId)
            else emptyList()
            (sevenTv + state.twitchSubscriberEmotes).distinctBy { "${it.provider.name}_${it.id}" }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
        ) {
            EmotePickerSheet(
                channelEmotes = resolvedEmotes,
                personalEmotes = pickerPersonalEmotes,
                closeOnMouseLeave = settingsState.closeEmotePickerOnMouseLeave,
                onEmoteSelected = { emote ->
                    val current = state.messageInput
                    val newInput =
                        if (current.isEmpty() || current.endsWith(" ")) "$current${emote.code} " else "$current ${emote.code} "
                    viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
                },
                onEmojiSelected = { emoji ->
                    val current = state.messageInput
                    val newInput =
                        if (current.isEmpty() || current.endsWith(" ")) "$current$emoji" else "$current $emoji"
                    viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
                },
                onDismiss = {
                    viewModel.sendEvent(ChatEvent.OnToggleEmotePicker)
                }
            )
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

private sealed class PendingModAction {
    data class Timeout(val userId: String, val displayName: String, val duration: Int) :
        PendingModAction()

    data class Ban(val userId: String, val displayName: String) : PendingModAction()
}

@Composable
private fun ModActionConfirmDialog(
    action: PendingModAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val (title, text) = when (action) {
        is PendingModAction.Timeout -> {
            val d = when {
                action.duration < 60 -> "${action.duration}s"; action.duration < 3600 -> "${action.duration / 60}m"; action.duration < 86400 -> "${action.duration / 3600}h"; else -> "${action.duration / 86400}d"
            }
            "${s.chatTimeoutUser} ${action.displayName}?" to "${s.chatTimeoutUser}: $d"
        }

        is PendingModAction.Ban -> "${s.chatBanUser} ${action.displayName}?" to "This will permanently ban the user from chat."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (action is PendingModAction.Ban) ChatoneTheme.extraColors.modBan else ChatoneTheme.extraColors.modTimeout)
            ) { Text(s.confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    channelLogin: String,
    connectionStatus: String,
    isConnected: Boolean,
    roomState: RoomState,
    isMod: Boolean,
    modModeEnabled: Boolean,
    modPanelOpen: Boolean = false,
    pinnedMacros: List<Macro> = emptyList(),
    onBack: () -> Unit,
    onToggleModMode: () -> Unit,
    onOpenModPanel: () -> Unit = {},
    onExecuteMacro: (Macro) -> Unit = {},
    onRefresh: () -> Unit = {},
    isCompact: Boolean = false,
    showMenuButton: Boolean = false,
) {
    val topBarWallpaper = LocalWallpaperController.current.state
    val topBarBlur = topBarWallpaper.panelColorConfig.topBarBlurRadius
    val topBarColor = topBarBackgroundColor(topBarWallpaper, MaterialTheme.colorScheme.surface)
    val s = LocalStrings.current

    Box(modifier = Modifier.fillMaxWidth()) {
        if (topBarBlur > 0f) {
            Box(modifier = Modifier.matchParentSize().background(topBarColor).panelBlur(topBarBlur))
        } else {
            Box(modifier = Modifier.matchParentSize().background(topBarColor))
        }
        Column {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showMenuButton) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            "#$channelLogin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(6.dp).clip(CircleShape)
                                    .background(if (isConnected) ChatoneTheme.extraColors.connected else MaterialTheme.colorScheme.error)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                connectionStatus, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    val toolbarScrollState = androidx.compose.foundation.rememberScrollState()
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .horizontalScroll(toolbarScrollState)
                            .then(
                                io.rudione.chatone.presentation.chat.multichat.horizontalMouseWheelScrollModifier(
                                    toolbarScrollState
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        var refreshSpinning by remember { mutableStateOf(false) }
                        val refreshRotation by animateFloatAsState(
                            targetValue = if (refreshSpinning) 360f else 0f,
                            animationSpec = tween(500),
                            finishedListener = { refreshSpinning = false }
                        )
                        IconButton(
                            onClick = {
                                refreshSpinning = true
                                onRefresh()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp).rotate(refreshRotation)
                            )
                        }
                        io.rudione.chatone.presentation.chat.multichat.ChatTopBarAddPanelButton(
                            currentChannel = channelLogin
                        )
                        if (isMod) {

                            if (pinnedMacros.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    pinnedMacros.forEach { macro ->
                                        io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox(
                                            tooltip = macro.name.ifBlank { macro.icon }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                                            alpha = 0.5f
                                                        )
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onExecuteMacro(macro) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    macro.icon,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox(
                                tooltip = "Mod Mode"
                            ) {
                                FilledIconToggleButton(
                                    checked = modModeEnabled,
                                    onCheckedChange = { onToggleModMode() },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.15f
                                        ),
                                        checkedContentColor = MaterialTheme.colorScheme.primary,
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_sword),
                                        contentDescription = "Mod Mode",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox(
                                tooltip = "Mod Panel"
                            ) {
                                FilledIconToggleButton(
                                    checked = modPanelOpen,
                                    onCheckedChange = { onOpenModPanel() },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.Build,
                                        contentDescription = "Mod Panel",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                val roomChips = buildList {
                    if (roomState.emoteOnly) add("Emote-only"); if (roomState.subsOnly) add("Sub-only")
                    if (roomState.slowMode > 0) add("Slow: ${roomState.slowMode}s")
                    if (roomState.followersOnly >= 0) add(
                        if (roomState.followersOnly == 0) s.profileFollow else s.chatFollowersOnly.replace(
                            "{0}",
                            roomState.followersOnly.toString()
                        )
                    )
                    if (roomState.r9k) add("R9K")
                }
                if (roomChips.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        roomChips.forEach { chip ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    chip, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PrivMsgItem(
    message: DisplayMessage.PrivMsg,
    isCompact: Boolean = false,
    showModActions: Boolean = false,
    timestampFormat: SettingsState.TimestampFormat = SettingsState.TimestampFormat.H24,
    showBadges: Boolean = true,
    isMod: Boolean = false,
    currentUserId: String = "",
    emoteSize: SettingsState.EmoteSize = SettingsState.EmoteSize.SMALL,
    customModButtons: List<ModActionButton> = emptyList(),
    allModButtons: List<ModActionButton> = emptyList(),
    modButtonsVersion: Int = 0,
    showCustomModButtons: Boolean = true,
    showDefaultDeleteButton: Boolean = true,
    showDefaultTimeoutButton: Boolean = true,
    showDefaultBanButton: Boolean = true,
    chatFontSizeSp: Float = 13f,
    onUsernameClick: () -> Unit = {},
    onRightClickUsername: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onReply: () -> Unit = {},
    onCopyText: () -> Unit = {},
    onPin: () -> Unit = {},
    onTimeout: () -> Unit = {},
    onCustomTimeout: (Int) -> Unit = {},
    onBan: () -> Unit = {},
    onDelete: () -> Unit = {},
    actorCanModerate: Boolean = false,
    actorIsBroadcaster: Boolean = false,
    searchHighlightQuery: String = "",
    highlightRules: List<io.rudione.chatone.domain.model.HighlightRule> = emptyList(),
    zebraTint: Color = Color.Transparent,
    extraVerticalPadding: androidx.compose.ui.unit.Dp = 0.dp,
    repeatCount: Int = 1,
    userColorByLogin: Map<String, Color> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val extraColors = ChatoneTheme.extraColors
    val mentionColor =
        if (message.highlightColor != null) Color(message.highlightColor) else MaterialTheme.colorScheme.primary
    val s = LocalStrings.current
    fun ruleColor(id: String, default: Long) =
        Color(highlightRules.firstOrNull { it.id == id }?.color ?: default)

    val highlightedMessageColor = ruleColor("channel_points", 0xFF9146FF)
    val fmColor = ruleColor("first_message", 0xFFF39C12)
    val searchBgColor = ruleColor("search_match", 0xFF4FC3F7).copy(alpha = 0.18f)
    val mentionBgColor = ruleColor("username", 0xFFFF6B6B).copy(alpha = 0.12f)
    val mentionAccentColor = ruleColor("username", 0xFFFF6B6B)
    val isOwnMessage = currentUserId.isNotEmpty() && message.userId == currentUserId
    val isSearchMatch = searchHighlightQuery.isNotBlank() && run {
        val msgText = message.rawMessage?.message ?: message.tokens.joinToString("") {
            when (it) {
                is MessageToken.Text -> it.text
                is MessageToken.TwitchEmoteToken -> it.name
                is MessageToken.ThirdPartyEmoteToken -> it.emote.code
                is MessageToken.Link -> it.displayText
                is MessageToken.Mention -> it.username
            }
        }
        msgText.contains(searchHighlightQuery, ignoreCase = true)
    }
    val backgroundColor = when {
        isSearchMatch -> searchBgColor
        message.isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
        message.isHighlighted -> highlightedMessageColor.copy(alpha = 0.10f)
        message.isMention && message.highlightColor != null -> Color(message.highlightColor).copy(
            alpha = 0.12f
        )

        message.isMention -> mentionBgColor
        message.isFirstMessage -> fmColor.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val accentBarModifier = when {
        message.isMention -> Modifier.drawWithContent {
            drawContent(); drawRect(
            color = mentionAccentColor.copy(alpha = 0.85f),
            size = Size(4.dp.toPx(), size.height)
        )
        }

        message.isHighlighted -> Modifier.drawWithContent {
            drawContent(); drawRect(
            color = highlightedMessageColor.copy(alpha = 0.9f),
            size = Size(4.dp.toPx(), size.height)
        )
        }

        message.isFirstMessage -> Modifier.drawWithContent {
            drawContent(); drawRect(
            color = fmColor.copy(alpha = 0.75f),
            size = Size(4.dp.toPx(), size.height)
        )
        }

        else -> Modifier
    }
    val hasAccentBar = message.isMention || message.isFirstMessage || message.isHighlighted


    val imageLinks = remember(message.tokens) {
        message.tokens.filterIsInstance<MessageToken.Link>().filter { isImageUrl(it.url) }
    }

    val inlineSettings = remember { SettingsViewModel.loadInitialState() }

    var rowHovered by remember { mutableStateOf(false) }
    val hoverOverlay by animateColorAsState(
        if (rowHovered && backgroundColor == Color.Transparent)
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        else Color.Transparent,
        tween(100)
    )

    val effectiveBg = when {
        backgroundColor != Color.Transparent -> backgroundColor
        hoverOverlay != Color.Transparent -> hoverOverlay
        else -> zebraTint
    }
    Column(
        modifier = modifier.fillMaxWidth()
            .background(effectiveBg)
            .then(accentBarModifier)
            .handleHover(onEnter = { rowHovered = true }, onExit = { rowHovered = false })
            .padding(
                start = if (hasAccentBar) 6.dp else 2.dp,
                end = 1.dp,
                top = 2.dp + extraVerticalPadding,
                bottom = 2.dp + extraVerticalPadding
            )
    ) {
        if (message.rewardName != null) {
            val rewardLabel = when (message.rewardName) {
                "Highlight My Message" -> s.rewardHighlightMyMessage
                "Channel Points Reward" -> s.rewardChannelPoints
                else -> message.rewardName
            }
            Row(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(highlightedMessageColor.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = highlightedMessageColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${s.rewardRedeemedPrefix}: $rewardLabel",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = highlightedMessageColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        val showTimestamp = timestampFormat != SettingsState.TimestampFormat.OFF
        val hasBadges = showBadges && (message.badges.any { it.imageUrl.isNotEmpty() } ||
                (message.sevenTvBadge?.let { it.url2x.isNotEmpty() || it.url1x.isNotEmpty() }
                    ?: false))
        val useCompact = isCompact && (showTimestamp || hasBadges || showModActions)

        if (useCompact) {
            CompactMessageLayout(
                message = message,
                showModActions = showModActions,
                showTimestamp = showTimestamp,
                timestampFormat = timestampFormat,
                hasBadges = hasBadges,
                showBadges = showBadges,
                isMod = isMod,
                isOwnMessage = isOwnMessage,
                currentUserId = currentUserId,
                emoteSize = emoteSize,
                customModButtons = customModButtons,
                allModButtons = allModButtons,
                modButtonsVersion = modButtonsVersion,
                showCustomModButtons = showCustomModButtons,
                showDefaultDeleteButton = showDefaultDeleteButton,
                showDefaultTimeoutButton = showDefaultTimeoutButton,
                showDefaultBanButton = showDefaultBanButton,
                chatFontSizeSp = chatFontSizeSp,
                onUsernameClick = onUsernameClick,
                onRightClickUsername = onRightClickUsername,
                onMentionClick = onMentionClick,
                onReply = onReply,
                onCopyText = onCopyText,
                onPin = onPin,
                onTimeout = onTimeout,
                onCustomTimeout = onCustomTimeout,
                onBan = onBan,
                onDelete = onDelete,
                actorCanModerate = actorCanModerate,
                actorIsBroadcaster = actorIsBroadcaster,
                extraColors = extraColors,
                mentionColor = mentionColor,
                backgroundColor = backgroundColor,
                hasAccentBar = hasAccentBar,
                s = s,
                userColorByLogin = userColorByLogin
            )
        } else {
            val replyParentNameOuter = message.rawMessage?.replyParentDisplayName
            val replyParentBodyOuter = message.rawMessage?.replyParentMsgBody
            val replyParentLoginOuter = message.rawMessage?.replyParentUserLogin
            if (replyParentNameOuter != null && replyParentBodyOuter != null) {
                val parentColor = replyParentLoginOuter?.lowercase()?.let { userColorByLogin[it] }
                    ?: replyParentLoginOuter?.let { stableUserColor(it.lowercase()) }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant
                DisableSelection {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 4.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = buildAnnotatedString {
                                pushStringAnnotation("mention", "@$replyParentLoginOuter")
                                withStyle(
                                    SpanStyle(
                                        color = parentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) { append("@$replyParentNameOuter") }
                                pop()
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.65f
                                        )
                                    )
                                ) { append(": $replyParentBodyOuter") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                replyParentLoginOuter?.let { login -> onMentionClick("@$login") }
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                val prefixTopOffset = 3.dp
                if (showModActions || timestampFormat != SettingsState.TimestampFormat.OFF || showBadges) {
                    Row(
                        modifier = Modifier
                            .wrapContentHeight()
                            .padding(top = prefixTopOffset),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (showModActions) {
                            val canAct = canActOnUser(
                                actorIsBroadcaster = actorIsBroadcaster,
                                actorIsMod = actorCanModerate,
                                targetIsBroadcaster = message.isBroadcaster,
                                targetIsMod = message.isModerator,
                                targetIsVip = message.isVip,
                                targetIsSubscriber = message.isSubscriber,
                                actorIsGrandMod = false,
                                targetIsGrandMod = message.isGrandMod
                            )

                            val modOrderKey = "$modButtonsVersion:" + if (allModButtons.isNotEmpty())
                                allModButtons.joinToString("|") { "${it.id}:${it.sortOrder}:${it.enabled}" }
                            else
                                customModButtons.joinToString("|") { it.id }
                            key(modOrderKey) {
                                Row(
                                    modifier = Modifier.padding(end = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val orderedButtons = if (allModButtons.isNotEmpty()) {
                                        allModButtons.sortedBy { it.sortOrder }
                                    } else {
                                        ModActionButton.defaultOrderedList() + customModButtons
                                    }
                                    orderedButtons.forEach { btn ->
                                        if (!btn.enabled) return@forEach
                                        key(btn.id) {
                                            when (btn.id) {
                                                "default_delete" -> {
                                                    if (showDefaultDeleteButton && (canAct || isOwnMessage)) {
                                                        ModActionIconBtn(
                                                            icon = Icons.Outlined.Delete, label = "Del",
                                                            tint = extraColors.modDelete, onClick = onDelete
                                                        )
                                                    }
                                                }

                                                "default_timeout" -> {
                                                    if (showDefaultTimeoutButton && canAct && !isOwnMessage) {
                                                        ModActionIconBtn(
                                                            icon = Icons.Outlined.Refresh,
                                                            label = "10m",
                                                            tint = extraColors.modTimeout,
                                                            onClick = onTimeout,
                                                            visible = false
                                                        )
                                                    }
                                                }

                                                "default_ban" -> {
                                                    if (showDefaultBanButton && canAct && !isOwnMessage) {
                                                        ModActionIconBtn(
                                                            icon = Icons.Filled.Close,
                                                            label = s.chatBanUser,
                                                            tint = extraColors.modBan,
                                                            onClick = onBan
                                                        )
                                                    }
                                                }

                                                else -> {
                                                    if (showCustomModButtons && canAct && !isOwnMessage) {
                                                        ModActionIconBtn(
                                                            icon = Icons.Outlined.Refresh,
                                                            label = btn.displayLabel,
                                                            tint = extraColors.modTimeout,
                                                            onClick = { onCustomTimeout(btn.durationSeconds) },
                                                            visible = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (timestampFormat != SettingsState.TimestampFormat.OFF) {
                            Text(
                                formatTimestamp(message.timestamp, timestampFormat),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                            )
                        }

                        if (showBadges) {
                            val hasBadgesInner = message.badges.any { it.imageUrl.isNotEmpty() } ||
                                    (message.sevenTvBadge?.let { it.url2x.isNotEmpty() || it.url1x.isNotEmpty() }
                                        ?: false)

                            if (hasBadgesInner) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                ) {
                                    message.badges.forEach { badge ->
                                        if (badge.imageUrl.isNotEmpty()) {
                                            io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox(
                                                tooltip = badge.tooltip.ifBlank { badge.id }
                                            ) {
                                                AsyncImage(
                                                    model = badge.imageUrl,
                                                    contentDescription = badge.tooltip,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    message.sevenTvBadge?.let { stvBadge ->
                                        val badgeUrl = stvBadge.url2x.ifEmpty { stvBadge.url1x }
                                        if (badgeUrl.isNotEmpty()) {
                                            io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox(
                                                tooltip = stvBadge.tooltip.ifBlank { "7TV Badge" }
                                            ) {
                                                AsyncImage(
                                                    model = badgeUrl,
                                                    contentDescription = stvBadge.tooltip,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                var showContextMenu by remember { mutableStateOf(false) }
                val emoteSizeSp = when (emoteSize) {
                    SettingsState.EmoteSize.SMALL -> 20.sp; SettingsState.EmoteSize.MEDIUM -> 28.sp; SettingsState.EmoteSize.LARGE -> 36.sp
                }
                val userColor = parseColor(message.color) ?: MaterialTheme.colorScheme.primary
                val inlineContent = mutableMapOf<String, InlineTextContent>()
                val emoteKeyMap = mutableMapOf<String, GenericEmote>()
                var emoteCounter = 0

                val annotatedString = buildAnnotatedString {
                    if (message.isDeleted) {
                        pushStringAnnotation("username", message.userId)
                        withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                            append(
                                message.displayName
                            )
                        }
                        pop()
                        append(": ")
                        val originalText = message.tokens.joinToString("") { token ->
                            when (token) {
                                is MessageToken.Text -> token.text; is MessageToken.TwitchEmoteToken -> token.name
                                is MessageToken.ThirdPartyEmoteToken -> token.emote.code; is MessageToken.Link -> token.displayText
                                is MessageToken.Mention -> token.username
                            }
                        }
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                                textDecoration = TextDecoration.LineThrough
                            )
                        ) {
                            append(originalText.ifEmpty { "message deleted" })
                        }
                    } else {
                        pushStringAnnotation("username", message.userId)
                        if (message.isAction) {
                            withStyle(
                                SpanStyle(
                                    color = userColor,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append(message.displayName); append(" ") }
                        } else {
                            withStyle(
                                SpanStyle(
                                    color = userColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) { append(message.displayName) }
                            append(": ")
                        }
                        pop()
                        val messageColor = if (message.isAction) userColor else Color.Unspecified
                        message.tokens.forEach { token ->
                            when (token) {
                                is MessageToken.Text -> {
                                    if (message.isAction) withStyle(
                                        SpanStyle(
                                            color = messageColor,
                                            fontStyle = FontStyle.Italic
                                        )
                                    ) { append(token.text) }
                                    else append(token.text)
                                }

                                is MessageToken.TwitchEmoteToken -> {
                                    val key = "emote_${emoteCounter++}"; appendInlineContent(
                                        key,
                                        token.name
                                    )
                                    inlineContent[key] = InlineTextContent(
                                        Placeholder(
                                            emoteSizeSp,
                                            emoteSizeSp,
                                            PlaceholderVerticalAlign.TextCenter
                                        )
                                    ) {
                                        AnimatedEmoteImage(
                                            url = token.url,
                                            contentDescription = token.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                is MessageToken.ThirdPartyEmoteToken -> {
                                    val key = "emote_${emoteCounter++}"; appendInlineContent(
                                        key,
                                        token.emote.code
                                    )
                                    val (emoteW, emoteH) = computeEmoteDisplaySize(
                                        token.emote.width,
                                        token.emote.height,
                                        emoteSizeSp
                                    )
                                    emoteKeyMap[key] = token.emote
                                    inlineContent[key] = InlineTextContent(
                                        Placeholder(
                                            emoteW,
                                            emoteH,
                                            PlaceholderVerticalAlign.TextCenter
                                        )
                                    ) {
                                        Box {
                                            EmoteImageWithTooltip(
                                                emote = token.emote,
                                                modifier = Modifier.fillMaxSize(),
                                                onShowContextMenu = null
                                            )
                                            token.overlays.forEach { overlay ->
                                                EmoteImageWithTooltip(
                                                    emote = overlay,
                                                    modifier = Modifier.fillMaxSize(),
                                                    onShowContextMenu = { }
                                                )
                                            }
                                        }
                                    }
                                }

                                is MessageToken.Link -> {
                                    pushStringAnnotation("url", token.url)
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) { append(token.displayText) }
                                    pop()
                                }

                                is MessageToken.Mention -> {

                                    pushStringAnnotation("mention", token.username)
                                    val mentionedLogin = token.username
                                        .removePrefix("@")
                                        .lowercase()
                                    val mentionColor = userColorByLogin[mentionedLogin]
                                        ?: stableUserColor(mentionedLogin)
                                    withStyle(
                                        SpanStyle(
                                            color = mentionColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) { append(token.username) }
                                    pop()
                                }
                            }
                        }
                    }
                }

                val clipboardManager = LocalClipboardManager.current
                var contextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
                val uriHandler = LocalUriHandler.current
                var textLayoutResult by remember {
                    mutableStateOf<TextLayoutResult?>(
                        null
                    )
                }

                var hoveredUrl by remember { mutableStateOf<String?>(null) }
                var hoveredEmote by remember { mutableStateOf<GenericEmote?>(null) }
                var hoverOffset by remember { mutableStateOf(IntOffset.Zero) }

                Box(modifier = Modifier.weight(1f)) {
                    if (message.isFirstMessage && !message.isDeleted) {
                        DisableSelection {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(fmColor.copy(alpha = 0.18f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                }
                            }
                            Spacer(Modifier.height(1.dp))
                        }
                    }
                    SelectionContainer {
                        Text(
                            text = buildAnnotatedString {
                                append(annotatedString)
                                if (repeatCount > 1) {
                                    append("  ")
                                    withStyle(
                                        SpanStyle(
                                            color = mentionColor,
                                            fontWeight = FontWeight.Bold,
                                            background = mentionColor.copy(alpha = 0.12f)
                                        )
                                    ) {
                                        append(" ×$repeatCount ")
                                    }
                                }
                            },
                            inlineContent = inlineContent,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = when (chatFontSizeSp) {
                                    in 0f..11f -> 11.sp
                                    in 11f..16f -> chatFontSizeSp.sp
                                    else -> 16.sp
                                }
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                                .focusable()
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.isCtrlPressed || event.isMetaPressed) &&
                                        event.key == Key.C
                                    ) {
                                        val rawText = message.tokens.joinToString("") { token ->
                                            when (token) {
                                                is MessageToken.Text -> token.text
                                                is MessageToken.TwitchEmoteToken -> token.name
                                                is MessageToken.ThirdPartyEmoteToken -> token.emote.code
                                                is MessageToken.Link -> token.displayText
                                                is MessageToken.Mention -> token.username
                                            }
                                        }
                                        clipboardManager.setText(AnnotatedString(rawText))
                                        true
                                    } else false
                                }
                                .pointerInput(annotatedString) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            textLayoutResult?.let { layoutResult ->
                                                val charOffset =
                                                    layoutResult.getOffsetForPosition(offset)
                                                annotatedString.getStringAnnotations(
                                                    "url",
                                                    charOffset,
                                                    charOffset
                                                )
                                                    .firstOrNull()?.let { annotation ->
                                                        try {
                                                            openUrl(
                                                                annotation.item,
                                                                inlineSettings.linkOpenMode
                                                            )
                                                        } catch (_: Exception) {
                                                        }
                                                        return@detectTapGestures
                                                    }
                                                annotatedString.getStringAnnotations(
                                                    "username",
                                                    charOffset,
                                                    charOffset
                                                )
                                                    .firstOrNull()?.let {
                                                        onUsernameClick(); return@detectTapGestures
                                                    }
                                                annotatedString.getStringAnnotations(
                                                    "mention",
                                                    charOffset,
                                                    charOffset
                                                )
                                                    .firstOrNull()?.let { annotation ->
                                                        onMentionClick(annotation.item)
                                                        return@detectTapGestures
                                                    }
                                            }
                                        },
                                        onLongPress = { offset ->
                                            contextMenuOffset =
                                                IntOffset(
                                                    offset.x.roundToInt(),
                                                    offset.y.roundToInt()
                                                )
                                            showContextMenu = true
                                        }
                                    )
                                }
                                .pointerInput(message.id, annotatedString, emoteKeyMap) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val pos =
                                                event.changes.firstOrNull()?.position ?: continue

                                            when (event.type) {
                                                PointerEventType.Move, PointerEventType.Enter -> {
                                                    textLayoutResult?.let { layout ->
                                                        val charOffset =
                                                            layout.getOffsetForPosition(pos)

                                                        val urlAnn =
                                                            annotatedString.getStringAnnotations(
                                                                "url",
                                                                charOffset,
                                                                charOffset
                                                            ).firstOrNull()
                                                        hoveredUrl = urlAnn?.item

                                                        val inlineAnn =
                                                            annotatedString.getStringAnnotations(
                                                                "androidx.compose.foundation.text.inlineContent",
                                                                charOffset,
                                                                charOffset
                                                            ).firstOrNull()

                                                        var newHoveredEmote: GenericEmote? = null
                                                        if (inlineAnn != null) {
                                                            val bbox =
                                                                layout.getBoundingBox(charOffset)
                                                            if (bbox.contains(
                                                                    Offset(
                                                                        pos.x,
                                                                        pos.y
                                                                    )
                                                                )
                                                            ) {
                                                                newHoveredEmote =
                                                                    emoteKeyMap[inlineAnn.item]
                                                            }
                                                        }
                                                        if (newHoveredEmote != hoveredEmote) hoveredEmote =
                                                            newHoveredEmote
                                                        hoverOffset =
                                                            IntOffset(pos.x.toInt(), pos.y.toInt())
                                                    }
                                                }

                                                PointerEventType.Exit -> {
                                                    hoveredUrl = null
                                                    if (hoveredEmote != null) hoveredEmote = null
                                                }

                                                PointerEventType.Press -> {
                                                    if (event.buttons.isSecondaryPressed) {
                                                        textLayoutResult?.let { layout ->
                                                            val charOffset =
                                                                layout.getOffsetForPosition(pos)

                                                            val inlineAnn =
                                                                annotatedString.getStringAnnotations(
                                                                    "androidx.compose.foundation.text.inlineContent",
                                                                    charOffset,
                                                                    charOffset
                                                                ).firstOrNull()

                                                            var handled = false
                                                            if (inlineAnn != null) {
                                                                val bbox =
                                                                    layout.getBoundingBox(charOffset)
                                                                if (bbox.contains(
                                                                        Offset(
                                                                            pos.x,
                                                                            pos.y
                                                                        )
                                                                    )
                                                                ) {
                                                                    val emote =
                                                                        emoteKeyMap[inlineAnn.item]
                                                                    if (emote != null && emote.provider == EmoteProvider.SEVEN_TV && emote.id.isNotEmpty()) {
                                                                        try {
                                                                            uriHandler.openUri("https://7tv.app/emotes/${emote.id}")
                                                                        } catch (_: Exception) {
                                                                        }
                                                                        event.changes.forEach { it.consume() }
                                                                        handled = true
                                                                    }
                                                                }
                                                            }

                                                            if (!handled) {
                                                                val onUsername =
                                                                    annotatedString.getStringAnnotations(
                                                                        "username",
                                                                        charOffset,
                                                                        charOffset
                                                                    ).isNotEmpty()
                                                                if (onUsername) {
                                                                    onRightClickUsername(message.displayName)
                                                                    event.changes.forEach { it.consume() }
                                                                    handled = true
                                                                }
                                                            }

                                                            if (!handled) {
                                                                contextMenuOffset = IntOffset(
                                                                    pos.x.toInt(),
                                                                    pos.y.toInt()
                                                                )
                                                                showContextMenu = true
                                                                event.changes.forEach { it.consume() }
                                                            }

                                                        } ?: run {
                                                            contextMenuOffset = IntOffset(
                                                                pos.x.toInt(),
                                                                pos.y.toInt()
                                                            )
                                                            showContextMenu = true
                                                            event.changes.forEach { it.consume() }
                                                        }
                                                    }
                                                }

                                                else -> {}
                                            }
                                        }
                                    }
                                },
                            onTextLayout = { textLayoutResult = it }
                        )
                    }

                    hoveredUrl?.let { url ->
                        Popup(
                            popupPositionProvider = object : PopupPositionProvider {
                                override fun calculatePosition(
                                    anchorBounds: IntRect,
                                    windowSize: IntSize,
                                    layoutDirection: LayoutDirection,
                                    popupContentSize: IntSize
                                ): IntOffset {
                                    val x = anchorBounds.left + hoverOffset.x + 16
                                    val yBelow = anchorBounds.top + hoverOffset.y + 24
                                    val yAbove =
                                        anchorBounds.top + hoverOffset.y - popupContentSize.height - 8
                                    val finalY =
                                        if (yBelow + popupContentSize.height > windowSize.height) yAbove else yBelow
                                    val finalX =
                                        if (x + popupContentSize.width > windowSize.width) windowSize.width - popupContentSize.width - 8 else x
                                    return IntOffset(finalX, finalY)
                                }
                            },
                            properties = PopupProperties(
                                focusable = false,
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false,
                                clippingEnabled = false
                            )
                        ) {
                            LinkHoverPopup(url = url)
                        }
                    }
                }

                if (showContextMenu) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = contextMenuOffset + IntOffset(8, 8),
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        ),
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        LiquidGlassSurface(
                            modifier = Modifier.widthIn(min = 160.dp, max = 240.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            backgroundAlphaHigh = 0.94f,
                            backgroundAlphaLow = 0.85f,
                            borderAlphaHigh = 0f,
                            borderAlphaLow = 0f
                        ) {
                            Column {
                                if (isMod) {
                                    LiquidGlassDropdownItem(
                                        text = "Pin",
                                        icon = Icons.Filled.Place,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        onClick = { showContextMenu = false; onPin() }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                }
                                LiquidGlassDropdownItem(
                                    text = s.chatReplyTo,
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClick = { showContextMenu = false; onReply() }
                                )
                                LiquidGlassDropdownItem(
                                    text = s.chatCopyMessage,
                                    icon = Icons.Outlined.CopyAll,
                                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = {
                                        showContextMenu = false
                                        val rawText = message.tokens.joinToString("") { token ->
                                            when (token) {
                                                is MessageToken.Text -> token.text
                                                is MessageToken.TwitchEmoteToken -> token.name
                                                is MessageToken.ThirdPartyEmoteToken -> token.emote.code
                                                is MessageToken.Link -> token.displayText
                                                is MessageToken.Mention -> token.username
                                            }
                                        }
                                        clipboardManager.setText(AnnotatedString(rawText))
                                    }
                                )
                            }
                        }
                    }
                }

            }
        }

        if (imageLinks.isNotEmpty() && inlineSettings.showInlineImages != InlineImageMode.OFF && !message.isDeleted) {
            imageLinks.forEach { link ->
                var isRevealed by remember { mutableStateOf(inlineSettings.showInlineImages == InlineImageMode.ON) }
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp)
                        .heightIn(max = inlineSettings.inlineImageMaxHeight.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isRevealed) isRevealed = true
                            else try {
                                openUrl(link.url, inlineSettings.linkOpenMode)
                            } catch (_: Exception) {
                            }
                        }
                ) {
                    AsyncImage(
                        model = link.url,
                        contentDescription = "Image preview",
                        modifier = Modifier
                            .heightIn(max = inlineSettings.inlineImageMaxHeight.dp)
                            .widthIn(max = 400.dp)
                            .then(if (!isRevealed) Modifier.blur(20.dp) else Modifier),
                        contentScale = ContentScale.Fit
                    )
                    if (!isRevealed) {
                        Box(
                            modifier = Modifier.matchParentSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                LocalStrings.current.chatClickToReveal,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CompactMessageLayout(
    message: DisplayMessage.PrivMsg,
    showModActions: Boolean,
    showTimestamp: Boolean,
    timestampFormat: SettingsState.TimestampFormat,
    hasBadges: Boolean,
    showBadges: Boolean,
    isMod: Boolean,
    isOwnMessage: Boolean,
    currentUserId: String,
    emoteSize: SettingsState.EmoteSize,
    customModButtons: List<ModActionButton>,
    allModButtons: List<ModActionButton>,
    modButtonsVersion: Int = 0,
    showCustomModButtons: Boolean,
    showDefaultDeleteButton: Boolean,
    showDefaultTimeoutButton: Boolean,
    showDefaultBanButton: Boolean,
    chatFontSizeSp: Float,
    onUsernameClick: () -> Unit,
    onRightClickUsername: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onReply: () -> Unit,
    onCopyText: () -> Unit,
    onPin: () -> Unit,
    onTimeout: () -> Unit,
    onCustomTimeout: (Int) -> Unit,
    onBan: () -> Unit,
    onDelete: () -> Unit,
    actorCanModerate: Boolean,
    actorIsBroadcaster: Boolean,
    extraColors: io.rudione.chatone.presentation.theme.ChatoneExtraColors,
    mentionColor: Color,
    backgroundColor: Color,
    hasAccentBar: Boolean,
    s: AppStrings,
    userColorByLogin: Map<String, Color> = emptyMap()
) {
    var prefixWidthPx by remember { mutableStateOf(0f) }
    val dynamicTopPadding = (chatFontSizeSp * 0.067f).dp

    Column(modifier = Modifier.fillMaxWidth()) {
        val replyParentName = message.rawMessage?.replyParentDisplayName
        val replyParentBody = message.rawMessage?.replyParentMsgBody
        val replyParentLogin = message.rawMessage?.replyParentUserLogin
        if (replyParentName != null && replyParentBody != null && replyParentLogin != null) {
            val parentColor = userColorByLogin[replyParentLogin.lowercase()]
                ?: stableUserColor(replyParentLogin.lowercase())
                ?: MaterialTheme.colorScheme.onSurfaceVariant
            DisableSelection {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 2.dp, end = 4.dp, top = 1.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = buildAnnotatedString {
                            pushStringAnnotation("mention", "@$replyParentLogin")
                            withStyle(
                                SpanStyle(
                                    color = parentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append("@$replyParentName") }
                            pop()
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.6f
                                    )
                                )
                            ) { append(": $replyParentBody") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onMentionClick("@$replyParentLogin") }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            CompactMessageContentWithIndent(
                message = message,
                emoteSize = emoteSize,
                chatFontSizeSp = chatFontSizeSp,
                isMod = isMod,
                onUsernameClick = onUsernameClick,
                onRightClickUsername = onRightClickUsername,
                onMentionClick = onMentionClick,
                onReply = onReply,
                onCopyText = onCopyText,
                onPin = onPin,
                mentionColor = mentionColor,
                backgroundColor = backgroundColor,
                hasAccentBar = hasAccentBar,
                s = s,
                prefixWidthPx = prefixWidthPx,
                userColorByLogin = userColorByLogin
            )
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .onSizeChanged { prefixWidthPx = it.width.toFloat() }
                    .padding(top = dynamicTopPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showModActions) {
                    val canAct = canActOnUser(
                        actorIsBroadcaster = actorIsBroadcaster,
                        actorIsMod = actorCanModerate,
                        targetIsBroadcaster = message.isBroadcaster,
                        targetIsMod = message.isModerator,
                        targetIsVip = message.isVip,
                        targetIsSubscriber = message.isSubscriber,
                        actorIsGrandMod = false,
                        targetIsGrandMod = message.isGrandMod
                    )
                    val compactModOrderKey = "$modButtonsVersion:" + if (allModButtons.isNotEmpty())
                        allModButtons.joinToString("|") { "${it.id}:${it.sortOrder}:${it.enabled}" }
                    else
                        customModButtons.joinToString("|") { it.id }
                    key(compactModOrderKey) {
                        Row(
                            modifier = Modifier.padding(end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val orderedButtons =
                                if (allModButtons.isNotEmpty()) allModButtons.sortedBy { it.sortOrder } else ModActionButton.defaultOrderedList() + customModButtons
                            orderedButtons.forEach { btn ->
                                if (!btn.enabled) return@forEach
                                key(btn.id) {
                                    when (btn.id) {
                                        "default_delete" -> if (showDefaultDeleteButton && (canAct || isOwnMessage)) ModActionIconBtn(
                                            icon = Icons.Outlined.Delete,
                                            label = "Del",
                                            tint = extraColors.modDelete,
                                            onClick = onDelete
                                        )

                                        "default_timeout" -> if (showDefaultTimeoutButton && canAct && !isOwnMessage) ModActionIconBtn(
                                            icon = Icons.Outlined.Refresh,
                                            label = "10m",
                                            tint = extraColors.modTimeout,
                                            onClick = onTimeout,
                                            visible = false
                                        )

                                        "default_ban" -> if (showDefaultBanButton && canAct && !isOwnMessage) ModActionIconBtn(
                                            icon = Icons.Filled.Close,
                                            label = s.chatBanUser,
                                            tint = extraColors.modBan,
                                            onClick = onBan
                                        )

                                        else -> if (showCustomModButtons && canAct && !isOwnMessage) ModActionIconBtn(
                                            icon = Icons.Outlined.Refresh,
                                            label = btn.displayLabel,
                                            tint = extraColors.modTimeout,
                                            onClick = { onCustomTimeout(btn.durationSeconds) },
                                            visible = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (showTimestamp) {
                    Text(
                        formatTimestamp(message.timestamp, timestampFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp, end = 5.dp)
                    )
                }
                if (hasBadges && showBadges) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                    ) {
                        message.badges.forEach { badge ->
                            if (badge.imageUrl.isNotEmpty()) io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox(
                                tooltip = badge.tooltip.ifBlank { badge.id }) {
                                AsyncImage(
                                    model = badge.imageUrl,
                                    contentDescription = badge.tooltip,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        message.sevenTvBadge?.let { stvBadge ->
                            val badgeUrl = stvBadge.url2x.ifEmpty { stvBadge.url1x }
                            if (badgeUrl.isNotEmpty()) io.rudione.chatone.presentation.chat.components.LiquidGlassTooltipBox(
                                tooltip = stvBadge.tooltip.ifBlank { "7TV Badge" }) {
                                AsyncImage(
                                    model = badgeUrl,
                                    contentDescription = stvBadge.tooltip,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactMessageContentWithIndent(
    message: DisplayMessage.PrivMsg,
    emoteSize: SettingsState.EmoteSize,
    chatFontSizeSp: Float,
    isMod: Boolean,
    onUsernameClick: () -> Unit,
    onRightClickUsername: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onReply: () -> Unit,
    onCopyText: () -> Unit,
    onPin: () -> Unit,
    mentionColor: Color,
    backgroundColor: Color,
    hasAccentBar: Boolean,
    s: AppStrings,
    prefixWidthPx: Float,
    userColorByLogin: Map<String, Color> = emptyMap()
) {
    val emoteSizeSp = when (emoteSize) {
        SettingsState.EmoteSize.SMALL -> 20.sp
        SettingsState.EmoteSize.MEDIUM -> 28.sp
        SettingsState.EmoteSize.LARGE -> 36.sp
    }
    val userColor = parseColor(message.color) ?: MaterialTheme.colorScheme.primary
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val emoteKeyMap = mutableMapOf<String, GenericEmote>()
    var emoteCounter = 0
    val annotatedString = buildAnnotatedString {
        if (message.isDeleted) {
            pushStringAnnotation("username", message.userId)
            withStyle(
                SpanStyle(
                    color = userColor,
                    fontWeight = FontWeight.Bold
                )
            ) { append(message.displayName) }
            pop()
            append(": ")
            val originalText = message.tokens.joinToString("") {
                when (it) {
                    is MessageToken.Text -> it.text
                    is MessageToken.TwitchEmoteToken -> it.name
                    is MessageToken.ThirdPartyEmoteToken -> it.emote.code
                    is MessageToken.Link -> it.displayText
                    is MessageToken.Mention -> it.username
                }
            }
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
                    textDecoration = TextDecoration.LineThrough
                )
            ) {
                append(originalText.ifEmpty { "message deleted" })
            }
        } else {
            pushStringAnnotation("username", message.userId)
            if (message.isAction) {
                withStyle(
                    SpanStyle(
                        color = userColor,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold
                    )
                ) { append(message.displayName); append(" ") }
            } else {
                withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                    append(
                        message.displayName
                    )
                }
                append(": ")
            }
            pop()
            val messageColor = if (message.isAction) userColor else Color.Unspecified
            message.tokens.forEach { token ->
                when (token) {
                    is MessageToken.Text -> {
                        if (message.isAction) withStyle(
                            SpanStyle(
                                color = messageColor,
                                fontStyle = FontStyle.Italic
                            )
                        ) { append(token.text) } else append(token.text)
                    }

                    is MessageToken.TwitchEmoteToken -> {
                        val key = "emote_${emoteCounter++}"
                        appendInlineContent(key, token.name)
                        inlineContent[key] = InlineTextContent(
                            Placeholder(
                                emoteSizeSp,
                                emoteSizeSp,
                                PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            AnimatedEmoteImage(
                                url = token.url,
                                contentDescription = token.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    is MessageToken.ThirdPartyEmoteToken -> {
                        val key = "emote_${emoteCounter++}"
                        appendInlineContent(key, token.emote.code)
                        val (emoteW, emoteH) = computeEmoteDisplaySize(
                            token.emote.width,
                            token.emote.height,
                            emoteSizeSp
                        )
                        emoteKeyMap[key] = token.emote
                        inlineContent[key] = InlineTextContent(
                            Placeholder(
                                emoteW,
                                emoteH,
                                PlaceholderVerticalAlign.TextCenter
                            )
                        ) {
                            Box {
                                EmoteImageWithTooltip(
                                    emote = token.emote,
                                    modifier = Modifier.fillMaxSize(),
                                    onShowContextMenu = null
                                )
                                token.overlays.forEach { overlay ->
                                    EmoteImageWithTooltip(
                                        emote = overlay,
                                        modifier = Modifier.fillMaxSize(),
                                        onShowContextMenu = {})
                                }
                            }
                        }
                    }

                    is MessageToken.Link -> {
                        pushStringAnnotation("url", token.url)
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) { append(token.displayText) }
                        pop()
                    }

                    is MessageToken.Mention -> {
                        pushStringAnnotation("mention", token.username)
                        val mentionedLogin = token.username.removePrefix("@").lowercase()
                        val mentionColorReal =
                            userColorByLogin[mentionedLogin] ?: stableUserColor(mentionedLogin)
                        withStyle(
                            SpanStyle(
                                color = mentionColorReal,
                                fontWeight = FontWeight.Bold
                            )
                        ) { append(token.username) }
                        pop()
                    }
                }
            }
        }
    }
    var showContextMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var contextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val uriHandler = LocalUriHandler.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val inlineSettings = remember { SettingsViewModel.loadInitialState() }
    var hoveredUrl by remember { mutableStateOf<String?>(null) }
    var hoverOffset by remember { mutableStateOf(IntOffset.Zero) }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (message.isFirstMessage && !message.isDeleted) {
            DisableSelection {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(3.dp))
                            .background(FirstMessageColor.copy(alpha = 0.18f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) { }
                }
                Spacer(Modifier.height(1.dp))
            }
        }
        val density = LocalDensity.current
        val prefixIndent = with(density) { prefixWidthPx.toSp() }
        SelectionContainer {
            Text(
                text = annotatedString,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = when (chatFontSizeSp) {
                        13f -> 14.sp; 15f -> 16.sp; 17f -> 18.sp; else -> chatFontSizeSp.sp
                    },
                    lineHeight = when (chatFontSizeSp) {
                        13f -> 20.sp; 15f -> 22.sp; 17f -> 24.sp; else -> (chatFontSizeSp + 6).sp
                    },
                    textIndent = androidx.compose.ui.text.style.TextIndent(
                        firstLine = prefixIndent,
                        restLine = 0.sp
                    )
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
                    .pointerInput(annotatedString) {
                        detectTapGestures(
                            onTap = { offset ->
                                textLayoutResult?.let { layoutResult ->
                                    val charOffset = layoutResult.getOffsetForPosition(offset)
                                    annotatedString.getStringAnnotations(
                                        "url",
                                        charOffset,
                                        charOffset
                                    ).firstOrNull()?.let {
                                        try {
                                            openUrl(it.item, inlineSettings.linkOpenMode)
                                        } catch (_: Exception) {
                                        }; return@detectTapGestures
                                    }
                                    annotatedString.getStringAnnotations(
                                        "username",
                                        charOffset,
                                        charOffset
                                    ).firstOrNull()
                                        ?.let { onUsernameClick(); return@detectTapGestures }
                                    annotatedString.getStringAnnotations(
                                        "mention",
                                        charOffset,
                                        charOffset
                                    ).firstOrNull()
                                        ?.let { onMentionClick(it.item); return@detectTapGestures }
                                }
                            },
                            onLongPress = { offset ->
                                contextMenuOffset =
                                    IntOffset(offset.x.toInt(), offset.y.toInt()); showContextMenu =
                                true
                            }
                        )
                    }
                    .pointerInput(message.id, annotatedString) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pos = event.changes.firstOrNull()?.position ?: continue
                                when (event.type) {
                                    PointerEventType.Move, PointerEventType.Enter -> {
                                        textLayoutResult?.let { layout ->
                                            val charOffset = layout.getOffsetForPosition(pos)
                                            val urlAnn = annotatedString.getStringAnnotations(
                                                "url",
                                                charOffset,
                                                charOffset
                                            ).firstOrNull()
                                            hoveredUrl = urlAnn?.item
                                            hoverOffset = IntOffset(pos.x.toInt(), pos.y.toInt())
                                        }
                                    }

                                    PointerEventType.Exit -> hoveredUrl = null
                                    else -> {}
                                }
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                    textLayoutResult?.let { layout ->
                                        val charOffset = layout.getOffsetForPosition(pos)
                                        val onUsername = annotatedString.getStringAnnotations(
                                            "username",
                                            charOffset,
                                            charOffset
                                        ).isNotEmpty()
                                        if (onUsername) {
                                            onRightClickUsername(message.displayName)
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            contextMenuOffset =
                                                IntOffset(pos.x.toInt(), pos.y.toInt())
                                            showContextMenu = true
                                            event.changes.forEach { it.consume() }
                                        }
                                    } ?: run {
                                        contextMenuOffset = IntOffset(pos.x.toInt(), pos.y.toInt())
                                        showContextMenu = true
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    },
                onTextLayout = { textLayoutResult = it }
            )
        }
        hoveredUrl?.let { url ->
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val x = anchorBounds.left + hoverOffset.x + 16
                        val yBelow = anchorBounds.top + hoverOffset.y + 24
                        val yAbove = anchorBounds.top + hoverOffset.y - popupContentSize.height - 8
                        val finalY =
                            if (yBelow + popupContentSize.height > windowSize.height) yAbove else yBelow
                        val finalX =
                            if (x + popupContentSize.width > windowSize.width) windowSize.width - popupContentSize.width - 8 else x
                        return IntOffset(finalX, finalY)
                    }
                },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) { LinkHoverPopup(url = url) }
        }
        if (showContextMenu) {
            Popup(
                alignment = Alignment.TopStart,
                offset = contextMenuOffset + IntOffset(8, 8),
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                ),
                onDismissRequest = { showContextMenu = false }
            ) {
                LiquidGlassSurface(
                    modifier = Modifier.widthIn(min = 160.dp, max = 240.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    backgroundAlphaHigh = 0.94f,
                    backgroundAlphaLow = 0.85f,
                    borderAlphaHigh = 0f,
                    borderAlphaLow = 0f
                ) {
                    Column {
                        if (isMod) {
                            LiquidGlassDropdownItem(
                                text = "Pin",
                                icon = Icons.Filled.Place,
                                iconTint = MaterialTheme.colorScheme.primary,
                                onClick = { showContextMenu = false; onPin() })
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                        LiquidGlassDropdownItem(
                            text = s.chatReplyTo,
                            icon = Icons.AutoMirrored.Filled.Send,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { showContextMenu = false; onReply() })
                        LiquidGlassDropdownItem(
                            text = s.chatCopyMessage,
                            icon = Icons.Outlined.CopyAll,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                showContextMenu = false;
                                val rawText = message.tokens.joinToString("") {
                                    when (it) {
                                        is MessageToken.Text -> it.text; is MessageToken.TwitchEmoteToken -> it.name; is MessageToken.ThirdPartyEmoteToken -> it.emote.code; is MessageToken.Link -> it.displayText; is MessageToken.Mention -> it.username
                                    }
                                }; clipboardManager.setText(AnnotatedString(rawText))
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModActionIconBtn(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(4.dp))
            .background(tint.copy(alpha = 0.08f))
            .widthIn(min = 0.dp)
            .heightIn(min = 0.dp)
            .wrapContentWidth()
            .wrapContentHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
        ) {

            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier
                    .size(8.dp)
                    .wrapContentSize(),
                tint = tint
            )


            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 9.sp,
                    lineHeight = 9.sp
                ),
                color = tint,
                modifier = Modifier.wrapContentHeight()
            )
        }
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