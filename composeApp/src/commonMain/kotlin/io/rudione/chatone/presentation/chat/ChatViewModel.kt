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
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.domain.usecase.JoinChannelUseCase
import io.rudione.chatone.domain.usecase.SendMessageUseCase
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.MessageTokenizer
import io.rudione.chatone.util.NotificationSoundPlayer
import kotlinx.coroutines.delay
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
    val pinnedMessage: DisplayMessage.PrivMsg? = null,
    // Message history (own sent messages for arrow up/down navigation)
    val sentMessageHistory: List<String> = emptyList(),
    val historyIndex: Int = -1  // -1 = not navigating
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
    // Ctrl+Enter — sends but keeps text in input field
    object OnSendMessageKeepText : ChatEvent()
    // Arrow up/down in input — navigate sent message history
    object OnHistoryUp : ChatEvent()
    object OnHistoryDown : ChatEvent()
    object OnReconnect : ChatEvent()
    object OnToggleModMode : ChatEvent()
    data class OnTimeoutUser(val userId: String, val duration: Int) : ChatEvent()
    data class OnBanUser(val userId: String) : ChatEvent()
    data class OnUnbanUser(val userId: String) : ChatEvent()
    data class OnDeleteMessage(val messageId: String) : ChatEvent()
    data class OnWhisper(val username: String) : ChatEvent()
    data class OnInsertMention(val displayName: String) : ChatEvent()
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
    data class OnSendMessageText(val text: String) : ChatEvent()
    data class OnExecuteMacro(val macro: Macro) : ChatEvent()
    data class OnAllowAutoModMessage(val msgId: String) : ChatEvent()
    data class OnDenyAutoModMessage(val msgId: String) : ChatEvent()
}

