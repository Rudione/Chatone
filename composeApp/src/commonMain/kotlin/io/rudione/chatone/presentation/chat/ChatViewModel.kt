package io.rudione.chatone.presentation.chat

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.remote.RecentMessagesClient
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.data.remote.emote.SevenTvEventApi
import io.rudione.chatone.data.repository.BadgeRepository
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.usecase.JoinChannelUseCase
import io.rudione.chatone.domain.usecase.SendMessageUseCase
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.MessageTokenizer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ChatState(
    val channelLogin: String = "",
    val channelId: String = "",
    val messages: List<DisplayMessage> = emptyList(),
    val messageInput: String = "",
    val isConnected: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val isLoading: Boolean = false,
    val isMod: Boolean = false,
    val modModeEnabled: Boolean = false,
    val roomState: RoomState = RoomState(),
    val currentUserId: String = "",
    val currentUserLogin: String = "",
    val currentDisplayName: String = "",
    val currentUserColor: String = "",
    val currentAccessToken: String = "",
    // Emote autocomplete
    val emoteCompletions: List<GenericEmote> = emptyList(),
    val showEmoteCompletions: Boolean = false,
    // Username @mention autocomplete
    val mentionCompletions: List<String> = emptyList(),
    val showMentionCompletions: Boolean = false,
    val mentionCount: Int = 0,
    // Reply
    val replyingTo: DisplayMessage.PrivMsg? = null,
    // Pinned message
    val pinnedMessage: DisplayMessage.PrivMsg? = null
) : UiState

data class RoomState(
    val emoteOnly: Boolean = false,
    val followersOnly: Int = -1,
    val slowMode: Int = 0,
    val subsOnly: Boolean = false,
    val r9k: Boolean = false
)

sealed class ChatEvent : UiEvent {
    data class OnInit(
        val channelLogin: String,
        val accessToken: String = "",
        val userId: String = "",
        val userLogin: String = "",
        val userDisplayName: String = ""
    ) : ChatEvent()
    data class OnMessageInputChanged(val input: String) : ChatEvent()
    object OnSendMessage : ChatEvent()
    object OnReconnect : ChatEvent()
    object OnToggleModMode : ChatEvent()
    data class OnTimeoutUser(val userId: String, val duration: Int) : ChatEvent()
    data class OnBanUser(val userId: String) : ChatEvent()
    data class OnUnbanUser(val userId: String) : ChatEvent()
    data class OnDeleteMessage(val messageId: String) : ChatEvent()
    data class OnWhisper(val username: String) : ChatEvent()
    data class OnModUser(val userId: String) : ChatEvent()
    data class OnUnmodUser(val userId: String) : ChatEvent()
    data class OnVipUser(val userId: String) : ChatEvent()
    data class OnUnvipUser(val userId: String) : ChatEvent()
    // Emote completion
    data class OnSelectEmoteCompletion(val emote: GenericEmote) : ChatEvent()
    object OnDismissCompletions : ChatEvent()
    data class OnSelectMentionCompletion(val username: String) : ChatEvent()
    object OnDismissMentionCompletions : ChatEvent()
    // Chat settings (mod panel)
    data class OnUpdateChatSettings(val settings: Map<String, Any>) : ChatEvent()
    object OnClearChat : ChatEvent()
    // Reply
    data class OnReplyToMessage(val message: DisplayMessage.PrivMsg) : ChatEvent()
    object OnCancelReply : ChatEvent()
    // Pin message
    data class OnPinMessage(val messageId: String) : ChatEvent()
    object OnUnpinMessage : ChatEvent()
    // Mod panel actions
    data class OnSendAnnouncement(val message: String, val color: String = "primary") : ChatEvent()
    data class OnStartRaid(val targetLogin: String) : ChatEvent()
    object OnCancelRaid : ChatEvent()
    data class OnSendShoutout(val targetUserId: String) : ChatEvent()
}

