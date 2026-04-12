package io.rudione.chatone.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.MentionEntry
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.ModActionButton
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.presentation.components.GlowSurface
import io.rudione.chatone.presentation.components.LiquidGlassDropdownItem
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.LocalWallpaper
import io.rudione.chatone.presentation.theme.WallpaperState
import io.rudione.chatone.util.EmoteImageWithTooltip
import io.rudione.chatone.util.GlobalKeyDispatcher
import io.rudione.chatone.util.MessageToken
import io.rudione.chatone.util.NotificationSoundPlayer
import io.rudione.chatone.util.handleHover
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private val FirstMessageColor = Color(0xFF7B2FBE)

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
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var showEmotePicker by remember { mutableStateOf(false) }
    var showModPanel by remember { mutableStateOf(false) }
    var profilePopupUserId by remember { mutableStateOf<String?>(null) }
    var profilePopupMessage by remember { mutableStateOf<DisplayMessage.PrivMsg?>(null) }
    var pendingModAction by remember { mutableStateOf<PendingModAction?>(null) }
    var isPausedByHotkey by remember { mutableStateOf(false) }
    var isHoveredOverChat by remember { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    var emoteTabIndex by remember { mutableStateOf(-1) }
    var mentionTabIndex by remember { mutableStateOf(-1) }
    var isHoveredOverEmoteTooltip by remember { mutableStateOf(false) }
    var hasNewMessagesWhilePaused by remember { mutableStateOf(false) }

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
                is ChatEffect.MentionDetected -> {
                    val mentionChannel = effect.channelLogin
                    val currentChannel = viewModel.state.value.channelLogin
                    val isActiveChannel = mentionChannel.equals(currentChannel, ignoreCase = true)

                    if (!isActiveChannel) {
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
                                    is io.rudione.chatone.util.MessageToken.Text -> token.text
                                    is io.rudione.chatone.util.MessageToken.TwitchEmoteToken -> token.name
                                    is io.rudione.chatone.util.MessageToken.ThirdPartyEmoteToken -> token.emote.code
                                    is io.rudione.chatone.util.MessageToken.Link -> token.displayText
                                    is io.rudione.chatone.util.MessageToken.Mention -> token.username
                                }
                            },
                            timestamp = mentionMsg.timestamp
                        )
                        onMentionReceived(entry)
                    }
                }
            }
        }
    }



    LaunchedEffect(Unit) {
        chatBoxFocusRequester.requestFocus()
    }











    val currentSettings by rememberUpdatedState(settingsState)
    val currentIsPausedByHotkey by rememberUpdatedState(isPausedByHotkey)
    val hotkeyHandler = remember<(androidx.compose.ui.input.key.KeyEvent) -> Boolean> {
        handler@{ event ->
            val hotkey = currentSettings.pauseHotkey
            if (hotkey.isBlank()) return@handler false
            val isHoldMode = currentSettings.pauseHotkeyMode ==
                    io.rudione.chatone.presentation.settings.PauseHotkeyMode.HOLD

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



    Box(modifier = modifier.fillMaxSize()
        .focusRequester(chatBoxFocusRequester)
        .focusable()
        .onPreviewKeyEvent { event -> hotkeyHandler(event) }) {

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

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
                    isMod = state.isMod,
                    modModeEnabled = state.modModeEnabled,
                    modPanelOpen = showModPanel,
                    pinnedMacros = settingsState.pinnedMacros,
                    onBack = onNavigateBack,
                    onToggleModMode = { viewModel.sendEvent(ChatEvent.OnToggleModMode) },
                    onOpenModPanel = { showModPanel = !showModPanel },
                    onExecuteMacro = { macro -> viewModel.sendEvent(ChatEvent.OnExecuteMacro(macro)) },
                    isCompact = !isWideScreen
                )
            }

            state.pinnedMessage?.let { pinned ->
                PinnedMessageBar(
                    message = pinned,
                    canUnpin = true,
                    onUnpin = { viewModel.sendEvent(ChatEvent.OnUnpinMessage) })
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                wallpaper.imageBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(wallpaper.blurRadius.dp)
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(wallpaper.dominantColor.copy(alpha = 0.4f))
                )

                Box(modifier = Modifier.fillMaxSize()) {
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
                                "Waiting for messages...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().handleHover(
                                onEnter = { if (settingsState.pauseOnHover) isHoveredOverChat = true },
                                onExit = { isHoveredOverChat = false }
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(items = state.messages, key = { it.id }) { message ->
                                when (message) {
                                    is DisplayMessage.PrivMsg -> PrivMsgItem(
                                        message = message,
                                        showModActions = state.modModeEnabled,
                                        timestampFormat = settingsState.timestampFormat,
                                        showBadges = settingsState.showBadges,
                                        isMod = state.isMod || message.isBroadcaster,
                                        emoteSize = settingsState.emoteSize,
                                        customModButtons = settingsState.customModButtons,
                                        chatFontSizeSp = when(settingsState.fontSize) {
                                            io.rudione.chatone.presentation.settings.SettingsState.FontSize.SMALL -> 12f
                                            io.rudione.chatone.presentation.settings.SettingsState.FontSize.MEDIUM -> 15f
                                            io.rudione.chatone.presentation.settings.SettingsState.FontSize.LARGE -> 18f
                                        },
                                        onUsernameClick = {
                                            profilePopupMessage = message
                                            profilePopupUserId = message.userId
                                        },
                                        onRightClickUsername = { displayName ->

                                            viewModel.sendEvent(ChatEvent.OnInsertMention(displayName))
                                        },
                                        onMentionClick = { mentionedUsername ->

                                            val mentionedMsg = state.messages
                                                .filterIsInstance<DisplayMessage.PrivMsg>()
                                                .lastOrNull { it.username.equals(mentionedUsername.removePrefix("@"), ignoreCase = true) }
                                            if (mentionedMsg != null) {
                                                profilePopupMessage = mentionedMsg
                                                profilePopupUserId = mentionedMsg.userId
                                            }
                                        },
                                        onReply = { viewModel.sendEvent(ChatEvent.OnReplyToMessage(message)) },
                                        onPin = { viewModel.sendEvent(ChatEvent.OnPinMessage(message.id)) },
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
                                            viewModel.sendEvent(ChatEvent.OnTimeoutUser(message.userId, seconds))
                                        },
                                        onBan = {
                                            if (settingsState.confirmModActions) {
                                                pendingModAction = PendingModAction.Ban(
                                                    message.userId,
                                                    message.displayName
                                                )
                                            } else {
                                                viewModel.sendEvent(ChatEvent.OnBanUser(message.userId))
                                            }
                                        },
                                        onDelete = { viewModel.sendEvent(ChatEvent.OnDeleteMessage(message.id)) }
                                    )
                                    is DisplayMessage.SystemMsg -> SystemMsgItem(message)
                                    is DisplayMessage.UserNoticeMsg -> UserNoticeMsgItem(message)
                                    is DisplayMessage.ModerationMsg -> ModerationMsgItem(message)
                                    is DisplayMessage.AutoModMsg -> AutoModMsgItem(
                                        message = message,
                                        onAllow = { viewModel.sendEvent(ChatEvent.OnAllowAutoModMessage(message.msgId)) },
                                        onDeny = { viewModel.sendEvent(ChatEvent.OnDenyAutoModMessage(message.msgId)) }
                                    )
                                }
                            }
                        }
                    }

                    if ((hasNewMessagesWhilePaused || !isAtBottom.value || isPausedByHotkey) && state.messages.isNotEmpty()) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isAutoScrolling = true
                                        listState.animateScrollToItem(state.messages.size - 1)
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
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                            }
                            if (unreadCount > 0) {
                                val badgeText = if (unreadCount > 99) "99+" else "$unreadCount"
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                        .widthIn(min = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showModPanel && state.isMod,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
            ) {
                ModerationPanel(
                    roomState = state.roomState, channelLogin = channelLogin, isMod = state.isMod,
                    pinnedMacros = settingsState.pinnedMacros,
                    onUpdateChatSettings = { settings -> viewModel.sendEvent(ChatEvent.OnUpdateChatSettings(settings)) },
                    onClearChat = { viewModel.sendEvent(ChatEvent.OnClearChat) },
                    onSendAnnouncement = { message, color -> viewModel.sendEvent(ChatEvent.OnSendAnnouncement(message, color)) },
                    onStartRaid = { targetLogin -> viewModel.sendEvent(ChatEvent.OnStartRaid(targetLogin)) },
                    onCancelRaid = { viewModel.sendEvent(ChatEvent.OnCancelRaid) },
                    onExecuteMacro = { macro -> viewModel.sendEvent(ChatEvent.OnExecuteMacro(macro)) },
                    onSendPinMessage = { msg -> viewModel.sendEvent(ChatEvent.OnSendMessageText(msg)) },
                    onClose = { showModPanel = false }
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
                onEmotePickerClick = { showEmotePicker = true },
                enabled = state.isConnected,
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
                }
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

    if (showEmotePicker) {
        val resolvedEmotes = emoteRepository.getResolvedEmotes(channelLogin)
        EmotePickerSheet(
            emotes = resolvedEmotes.all,
            closeOnMouseLeave = settingsState.closeEmotePickerOnMouseLeave,
            onEmoteSelected = { emote ->
                val current = state.messageInput
                val newInput = if (current.isEmpty() || current.endsWith(" ")) "$current${emote.code} " else "$current ${emote.code} "
                viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))

            },
            onEmojiSelected = { emoji ->
                val current = state.messageInput
                val newInput = if (current.isEmpty() || current.endsWith(" ")) "$current$emoji" else "$current $emoji"
                viewModel.sendEvent(ChatEvent.OnMessageInputChanged(newInput))
            },
            onDismiss = { showEmotePicker = false }
        )
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
                showModActions = state.modModeEnabled || state.isMod,
                currentUserIsBroadcaster = state.currentUserLogin.equals(channelLogin, ignoreCase = true),
                onTimeout = { seconds -> viewModel.sendEvent(ChatEvent.OnTimeoutUser(msg.userId, seconds)) },
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
                onDismiss = { profilePopupMessage = null; profilePopupUserId = null }
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
    val (title, text) = when (action) {
        is PendingModAction.Timeout -> {
            val d = when {
                action.duration < 60 -> "${action.duration}s"; action.duration < 3600 -> "${action.duration / 60}m"; action.duration < 86400 -> "${action.duration / 3600}h"; else -> "${action.duration / 86400}d"
            }
            "Timeout ${action.displayName}?" to "Timeout for $d"
        }

        is PendingModAction.Ban -> "Ban ${action.displayName}?" to "This will permanently ban the user from chat."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (action is PendingModAction.Ban) ChatoneTheme.extraColors.modBan else ChatoneTheme.extraColors.modTimeout)
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
private fun ChatTopBar(
    channelLogin: String,
    connectionStatus: String,
    isConnected: Boolean,
    roomState: RoomState,
    isMod: Boolean,
    modModeEnabled: Boolean,
    modPanelOpen: Boolean = false,
    pinnedMacros: List<io.rudione.chatone.domain.model.Macro> = emptyList(),
    onBack: () -> Unit,
    onToggleModMode: () -> Unit,
    onOpenModPanel: () -> Unit = {},
    onExecuteMacro: (io.rudione.chatone.domain.model.Macro) -> Unit = {},
    isCompact: Boolean = false
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompact) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text("#$channelLogin", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(if (isConnected) ChatoneTheme.extraColors.connected else MaterialTheme.colorScheme.error))
                        Spacer(Modifier.width(4.dp))
                        Text(connectionStatus, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isMod) {

                    if (pinnedMacros.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            pinnedMacros.forEach { macro ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { onExecuteMacro(macro) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(macro.icon, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    FilledIconToggleButton(
                        checked = modModeEnabled,
                        onCheckedChange = { onToggleModMode() },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            checkedContentColor = MaterialTheme.colorScheme.primary,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Mod Mode", modifier = Modifier.size(18.dp))
                    }
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
                        Icon(Icons.Outlined.Build, contentDescription = "Mod Panel", modifier = Modifier.size(18.dp))
                    }
                }
            }
            val roomChips = buildList {
                if (roomState.emoteOnly) add("Emote-only"); if (roomState.subsOnly) add("Sub-only")
                if (roomState.slowMode > 0) add("Slow: ${roomState.slowMode}s")
                if (roomState.followersOnly >= 0) add(if (roomState.followersOnly == 0) "Followers" else "Followers: ${roomState.followersOnly}m")
                if (roomState.r9k) add("R9K")
            }
            if (roomChips.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    roomChips.forEach { chip ->
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(4.dp)) {
                            Text(chip, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrivMsgItem(
    message: DisplayMessage.PrivMsg,
    showModActions: Boolean = false,
    timestampFormat: SettingsState.TimestampFormat = SettingsState.TimestampFormat.H24,
    showBadges: Boolean = true,
    isMod: Boolean = false,
    emoteSize: SettingsState.EmoteSize = SettingsState.EmoteSize.SMALL,
    customModButtons: List<ModActionButton> = emptyList(),
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
    modifier: Modifier = Modifier
) {
    val extraColors = ChatoneTheme.extraColors
    val mentionColor =
        if (message.highlightColor != null) Color(message.highlightColor) else MaterialTheme.colorScheme.primary
    val backgroundColor = when {
        message.isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
        message.isMention && message.highlightColor != null -> Color(message.highlightColor).copy(
            alpha = 0.12f
        )

        message.isMention -> extraColors.mentionHighlight
        message.isFirstMessage -> FirstMessageColor.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val accentBarModifier = when {
        message.isMention -> Modifier.drawWithContent {
            drawContent(); drawRect(
            color = mentionColor.copy(
                alpha = 0.85f
            ), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
        )
        }

        message.isFirstMessage -> Modifier.drawWithContent {
            drawContent(); drawRect(
            color = FirstMessageColor.copy(
                alpha = 0.75f
            ), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
        )
        }

        else -> Modifier
    }
    val hasAccentBar = message.isMention || message.isFirstMessage


    val imageLinks = remember(message.tokens) {
        message.tokens.filterIsInstance<MessageToken.Link>().filter { isImageUrl(it.url) }
    }

    val inlineSettings = remember { SettingsViewModel.loadInitialState() }

    Column(
        modifier = modifier.fillMaxWidth().background(backgroundColor).then(accentBarModifier)
            .padding(
                start = if (hasAccentBar) 7.dp else 8.dp,
                end = 8.dp,
                top = 3.dp,
                bottom = 3.dp
            )
    ) {
        val uriHandler = LocalUriHandler.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (showModActions) {

                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    ModActionIconBtn(icon = Icons.Outlined.Delete, label = "Del",
                        tint = extraColors.modDelete, onClick = onDelete)


                    customModButtons.filter { it.enabled }.take(8).forEach { btn ->
                        ModActionIconBtn(
                            icon = Icons.Outlined.Refresh,
                            label = btn.displayLabel,
                            tint = extraColors.modTimeout,
                            onClick = { onCustomTimeout(btn.durationSeconds) }
                        )
                    }


                    ModActionIconBtn(icon = Icons.Outlined.Refresh, label = "10m",
                        tint = extraColors.modTimeout, onClick = onTimeout)


                    ModActionIconBtn(icon = Icons.Filled.Close, label = "Ban",
                        tint = extraColors.modBan, onClick = onBan)
                }
            }

            if (timestampFormat != SettingsState.TimestampFormat.OFF) {
                Text(
                    formatTimestamp(message.timestamp, timestampFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .offset(y = 4.dp)
                )
            }

            if (showBadges) {


                val hasBadges = message.badges.any { it.imageUrl.isNotEmpty() } ||
                        (message.sevenTvBadge?.let { it.url2x.isNotEmpty() || it.url1x.isNotEmpty() } ?: false)
                if (hasBadges) {
                    Column(
                        modifier = Modifier.padding(top = 3.dp, end = 2.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            message.badges.forEach { badge ->
                                if (badge.imageUrl.isNotEmpty()) AsyncImage(
                                    model = badge.imageUrl,
                                    contentDescription = badge.id,
                                    modifier = Modifier.size(18.dp).padding(end = 2.dp)
                                )
                            }
                            message.sevenTvBadge?.let { stvBadge ->
                                val badgeUrl = stvBadge.url2x.ifEmpty { stvBadge.url1x }
                                if (badgeUrl.isNotEmpty()) AsyncImage(
                                    model = badgeUrl,
                                    contentDescription = stvBadge.tooltip,
                                    modifier = Modifier.size(18.dp).padding(end = 2.dp)
                                )
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
            val paintBrush = null
            val inlineContent = mutableMapOf<String, InlineTextContent>()
            var emoteCounter = 0

            val annotatedString = buildAnnotatedString {
                if (message.isDeleted) {
                    pushStringAnnotation("username", message.userId)
                    if (paintBrush != null) withStyle(
                        SpanStyle(
                            brush = paintBrush,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append(message.displayName) }
                    else withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
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
                    if (message.isFirstMessage) {
                        withStyle(
                            SpanStyle(
                                color = FirstMessageColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                background = FirstMessageColor.copy(alpha = 0.18f)
                            )
                        ) { append(" FIRST ") }
                        append(" ")
                    }
                    pushStringAnnotation("username", message.userId)
                    if (message.isAction) {
                        if (paintBrush != null) withStyle(
                            SpanStyle(
                                brush = paintBrush,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) { append(message.displayName); append(" ") }
                        else withStyle(
                            SpanStyle(
                                color = userColor,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) { append(message.displayName); append(" ") }
                    } else {
                        if (paintBrush != null) withStyle(
                            SpanStyle(
                                brush = paintBrush,
                                fontWeight = FontWeight.Bold
                            )
                        ) { append(message.displayName) }
                        else withStyle(
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
                                            onShowContextMenu = { showContextMenu = true })
                                        token.overlays.forEach { overlay ->
                                            EmoteImageWithTooltip(
                                                emote = overlay,
                                                modifier = Modifier.fillMaxSize()
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
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
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
                mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(
                    null
                )
            }


            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = annotatedString,
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
                        .pointerInput(annotatedString) {
                            detectTapGestures(
                                onTap = { offset ->
                                    textLayoutResult?.let { layoutResult ->
                                        val charOffset = layoutResult.getOffsetForPosition(offset)

                                        annotatedString.getStringAnnotations("url", charOffset, charOffset)
                                            .firstOrNull()?.let { annotation ->
                                                try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
                                                return@detectTapGestures
                                            }

                                        annotatedString.getStringAnnotations("username", charOffset, charOffset)
                                            .firstOrNull()?.let {
                                                onUsernameClick(); return@detectTapGestures
                                            }

                                        annotatedString.getStringAnnotations("mention", charOffset, charOffset)
                                            .firstOrNull()?.let { annotation ->
                                                onMentionClick(annotation.item)
                                                return@detectTapGestures
                                            }
                                    }
                                },
                                onLongPress = { offset ->
                                    contextMenuOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                                    showContextMenu = true
                                }
                            )
                        }
                        .pointerInput(message.displayName) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press &&
                                        event.buttons.isSecondaryPressed) {

                                        val pos = event.changes.firstOrNull()?.position ?: continue
                                        val layoutResult = textLayoutResult ?: continue
                                        val charOffset = layoutResult.getOffsetForPosition(pos)
                                        val onUsername = annotatedString.getStringAnnotations("username", charOffset, charOffset).isNotEmpty()
                                        if (onUsername) {

                                            onRightClickUsername(message.displayName)
                                        } else {

                                            contextMenuOffset = IntOffset(pos.x.toInt(), pos.y.toInt())
                                            showContextMenu = true
                                        }
                                    }
                                }
                            }
                        },
                    onTextLayout = { textLayoutResult = it }
                )
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
                                text = "Reply",
                                icon = Icons.AutoMirrored.Filled.Send,
                                iconTint = MaterialTheme.colorScheme.primary,
                                onClick = { showContextMenu = false; onReply() }
                            )
                            LiquidGlassDropdownItem(
                                text = "Copy Text",
                                icon = Icons.Outlined.Info,
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
                            else try { uriHandler.openUri(link.url) } catch (_: Exception) {}
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
                            modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Click to reveal", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun ModActionIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
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

            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
        ) {

            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier
                    .size(12.dp)
                    .wrapContentSize()
                ,
                tint = tint
            )

            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 6.sp,
                    lineHeight = 6.sp
                ),
                color = tint,
                modifier = Modifier.wrapContentHeight()
            )
        }
    }
}


@Composable
private fun SystemMsgItem(message: DisplayMessage.SystemMsg) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = ChatoneTheme.extraColors.systemMessage
        )
        Spacer(Modifier.width(4.dp))
        Text(
            message.text,
            style = MaterialTheme.typography.bodySmall,
            color = ChatoneTheme.extraColors.systemMessage
        )
    }
}

@Composable
private fun UserNoticeMsgItem(message: DisplayMessage.UserNoticeMsg) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            message.systemText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        message.innerMessage?.let { Spacer(modifier = Modifier.height(2.dp)); PrivMsgItem(message = it) }
    }
}

@Composable
private fun ModerationMsgItem(message: DisplayMessage.ModerationMsg) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color) = when (message.action) {
            DisplayMessage.ModerationMsg.ModerationAction.BAN -> Icons.Filled.Close to ChatoneTheme.extraColors.modBan
            DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT -> Icons.Outlined.Refresh to ChatoneTheme.extraColors.modTimeout
            DisplayMessage.ModerationMsg.ModerationAction.DELETE -> Icons.Outlined.Delete to ChatoneTheme.extraColors.modDelete
            DisplayMessage.ModerationMsg.ModerationAction.CLEAR -> Icons.Outlined.Clear to ChatoneTheme.extraColors.modDelete
            DisplayMessage.ModerationMsg.ModerationAction.UNBAN -> Icons.Outlined.CheckCircle to ChatoneTheme.extraColors.modUnban
        }
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            message.text,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.7f),
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun AutoModMsgItem(
    message: DisplayMessage.AutoModMsg,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val bgColor = when (message.status) {
        DisplayMessage.AutoModMsg.AutoModStatus.PENDING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        DisplayMessage.AutoModMsg.AutoModStatus.ALLOWED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.10f)
        DisplayMessage.AutoModMsg.AutoModStatus.DENIED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
    }
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        backgroundAlphaHigh = 0.85f,
        backgroundAlphaLow = 0.75f,
        borderAlphaHigh = 0.25f,
        borderAlphaLow = 0.10f
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(bgColor).padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "AutoMod",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
                Spacer(Modifier.weight(1f))
                if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.PENDING) {
                    TextButton(
                        onClick = onAllow,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Allow", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = onDeny,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Deny", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                } else {
                    Text(
                        if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.ALLOWED) "Allowed" else "Denied",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.ALLOWED)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            val nameColor = message.color?.let { hex ->
                try {
                    val v = hex.removePrefix("#").toLong(16)
                    Color(red = ((v shr 16) and 0xFF) / 255f, green = ((v shr 8) and 0xFF) / 255f, blue = (v and 0xFF) / 255f)
                } catch (_: Exception) { null }
            } ?: MaterialTheme.colorScheme.primary
            Text(
                text = "${message.displayName}: ${message.text}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (message.status == DisplayMessage.AutoModMsg.AutoModStatus.DENIED) 0.4f else 0.85f
                )
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendKeepText: () -> Unit = {},
    onHistoryUp: () -> Unit = {},
    onHistoryDown: () -> Unit = {},
    onEmotePickerClick: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    pauseHotkey: String = "",
    enabled: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },

    showEmoteCompletions: Boolean = false,
    showMentionCompletions: Boolean = false,
    emoteCount: Int = 0,
    mentionCount: Int = 0,
    emoteTabIndex: Int = -1,
    mentionTabIndex: Int = -1,
    onTabEmote: (Int) -> Unit = {},
    onTabMention: (Int) -> Unit = {},
    onConfirmEmoteTab: () -> Unit = {},
    onConfirmMentionTab: () -> Unit = {}
) {
    var tfv by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (tfv.text != value) {
            tfv = TextFieldValue(value, selection = TextRange(value.length))
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ChatoneTheme.extraColors.chatInputSurface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEmotePickerClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Text(
                    ":)", style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            OutlinedTextField(
                value = tfv,
                onValueChange = { newTfv ->
                    tfv = newTfv
                    onValueChange(newTfv.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        val isCtrl = event.isCtrlPressed || event.isMetaPressed

                        when {

                            event.key == Key.Tab && !isCtrl && !event.isAltPressed -> {
                                when {
                                    showEmoteCompletions && emoteCount > 0 -> {
                                        val next = if (emoteTabIndex < 0) 0
                                        else (emoteTabIndex + 1) % emoteCount
                                        onTabEmote(next)
                                    }
                                    showMentionCompletions && mentionCount > 0 -> {
                                        val next = if (mentionTabIndex < 0) 0
                                        else (mentionTabIndex + 1) % mentionCount
                                        onTabMention(next)
                                    }
                                }
                                true
                            }


                            event.key == Key.Enter && !isCtrl && !event.isShiftPressed -> {
                                when {
                                    showEmoteCompletions && emoteTabIndex >= 0 -> {
                                        onConfirmEmoteTab(); true
                                    }
                                    showMentionCompletions && mentionTabIndex >= 0 -> {
                                        onConfirmMentionTab(); true
                                    }
                                    else -> { onSend(); true }
                                }
                            }


                            isCtrl && event.key == Key.Enter -> { onSendKeepText(); true }


                            event.key == Key.DirectionUp && !isCtrl && !event.isShiftPressed && !event.isAltPressed
                                    && (tfv.text.isEmpty() || tfv.selection.start == 0) -> {
                                onHistoryUp(); true
                            }


                            event.key == Key.DirectionDown && !isCtrl && !event.isShiftPressed && !event.isAltPressed
                                    && (tfv.text.isEmpty() || tfv.selection.end == tfv.text.length) -> {
                                onHistoryDown(); true
                            }

                            event.key == Key.Backspace && event.isShiftPressed -> false

                            pauseHotkeyMatches(event, pauseHotkey) -> { onTogglePause(); true }

                            else -> false
                        }
                    },
                placeholder = {
                    Text(
                        "Send a message...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                enabled = enabled,
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.width(4.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun pauseHotkeyMatches(event: KeyEvent, hotkey: String): Boolean {
    if (hotkey.isBlank()) return false

    val parts = hotkey.lowercase().split("+").map { it.trim() }
    val mainKey = parts.last()

    val needsCtrl = parts.contains("ctrl")
    val needsAlt = parts.contains("alt")
    val needsShift = parts.contains("shift")

    val isCtrl = event.isCtrlPressed || event.isMetaPressed


    if (mainKey == "alt") {
        return event.isAltPressed && !event.isShiftPressed && isCtrl == needsCtrl
    }
    if (mainKey == "ctrl") {
        return isCtrl && !event.isAltPressed && !event.isShiftPressed
    }
    if (mainKey == "shift") {
        return event.isShiftPressed && !event.isAltPressed && !isCtrl
    }

    return keyNameMatches(event.key, mainKey) &&
            isCtrl == needsCtrl &&
            event.isAltPressed == needsAlt &&
            event.isShiftPressed == needsShift
}


private fun isImageUrl(url: String): Boolean {
    val lower = url.lowercase()

    if (lower.matches(Regex(""".*\.(png|jpg|jpeg|gif|webp|bmp|svg)(\?.*)?$"""))) return true

    if (lower.contains("i.imgur.com/")) return true
    if (lower.contains("imgur.com/") && !lower.contains("/a/") && !lower.contains("/gallery/")) return true
    if (lower.contains("kappa.lol/")) return true
    if (lower.contains("kappalol.com/")) return true
    if (lower.contains("cdn.7tv.app/")) return true
    if (lower.contains("cdn.betterttv.net/")) return true
    if (lower.contains("cdn.frankerfacez.com/")) return true
    if (lower.contains("pbs.twimg.com/")) return true
    if (lower.contains("media.discordapp.net/")) return true
    if (lower.contains("cdn.discordapp.com/attachments/")) return true
    if (lower.contains("i.redd.it/")) return true
    if (lower.contains("preview.redd.it/")) return true
    return false
}


private fun pauseHotkeyMatchesRelease(event: KeyEvent, hotkey: String): Boolean {
    if (hotkey.isBlank()) return false
    val mainKey = hotkey.lowercase().split("+").map { it.trim() }.last()

    return when (mainKey) {
        "alt" -> !event.isAltPressed
        "ctrl" -> !(event.isCtrlPressed || event.isMetaPressed)
        "shift" -> !event.isShiftPressed
        else -> keyNameMatches(event.key, mainKey)
    }
}


private fun keyNameMatches(key: Key, name: String): Boolean = when (name) {
    "space" -> key == Key.Spacebar
    "enter" -> key == Key.Enter
    "tab" -> key == Key.Tab
    "escape", "esc" -> key == Key.Escape
    "backspace" -> key == Key.Backspace
    "delete" -> key == Key.Delete
    "home" -> key == Key.MoveHome
    "end" -> key == Key.MoveEnd
    "pageup" -> key == Key.PageUp
    "pagedown" -> key == Key.PageDown
    "up" -> key == Key.DirectionUp
    "down" -> key == Key.DirectionDown
    "left" -> key == Key.DirectionLeft
    "right" -> key == Key.DirectionRight
    else -> if (name.length == 1) {
        val code = name[0].uppercaseChar().code.toLong()
        key.keyCode == code
    } else false
}

@Composable
private fun EmoteAutocompleteRow(
    emotes: List<io.rudione.chatone.domain.model.GenericEmote>,
    selectedIndex: Int = -1,
    onSelect: (io.rudione.chatone.domain.model.GenericEmote) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()


    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .pointerInput(Unit) { detectTapGestures(onPress = { onDismiss() }) }
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 8.dp, bottom = 64.dp)
                .heightIn(max = 120.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            backgroundAlphaHigh = 0.80f,
            backgroundAlphaLow = 0.65f,
            borderAlphaHigh = 0f,
            borderAlphaLow = 0f
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(emotes, key = { _, e -> e.id }) { idx, emote ->
                    val isSelected = idx == selectedIndex
                    Surface(
                        onClick = { onSelect(emote); onDismiss() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) Modifier.border(
                                    1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedEmoteImage(
                                url = emote.url2x.ifEmpty { emote.url1x },
                                contentDescription = emote.code,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = emote.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MentionAutocompleteRow(
    usernames: List<String>,
    selectedIndex: Int = -1,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
            .pointerInput(Unit) { detectTapGestures(onPress = { onDismiss() }) }
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 8.dp, bottom = 64.dp)
                .heightIn(max = 120.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            backgroundAlphaHigh = 0.80f,
            backgroundAlphaLow = 0.65f,
            borderAlphaHigh = 0f,
            borderAlphaLow = 0f
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(usernames, key = { _, u -> u }) { idx, username ->
                    val isSelected = idx == selectedIndex
                    Surface(
                        onClick = { onSelect(username); onDismiss() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) Modifier.border(
                                    1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PinnedMessageBar(
    message: DisplayMessage.PrivMsg,
    canUnpin: Boolean,
    onUnpin: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent, tonalElevation = 2.dp) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.08f),
                        surfaceColor.copy(alpha = 0.92f),
                        primaryColor.copy(alpha = 0.05f)
                    )
                )
            ).border(
                width = 0.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.1f),
                        primaryColor.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            ).padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = "Pinned",
                    modifier = Modifier.size(16.dp),
                    tint = primaryColor.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        message.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = parseColor(message.color) ?: primaryColor
                    )
                    Text(
                        message.tokens.joinToString("") { token ->
                            when (token) {
                                is MessageToken.Text -> token.text; is MessageToken.TwitchEmoteToken -> token.name; is MessageToken.ThirdPartyEmoteToken -> token.emote.code; is MessageToken.Link -> token.displayText; is MessageToken.Mention -> token.username
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canUnpin) IconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Unpin",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun ReplyBar(displayName: String, messagePreview: String, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(3.dp).height(28.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Replying to $displayName",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    messagePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Cancel reply",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


private fun createPaintBrush(paint: SevenTvCosmetics.Paint): Brush? {
    if (paint.stops.isEmpty()) {
        paint.color?.let { return Brush.linearGradient(listOf(argbToColor(it), argbToColor(it))) }
        return null
    }
    val colorStops = paint.stops.map { stop -> stop.at to argbToColor(stop.color) }.toTypedArray()
    return when (paint.function) {
        "LINEAR_GRADIENT" -> Brush.linearGradient(colorStops = colorStops)
        "RADIAL_GRADIENT" -> Brush.radialGradient(colorStops = colorStops)
        else -> if (colorStops.isNotEmpty()) Brush.linearGradient(colorStops = colorStops) else null
    }
}

private fun argbToColor(argb: Int): Color {
    val a = ((argb shr 24) and 0xFF) / 255f;
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f;
    val b = (argb and 0xFF) / 255f

    return Color(r, g, b, a)
}


private fun formatTimestamp(
    timestamp: Long,
    format: SettingsState.TimestampFormat = SettingsState.TimestampFormat.H24
): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return when (format) {
        SettingsState.TimestampFormat.H24 -> "${
            dateTime.hour.toString().padStart(2, '0')
        }:${dateTime.minute.toString().padStart(2, '0')}"

        SettingsState.TimestampFormat.H12 -> {
            val hour12 =
                if (dateTime.hour == 0) 12 else if (dateTime.hour > 12) dateTime.hour - 12 else dateTime.hour
            "$hour12:${
                dateTime.minute.toString().padStart(2, '0')
            } ${if (dateTime.hour < 12) "AM" else "PM"}"
        }

        SettingsState.TimestampFormat.OFF -> ""
    }
}

private fun parseColor(hexColor: String?): Color? {
    if (hexColor == null || !hexColor.startsWith("#")) return null
    return try {
        val c = hexColor.substring(1).toLong(16)
        Color(
            red = ((c shr 16) and 0xFF) / 255f,
            green = ((c shr 8) and 0xFF) / 255f,
            blue = (c and 0xFF) / 255f
        )
    } catch (_: Exception) {
        null
    }
}

private fun computeEmoteDisplaySize(
    origWidth: Int,
    origHeight: Int,
    baseHeightSp: TextUnit
): Pair<TextUnit, TextUnit> {
    if (origWidth <= 0 || origHeight <= 0) return baseHeightSp to baseHeightSp
    return (baseHeightSp.value * (origWidth.toFloat() / origHeight.toFloat()).coerceIn(
        0.5f,
        4.0f
    )).sp to baseHeightSp
}