sealed class ChatEffect : UIEffect {
    data class ShowError(val message: String) : ChatEffect()
    object ScrollToBottom : ChatEffect()
    data class MentionDetected(val channelLogin: String, val message: DisplayMessage.PrivMsg? = null) : ChatEffect()
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
        private const val MAX_CACHED_CHANNELS = 10
        private const val MAX_HISTORY = 50
    }

    /** Read scrollbackLimit from Settings; falls back to 500 */
    private val maxMessages: Int
        get() = SettingsViewModel.loadInitialState().scrollbackLimit.coerceIn(100, 5000)

    private val channelMessageCache = mutableMapOf<String, List<DisplayMessage>>()
    private val channelRoomStateCache = mutableMapOf<String, RoomState>()
    private val channelIdCache = mutableMapOf<String, String>()
    private val channelModCache = mutableMapOf<String, Boolean>()
    private var currentUserBadgeRaw: String = ""

    private data class HighlightMatch(val color: Long, val playSound: Boolean)

    private suspend fun checkHighlightRules(messageText: String, currentUserLogin: String): HighlightMatch? {
        val settings = SettingsViewModel.loadInitialState()
        val rules = settings.highlightRules.filter { it.enabled }

        // Resolve effective login — also use displayName (Twitch uses it in @mentions)
        val stateLogin = state.value.currentUserLogin
        val stateDisplay = state.value.currentDisplayName
        val effectiveLogin = when {
            currentUserLogin.isNotEmpty() -> currentUserLogin.lowercase()
            stateLogin.isNotEmpty() -> stateLogin.lowercase()
            stateDisplay.isNotEmpty() -> stateDisplay.lowercase()
            else -> ""
        }
        // Also collect display name variations for matching
        val effectiveDisplay = when {
            stateDisplay.isNotEmpty() -> stateDisplay.lowercase()
            stateLogin.isNotEmpty() -> stateLogin.lowercase()
            currentUserLogin.isNotEmpty() -> currentUserLogin.lowercase()
            else -> ""
        }

        // Always check @mention in text: try both login and display name
        if (effectiveLogin.isNotEmpty() || effectiveDisplay.isNotEmpty()) {
            val loginMatch = effectiveLogin.isNotEmpty() && messageText.contains("@$effectiveLogin", ignoreCase = true)
            val displayMatch = effectiveDisplay.isNotEmpty() && effectiveDisplay != effectiveLogin &&
                    messageText.contains("@$effectiveDisplay", ignoreCase = true)
            if (loginMatch || displayMatch) {
                return HighlightMatch(
                    color = rules.firstOrNull { it.id == "username" }?.color ?: 0xFFFF6B6B,
                    playSound = settings.mentionSoundEnabled
                )
            }
        }

        // If no login resolved, skip rule-based check to avoid false positives
        if (effectiveLogin.isEmpty() && effectiveDisplay.isEmpty()) return null

        for (rule in rules) {
            val pattern = when (rule.id) {
                "username" -> effectiveLogin
                "whispers", "subscriptions", "first_message" -> continue
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
                return HighlightMatch(
                    color = rule.color,
                    playSound = rule.playSound && settings.mentionSoundEnabled
                )
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
            ChatEvent.OnSendMessage -> sendMessage(keepText = false)
            ChatEvent.OnSendMessageKeepText -> sendMessage(keepText = true)
            ChatEvent.OnHistoryUp -> navigateHistory(up = true)
            ChatEvent.OnHistoryDown -> navigateHistory(up = false)
            ChatEvent.OnReconnect -> reconnect()
            ChatEvent.OnToggleModMode -> toggleModMode()
            is ChatEvent.OnTimeoutUser -> timeoutUser(event.userId, event.duration)
            is ChatEvent.OnBanUser -> banUser(event.userId)
            is ChatEvent.OnUnbanUser -> unbanUser(event.userId)
            is ChatEvent.OnDeleteMessage -> deleteMessage(event.messageId)
            is ChatEvent.OnWhisper -> whisperUser(event.username)
            is ChatEvent.OnInsertMention -> insertMention(event.displayName)
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
            is ChatEvent.OnSendMessageText -> sendRawMessage(event.text)
            is ChatEvent.OnExecuteMacro -> executeMacro(event.macro)
            is ChatEvent.OnAllowAutoModMessage -> handleAutoMod(event.msgId, "ALLOW")
            is ChatEvent.OnDenyAutoModMessage -> handleAutoMod(event.msgId, "DENY")
        }
    }

    private fun handleAutoMod(msgId: String, action: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.currentUserId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.manageAutoModMessage(s.currentAccessToken, s.currentUserId, msgId, action)
            val newStatus = if (action == "ALLOW") DisplayMessage.AutoModMsg.AutoModStatus.ALLOWED else DisplayMessage.AutoModMsg.AutoModStatus.DENIED
            update { state ->
                state.copy(messages = state.messages.map { dm ->
                    if (dm is DisplayMessage.AutoModMsg && dm.msgId == msgId) dm.copy(status = newStatus) else dm
                })
            }
            if (result is io.rudione.chatone.util.Result.Error) {
                sendEffect(ChatEffect.ShowError("AutoMod action failed: ${result.exception.message}"))
            }
        }
    }

    // ── Message history navigation (Arrow Up/Down) ──────────────────

    private fun navigateHistory(up: Boolean) {
        val s = state.value
        val history = s.sentMessageHistory
        if (history.isEmpty()) return

        val newIndex = if (up) {
            if (s.historyIndex == -1) 0
            else (s.historyIndex + 1).coerceAtMost(history.lastIndex)
        } else {
            if (s.historyIndex <= 0) {
                update { it.copy(historyIndex = -1, messageInput = "") }
                return
            }
            s.historyIndex - 1
        }

        update { it.copy(historyIndex = newIndex, messageInput = history[newIndex]) }
    }

    private fun sendRawMessage(text: String) {
        val s = state.value
        if (text.isBlank() || s.channelLogin.isEmpty()) return
        viewModelScope.launch {
            try {
                // /pin is not supported in Twitch IRC — send as announcement instead
                if (text.startsWith("/pin ", ignoreCase = true)) {
                    val pinMsg = text.removePrefix("/pin ").removePrefix("/PIN ").trim()
                    if (pinMsg.isNotBlank() && s.currentAccessToken.isNotEmpty() && s.channelId.isNotEmpty()) {
                        apiClient.sendAnnouncement(s.currentAccessToken, s.channelId, s.currentUserId, "📌 $pinMsg", "primary")
                    }
                    return@launch
                }
                sendMessageUseCase(s.channelLogin, text)
            } catch (e: Exception) {
                sendEffect(ChatEffect.ShowError("Failed to send: ${e.message}"))
            }
        }
    }

    private fun initChannel(channelLogin: String, accessToken: String, userId: String, userLogin: String, userDisplayName: String) {
        val oldState = state.value
        val oldChannel = oldState.channelLogin

        if (oldChannel.isNotEmpty() && oldChannel != channelLogin) {
            channelMessageCache[oldChannel] = oldState.messages
            channelRoomStateCache[oldChannel] = oldState.roomState
            if (oldState.channelId.isNotEmpty()) channelIdCache[oldChannel] = oldState.channelId
            channelModCache[oldChannel] = oldState.isMod
            if (channelMessageCache.size > MAX_CACHED_CHANNELS) {
                val oldest = channelMessageCache.keys.first()
                channelMessageCache.remove(oldest)
                channelRoomStateCache.remove(oldest)
                channelIdCache.remove(oldest)
                channelModCache.remove(oldest)
            }
        }

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
                currentDisplayName = if (userDisplayName.isNotEmpty()) userDisplayName else it.currentDisplayName,
                sentMessageHistory = emptyList(),
                historyIndex = -1
            )
        }

        viewModelScope.launch {
            try {
                joinChannelUseCase(channelLogin)
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
                                }
                            }
                        } catch (e: Exception) {
                            Napier.w("Failed to resolve channelId: ${e.message}", tag = TAG)
                        }
                    }
                }
                launch { emoteRepository.loadGlobalEmotes(); retokenizeMessages() }
                launch { loadBadgesWithToken(); retokenizeMessages() }
                if (resolvedChannelId.isNotEmpty()) {
                    launch { loadChannelEmotesAndBadges(resolvedChannelId) }
                    // FIX: Check mod status via API immediately so buttons appear without sending a message
                    launch { checkAndSetModStatus(resolvedChannelId) }
                }
                launch { loadRecentMessages(channelLogin) }
                update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Napier.e("Failed to initialize channel: ${e.message}", e, tag = TAG)
                update { it.copy(isLoading = false) }
                sendEffect(ChatEffect.ShowError("Failed to join channel: ${e.message}"))
            }
        }
    }

    /** Check moderator status via API right after joining a channel */
    private suspend fun checkAndSetModStatus(channelId: String) {
        val s = state.value
        val token = s.currentAccessToken
        val userId = s.currentUserId
        if (token.isEmpty() || userId.isEmpty()) return
        try {
            // Check if viewer count of channel equals userId → broadcaster
            val channelLogin = s.channelLogin
            val streamerResult = apiClient.getUsers(token, logins = listOf(channelLogin))
            if (streamerResult is io.rudione.chatone.util.Result.Success) {
                val streamerId = streamerResult.data.data.firstOrNull()?.id
                if (streamerId != null && streamerId == userId) {
                    // We ARE the broadcaster
                    channelModCache[channelLogin.lowercase()] = true
                    update { it.copy(isMod = true) }
                    return
                }
                // Check moderators list
                if (streamerId != null) {
                    val modResult = apiClient.getChannelModerators(token, streamerId, userId)
                    if (modResult is io.rudione.chatone.util.Result.Success) {
                        val isMod = modResult.data.data.any { it.userId == userId }
                        channelModCache[channelLogin.lowercase()] = isMod
                        update { it.copy(isMod = isMod) }
                    }
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to check mod status: ${e.message}", tag = TAG)
        }
    }

    // ▼▼▼ Добавление сообщения в историю ▼▼▼
    private fun addToHistory(state: ChatState, message: String): ChatState {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return state

        // Не добавляем дубликаты подряд
        if (state.sentMessageHistory.firstOrNull() == trimmed) return state

        return state.copy(
            sentMessageHistory = listOf(trimmed) + state.sentMessageHistory.take(MAX_HISTORY - 1),
            historyIndex = -1  // Сбрасываем позицию при новом сообщении
        )
    }
    // ▲▲▲ ▲▲▲

    private suspend fun loadRecentMessages(channelLogin: String) {
        try {
            val recent = recentMessagesClient.getRecentMessages(channelLogin, limit = maxMessages)
            if (recent.isNotEmpty()) {
                val displayMessages = recent.map { msg -> chatMessageToDisplay(msg) }
                update { state ->
                    if (state.channelLogin == channelLogin) state.copy(messages = displayMessages.takeLast(maxMessages))
                    else state
                }
                sendEffect(ChatEffect.ScrollToBottom)
                val uniqueUserIds = recent.map { it.userId }.distinct().take(50)
                uniqueUserIds.forEach { userId ->
                    viewModelScope.launch {
                        try {
                            val cosmetics = sevenTvCosmeticsClient.getUserCosmetics(userId)
                            if (cosmetics != null && (cosmetics.paint != null || cosmetics.badge != null)) {
                                if (state.value.channelLogin == channelLogin) {
                                    update { state ->
                                        // FIX Bug 2: always apply latest 7TV cosmetics, not just when both are null
                                        state.copy(messages = state.messages.map { dm ->
                                            if (dm is DisplayMessage.PrivMsg && dm.userId == userId)
                                                dm.copy(sevenTvPaint = cosmetics.paint, sevenTvBadge = cosmetics.badge)
                                            else dm
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
        if (token.isEmpty()) return
        try {
            badgeRepository.loadGlobalBadges(token)
        } catch (e: Exception) {
            Napier.e("Failed to load global badges: ${e.message}", tag = TAG)
        }
    }

    private fun loadChannelEmotesAndBadges(channelId: String) {
        val channelLogin = state.value.channelLogin
        viewModelScope.launch {
            launch {
                try {
                    emoteRepository.loadChannelEmotes(channelLogin, channelId)
                    if (state.value.channelLogin == channelLogin) retokenizeMessages()
                } catch (e: Exception) {
                    Napier.e("Failed to load channel emotes: ${e.message}", tag = TAG)
                }
            }
            launch {
                val token = state.value.currentAccessToken
                if (token.isNotEmpty()) {
                    try {
                        badgeRepository.loadChannelBadges(channelId, token)
                        retokenizeMessages()
                    } catch (e: Exception) {
                        Napier.e("Failed to load channel badges: ${e.message}", tag = TAG)
                    }
                }
            }
            launch {
                try {
                    val emoteSetId = emoteRepository.getSevenTvEmoteSetId(channelLogin)
                    if (emoteSetId != null) sevenTvEventApi.subscribeToEmoteSet(emoteSetId)
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
            state.copy(messages = state.messages.map { msg ->
                if (msg is DisplayMessage.PrivMsg && msg.rawMessage != null) {
                    val newTokens = MessageTokenizer.tokenize(msg.rawMessage, channelEmotes)
                    val newBadges = badgeRepository.resolveBadges(msg.rawMessage.badges, msg.rawMessage.channelId)
                    msg.copy(tokens = newTokens, badges = newBadges)
                } else msg
            })
        }
    }

    private fun chatMessageToDisplay(message: ChatMessage): DisplayMessage.PrivMsg {
        val channelEmotes = emoteRepository.getResolvedEmotes(message.channelName)
        val tokens = MessageTokenizer.tokenize(message, channelEmotes)
        val resolvedBadges = badgeRepository.resolveBadges(message.badges, message.channelId)
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
            isFirstMessage = message.isFirstMessage,
            rawMessage = message,
            sevenTvPaint = cosmetics?.paint,
            sevenTvBadge = cosmetics?.badge
        )
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.messages.collect { message ->
                if (message.channelName.equals(state.value.channelLogin, ignoreCase = true)) {
                    if (state.value.channelId.isEmpty() && message.channelId.isNotEmpty()) {
                        update { it.copy(channelId = message.channelId) }
                        channelIdCache[message.channelName.lowercase()] = message.channelId
                        loadChannelEmotesAndBadges(message.channelId)
                    }
                    val msgId = message.id
                    launch {
                        try {
                            val cosmetics = sevenTvCosmeticsClient.getUserCosmetics(message.userId)
                            if (cosmetics != null && (cosmetics.paint != null || cosmetics.badge != null)) {
                                update { state ->
                                    // FIX Bug 2: always apply latest 7TV cosmetics unconditionally
                                    state.copy(messages = state.messages.map { dm ->
                                        if (dm is DisplayMessage.PrivMsg && dm.id == msgId)
                                            dm.copy(sevenTvPaint = cosmetics.paint, sevenTvBadge = cosmetics.badge)
                                        else dm
                                    })
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    val displayMsg = chatMessageToDisplay(message)
                    val s = state.value
                    val isOwnMessage = message.userId == s.currentUserId

                    val matchResult = if (!isOwnMessage) {
                        checkHighlightRules(message.message, s.currentUserLogin)
                    } else null

                    val finalMsg = if (matchResult != null) displayMsg.copy(isMention = true, highlightColor = matchResult.color) else displayMsg
                    update { state ->
                        if (!state.channelLogin.equals(message.channelName, ignoreCase = true)) return@update state
                        val newMessages = (state.messages + finalMsg).takeLast(maxMessages)
                        state.copy(messages = newMessages, mentionCount = if (matchResult != null) state.mentionCount + 1 else state.mentionCount)
                    }
                    sendEffect(ChatEffect.ScrollToBottom)

                    if (matchResult != null) {
                        // Always fire MentionDetected so MentionsFeed gets it regardless of sound setting
                        sendEffect(ChatEffect.MentionDetected(s.channelLogin, finalMsg as? DisplayMessage.PrivMsg))
                        if (matchResult.playSound) {
                            val settings = SettingsViewModel.loadInitialState()
                            if (settings.customMentionSoundPath.isNotBlank()) {
                                NotificationSoundPlayer.playMentionSound(
                                    settings.mentionSoundVolume,
                                    settings.customMentionSoundPath
                                )
                            } else {
                                NotificationSoundPlayer.playMentionSound()
                            }
                        }
                    }

                    chatRepository.saveMessage(message)
                } else {
                    val s = state.value
                    val isOwnMessage = message.userId == s.currentUserId
                    if (!isOwnMessage) {
                        val matchResult = checkHighlightRules(message.message, s.currentUserLogin)
                        if (matchResult != null) {
                            // Build display message for the mention entry
                            val otherDisplayMsg = chatMessageToDisplay(message)
                            val otherFinalMsg = otherDisplayMsg.copy(isMention = true, highlightColor = matchResult.color)
                            sendEffect(ChatEffect.MentionDetected(message.channelName, otherFinalMsg))
                            if (matchResult.playSound) {
                                val settings = SettingsViewModel.loadInitialState()
                                if (settings.customMentionSoundPath.isNotBlank()) {
                                    NotificationSoundPlayer.playMentionSound(
                                        settings.mentionSoundVolume,
                                        settings.customMentionSoundPath
                                    )
                                } else {
                                    NotificationSoundPlayer.playMentionSound()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeIrcEvents() {
        viewModelScope.launch {
            chatRepository.events.collect { event ->
                val channelLogin = state.value.channelLogin
                when (event) {
                    is IrcEvent.Notice -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            addMessage(DisplayMessage.SystemMsg(
                                id = "notice_${Clock.System.now().toEpochMilliseconds()}",
                                timestamp = Clock.System.now().toEpochMilliseconds(),
                                channel = event.channel, text = event.message,
                                type = DisplayMessage.SystemMsg.SystemType.NOTICE
                            ))
                        }
                    }
                    is IrcEvent.UserNotice -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            addMessage(DisplayMessage.UserNoticeMsg(
                                id = "usernotice_${Clock.System.now().toEpochMilliseconds()}",
                                timestamp = Clock.System.now().toEpochMilliseconds(),
                                channel = event.channel, systemText = event.systemMsg,
                                innerMessage = event.message?.let { chatMessageToDisplay(it) },
                                noticeType = event.msgId ?: ""
                            ))
                        }
                    }
                    is IrcEvent.ClearChat -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            if (event.targetUser != null) {
                                val action = if (event.banDuration != null) DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT else DisplayMessage.ModerationMsg.ModerationAction.BAN
                                val text = if (event.banDuration != null) "${event.targetUser} was timed out for ${event.banDuration}s" else "${event.targetUser} was banned"
                                addMessage(DisplayMessage.ModerationMsg(
                                    id = "mod_${Clock.System.now().toEpochMilliseconds()}",
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel, text = text, action = action,
                                    targetUser = event.targetUser, duration = event.banDuration
                                ))
                                update { state ->
                                    state.copy(messages = state.messages.map { dm ->
                                        if (dm is DisplayMessage.PrivMsg && dm.username.equals(event.targetUser, ignoreCase = true)) dm.copy(isDeleted = true) else dm
                                    })
                                }
                            } else {
                                addMessage(DisplayMessage.ModerationMsg(
                                    id = "clear_${Clock.System.now().toEpochMilliseconds()}",
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel, text = "Chat was cleared by a moderator",
                                    action = DisplayMessage.ModerationMsg.ModerationAction.CLEAR
                                ))
                            }
                        }
                    }
                    is IrcEvent.ClearMsg -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            update { state ->
                                state.copy(messages = state.messages.map { dm ->
                                    if (dm is DisplayMessage.PrivMsg && dm.id == event.targetMessageId) dm.copy(isDeleted = true) else dm
                                })
                            }
                        }
                    }
                    is IrcEvent.RoomState -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
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
                            if (event.badges.isNotEmpty()) currentUserBadgeRaw = event.badges

                            // FIX Bug 3: parse the freshly received badges and retroactively patch
                            // the most recent local-echo message (id starts with "local_") that
                            // was sent by the current user and still has an empty badge list.
                            // This happens when the user sends their first message and USERSTATE
                            // arrives after the local echo was already added.
                            if (event.badges.isNotEmpty()) {
                                val freshBadges = event.badges.split(",").mapNotNull { pair ->
                                    val parts = pair.split("/", limit = 2)
                                    if (parts.size == 2) Badge(id = parts[0], version = parts[1], imageUrl = "") else null
                                }
                                val resolvedFreshBadges = badgeRepository.resolveBadges(
                                    freshBadges,
                                    state.value.channelId.ifEmpty { null }
                                )
                                val currentUserId = state.value.currentUserId
                                update { st ->
                                    // Find the last local-echo message for the current user that has no badges
                                    val targetIndex = st.messages.indexOfLast { dm ->
                                        dm is DisplayMessage.PrivMsg &&
                                                dm.id.startsWith("local_") &&
                                                dm.userId == currentUserId &&
                                                dm.badges.all { it.imageUrl.isEmpty() }
                                    }
                                    if (targetIndex == -1) return@update st
                                    val patched = (st.messages[targetIndex] as DisplayMessage.PrivMsg)
                                        .copy(badges = resolvedFreshBadges, isModerator = event.isMod)
                                    val newMessages = st.messages.toMutableList()
                                    newMessages[targetIndex] = patched
                                    st.copy(messages = newMessages)
                                }
                            }

                            update { it.copy(
                                isMod = event.isMod,
                                currentUserColor = event.color ?: it.currentUserColor,
                                currentDisplayName = if (event.displayName.isNotEmpty()) event.displayName else it.currentDisplayName
                            ) }
                        }
                    }
                    is IrcEvent.GlobalUserState -> {
                        update { it.copy(
                            currentUserId = event.userId,
                            currentUserColor = event.color ?: it.currentUserColor,
                            currentDisplayName = if (event.displayName.isNotEmpty()) event.displayName else it.currentDisplayName,
                            currentUserLogin = if (event.displayName.isNotEmpty()) event.displayName.lowercase() else it.currentUserLogin
                        ) }
                    }
                    is IrcEvent.AutoModHeld -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true) && (state.value.isMod || state.value.channelLogin == state.value.currentUserLogin)) {
                            addMessage(DisplayMessage.AutoModMsg(
                                id = "automod_${event.msgId}",
                                timestamp = Clock.System.now().toEpochMilliseconds(),
                                channel = event.channel,
                                msgId = event.msgId,
                                userId = event.userId,
                                username = event.username,
                                displayName = event.displayName,
                                text = event.message,
                                color = event.color
                            ))
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
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteAdded -> "[7TV] ${event.actorName} added ${event.emoteName}"
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRemoved -> "[7TV] ${event.actorName} removed ${event.emoteName}"
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRenamed -> "[7TV] ${event.actorName} renamed ${event.oldName} to ${event.newName}"
                }
                addMessage(DisplayMessage.SystemMsg(
                    id = "7tv_${Clock.System.now().toEpochMilliseconds()}",
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    channel = state.value.channelLogin, text = text,
                    type = DisplayMessage.SystemMsg.SystemType.NOTICE
                ))
                val channelId = state.value.channelId
                if (channelId.isNotEmpty()) {
                    launch { try { emoteRepository.loadChannelEmotes(state.value.channelLogin, channelId) } catch (_: Exception) {} }
                }
            }
        }
    }

    private fun addMessage(msg: DisplayMessage) {
        update { state ->
            state.copy(messages = (state.messages + msg).takeLast(maxMessages))
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

    // ── Autocomplete ────────────────────────────────────────────────

    private fun updateMessageInput(input: String) {
        update { it.copy(messageInput = input, historyIndex = -1) }

        val lastWord = input.trimEnd().split(" ").lastOrNull() ?: ""

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
                update { it.copy(mentionCompletions = recentUsers, showMentionCompletions = true, showEmoteCompletions = false, emoteCompletions = emptyList()) }
            } else {
                update { it.copy(showMentionCompletions = false, mentionCompletions = emptyList()) }
            }
            return
        }
        update { it.copy(showMentionCompletions = false, mentionCompletions = emptyList()) }

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
        if (words.isNotEmpty()) words[words.size - 1] = "@$username"
        val newInput = words.joinToString(" ") + " "
        update { it.copy(messageInput = newInput, showMentionCompletions = false, mentionCompletions = emptyList()) }
    }

    private fun selectEmoteCompletion(emote: GenericEmote) {
        val current = state.value.messageInput
        val words = current.split(" ").toMutableList()
        if (words.isNotEmpty()) words[words.size - 1] = emote.code
        val newInput = words.joinToString(" ") + " "
        update { it.copy(messageInput = newInput, showEmoteCompletions = false, emoteCompletions = emptyList()) }
    }

    // ── Send Message ────────────────────────────────────────────────

    private fun sendMessage(keepText: Boolean = false) {
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
                if (message.startsWith("/w ")) {
                    val parts = message.removePrefix("/w ").split(" ", limit = 2)
                    if (parts.size == 2) {
                        val targetUser = parts[0]
                        val whisperMsg = parts[1]
                        val targetMsg = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
                            .firstOrNull { it.username.equals(targetUser, ignoreCase = true) }
                        if (targetMsg != null && s.currentAccessToken.isNotEmpty()) {
                            val result = apiClient.sendWhisper(
                                accessToken = s.currentAccessToken,
                                fromUserId = s.currentUserId,
                                toUserId = targetMsg.userId,
                                message = whisperMsg
                            )
                            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to send whisper"))
                        } else {
                            sendMessageUseCase(channelLogin, message)
                        }
                        if (!keepText) update { it.copy(messageInput = "") }
                        return@launch
                    }
                }

                sendMessageUseCase(channelLogin, message)

                val now = Clock.System.now().toEpochMilliseconds()
                // Use currentUserBadgeRaw if available (for non-first messages).
                // For the very first message, this may be empty — the badges will be
                // retroactively patched when USERSTATE arrives (see observeIrcEvents).
                val ownBadges = if (currentUserBadgeRaw.isNotEmpty()) {
                    currentUserBadgeRaw.split(",").mapNotNull { pair ->
                        val parts = pair.split("/", limit = 2)
                        if (parts.size == 2) Badge(id = parts[0], version = parts[1], imageUrl = "") else null
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

                val newHistory = (listOf(message) + s.sentMessageHistory).take(MAX_HISTORY)

                update { state ->
                    val newMessages = (state.messages + displayMsg).takeLast(maxMessages)
                    state.copy(
                        messageInput = if (keepText) state.messageInput else "",
                        showEmoteCompletions = false,
                        emoteCompletions = emptyList(),
                        messages = newMessages,
                        replyingTo = if (keepText) state.replyingTo else null,
                        sentMessageHistory = newHistory,
                        historyIndex = -1
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
                if (channelLogin.isNotEmpty()) joinChannelUseCase(channelLogin)
            } catch (e: Exception) {
                Napier.e("Failed to reconnect: ${e.message}", e, tag = TAG)
                sendEffect(ChatEffect.ShowError("Failed to reconnect: ${e.message}"))
            }
        }
    }

    private fun toggleModMode() { update { it.copy(modModeEnabled = !it.modModeEnabled) } }

    private fun timeoutUser(userId: String, duration: Int) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.banUser(s.currentAccessToken, s.channelId, s.currentUserId, userId, duration)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to timeout user"))
        }
    }

    private fun banUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.banUser(s.currentAccessToken, s.channelId, s.currentUserId, userId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to ban user"))
        }
    }

    private fun deleteMessage(messageId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.deleteMessage(s.currentAccessToken, s.channelId, s.currentUserId, messageId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to delete message"))
        }
    }

    private fun unbanUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.unbanUser(s.currentAccessToken, s.channelId, s.currentUserId, userId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to unban user"))
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

    private fun whisperUser(username: String) { update { it.copy(messageInput = "/w $username ") } }

    private fun insertMention(displayName: String) {
        update { st ->
            val current = st.messageInput
            val tag = "@$displayName "
            val newInput = when {
                current.isEmpty() -> tag
                current.endsWith(" ") -> "$current$tag"
                else -> "$current $tag"
            }
            // showMentionCompletions = false so autocomplete does NOT appear
            st.copy(messageInput = newInput, showMentionCompletions = false, mentionCompletions = emptyList())
        }
    }

    private fun updateChatSettings(settings: Map<String, Any>) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.updateChatSettings(s.currentAccessToken, s.channelId, s.currentUserId, settings)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to update chat settings"))
        }
    }

    private fun pinMessage(messageId: String) {
        val msg = state.value.messages.filterIsInstance<DisplayMessage.PrivMsg>().firstOrNull { it.id == messageId }
        if (msg != null) update { it.copy(pinnedMessage = msg) }
    }

    private fun clearChat() {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.clearChat(s.currentAccessToken, s.channelId, s.currentUserId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to clear chat"))
        }
    }

    private fun sendAnnouncement(message: String, color: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.sendAnnouncement(s.currentAccessToken, s.channelId, s.currentUserId, message, color)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to send announcement"))
        }
    }

    private fun startRaid(targetLogin: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val usersResult = apiClient.getUsers(s.currentAccessToken, logins = listOf(targetLogin))
            if (usersResult is io.rudione.chatone.util.Result.Success) {
                val targetId = usersResult.data.data.firstOrNull()?.id
                if (targetId != null) {
                    val result = apiClient.startRaid(s.currentAccessToken, s.channelId, targetId)
                    if (result.isError) sendEffect(ChatEffect.ShowError("Failed to start raid"))
                } else sendEffect(ChatEffect.ShowError("User not found: $targetLogin"))
            } else sendEffect(ChatEffect.ShowError("Failed to resolve user for raid"))
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
            val result = apiClient.sendShoutout(s.currentAccessToken, s.channelId, targetUserId, s.currentUserId)
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to send shoutout"))
        }
    }

    private fun executeMacro(macro: Macro) {
        val s = state.value
        if (s.channelLogin.isEmpty()) return
        viewModelScope.launch {
            macro.steps.forEach { step ->
                when (step) {
                    is MacroStep.SendMessage -> {
                        try { sendMessageUseCase(s.channelLogin, step.text) } catch (_: Exception) {}
                    }
                    is MacroStep.SubMode ->
                        updateChatSettings(mapOf("subscriber_mode" to step.enable))
                    is MacroStep.EmoteMode ->
                        updateChatSettings(mapOf("emote_mode" to step.enable))
                    is MacroStep.SlowMode ->
                        if (step.enable) updateChatSettings(mapOf("slow_mode" to true, "slow_mode_wait_time" to step.seconds))
                        else updateChatSettings(mapOf("slow_mode" to false))
                    is MacroStep.FollowerMode ->
                        if (step.enable) updateChatSettings(mapOf("follower_mode" to true, "follower_mode_duration" to step.minutes))
                        else updateChatSettings(mapOf("follower_mode" to false))
                    is MacroStep.R9KMode ->
                        updateChatSettings(mapOf("unique_chat_mode" to step.enable))
                    is MacroStep.StartRaid ->
                        startRaid(step.targetLogin)
                    is MacroStep.PinMessage ->
                        try { sendMessageUseCase(s.channelLogin, "/pin ${step.message}") } catch (_: Exception) {}
                    is MacroStep.Delay ->
                        delay(step.seconds * 1000L)
                    is MacroStep.ClearChat ->
                        clearChat()
                }
            }
        }
    }
}