sealed class ChatEffect : UIEffect {
    data class ShowError(val message: String) : ChatEffect()
    object ScrollToBottom : ChatEffect()
    data class MentionDetected(val channelLogin: String) : ChatEffect()
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val emoteRepository: EmoteRepository,
    private val badgeRepository: BadgeRepository,
    private val recentMessagesClient: RecentMessagesClient,
    private val sendMessageUseCase: SendMessageUseCase,
    private val joinChannelUseCase: JoinChannelUseCase,
    private val apiClient: TwitchApiClient,
    private val sevenTvCosmeticsClient: SevenTvCosmeticsClient,
    private val sevenTvEventApi: SevenTvEventApi
) : BaseViewModel<ChatState, ChatEvent, ChatEffect>(ChatState()) {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_MESSAGES = 500
        private const val MAX_CACHED_CHANNELS = 10
    }

    // Per-channel message cache for preserving history when switching channels
    private val channelMessageCache = mutableMapOf<String, List<DisplayMessage>>()
    private val channelRoomStateCache = mutableMapOf<String, RoomState>()
    private val channelIdCache = mutableMapOf<String, String>()
    private val channelModCache = mutableMapOf<String, Boolean>()

    // Current user's badges (from USERSTATE IRC event)
    private var currentUserBadgeRaw: String = ""

    private data class HighlightMatch(val color: Long, val playSound: Boolean)

    private fun checkHighlightRules(messageText: String, currentUserLogin: String): HighlightMatch? {
        val settings = SettingsViewModel.loadInitialState()
        val rules = settings.highlightRules.filter { it.enabled }

        for (rule in rules) {
            val pattern = when (rule.id) {
                "username" -> currentUserLogin
                "whispers", "subscriptions", "first_message" -> continue // handled by other mechanisms
                else -> rule.pattern
            }
            if (pattern.isEmpty()) continue

            val matches = if (rule.isRegex) {
                try {
                    val options = if (rule.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                    Regex(pattern, options).containsMatchIn(messageText)
                } catch (_: Exception) { false }
            } else {
                messageText.contains(pattern, ignoreCase = !rule.caseSensitive)
            }

            if (matches) {
                return HighlightMatch(color = rule.color, playSound = rule.playSound)
            }
        }
        return null
    }

    init {
        subscribeToEvents()
        observeMessages()
        observeIrcEvents()
        observeConnectionState()
        observeEmoteSetUpdates()
    }

    override suspend fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnInit -> initChannel(event.channelLogin, event.accessToken, event.userId, event.userLogin, event.userDisplayName)
            is ChatEvent.OnMessageInputChanged -> updateMessageInput(event.input)
            ChatEvent.OnSendMessage -> sendMessage()
            ChatEvent.OnReconnect -> reconnect()
            ChatEvent.OnToggleModMode -> toggleModMode()
            is ChatEvent.OnTimeoutUser -> timeoutUser(event.userId, event.duration)
            is ChatEvent.OnBanUser -> banUser(event.userId)
            is ChatEvent.OnUnbanUser -> unbanUser(event.userId)
            is ChatEvent.OnDeleteMessage -> deleteMessage(event.messageId)
            is ChatEvent.OnWhisper -> whisperUser(event.username)
            is ChatEvent.OnModUser -> modUser(event.userId)
            is ChatEvent.OnUnmodUser -> unmodUser(event.userId)
            is ChatEvent.OnVipUser -> vipUser(event.userId)
            is ChatEvent.OnUnvipUser -> unvipUser(event.userId)
            is ChatEvent.OnSelectEmoteCompletion -> selectEmoteCompletion(event.emote)
            ChatEvent.OnDismissCompletions -> update { it.copy(showEmoteCompletions = false, emoteCompletions = emptyList()) }
            is ChatEvent.OnSelectMentionCompletion -> selectMentionCompletion(event.username)
            ChatEvent.OnDismissMentionCompletions -> update { it.copy(showMentionCompletions = false, mentionCompletions = emptyList()) }
            is ChatEvent.OnUpdateChatSettings -> updateChatSettings(event.settings)
            ChatEvent.OnClearChat -> clearChat()
            is ChatEvent.OnReplyToMessage -> update { it.copy(replyingTo = event.message, messageInput = "@${event.message.displayName} ") }
            ChatEvent.OnCancelReply -> update { it.copy(replyingTo = null) }
            is ChatEvent.OnPinMessage -> pinMessage(event.messageId)
            ChatEvent.OnUnpinMessage -> update { it.copy(pinnedMessage = null) }
            is ChatEvent.OnSendAnnouncement -> sendAnnouncement(event.message, event.color)
            is ChatEvent.OnStartRaid -> startRaid(event.targetLogin)
            ChatEvent.OnCancelRaid -> cancelRaid()
            is ChatEvent.OnSendShoutout -> sendShoutout(event.targetUserId)
        }
    }

    private fun initChannel(channelLogin: String, accessToken: String, userId: String, userLogin: String, userDisplayName: String) {
        val oldState = state.value
        val oldChannel = oldState.channelLogin

        // Save current channel's state to cache before switching
        if (oldChannel.isNotEmpty() && oldChannel != channelLogin) {
            channelMessageCache[oldChannel] = oldState.messages
            channelRoomStateCache[oldChannel] = oldState.roomState
            if (oldState.channelId.isNotEmpty()) channelIdCache[oldChannel] = oldState.channelId
            channelModCache[oldChannel] = oldState.isMod

            // Evict oldest cached channels if over limit
            if (channelMessageCache.size > MAX_CACHED_CHANNELS) {
                val oldest = channelMessageCache.keys.first()
                channelMessageCache.remove(oldest)
                channelRoomStateCache.remove(oldest)
                channelIdCache.remove(oldest)
                channelModCache.remove(oldest)
            }
        }

        // Restore cached state or start fresh for the new channel
        val cachedMessages = channelMessageCache[channelLogin] ?: emptyList()
        val cachedRoomState = channelRoomStateCache[channelLogin] ?: RoomState()
        val cachedChannelId = channelIdCache[channelLogin] ?: ""
        val cachedIsMod = channelModCache[channelLogin] ?: false

        update {
            it.copy(
                channelLogin = channelLogin,
                channelId = cachedChannelId,
                messages = cachedMessages,
                roomState = cachedRoomState,
                isMod = cachedIsMod,
                isLoading = cachedMessages.isEmpty(),
                modModeEnabled = false,
                emoteCompletions = emptyList(),
                showEmoteCompletions = false,
                mentionCompletions = emptyList(),
                showMentionCompletions = false,
                mentionCount = 0,
                replyingTo = null,
                pinnedMessage = null,
                currentAccessToken = if (accessToken.isNotEmpty()) accessToken else it.currentAccessToken,
                currentUserId = if (userId.isNotEmpty()) userId else it.currentUserId,
                currentUserLogin = if (userLogin.isNotEmpty()) userLogin else it.currentUserLogin,
                currentDisplayName = if (userDisplayName.isNotEmpty()) userDisplayName else it.currentDisplayName
            )
        }

        viewModelScope.launch {
            try {
                joinChannelUseCase(channelLogin)

                // Resolve channelId eagerly from Twitch API if not cached
                var resolvedChannelId = cachedChannelId
                if (resolvedChannelId.isEmpty()) {
                    val token = state.value.currentAccessToken
                    if (token.isNotEmpty()) {
                        try {
                            val result = apiClient.getUsers(token, logins = listOf(channelLogin))
                            if (result is io.rudione.chatone.util.Result.Success) {
                                result.data.data.firstOrNull()?.let { user ->
                                    resolvedChannelId = user.id
                                    update { it.copy(channelId = user.id) }
                                    channelIdCache[channelLogin.lowercase()] = user.id
                                    Napier.d("Resolved channelId for $channelLogin: ${user.id}", tag = TAG)
                                }
                            }
                        } catch (e: Exception) {
                            Napier.w("Failed to resolve channelId for $channelLogin: ${e.message}", tag = TAG)
                        }
                    }
                }

                // Load emotes, badges in parallel — channel emotes always load eagerly now
                launch {
                    emoteRepository.loadGlobalEmotes()
                    retokenizeMessages()
                }
                launch {
                    loadBadgesWithToken()
                    retokenizeMessages()
                }
                if (resolvedChannelId.isNotEmpty()) {
                    launch { loadChannelEmotesAndBadges(resolvedChannelId) }
                }
                // Load recent messages after a small delay to let emotes start loading
                launch { loadRecentMessages(channelLogin) }

                update { it.copy(isLoading = false) }
                Napier.d("Initialized channel: $channelLogin", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Failed to initialize channel: ${e.message}", e, tag = TAG)
                update { it.copy(isLoading = false) }
                sendEffect(ChatEffect.ShowError("Failed to join channel: ${e.message}"))
            }
        }
    }

    private suspend fun loadRecentMessages(channelLogin: String) {
        try {
            val recent = recentMessagesClient.getRecentMessages(channelLogin)
            if (recent.isNotEmpty()) {
                val displayMessages = recent.map { msg -> chatMessageToDisplay(msg) }
                update { state ->
                    if (state.channelLogin == channelLogin) {
                        state.copy(messages = displayMessages.takeLast(MAX_MESSAGES))
                    } else state
                }
                sendEffect(ChatEffect.ScrollToBottom)

                // Async fetch 7TV cosmetics for recent message authors
                val uniqueUserIds = recent.map { it.userId }.distinct().take(50)
                uniqueUserIds.forEach { userId ->
                    viewModelScope.launch {
                        try {
                            val cosmetics = sevenTvCosmeticsClient.getUserCosmetics(userId)
                            if (cosmetics != null && (cosmetics.paint != null || cosmetics.badge != null)) {
                                if (state.value.channelLogin == channelLogin) {
                                    update { state ->
                                        state.copy(messages = state.messages.map { dm ->
                                            if (dm is DisplayMessage.PrivMsg && dm.userId == userId && dm.sevenTvPaint == null && dm.sevenTvBadge == null) {
                                                dm.copy(sevenTvPaint = cosmetics.paint, sevenTvBadge = cosmetics.badge)
                                            } else dm
                                        })
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to load recent messages: ${e.message}", tag = TAG)
        }
    }

    private suspend fun loadBadgesWithToken() {
        val token = state.value.currentAccessToken
        if (token.isEmpty()) {
            Napier.w("No access token available for badge loading", tag = TAG)
            return
        }

        try {
            badgeRepository.loadGlobalBadges(token)
            Napier.d("Global badges loaded", tag = TAG)
        } catch (e: Exception) {
            Napier.e("Failed to load global badges: ${e.message}", tag = TAG)
        }
    }

    private fun loadChannelEmotesAndBadges(channelId: String) {
        val channelLogin = state.value.channelLogin
        viewModelScope.launch {
            // Channel emotes from 7TV/BTTV/FFZ
            launch {
                try {
                    emoteRepository.loadChannelEmotes(channelLogin, channelId)
                    Napier.d("Channel emotes loaded for $channelLogin", tag = TAG)
                    // Re-tokenize existing messages now that emotes are available
                    // Only retokenize if we're still on the same channel
                    if (state.value.channelLogin == channelLogin) {
                        retokenizeMessages()
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to load channel emotes: ${e.message}", tag = TAG)
                }
            }
            // Channel badges from Twitch
            launch {
                val token = state.value.currentAccessToken
                if (token.isNotEmpty()) {
                    try {
                        badgeRepository.loadChannelBadges(channelId, token)
                        Napier.d("Channel badges loaded for $channelId", tag = TAG)
                        retokenizeMessages()
                    } catch (e: Exception) {
                        Napier.e("Failed to load channel badges: ${e.message}", tag = TAG)
                    }
                }
            }
            // Subscribe to 7TV EventAPI for this channel's emote set
            launch {
                try {
                    // Use the actual 7TV emote set ID from the channel lookup
                    val emoteSetId = emoteRepository.getSevenTvEmoteSetId(channelLogin)
                    if (emoteSetId != null) {
                        sevenTvEventApi.subscribeToEmoteSet(emoteSetId)
                        Napier.d("Subscribed to 7TV emote set: $emoteSetId", tag = TAG)
                    } else {
                        Napier.w("No 7TV emote set ID for $channelLogin", tag = TAG)
                    }
                } catch (e: Exception) {
                    Napier.w("Failed to subscribe to 7TV events: ${e.message}", tag = TAG)
                }
            }
        }
    }

    private fun retokenizeMessages() {
        update { state ->
            val channelLogin = state.channelLogin
            if (channelLogin.isEmpty()) return@update state
            val channelEmotes = emoteRepository.getResolvedEmotes(channelLogin)
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg is DisplayMessage.PrivMsg && msg.rawMessage != null) {
                        val newTokens = MessageTokenizer.tokenize(msg.rawMessage, channelEmotes)
                        val newBadges = badgeRepository.resolveBadges(msg.rawMessage.badges, msg.rawMessage.channelId)
                        msg.copy(tokens = newTokens, badges = newBadges)
                    } else msg
                }
            )
        }
    }

    private fun chatMessageToDisplay(message: ChatMessage): DisplayMessage.PrivMsg {
        val channelEmotes = emoteRepository.getResolvedEmotes(message.channelName)
        val tokens = MessageTokenizer.tokenize(message, channelEmotes)
        val resolvedBadges = badgeRepository.resolveBadges(message.badges, message.channelId)

        // Check for 7TV paint
        val cosmetics = sevenTvCosmeticsClient.getCachedCosmetics(message.userId)

        return DisplayMessage.PrivMsg(
            id = message.id,
            timestamp = message.timestamp,
            channel = message.channelName,
            userId = message.userId,
            username = message.username,
            displayName = message.displayName,
            tokens = tokens,
            color = message.color,
            badges = resolvedBadges,
            isModerator = message.isModerator,
            isSubscriber = message.isSubscriber,
            isVip = message.isVip,
            isBroadcaster = message.isBroadcaster,
            isMention = message.isMention,
            isAction = message.isAction,
            rawMessage = message,
            sevenTvPaint = cosmetics?.paint,
            sevenTvBadge = cosmetics?.badge
        )
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.messages.collectLatest { message ->
                if (message.channelName.equals(state.value.channelLogin, ignoreCase = true)) {
                    // Capture channelId from first message and trigger emote/badge loading
                    if (state.value.channelId.isEmpty() && message.channelId.isNotEmpty()) {
                        update { it.copy(channelId = message.channelId) }
                        channelIdCache[message.channelName.lowercase()] = message.channelId
                        loadChannelEmotesAndBadges(message.channelId)
                    }

                    // Async fetch 7TV cosmetics for this user (non-blocking)
                    val msgId = message.id
                    launch {
                        try {
                            val cosmetics = sevenTvCosmeticsClient.getUserCosmetics(message.userId)
                            if (cosmetics != null && (cosmetics.paint != null || cosmetics.badge != null)) {
                                // Update the message now that cosmetics are cached
                                update { state ->
                                    state.copy(messages = state.messages.map { dm ->
                                        if (dm is DisplayMessage.PrivMsg && dm.id == msgId && dm.sevenTvPaint == null && dm.sevenTvBadge == null) {
                                            dm.copy(sevenTvPaint = cosmetics.paint, sevenTvBadge = cosmetics.badge)
                                        } else dm
                                    })
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    val displayMsg = chatMessageToDisplay(message)

                    // Check highlight rules
                    val s = state.value
                    val isOwnMessage = message.userId == s.currentUserId
                    val matchResult = if (!isOwnMessage) {
                        checkHighlightRules(message.message, s.currentUserLogin)
                    } else null

                    val finalMsg = if (matchResult != null) {
                        displayMsg.copy(isMention = true, highlightColor = matchResult.color)
                    } else displayMsg

                    update { state ->
                        // Guard: only add if still on same channel
                        if (!state.channelLogin.equals(message.channelName, ignoreCase = true)) return@update state
                        val newMessages = (state.messages + finalMsg).takeLast(MAX_MESSAGES)
                        state.copy(
                            messages = newMessages,
                            mentionCount = if (matchResult != null) state.mentionCount + 1 else state.mentionCount
                        )
                    }
                    sendEffect(ChatEffect.ScrollToBottom)

                    if (matchResult != null && matchResult.playSound) {
                        sendEffect(ChatEffect.MentionDetected(s.channelLogin))
                    }

                    chatRepository.saveMessage(message)
                } else {
                    // Background channel: still check for mentions to trigger sound/badge
                    val s = state.value
                    val isOwnMessage = message.userId == s.currentUserId
                    if (!isOwnMessage) {
                        val matchResult = checkHighlightRules(message.message, s.currentUserLogin)
                        if (matchResult != null && matchResult.playSound) {
                            sendEffect(ChatEffect.MentionDetected(message.channelName))
                        }
                    }
                }
            }
        }
    }

    private fun observeIrcEvents() {
        viewModelScope.launch {
            chatRepository.events.collectLatest { event ->
                val channelLogin = state.value.channelLogin
                when (event) {
                    is IrcEvent.Notice -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            val msg = DisplayMessage.SystemMsg(
                                id = "notice_${Clock.System.now().toEpochMilliseconds()}",
                                timestamp = Clock.System.now().toEpochMilliseconds(),
                                channel = event.channel,
                                text = event.message,
                                type = DisplayMessage.SystemMsg.SystemType.NOTICE
                            )
                            addMessage(msg)
                        }
                    }
                    is IrcEvent.UserNotice -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            val innerMsg = event.message?.let { chatMessageToDisplay(it) }
                            val msg = DisplayMessage.UserNoticeMsg(
                                id = "usernotice_${Clock.System.now().toEpochMilliseconds()}",
                                timestamp = Clock.System.now().toEpochMilliseconds(),
                                channel = event.channel,
                                systemText = event.systemMsg,
                                innerMessage = innerMsg,
                                noticeType = event.msgId ?: ""
                            )
                            addMessage(msg)
                        }
                    }
                    is IrcEvent.ClearChat -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            if (event.targetUser != null) {
                                val action = if (event.banDuration != null)
                                    DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT
                                else
                                    DisplayMessage.ModerationMsg.ModerationAction.BAN

                                val text = if (event.banDuration != null) {
                                    "${event.targetUser} was timed out for ${event.banDuration}s"
                                } else {
                                    "${event.targetUser} was banned"
                                }

                                val msg = DisplayMessage.ModerationMsg(
                                    id = "mod_${Clock.System.now().toEpochMilliseconds()}",
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel,
                                    text = text,
                                    action = action,
                                    targetUser = event.targetUser,
                                    duration = event.banDuration
                                )
                                addMessage(msg)

                                update { state ->
                                    state.copy(messages = state.messages.map { dm ->
                                        if (dm is DisplayMessage.PrivMsg && dm.username.equals(event.targetUser, ignoreCase = true)) {
                                            dm.copy(isDeleted = true)
                                        } else dm
                                    })
                                }
                            } else {
                                val msg = DisplayMessage.ModerationMsg(
                                    id = "clear_${Clock.System.now().toEpochMilliseconds()}",
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel,
                                    text = "Chat was cleared by a moderator",
                                    action = DisplayMessage.ModerationMsg.ModerationAction.CLEAR
                                )
                                addMessage(msg)
                            }
                        }
                    }
                    is IrcEvent.ClearMsg -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            update { state ->
                                state.copy(messages = state.messages.map { dm ->
                                    if (dm is DisplayMessage.PrivMsg && dm.id == event.targetMessageId) {
                                        dm.copy(isDeleted = true)
                                    } else dm
                                })
                            }
                        }
                    }
                    is IrcEvent.RoomState -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            // Capture channelId from room-id tag if available
                            val roomId = event.roomId
                            if (!roomId.isNullOrEmpty() && state.value.channelId.isEmpty()) {
                                update { it.copy(channelId = roomId) }
                                channelIdCache[channelLogin.lowercase()] = roomId
                                loadChannelEmotesAndBadges(roomId)
                            }
                            update { state ->
                                val newRoomState = RoomState(
                                    emoteOnly = event.emoteOnly ?: state.roomState.emoteOnly,
                                    followersOnly = event.followersOnly ?: state.roomState.followersOnly,
                                    slowMode = event.slowMode ?: state.roomState.slowMode,
                                    subsOnly = event.subsOnly ?: state.roomState.subsOnly,
                                    r9k = event.r9k ?: state.roomState.r9k
                                )
                                channelRoomStateCache[channelLogin.lowercase()] = newRoomState
                                state.copy(roomState = newRoomState)
                            }
                        }
                    }
                    is IrcEvent.UserState -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            channelModCache[channelLogin.lowercase()] = event.isMod
                            if (event.badges.isNotEmpty()) {
                                currentUserBadgeRaw = event.badges
                            }
                            update {
                                it.copy(
                                    isMod = event.isMod,
                                    currentUserColor = event.color ?: it.currentUserColor,
                                    currentDisplayName = if (event.displayName.isNotEmpty()) event.displayName else it.currentDisplayName
                                )
                            }
                        }
                    }
                    is IrcEvent.GlobalUserState -> {
                        update {
                            it.copy(
                                currentUserId = event.userId,
                                currentUserColor = event.color ?: it.currentUserColor,
                                currentDisplayName = if (event.displayName.isNotEmpty()) event.displayName else it.currentDisplayName,
                                currentUserLogin = if (event.displayName.isNotEmpty()) event.displayName.lowercase() else it.currentUserLogin
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeEmoteSetUpdates() {
        viewModelScope.launch {
            sevenTvEventApi.emoteSetUpdates.collect { event ->
                val text = when (event) {
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteAdded ->
                        "[7TV] ${event.actorName} added ${event.emoteName}"
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRemoved ->
                        "[7TV] ${event.actorName} removed ${event.emoteName}"
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRenamed ->
                        "[7TV] ${event.actorName} renamed ${event.oldName} to ${event.newName}"
                }
                val msg = DisplayMessage.SystemMsg(
                    id = "7tv_${Clock.System.now().toEpochMilliseconds()}",
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    channel = state.value.channelLogin,
                    text = text,
                    type = DisplayMessage.SystemMsg.SystemType.NOTICE
                )
                addMessage(msg)

                // Reload channel emotes after update
                val channelId = state.value.channelId
                if (channelId.isNotEmpty()) {
                    launch {
                        try {
                            emoteRepository.loadChannelEmotes(state.value.channelLogin, channelId)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun addMessage(msg: DisplayMessage) {
        update { state ->
            val newMessages = (state.messages + msg).takeLast(MAX_MESSAGES)
            state.copy(messages = newMessages)
        }
        sendEffect(ChatEffect.ScrollToBottom)
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            chatRepository.connectionState.collectLatest { connectionState ->
                val (isConnected, status) = when (connectionState) {
                    is TwitchIrcClient.ConnectionState.Connected -> true to "Connected"
                    is TwitchIrcClient.ConnectionState.Connecting -> false to "Connecting..."
                    is TwitchIrcClient.ConnectionState.Disconnected -> false to "Disconnected"
                    is TwitchIrcClient.ConnectionState.Error -> false to "Error: ${connectionState.message}"
                }
                update { it.copy(isConnected = isConnected, connectionStatus = status) }
            }
        }
    }

    // ── Emote Autocomplete ──────────────────────────────────────────

    private fun updateMessageInput(input: String) {
        update { it.copy(messageInput = input) }

        val lastWord = input.trimEnd().split(" ").lastOrNull() ?: ""

        // Check for @mention completion trigger
        if (lastWord.startsWith("@") && lastWord.length >= 2) {
            val query = lastWord.removePrefix("@")
            val recentUsers = state.value.messages
                .filterIsInstance<DisplayMessage.PrivMsg>()
                .takeLast(1000)
                .map { it.displayName }
                .distinct()
                .filter { it.contains(query, ignoreCase = true) }
                .sortedBy { it.length }
                .take(8)

            if (recentUsers.isNotEmpty()) {
                update { it.copy(
                    mentionCompletions = recentUsers,
                    showMentionCompletions = true,
                    showEmoteCompletions = false,
                    emoteCompletions = emptyList()
                ) }
            } else {
                update { it.copy(showMentionCompletions = false, mentionCompletions = emptyList()) }
            }
            return
        }

        // Clear mention completions if not @
        update { it.copy(showMentionCompletions = false, mentionCompletions = emptyList()) }

        // Check for emote completion trigger
        if (lastWord.length >= 2 && !lastWord.startsWith("/")) {
            val channelEmotes = emoteRepository.getResolvedEmotes(state.value.channelLogin)
            val matches = channelEmotes.allByCode.entries
                .filter { it.key.contains(lastWord, ignoreCase = true) }
                .sortedBy { it.key.length }
                .take(8)
                .map { it.value }

            if (matches.isNotEmpty()) {
                update { it.copy(emoteCompletions = matches, showEmoteCompletions = true) }
            } else {
                update { it.copy(showEmoteCompletions = false, emoteCompletions = emptyList()) }
            }
        } else {
            update { it.copy(showEmoteCompletions = false, emoteCompletions = emptyList()) }
        }
    }

    private fun selectMentionCompletion(username: String) {
        val current = state.value.messageInput
        val words = current.split(" ").toMutableList()
        if (words.isNotEmpty()) {
            words[words.size - 1] = "@$username"
        }
        val newInput = words.joinToString(" ") + " "
        update { it.copy(messageInput = newInput, showMentionCompletions = false, mentionCompletions = emptyList()) }
    }

    private fun selectEmoteCompletion(emote: GenericEmote) {
        val current = state.value.messageInput
        val words = current.split(" ").toMutableList()
        if (words.isNotEmpty()) {
            words[words.size - 1] = emote.code
        }
        val newInput = words.joinToString(" ") + " "
        update { it.copy(messageInput = newInput, showEmoteCompletions = false, emoteCompletions = emptyList()) }
    }

    // ── Actions ─────────────────────────────────────────────────────

    private fun sendMessage() {
        val s = state.value
        val message = s.messageInput.trim()
        val channelLogin = s.channelLogin

        if (message.isEmpty() || channelLogin.isEmpty()) return
        if (!s.isConnected) {
            sendEffect(ChatEffect.ShowError("Not connected to chat"))
            return
        }

        viewModelScope.launch {
            try {
                // Handle /w whisper command via API
                if (message.startsWith("/w ")) {
                    val parts = message.removePrefix("/w ").split(" ", limit = 2)
                    if (parts.size == 2) {
                        val targetUser = parts[0]
                        val whisperMsg = parts[1]
                        // Need target userId — look up from recent messages
                        val targetMsg = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
                            .firstOrNull { it.username.equals(targetUser, ignoreCase = true) }
                        if (targetMsg != null && s.currentAccessToken.isNotEmpty()) {
                            val result = apiClient.sendWhisper(
                                accessToken = s.currentAccessToken,
                                fromUserId = s.currentUserId,
                                toUserId = targetMsg.userId,
                                message = whisperMsg
                            )
                            if (result.isError) {
                                sendEffect(ChatEffect.ShowError("Failed to send whisper"))
                            }
                        } else {
                            // Fall back to IRC command
                            sendMessageUseCase(channelLogin, message)
                        }
                        update { it.copy(messageInput = "") }
                        return@launch
                    }
                }

                sendMessageUseCase(channelLogin, message)

                // Local echo — show own message immediately (Twitch IRC doesn't echo back)
                val now = Clock.System.now().toEpochMilliseconds()
                // Parse own badges from cached USERSTATE
                val ownBadges = if (currentUserBadgeRaw.isNotEmpty()) {
                    currentUserBadgeRaw.split(",").mapNotNull { pair ->
                        val parts = pair.split("/", limit = 2)
                        if (parts.size == 2) Badge(id = parts[0], version = parts[1], imageUrl = "")
                        else null
                    }
                } else emptyList()

                val rawMsg = ChatMessage(
                    id = "local_$now",
                    channelId = s.channelId,
                    channelName = channelLogin,
                    userId = s.currentUserId,
                    username = s.currentUserLogin,
                    displayName = s.currentDisplayName.ifEmpty { s.currentUserLogin },
                    message = message,
                    timestamp = now,
                    color = s.currentUserColor.ifEmpty { "#9146FF" },
                    badges = ownBadges,
                    isModerator = s.isMod
                )
                val displayMsg = chatMessageToDisplay(rawMsg)

                update { state ->
                    val newMessages = (state.messages + displayMsg).takeLast(MAX_MESSAGES)
                    state.copy(
                        messageInput = "",
                        showEmoteCompletions = false,
                        emoteCompletions = emptyList(),
                        messages = newMessages,
                        replyingTo = null
                    )
                }
                sendEffect(ChatEffect.ScrollToBottom)
            } catch (e: Exception) {
                Napier.e("Failed to send message: ${e.message}", e, tag = TAG)
                sendEffect(ChatEffect.ShowError("Failed to send message: ${e.message}"))
            }
        }
    }

    private fun reconnect() {
        viewModelScope.launch {
            try {
                val channelLogin = state.value.channelLogin
                if (channelLogin.isNotEmpty()) {
                    joinChannelUseCase(channelLogin)
                }
            } catch (e: Exception) {
                Napier.e("Failed to reconnect: ${e.message}", e, tag = TAG)
                sendEffect(ChatEffect.ShowError("Failed to reconnect: ${e.message}"))
            }
        }
    }

    private fun toggleModMode() {
        update { it.copy(modModeEnabled = !it.modModeEnabled) }
    }

    private fun timeoutUser(userId: String, duration: Int) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        viewModelScope.launch {
            val result = apiClient.banUser(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                moderatorId = s.currentUserId,
                userId = userId,
                duration = duration
            )
            if (result.isError) {
                sendEffect(ChatEffect.ShowError("Failed to timeout user"))
            }
        }
    }

    private fun banUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        viewModelScope.launch {
            val result = apiClient.banUser(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                moderatorId = s.currentUserId,
                userId = userId
            )
            if (result.isError) {
                sendEffect(ChatEffect.ShowError("Failed to ban user"))
            }
        }
    }

    private fun deleteMessage(messageId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        viewModelScope.launch {
            val result = apiClient.deleteMessage(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                moderatorId = s.currentUserId,
                messageId = messageId
            )
            if (result.isError) {
                sendEffect(ChatEffect.ShowError("Failed to delete message"))
            }
        }
    }

    private fun unbanUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        viewModelScope.launch {
            val result = apiClient.unbanUser(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                moderatorId = s.currentUserId,
                userId = userId
            )
            if (result.isError) {
                sendEffect(ChatEffect.ShowError("Failed to unban user"))
            }
        }
    }

    private fun modUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.addModerator(s.currentAccessToken, s.channelId, userId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to mod user"))
        }
    }

    private fun unmodUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.removeModerator(s.currentAccessToken, s.channelId, userId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to unmod user"))
        }
    }

    private fun vipUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.addVip(s.currentAccessToken, s.channelId, userId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to VIP user"))
        }
    }

    private fun unvipUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.removeVip(s.currentAccessToken, s.channelId, userId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to un-VIP user"))
        }
    }

    private fun whisperUser(username: String) {
        // Prefill message input with whisper command
        update { it.copy(messageInput = "/w $username ") }
    }

    private fun updateChatSettings(settings: Map<String, Any>) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        viewModelScope.launch {
            val result = apiClient.updateChatSettings(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                moderatorId = s.currentUserId,
                settings = settings
            )
            if (result.isError) {
                sendEffect(ChatEffect.ShowError("Failed to update chat settings"))
            }
        }
    }

    private fun pinMessage(messageId: String) {
        val msg = state.value.messages.filterIsInstance<DisplayMessage.PrivMsg>().firstOrNull { it.id == messageId }
        if (msg != null) {
            update { it.copy(pinnedMessage = msg) }
        }
    }

    private fun clearChat() {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        viewModelScope.launch {
            val result = apiClient.clearChat(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                moderatorId = s.currentUserId
            )
            if (result.isError) {
                sendEffect(ChatEffect.ShowError("Failed to clear chat"))
            }
        }
    }

    private fun sendAnnouncement(message: String, color: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.sendAnnouncement(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                moderatorId = s.currentUserId,
                message = message,
                color = color
            )
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to send announcement"))
        }
    }

    private fun startRaid(targetLogin: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            // Resolve target login to user ID
            val usersResult = apiClient.getUsers(s.currentAccessToken, logins = listOf(targetLogin))
            if (usersResult is io.rudione.chatone.util.Result.Success) {
                val targetId = usersResult.data.data.firstOrNull()?.id
                if (targetId != null) {
                    val result = apiClient.startRaid(s.currentAccessToken, s.channelId, targetId)
                    if (result.isError) sendEffect(ChatEffect.ShowError("Failed to start raid"))
                } else {
                    sendEffect(ChatEffect.ShowError("User not found: $targetLogin"))
                }
            } else {
                sendEffect(ChatEffect.ShowError("Failed to resolve user for raid"))
            }
        }
    }

    private fun cancelRaid() {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.cancelRaid(s.currentAccessToken, s.channelId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to cancel raid"))
        }
    }

    private fun sendShoutout(targetUserId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.sendShoutout(
                accessToken = s.currentAccessToken,
                fromBroadcasterId = s.channelId,
                toBroadcasterId = targetUserId,
                moderatorId = s.currentUserId
            )
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to send shoutout"))
        }
    }
}
