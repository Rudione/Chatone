package io.rudione.chatone.presentation.chat

import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.remote.RecentMessagesClient
import io.rudione.chatone.data.remote.RecentMessagesResult
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.data.remote.emote.SevenTvCosmeticsClient
import io.rudione.chatone.data.remote.emote.SevenTvEventApi
import io.rudione.chatone.data.repository.AutomodRepository
import io.rudione.chatone.data.repository.BadgeRepository
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.domain.model.Macro
import io.rudione.chatone.domain.model.MacroStep
import io.rudione.chatone.domain.model.hasGrandModBadge
import io.rudione.chatone.domain.usecase.JoinChannelUseCase
import io.rudione.chatone.domain.usecase.SendMessageUseCase
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.format
import io.rudione.chatone.util.automod.AutomodEngine
import io.rudione.chatone.util.automod.AutomodTarget
import io.rudione.chatone.util.automod.ChatRuleEngine
import io.rudione.chatone.util.automod.ChatRuleEventEngine
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.util.chat.MessageTokenizer
import io.rudione.chatone.util.media.NotificationSoundPlayer
import io.rudione.chatone.util.automod.RegexCache
import io.rudione.chatone.util.chat.SlashCommand
import io.rudione.chatone.data.remote.ImageUploaderClient
import io.rudione.chatone.data.repository.EnrichedPersonalEmoteBackfiller
import io.rudione.chatone.data.repository.ModerationHistoryRepository
import io.rudione.chatone.di.GlobalDi
import io.rudione.chatone.util.media.readLocalFileBytes
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.concurrent.Volatile

data class PollHistoryEntry(val title: String, val choices: List<String>, val durationSeconds: Int)
data class PredictionHistoryEntry(val title: String, val outcomes: List<String>, val windowSeconds: Int)

data class ChatState(
    val channelLogin: String = "",
    val channelDisplayName: String = "",
    val liveStream: io.rudione.chatone.data.remote.dto.ChannelData? = null,
    val channelId: String = "",
    val messages: List<DisplayMessage> = emptyList(),

    val messagesSeq: Long = 0L,

    val messagesStartOrdinal: Long = 0L,
    val messageInput: String = "",
    val lastMessageSentAtMs: Long = 0L,
    val isConnected: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val isLoading: Boolean = false,
    val isMod: Boolean = false,
    val isBroadcaster: Boolean = false,
    val isGrandMod: Boolean = false,
    val modModeEnabled: Boolean = false,
    val roomState: RoomState = RoomState(),
    val currentUserId: String = "",
    val currentUserLogin: String = "",
    val currentDisplayName: String = "",
    val currentUserColor: String = "",
    val currentAccessToken: String = "",
    val isEmotePickerVisible: Boolean = false,
    val emoteCompletions: List<GenericEmote> = emptyList(),
    val showEmoteCompletions: Boolean = false,

    val mentionCompletions: List<String> = emptyList(),
    val showMentionCompletions: Boolean = false,
    val mentionCount: Int = 0,

    val replyingTo: DisplayMessage.PrivMsg? = null,

    val pinnedMessage: DisplayMessage.PrivMsg? = null,
    val pinId: String? = null,
    val pinEndsAtMs: Long? = null,
    val pinnedByName: String? = null,
    val pinnedByBadges: List<Badge> = emptyList(),
    val pinLocallyHidden: Boolean = false,
    val hiddenEventIds: Set<String> = emptySet(),
    val showPollCreation: Boolean = false,
    val showPredictionCreation: Boolean = false,
    val recentPolls: List<PollHistoryEntry> = emptyList(),
    val recentPredictions: List<PredictionHistoryEntry> = emptyList(),
    val showPointsBitsPanel: Boolean = false,
    val pointsBitsLoading: Boolean = false,
    val pointsBalance: Long = 0L,
    val channelRewards: List<io.rudione.chatone.data.remote.GqlChannelPointReward> = emptyList(),
    val pointsIconUrl: String? = null,
    val pointsBitsError: String? = null,

    val sentMessageHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,

    val pendingRaidTarget: String? = null,
    val pendingRaidTargetId: String? = null,
    val pendingRaidStartedAt: Long = 0L,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val inputGlowIntensity: Float = 0f,
    val inputGlowTriggerTs: Long = 0L,
    val activePollId: String? = null,
    val activePredictionId: String? = null,
    val activePredictionOutcomes: List<Pair<String, String>> = emptyList(),
    val livePoll: io.rudione.chatone.data.remote.dto.PollData? = null,
    val livePrediction: io.rudione.chatone.data.remote.dto.PredictionData? = null,
    val blockedUserIds: Set<String> = emptySet(),
    val showBlockedMode: Int = 0,
    val twitchSubscriberEmotes: List<GenericEmote> = emptyList(),
    val twitchChannelEmotes: List<GenericEmote> = emptyList(),
    val twitchGlobalEmotes: List<GenericEmote> = emptyList(),
    val uploadProgress: Float? = null,
    val uploadedLink: String? = null,
    val uploadError: String? = null,
    val pendingUploadPath: String? = null
) : UiState {
    val canModerate: Boolean get() = isMod || isBroadcaster
}

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

    object OnSendMessageKeepText : ChatEvent()

    object OnHistoryUp : ChatEvent()
    object OnHistoryDown : ChatEvent()
    object OnReconnect : ChatEvent()
    object OnToggleModMode : ChatEvent()
    data class OnTimeoutUser(val userId: String, val duration: Int) : ChatEvent()
    data class OnBanUser(val userId: String, val reason: String? = null) : ChatEvent()
    data class OnUnbanUser(val userId: String) : ChatEvent()
    data class OnDeleteMessage(val messageId: String) : ChatEvent()
    data class OnWhisper(val username: String) : ChatEvent()
    data class OnInsertMention(val displayName: String) : ChatEvent()
    data class OnModUser(val userId: String) : ChatEvent()
    data class OnUnmodUser(val userId: String) : ChatEvent()
    data class OnVipUser(val userId: String) : ChatEvent()
    data class OnUnvipUser(val userId: String) : ChatEvent()

    data class OnSelectEmoteCompletion(val emote: GenericEmote) : ChatEvent()
    object OnDismissCompletions : ChatEvent()
    data class OnSelectMentionCompletion(val username: String) : ChatEvent()
    object OnDismissMentionCompletions : ChatEvent()

    data class OnUpdateChatSettings(val settings: Map<String, Any>) : ChatEvent()
    object OnClearChat : ChatEvent()

    data class OnReplyToMessage(val message: DisplayMessage.PrivMsg) : ChatEvent()
    object OnCancelReply : ChatEvent()

    data class OnPinMessage(val messageId: String) : ChatEvent()
    object OnUnpinMessage : ChatEvent()

    object OnOpenPollCreation : ChatEvent()
    object OnClosePollCreation : ChatEvent()
    data class OnCreatePoll(val title: String, val choices: List<String>, val durationSeconds: Int) : ChatEvent()
    object OnOpenPredictionCreation : ChatEvent()
    object OnClosePredictionCreation : ChatEvent()
    data class OnCreatePrediction(val title: String, val outcomes: List<String>, val windowSeconds: Int) : ChatEvent()
    data class OnVotePoll(val pollId: String, val choiceId: String) : ChatEvent()
    data class OnPlacePrediction(val eventId: String, val outcomeId: String, val points: Int) : ChatEvent()
    data class OnResolvePrediction(val eventId: String, val outcomeId: String) : ChatEvent()
    data class OnLockPrediction(val eventId: String) : ChatEvent()

    object OnOpenPointsBitsPanel : ChatEvent()
    object OnClosePointsBitsPanel : ChatEvent()
    object OnToggleHidePin : ChatEvent()
    data class OnHideEventBanner(val key: String) : ChatEvent()
    object OnRestoreHiddenBanners : ChatEvent()
    data class OnRedeemReward(
        val reward: io.rudione.chatone.data.remote.GqlChannelPointReward,
        val textInput: String = ""
    ) : ChatEvent()

    data class OnSendAnnouncement(val message: String, val color: String = "primary") : ChatEvent()
    data class OnStartRaid(val targetLogin: String) : ChatEvent()
    object OnCancelRaid : ChatEvent()
    object OnRaidNow : ChatEvent()
    data class OnSendShoutout(val targetUserId: String) : ChatEvent()
    data class OnSendMessageText(val text: String) : ChatEvent()
    data class OnExecuteMacro(val macro: Macro) : ChatEvent()
    data class OnAllowAutoModMessage(val msgId: String) : ChatEvent()
    data class OnDenyAutoModMessage(val msgId: String) : ChatEvent()
    object OnToggleEmotePicker : ChatEvent()
    object OnRefreshChannel : ChatEvent()
    data class OnBlockUser(val targetUserId: String, val targetLogin: String) : ChatEvent()
    data class OnUnblockUser(val targetUserId: String, val targetLogin: String) : ChatEvent()
    data class OnSetShowBlockedMode(val mode: Int) : ChatEvent()

    data class OnFilesDropped(val paths: List<String>) : ChatEvent()
    object OnConfirmPendingUpload : ChatEvent()
    object OnCancelPendingUpload : ChatEvent()
    object OnClearUploadedLink : ChatEvent()

    data class OnScrollbackPinned(val pinned: Boolean) : ChatEvent()
}

sealed class ChatEffect : UIEffect {
    data class ShowError(val message: String) : ChatEffect()
    object ScrollToBottom : ChatEffect()
    object HistoryMerged : ChatEffect()
    data class MentionDetected(
        val channelLogin: String,
        val message: DisplayMessage.PrivMsg? = null,
        val playSound: Boolean = false
    ) : ChatEffect()

    data class OpenUserProfile(
        val userId: String,
        val username: String,
        val displayName: String,
        val color: String?
    ) : ChatEffect()

    object FocusChatInput : ChatEffect()
}

private fun buildTwitchEmote(
    e: io.rudione.chatone.data.remote.dto.UserEmoteData,
    template: String,
    author: String
): GenericEmote {
    fun url(scale: String) = if (template.isNotEmpty()) {
        template
            .replace("{{id}}", e.id)
            .replace("{{format}}", "default")
            .replace("{{theme_mode}}", "dark")
            .replace("{{scale}}", scale)
    } else {
        "https://static-cdn.jtvnw.net/emoticons/v2/${e.id}/default/dark/$scale"
    }
    return GenericEmote(
        id = e.id,
        code = e.name,
        url1x = url("1.0"),
        url2x = url("2.0"),
        url3x = url("3.0"),
        provider = EmoteProvider.TWITCH,
        authorName = author
    )
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
    private val sevenTvEventApi: SevenTvEventApi,
    private val sevenTvApi: io.rudione.chatone.data.remote.emote.SevenTvApiClient,
    private val automodRepository: AutomodRepository,
    private val pubSubClient: io.rudione.chatone.data.remote.TwitchPubSubClient,
    private val eventSubClient: io.rudione.chatone.data.remote.TwitchEventSubClient,
    private val imageUploaderClient: io.rudione.chatone.data.remote.ImageUploaderClient,
    private val twitchGqlClient: io.rudione.chatone.data.remote.TwitchGqlClient,
    private val moderationAuthStore: io.rudione.chatone.data.repository.ModerationAuthStore,
    private val aiAssistantController: io.rudione.chatone.data.repository.AiAssistantController,
    private val aiAssistantClient: io.rudione.chatone.data.remote.AiAssistantClient,
    private val mentionMuteRepository: io.rudione.chatone.data.repository.MentionMuteRepository,
    private val messagePersistence: io.rudione.chatone.data.repository.MessagePersistenceQueue,
    private val persistedSettings: com.russhwolf.settings.Settings
) : BaseViewModel<ChatState, ChatEvent, ChatEffect>(ChatState()) {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_CACHED_CHANNELS = 10
        private const val MAX_HISTORY = 50
        private const val HIDDEN_EVENTS_MEMORY = 50
        private const val PINNED_SCROLLBACK_HEADROOM = 2000
        private const val LOCAL_SEED_MAX_AGE_MS = 24L * 60 * 60 * 1000
        private const val MOD_NOTICE_DEDUPE_MS = 4000L
        private const val GQL_TOKEN_REQUIRED_MSG =
            "Set up a first-party token in Settings → Moderation to use this command"
        private val msgCounter = atomic(0L)

        private fun uniqueId(prefix: String): String =
            "${prefix}_${Clock.System.now().toEpochMilliseconds()}_${msgCounter.incrementAndGet()}"
    }

    private val maxMessages: Int
        get() = cachedSettings().scrollbackLimit.coerceIn(100, 5000)

    @Volatile
    private var scrollbackPinned = false

    private val appendCap: Int
        get() = if (scrollbackPinned) maxMessages + PINNED_SCROLLBACK_HEADROOM else maxMessages

    private fun setScrollbackPinned(pinned: Boolean) {
        if (scrollbackPinned == pinned) return
        scrollbackPinned = pinned
        if (!pinned) {
            update { st ->
                if (st.messages.size <= maxMessages) return@update st
                val evicted = st.messages.size - maxMessages
                st.messages.take(evicted).forEach { messageIdSet.remove(it.id) }
                st.copy(
                    messages = st.messages.takeLast(maxMessages),
                    messagesStartOrdinal = st.messagesStartOrdinal + evicted
                )
            }
        }
    }

    @Volatile
    private var _cachedSettings: io.rudione.chatone.presentation.settings.SettingsState? = null
    private val settingsLock = SynchronizedObject()
    private fun cachedSettings(): io.rudione.chatone.presentation.settings.SettingsState {
        val s = _cachedSettings
        if (s != null) return s
        return synchronized(settingsLock) {
            _cachedSettings ?: SettingsViewModel.loadInitialState().also { _cachedSettings = it }
        }
    }

    private fun invalidateSettingsCache() {
        _cachedSettings = null
    }

    private var spamJob: kotlinx.coroutines.Job? = null

    private val INVISIBLE_DUP_MARKER = "󠀀"
    private var pollPollingJob: kotlinx.coroutines.Job? = null
    private var predictionPollingJob: kotlinx.coroutines.Job? = null

    private var pointsClaimErrorShown = false

    private val automationController = ChatAutomationController(
        scope = viewModelScope,
        apiClient = apiClient,
        getChatState = { state.value },
        getSettings = { cachedSettings() },
        sendViaHelixOnly = ::sendViaHelixOnly
    )

    private fun isPhraseMuted(text: String): Boolean {
        val phrases = cachedSettings().mutedPhrases
        if (phrases.isEmpty()) return false
        return phrases.any { text.contains(it, ignoreCase = true) }
    }

    private fun startPollPolling() {
        pollPollingJob?.cancel()
        pollPollingJob = viewModelScope.launch {
            while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == true) {
                val s = state.value
                if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) {
                    delay(5000L)
                    continue
                }
                val requestedChannelId = s.channelId
                val r = apiClient.getActivePoll(s.currentAccessToken, requestedChannelId)
                if (state.value.channelId != requestedChannelId) break
                if (r is io.rudione.chatone.util.Result.Success) {
                    val data = r.data
                    update {
                        if (it.channelId != requestedChannelId) it
                        else it.copy(livePoll = data, activePollId = data?.id ?: it.activePollId)
                    }
                    if (data == null || data.status != "ACTIVE") {
                        if (data == null) update {
                            if (it.channelId != requestedChannelId) it else it.copy(activePollId = null)
                        }
                        break
                    }
                }
                delay(4000L)
            }
        }
    }

    private fun startPredictionPolling() {
        predictionPollingJob?.cancel()
        predictionPollingJob = viewModelScope.launch {
            while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == true) {
                val s = state.value
                if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) {
                    kotlinx.coroutines.delay(5000L)
                    continue
                }
                val requestedChannelId = s.channelId
                val r = apiClient.getActivePrediction(s.currentAccessToken, requestedChannelId)
                if (state.value.channelId != requestedChannelId) break
                if (r is io.rudione.chatone.util.Result.Success) {
                    val data = r.data
                    update {
                        if (it.channelId != requestedChannelId) it
                        else it.copy(
                            livePrediction = data,
                            activePredictionId = data?.id ?: it.activePredictionId,
                            activePredictionOutcomes = data?.outcomes?.map { o -> o.id to o.title }
                                ?: it.activePredictionOutcomes
                        )
                    }
                    if (data == null || (data.status != "ACTIVE" && data.status != "LOCKED")) {
                        if (data == null) update {
                            if (it.channelId != requestedChannelId) it
                            else it.copy(
                                activePredictionId = null,
                                activePredictionOutcomes = emptyList()
                            )
                        }
                        break
                    }
                }
                kotlinx.coroutines.delay(4000L)
            }
        }
    }

    private fun fetchCurrentPoll(channelLogin: String, channelId: String) {
        viewModelScope.launch {
            if (!moderationAuthStore.hasCustomToken()) return@launch
            val token = moderationAuthStore.resolveToken(state.value.currentAccessToken)
            if (token.isBlank()) return@launch
            val poll = twitchGqlClient.getViewablePollGql(channelLogin, token) ?: return@launch
            if (!poll.status.equals("ACTIVE", ignoreCase = true)) return@launch
            update { if (it.channelId == channelId) it.copy(livePoll = poll, activePollId = poll.id) else it }
            startPollPollingGql(channelLogin, channelId)
        }
    }

    private fun fetchCurrentPrediction(channelLogin: String, channelId: String) {
        viewModelScope.launch {
            if (!moderationAuthStore.hasCustomToken()) return@launch
            val token = moderationAuthStore.resolveToken(state.value.currentAccessToken)
            if (token.isBlank()) return@launch
            val prediction = twitchGqlClient.getActivePredictionGql(channelLogin, token) ?: return@launch
            if (prediction.status != "ACTIVE" && prediction.status != "LOCKED") return@launch
            update {
                if (it.channelId == channelId) it.copy(
                    livePrediction = prediction,
                    activePredictionId = prediction.id,
                    activePredictionOutcomes = prediction.outcomes.map { o -> o.id to o.title }
                ) else it
            }
            twitchGqlClient.getChannelPointRewardsGql(channelLogin, token)?.let { info ->
                update { if (it.channelId == channelId) it.copy(pointsBalance = info.balance) else it }
            }
            startPredictionPollingGql(channelLogin, channelId)
        }
    }

    private fun syncPollAfterCreate(channelLogin: String, channelId: String) {
        viewModelScope.launch {
            repeat(6) { attempt ->
                if (attempt > 0) delay(600L)
                if (state.value.channelId != channelId) return@launch
                val token = moderationAuthStore.resolveToken(state.value.currentAccessToken)
                if (token.isBlank()) return@launch
                val poll = twitchGqlClient.getViewablePollGql(channelLogin, token)
                    ?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) } ?: return@repeat
                update { if (it.channelId == channelId) it.copy(livePoll = poll, activePollId = poll.id) else it }
                startPollPollingGql(channelLogin, channelId)
                return@launch
            }
        }
    }

    private fun syncPredictionAfterCreate(channelLogin: String, channelId: String) {
        viewModelScope.launch {
            repeat(6) { attempt ->
                if (attempt > 0) delay(600L)
                if (state.value.channelId != channelId) return@launch
                val token = moderationAuthStore.resolveToken(state.value.currentAccessToken)
                if (token.isBlank()) return@launch
                val prediction = twitchGqlClient.getActivePredictionGql(channelLogin, token)
                    ?.takeIf { it.status == "ACTIVE" || it.status == "LOCKED" } ?: return@repeat
                update {
                    if (it.channelId == channelId) it.copy(
                        livePrediction = prediction,
                        activePredictionId = prediction.id,
                        activePredictionOutcomes = prediction.outcomes.map { o -> o.id to o.title }
                    ) else it
                }
                startPredictionPollingGql(channelLogin, channelId)
                return@launch
            }
        }
    }

    private fun startPollPollingGql(channelLogin: String, channelId: String) {
        pollPollingJob?.cancel()
        pollPollingJob = viewModelScope.launch {
            while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == true) {
                delay(4000L)
                if (!moderationAuthStore.hasCustomToken()) break
                val token = moderationAuthStore.resolveToken(state.value.currentAccessToken)
                if (token.isBlank()) continue
                val poll = twitchGqlClient.getViewablePollGql(channelLogin, token)
                update { if (it.channelId == channelId) it.copy(livePoll = poll, activePollId = poll?.id) else it }
                if (poll == null || !poll.status.equals("ACTIVE", ignoreCase = true)) break
            }
        }
    }

    private fun startPredictionPollingGql(channelLogin: String, channelId: String) {
        predictionPollingJob?.cancel()
        predictionPollingJob = viewModelScope.launch {
            while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == true) {
                delay(4000L)
                if (!moderationAuthStore.hasCustomToken()) break
                val token = moderationAuthStore.resolveToken(state.value.currentAccessToken)
                if (token.isBlank()) continue
                val prediction = twitchGqlClient.getActivePredictionGql(channelLogin, token)
                update {
                    if (it.channelId == channelId) it.copy(
                        livePrediction = prediction,
                        activePredictionId = prediction?.id,
                        activePredictionOutcomes = prediction?.outcomes?.map { o -> o.id to o.title } ?: emptyList()
                    ) else it
                }
                if (prediction == null || (prediction.status != "ACTIVE" && prediction.status != "LOCKED")) break
            }
        }
    }

    private fun votePoll(pollId: String, choiceId: String) {
        val s = state.value
        if (!moderationAuthStore.hasCustomToken()) {
            sendEffect(ChatEffect.ShowError(GQL_TOKEN_REQUIRED_MSG)); return
        }
        if (s.currentUserId.isBlank()) return
        val poll = s.livePoll
        if (poll == null || poll.id != pollId || poll.selfVoteChoiceId != null) return
        val token = moderationAuthStore.resolveToken(s.currentAccessToken)
        update { st ->
            val p = st.livePoll ?: return@update st
            if (p.id != pollId || p.selfVoteChoiceId != null) return@update st
            st.copy(
                livePoll = p.copy(
                    choices = p.choices.map { c -> if (c.id == choiceId) c.copy(votes = c.votes + 1) else c },
                    selfVoteChoiceId = choiceId
                )
            )
        }
        viewModelScope.launch {
            val r = twitchGqlClient.voteInPollGql(
                pollId = pollId,
                choiceId = choiceId,
                userId = s.currentUserId,
                extraVotes = 0,
                pointsPerVote = null,
                token = token
            )
            if (r.isError) {
                sendEffect(ChatEffect.ShowError("Failed to vote: ${(r as io.rudione.chatone.util.Result.Error).exception.message}"))
            }
        }
    }

    private fun placePrediction(eventId: String, outcomeId: String, points: Int) {
        val s = state.value
        if (!moderationAuthStore.hasCustomToken()) {
            sendEffect(ChatEffect.ShowError(GQL_TOKEN_REQUIRED_MSG)); return
        }
        if (points <= 0) return
        val prediction = s.livePrediction
        if (prediction == null || prediction.id != eventId || prediction.selfOutcomeId != null) return
        val token = moderationAuthStore.resolveToken(s.currentAccessToken)
        update { st ->
            val p = st.livePrediction ?: return@update st
            if (p.id != eventId || p.selfOutcomeId != null) return@update st
            st.copy(
                livePrediction = p.copy(
                    outcomes = p.outcomes.map { o ->
                        if (o.id == outcomeId) o.copy(channelPoints = o.channelPoints + points, users = o.users + 1) else o
                    },
                    selfOutcomeId = outcomeId,
                    selfPoints = points
                )
            )
        }
        viewModelScope.launch {
            val r = twitchGqlClient.makePredictionGql(eventId, outcomeId, points, token)
            if (r.isError) {
                sendEffect(ChatEffect.ShowError("Failed to place prediction: ${(r as io.rudione.chatone.util.Result.Error).exception.message}"))
            }
        }
    }

    private fun applyLivePoll(channelId: String, poll: io.rudione.chatone.data.remote.dto.PollData?) {
        update { st ->
            if (st.channelId != channelId) return@update st
            if (poll == null) return@update st.copy(livePoll = null, activePollId = null)
            val previousVote = st.livePoll?.takeIf { it.id == poll.id }?.selfVoteChoiceId
            st.copy(
                livePoll = poll.copy(selfVoteChoiceId = poll.selfVoteChoiceId ?: previousVote),
                activePollId = poll.id
            )
        }
        if (poll != null && !poll.status.equals("ACTIVE", ignoreCase = true)) pollPollingJob?.cancel()
    }

    private fun applyLivePrediction(
        channelId: String,
        prediction: io.rudione.chatone.data.remote.dto.PredictionData?
    ) {
        update { st ->
            if (st.channelId != channelId) return@update st
            if (prediction == null) return@update st.copy(
                livePrediction = null,
                activePredictionId = null,
                activePredictionOutcomes = emptyList()
            )
            val previous = st.livePrediction?.takeIf { it.id == prediction.id }
            st.copy(
                livePrediction = prediction.copy(
                    selfOutcomeId = prediction.selfOutcomeId ?: previous?.selfOutcomeId,
                    selfPoints = prediction.selfPoints ?: previous?.selfPoints
                ),
                activePredictionId = prediction.id,
                activePredictionOutcomes = prediction.outcomes.map { o -> o.id to o.title }
            )
        }
        if (prediction != null && prediction.status != "ACTIVE" && prediction.status != "LOCKED") {
            predictionPollingJob?.cancel()
        }
    }

    private fun openPollCreation() {
        update { it.copy(showPollCreation = true) }
        val s = state.value
        if (s.channelLogin.isNotEmpty() && s.channelId.isNotEmpty()) {
            fetchCurrentPoll(s.channelLogin, s.channelId)
        }
    }

    private fun openPredictionCreation() {
        update { it.copy(showPredictionCreation = true) }
        val s = state.value
        if (s.channelLogin.isNotEmpty() && s.channelId.isNotEmpty()) {
            fetchCurrentPrediction(s.channelLogin, s.channelId)
        }
    }

    private fun resolvePrediction(eventId: String, outcomeId: String) {
        val s = state.value
        if (!s.canModerate) {
            sendEffect(ChatEffect.ShowError("Mod-only command")); return
        }
        viewModelScope.launch {
            if (moderationAuthStore.hasCustomToken()) {
                val token = moderationAuthStore.resolveToken(s.currentAccessToken)
                val r = twitchGqlClient.resolvePredictionGql(eventId, outcomeId, token)
                if (r.isError) {
                    sendEffect(ChatEffect.ShowError("Failed to complete prediction"))
                    return@launch
                }
            } else {
                if (!s.isBroadcaster) {
                    sendEffect(ChatEffect.ShowError(GQL_TOKEN_REQUIRED_MSG)); return@launch
                }
                val r = apiClient.endPrediction(
                    s.currentAccessToken, s.channelId, eventId, "RESOLVED", outcomeId
                )
                if (r.isError) {
                    sendEffect(ChatEffect.ShowError("Failed to complete prediction"))
                    return@launch
                }
            }
            predictionPollingJob?.cancel()
            update {
                if (it.channelId != s.channelId) it
                else it.copy(
                    livePrediction = null,
                    activePredictionId = null,
                    activePredictionOutcomes = emptyList()
                )
            }
        }
    }

    private fun lockPrediction(eventId: String) {
        val s = state.value
        if (!s.canModerate) {
            sendEffect(ChatEffect.ShowError("Mod-only command")); return
        }
        viewModelScope.launch {
            val failed = if (moderationAuthStore.hasCustomToken()) {
                val token = moderationAuthStore.resolveToken(s.currentAccessToken)
                twitchGqlClient.lockPredictionGql(eventId, token).isError
            } else {
                if (!s.isBroadcaster) {
                    sendEffect(ChatEffect.ShowError(GQL_TOKEN_REQUIRED_MSG)); return@launch
                }
                apiClient.endPrediction(s.currentAccessToken, s.channelId, eventId, "LOCKED").isError
            }
            if (failed) sendEffect(ChatEffect.ShowError("Failed to lock prediction"))
            else update {
                val p = it.livePrediction
                if (it.channelId != s.channelId || p == null || p.id != eventId) it
                else it.copy(livePrediction = p.copy(status = "LOCKED"))
            }
        }
    }

    private val channelMessageCache = mutableMapOf<String, List<DisplayMessage>>()
    private val channelRoomStateCache = mutableMapOf<String, RoomState>()
    private val channelIdCache = mutableMapOf<String, String>()
    private val channelModCache = mutableMapOf<String, Boolean>()
    private val channelDisplayNameCache = mutableMapOf<String, String>()
    private var currentUserBadgeRaw: String = ""

    private var retokenizeDebounceJob: kotlinx.coroutines.Job? = null
    private fun scheduleRetokenize(delayMs: Long = 150L) {
        retokenizeDebounceJob?.cancel()
        retokenizeDebounceJob = viewModelScope.launch {
            delay(delayMs)
            retokenizeMessages()
        }
    }

    private val pendingRetokenizeUsers = mutableSetOf<String>()
    private val pendingRetokenizeLock = SynchronizedObject()
    private var retokenizeUsersJob: kotlinx.coroutines.Job? = null
    private fun scheduleRetokenizeForUsers(userIds: Collection<String>, delayMs: Long = 250L) {
        if (userIds.isEmpty()) return
        synchronized(pendingRetokenizeLock) {
            pendingRetokenizeUsers.addAll(userIds)
            if (retokenizeUsersJob?.isActive == true) return
            retokenizeUsersJob = viewModelScope.launch {
                delay(delayMs)
                val snapshot = synchronized(pendingRetokenizeLock) {
                    val s = pendingRetokenizeUsers.toSet()
                    pendingRetokenizeUsers.clear()
                    s
                }
                if (snapshot.isNotEmpty()) retokenizeMessagesForUsers(snapshot)
            }
        }
    }

    private val isSendingMessage = atomic(false)
    private var lastMessageSentAt: Long = 0L
    private var lastSentBaseText: String = ""
    private var duplicateSuffixToggle: Boolean = false

    private fun modCacheKey(channel: String) = "mod_status_${channel.lowercase()}"
    private fun readPersistedMod(channel: String): Boolean =
        persistedSettings.getBoolean(modCacheKey(channel), false)

    private fun writePersistedMod(channel: String, isMod: Boolean) {
        persistedSettings.putBoolean(modCacheKey(channel), isMod)
    }

    @Volatile
    private var moderatedChannelIds: Set<String>? = null

    @Volatile
    private var moderatedChannelsUserId: String = ""

    private data class HighlightMatch(val color: Long, val playSound: Boolean)

    private fun containsUserMention(text: String, name: String): Boolean {
        if (name.isEmpty()) return false
        var index = text.indexOf(name, ignoreCase = true)
        while (index >= 0) {
            val before = text.getOrNull(index - 1)
            val after = text.getOrNull(index + name.length)
            val startOk = before == null || before == '@' || !before.isLetterOrDigit() && before != '_'
            val endOk = after == null || !after.isLetterOrDigit() && after != '_'
            if (startOk && endOk) return true
            index = text.indexOf(name, startIndex = index + 1, ignoreCase = true)
        }
        return false
    }

    private suspend fun checkHighlightRules(
        message: ChatMessage,
        currentUserLogin: String
    ): HighlightMatch? = checkHighlightRules(
        messageText = message.message,
        currentUserLogin = currentUserLogin,
        replyParentLogin = message.replyParentUserLogin,
        replyParentDisplayName = message.replyParentDisplayName
    )

    private suspend fun checkHighlightRules(
        messageText: String,
        currentUserLogin: String,
        replyParentLogin: String? = null,
        replyParentDisplayName: String? = null
    ): HighlightMatch? {
        val settings = cachedSettings()
        val rules = settings.highlightRules.filter { it.enabled }

        val stateLogin = state.value.currentUserLogin
        val stateDisplay = state.value.currentDisplayName
        val effectiveLogin = when {
            currentUserLogin.isNotEmpty() -> currentUserLogin.lowercase()
            stateLogin.isNotEmpty() -> stateLogin.lowercase()
            stateDisplay.isNotEmpty() -> stateDisplay.lowercase()
            else -> ""
        }

        val effectiveDisplay = when {
            stateDisplay.isNotEmpty() -> stateDisplay.lowercase()
            stateLogin.isNotEmpty() -> stateLogin.lowercase()
            currentUserLogin.isNotEmpty() -> currentUserLogin.lowercase()
            else -> ""
        }

        if (effectiveLogin.isNotEmpty() || effectiveDisplay.isNotEmpty()) {

            val repliedToMe = listOfNotNull(replyParentLogin, replyParentDisplayName).any { parent ->
                val normalized = parent.lowercase()
                (effectiveLogin.isNotEmpty() && normalized == effectiveLogin) ||
                        (effectiveDisplay.isNotEmpty() && normalized == effectiveDisplay)
            }

            val loginMatch = effectiveLogin.isNotEmpty() &&
                    containsUserMention(messageText, effectiveLogin)
            val displayMatch = effectiveDisplay.isNotEmpty() &&
                    effectiveDisplay != effectiveLogin &&
                    containsUserMention(messageText, effectiveDisplay)

            if (repliedToMe || loginMatch || displayMatch) {
                val usernameRule = rules.firstOrNull { it.id == "username" }
                return HighlightMatch(
                    color = usernameRule?.color ?: 0xFFFF6B6BL,
                    playSound = settings.mentionSoundEnabled && (usernameRule?.playSound ?: true)
                )
            }
        }

        if (effectiveLogin.isEmpty() && effectiveDisplay.isEmpty()) return null

        for (rule in rules) {
            val pattern = when (rule.id) {
                "username" -> effectiveLogin
                "whispers", "subscriptions", "first_message" -> continue
                else -> rule.pattern
            }

            if (pattern.isEmpty()) continue

            val matches = if (rule.isRegex) {
                RegexCache.regex(pattern, ignoreCase = !rule.caseSensitive)
                    ?.containsMatchIn(messageText) ?: false
            } else if (rule.matchSubstring) {
                messageText.contains(pattern, ignoreCase = !rule.caseSensitive)
            } else {
                RegexCache.wholeWord(pattern, ignoreCase = !rule.caseSensitive)
                    ?.containsMatchIn(messageText)
                    ?: messageText.equals(pattern, ignoreCase = !rule.caseSensitive)
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
        observePubSubEvents()
        observeConnectionState()
        observeEmoteSetUpdates()
        observePersonalEmoteGrants()
        viewModelScope.launch {
            SettingsViewModel.changeBroadcast.collect { _ -> invalidateSettingsCache() }
        }
        observeAiSnapshotDemand()
        observeAiAutoMod()
    }

    private val aiScannedIds = HashSet<String>()

    private fun observeAiAutoMod() {
        viewModelScope.launch {
            aiAssistantController.autoMod.collectLatest { cfg ->
                if (!cfg.enabled) return@collectLatest
                while (true) {
                    val hour = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault()).hour
                    if (aiAssistantController.enabled.value && aiAssistantController.autoModActiveAt(hour)) {
                        runCatching { runAiAutoModScan(cfg) }
                    }
                    kotlinx.coroutines.delay(40_000)
                }
            }
        }
    }

    private suspend fun runAiAutoModScan(
        cfg: io.rudione.chatone.data.repository.AiAssistantController.AutoModConfig
    ) {
        val s = state.value
        if (s.channelLogin.isEmpty()) return
        val fresh = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
            .filter { !it.isDeleted && !it.isModerator && !it.isBroadcaster && it.id !in aiScannedIds }
            .takeLast(40)
        if (fresh.size < 3) return
        fresh.forEach { aiScannedIds.add(it.id) }
        if (aiScannedIds.size > 4000) aiScannedIds.clear()

        val numbered = fresh.mapIndexed { i, m ->
            "${i + 1}. ${m.displayName.ifBlank { m.username }}: ${plainTextOf(m)}"
        }.joinToString("\n")
        val sens = when (cfg.sensitivity) {
            1 -> "only clearly severe content (slurs, threats, scams, coordinated raid spam)"
            3 -> "anything borderline, suspicious, or mildly toxic"
            else -> "clearly toxic, harmful, hateful, scam, or raid content"
        }
        val prompt = buildString {
            append("You are a Twitch chat safety filter. From the numbered messages, return ONLY a ")
            append("JSON array of the message numbers that are dangerous ($sens). ")
            if (cfg.extraInstructions.isNotBlank()) append(cfg.extraInstructions).append(" ")
            append("Return [] if none.\n\n")
            append(numbered)
        }
        val cf = aiAssistantController.config.value
        val r = aiAssistantClient.complete(
            baseUrl = cf.baseUrl,
            model = cf.model,
            messages = listOf(
                io.rudione.chatone.data.remote.AiChatMessage(
                    io.rudione.chatone.data.remote.AiChatMessage.SYSTEM,
                    "Respond with a JSON array of integers only, no prose."
                ),
                io.rudione.chatone.data.remote.AiChatMessage(
                    io.rudione.chatone.data.remote.AiChatMessage.USER, prompt
                )
            ),
            temperature = 0.0,
            apiKey = cf.apiKey
        )
        if (r !is io.rudione.chatone.util.Result.Success) return
        val nums = Regex("\\d+").findAll(r.data)
            .mapNotNull { it.value.toIntOrNull() }
            .filter { it in 1..fresh.size }
            .toSet()
        if (nums.isEmpty()) return
        val flaggedIds = nums.mapNotNull { fresh.getOrNull(it - 1)?.id }.toSet()
        if (flaggedIds.isEmpty()) return
        update { st ->
            st.copy(messages = st.messages.map { dm ->
                if (dm is DisplayMessage.PrivMsg && dm.id in flaggedIds && !dm.isHighlighted)
                    dm.copy(isHighlighted = true) else dm
            })
        }
        val strings = AppStrings.forLocale(cachedSettings().language)
        systemNotice(s.channelLogin, strings.aiFlaggedMessagesNotice.replace("{0}", flaggedIds.size.toString()))
    }

    private fun plainTextOf(m: DisplayMessage.PrivMsg): String = m.rawMessage?.message
        ?: m.tokens.joinToString("") { t ->
            when (t) {
                is MessageToken.Text -> t.text
                is MessageToken.TwitchEmoteToken -> t.name
                is MessageToken.ThirdPartyEmoteToken -> t.emote.code
                is MessageToken.Link -> t.displayText
                is MessageToken.Mention -> "@${t.username}"
                is MessageToken.Cheer -> "${t.prefix}${t.amount}"
            }
        }

    private fun observeAiSnapshotDemand() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                aiAssistantController.isOpen,
                aiAssistantController.autoMod
            ) { open, am -> open || am.enabled }.collectLatest { active ->
                while (active) {
                    publishAiSnapshot(state.value)
                    kotlinx.coroutines.delay(1500)
                }
            }
        }
    }

    private var lastAiSnapshotMessages: List<DisplayMessage>? = null

    private fun publishAiSnapshot(s: ChatState) {
        if (s.channelLogin.isEmpty()) return
        if (lastAiSnapshotMessages === s.messages) return
        lastAiSnapshotMessages = s.messages
        fun textOf(m: DisplayMessage.PrivMsg): String = m.rawMessage?.message
            ?: m.tokens.joinToString("") { t ->
                when (t) {
                    is MessageToken.Text -> t.text
                    is MessageToken.TwitchEmoteToken -> t.name
                    is MessageToken.ThirdPartyEmoteToken -> t.emote.code
                    is MessageToken.Link -> t.displayText
                    is MessageToken.Mention -> "@${t.username}"
                    is MessageToken.Cheer -> "${t.prefix}${t.amount}"
                }
            }
        val privs = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
        val lines = privs.map {
            io.rudione.chatone.data.repository.AiChatLine(
                s.channelLogin, it.displayName.ifBlank { it.username }, textOf(it), it.timestamp,
                it.username.lowercase()
            )
        }
        val mentions = lines.filterIndexed { index, _ -> privs[index].isMention }
        aiAssistantController.publishSnapshot(
            io.rudione.chatone.data.repository.AiChatSnapshot(
                activeChannel = s.channelLogin,
                openChannels = listOf(s.channelLogin),
                recentMessages = lines,
                mentions = mentions
            )
        )
    }

    override suspend fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnInit -> initChannel(
                event.channelLogin,
                event.accessToken,
                event.userId,
                event.userLogin,
                event.userDisplayName
            )

            is ChatEvent.OnMessageInputChanged -> updateMessageInput(event.input)
            ChatEvent.OnSendMessage -> sendMessage(keepText = false)
            ChatEvent.OnSendMessageKeepText -> sendMessage(keepText = true)
            ChatEvent.OnHistoryUp -> navigateHistory(up = true)
            ChatEvent.OnHistoryDown -> navigateHistory(up = false)
            ChatEvent.OnReconnect -> reconnect()
            ChatEvent.OnToggleModMode -> toggleModMode()
            is ChatEvent.OnTimeoutUser -> timeoutUser(event.userId, event.duration)
            is ChatEvent.OnBanUser -> banUser(event.userId, event.reason)
            is ChatEvent.OnUnbanUser -> unbanUser(event.userId)
            is ChatEvent.OnDeleteMessage -> deleteMessage(event.messageId)
            is ChatEvent.OnWhisper -> whisperUser(event.username)
            is ChatEvent.OnInsertMention -> insertMention(event.displayName)
            is ChatEvent.OnModUser -> modUser(event.userId)
            is ChatEvent.OnUnmodUser -> unmodUser(event.userId)
            is ChatEvent.OnVipUser -> vipUser(event.userId)
            is ChatEvent.OnUnvipUser -> unvipUser(event.userId)
            is ChatEvent.OnSelectEmoteCompletion -> selectEmoteCompletion(event.emote)
            ChatEvent.OnDismissCompletions -> update {
                it.copy(
                    showEmoteCompletions = false,
                    emoteCompletions = emptyList()
                )
            }

            is ChatEvent.OnSelectMentionCompletion -> selectMentionCompletion(event.username)
            ChatEvent.OnDismissMentionCompletions -> update {
                it.copy(
                    showMentionCompletions = false,
                    mentionCompletions = emptyList()
                )
            }

            is ChatEvent.OnUpdateChatSettings -> updateChatSettings(event.settings)
            ChatEvent.OnClearChat -> clearChat()
            is ChatEvent.OnScrollbackPinned -> setScrollbackPinned(event.pinned)
            is ChatEvent.OnReplyToMessage -> update {
                it.copy(
                    replyingTo = event.message,
                    messageInput = ""
                )
            }

            ChatEvent.OnCancelReply -> update { it.copy(replyingTo = null) }
            is ChatEvent.OnPinMessage -> pinMessage(event.messageId)
            ChatEvent.OnUnpinMessage -> unpinMessageRemote()
            ChatEvent.OnOpenPollCreation -> openPollCreation()
            ChatEvent.OnClosePollCreation -> update { it.copy(showPollCreation = false) }
            is ChatEvent.OnCreatePoll -> submitPollFromForm(event.title, event.choices, event.durationSeconds)
            ChatEvent.OnOpenPredictionCreation -> openPredictionCreation()
            ChatEvent.OnClosePredictionCreation -> update { it.copy(showPredictionCreation = false) }
            is ChatEvent.OnCreatePrediction -> submitPredictionFromForm(event.title, event.outcomes, event.windowSeconds)
            is ChatEvent.OnVotePoll -> votePoll(event.pollId, event.choiceId)
            is ChatEvent.OnPlacePrediction -> placePrediction(event.eventId, event.outcomeId, event.points)
            is ChatEvent.OnResolvePrediction -> resolvePrediction(event.eventId, event.outcomeId)
            is ChatEvent.OnLockPrediction -> lockPrediction(event.eventId)
            ChatEvent.OnOpenPointsBitsPanel -> openPointsBitsPanel()
            ChatEvent.OnClosePointsBitsPanel -> update { it.copy(showPointsBitsPanel = false) }
            ChatEvent.OnToggleHidePin -> update { it.copy(pinLocallyHidden = !it.pinLocallyHidden) }
            is ChatEvent.OnHideEventBanner -> update {
                val eventId = when (event.key) {
                    "poll" -> it.livePoll?.id
                    "prediction" -> it.livePrediction?.id
                    else -> null
                }
                it.copy(
                    pinLocallyHidden = it.pinLocallyHidden || event.key == "pin",
                    hiddenEventIds = if (eventId == null) it.hiddenEventIds
                    else (it.hiddenEventIds + eventId).toList().takeLast(HIDDEN_EVENTS_MEMORY).toSet()
                )
            }

            ChatEvent.OnRestoreHiddenBanners -> update {
                it.copy(
                    pinLocallyHidden = false,
                    hiddenEventIds = it.hiddenEventIds -
                            setOfNotNull(it.livePoll?.id, it.livePrediction?.id)
                )
            }

            is ChatEvent.OnRedeemReward -> redeemReward(event.reward, event.textInput)
            is ChatEvent.OnSendAnnouncement -> sendAnnouncement(event.message, event.color)
            is ChatEvent.OnStartRaid -> startRaid(event.targetLogin)
            ChatEvent.OnCancelRaid -> cancelRaid()
            ChatEvent.OnRaidNow -> raidNow()
            is ChatEvent.OnSendShoutout -> sendShoutout(event.targetUserId)
            is ChatEvent.OnSendMessageText -> sendRawMessage(event.text)
            is ChatEvent.OnExecuteMacro -> executeMacro(event.macro)
            is ChatEvent.OnAllowAutoModMessage -> handleAutoMod(event.msgId, "ALLOW")
            is ChatEvent.OnDenyAutoModMessage -> handleAutoMod(event.msgId, "DENY")
            is ChatEvent.OnBlockUser -> handleBlockUser(event.targetUserId, event.targetLogin)
            is ChatEvent.OnUnblockUser -> handleUnblockUser(event.targetUserId, event.targetLogin)
            is ChatEvent.OnSetShowBlockedMode -> update { it.copy(showBlockedMode = event.mode) }
            ChatEvent.OnToggleEmotePicker -> {
                update { state ->
                    state.copy(
                        isEmotePickerVisible = !state.isEmotePickerVisible,
                        showEmoteCompletions = false,
                        showMentionCompletions = false
                    )
                }
            }

            ChatEvent.OnRefreshChannel -> refreshChannel()

            is ChatEvent.OnFilesDropped -> handleFilesDropped(event.paths)
            ChatEvent.OnConfirmPendingUpload -> {
                val path = state.value.pendingUploadPath
                update { it.copy(pendingUploadPath = null) }
                if (path != null) startUpload(path)
            }

            ChatEvent.OnCancelPendingUpload -> update { it.copy(pendingUploadPath = null) }
            ChatEvent.OnClearUploadedLink -> update {
                it.copy(uploadedLink = null, uploadError = null)
            }
        }
    }

    private fun handleFilesDropped(paths: List<String>) {
        val config = SettingsViewModel.loadInitialState().imageUploader
        if (!config.isUsable) return
        if (state.value.uploadProgress != null) return
        val path = paths.firstOrNull {
            ImageUploaderClient.isSupportedFile(it.substringAfterLast('/').substringAfterLast('\\'))
        }
        if (path == null) {
            update { it.copy(uploadError = "unsupported") }
            return
        }
        if (config.askOnUpload) {
            update { it.copy(pendingUploadPath = path) }
        } else {
            startUpload(path)
        }
    }

    private fun startUpload(path: String) {
        val config = SettingsViewModel.loadInitialState().imageUploader
        if (!config.isUsable) return
        val fileName = path.substringAfterLast('/').substringAfterLast('\\')
        viewModelScope.launch {
            update {
                it.copy(uploadProgress = 0f, uploadedLink = null, uploadError = null)
            }
            val bytes = readLocalFileBytes(path)
            if (bytes == null || bytes.isEmpty()) {
                update { it.copy(uploadProgress = null, uploadError = "read failed") }
                return@launch
            }
            var lastReported = 0f
            val result = imageUploaderClient.upload(config, fileName, bytes) { progress ->
                if (progress - lastReported >= 0.02f || progress >= 1f) {
                    lastReported = progress
                    update { it.copy(uploadProgress = progress) }
                }
            }
            result.onSuccess { uploaded ->
                Napier.d("Upload ready: ${uploaded.link}", tag = TAG)
                update { it.copy(uploadProgress = null, uploadedLink = uploaded.link) }
            }.onFailure { e ->
                Napier.w("Upload failed: ${e.message}", tag = TAG)
                update {
                    it.copy(uploadProgress = null, uploadError = e.message ?: "error")
                }
            }
        }
    }

    private fun refreshChannel() {
        val s = state.value
        val channelLogin = s.channelLogin
        val channelId = s.channelId
        if (channelLogin.isEmpty()) return

        invalidateSettingsCache()

        viewModelScope.launch {
            emoteRepository.invalidateChannel(channelLogin)
            emoteRepository.invalidatePersonalEmotes()
            AutomodEngine.invalidate()
            ChatRuleEngine.invalidate()

            launch { emoteRepository.loadGlobalEmotes() }

            if (channelId.isNotEmpty()) {
                launch { loadChannelEmotesAndBadges(channelId) }
            }

            launch {
                delay(300)
                retokenizeMessages()
            }

            launch { loadRecentMessages(channelLogin) }

            sendEffect(ChatEffect.ScrollToBottom)
        }
    }

    private fun handleAutoMod(msgId: String, action: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.currentUserId.isEmpty()) return
        viewModelScope.launch {

            val newStatus =
                if (action == "ALLOW") DisplayMessage.AutoModMsg.AutoModStatus.ALLOWED else DisplayMessage.AutoModMsg.AutoModStatus.DENIED
            update { state ->
                state.copy(messages = state.messages.map { dm ->
                    if (dm is DisplayMessage.AutoModMsg && dm.msgId == msgId) dm.copy(status = newStatus) else dm
                })
            }

            val result =
                apiClient.manageAutoModMessage(s.currentAccessToken, s.currentUserId, msgId, action)
            if (result is io.rudione.chatone.util.Result.Error) {
                sendEffect(ChatEffect.ShowError("AutoMod action failed: ${result.exception.message}"))
            }

            kotlinx.coroutines.delay(800L)
            update { state ->
                state.copy(messages = state.messages.filter { dm ->
                    !(dm is DisplayMessage.AutoModMsg && dm.msgId == msgId)
                })
            }
        }
    }

    private fun handleBlockUser(targetUserId: String, targetLogin: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.blockUser(s.currentAccessToken, targetUserId)
            if (result is io.rudione.chatone.util.Result.Success) {
                update { it.copy(blockedUserIds = it.blockedUserIds + targetUserId) }
                systemNotice(s.channelLogin, "Blocked $targetLogin")
            } else {
                sendEffect(ChatEffect.ShowError("Failed to block $targetLogin"))
            }
        }
    }

    private fun handleUnblockUser(targetUserId: String, targetLogin: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.unblockUser(s.currentAccessToken, targetUserId)
            if (result is io.rudione.chatone.util.Result.Success) {
                update { it.copy(blockedUserIds = it.blockedUserIds - targetUserId) }
                systemNotice(s.channelLogin, "Unblocked $targetLogin")
            } else {
                sendEffect(ChatEffect.ShowError("Failed to unblock $targetLogin"))
            }
        }
    }

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
                val parsed = SlashCommand.parse(text)
                if (parsed != null) {
                    runSlashCommand(parsed, s.channelLogin, s)
                    return@launch
                }
                sendMessageUseCase(s.channelLogin, text)
            } catch (e: Exception) {
                sendEffect(ChatEffect.ShowError("Failed to send: ${e.message}"))
            }
        }
    }

    private fun initChannel(
        channelLogin: String,
        accessToken: String,
        userId: String,
        userLogin: String,
        userDisplayName: String
    ) {
        val oldState = state.value
        val oldChannel = oldState.channelLogin
        val key = channelLogin.lowercase()

        if (oldChannel.isNotEmpty() && oldChannel != channelLogin) {
            val oldKey = oldChannel.lowercase()
            channelMessageCache[oldKey] = oldState.messages
            channelRoomStateCache[oldKey] = oldState.roomState
            if (oldState.channelId.isNotEmpty()) channelIdCache[oldKey] = oldState.channelId
            channelModCache[oldKey] = oldState.isMod
            if (oldState.channelDisplayName.isNotBlank()) {
                channelDisplayNameCache[oldKey] = oldState.channelDisplayName
            }
            if (channelMessageCache.size > MAX_CACHED_CHANNELS) {
                val oldest = channelMessageCache.keys.first()
                channelMessageCache.remove(oldest)
                channelRoomStateCache.remove(oldest)
                channelIdCache.remove(oldest)
                channelModCache.remove(oldest)
            }

            currentUserBadgeRaw = ""
        }

        val cachedMessages = run {
            val seen = mutableSetOf<String>()
            (channelMessageCache[key] ?: emptyList()).filter { seen.add(it.id) }
        }
        messageIdSet.clear()
        cachedMessages.mapTo(messageIdSet) { it.id }
        scrollbackPinned = false
        val cachedRoomState = channelRoomStateCache[key] ?: RoomState()
        val cachedChannelId = channelIdCache[key] ?: ""

        val cachedIsMod = channelModCache[key] ?: readPersistedMod(key)

        val effectiveUserLogin =
            (if (userLogin.isNotEmpty()) userLogin else oldState.currentUserLogin).lowercase()
        val isBroadcaster = effectiveUserLogin.isNotEmpty() && effectiveUserLogin == key

        pollPollingJob?.cancel()
        pollPollingJob = null
        predictionPollingJob?.cancel()
        predictionPollingJob = null

        update {
            it.copy(
                channelLogin = channelLogin,
                channelId = cachedChannelId,

                channelDisplayName = channelDisplayNameCache[key] ?: "",
                liveStream = null,
                messages = cachedMessages,

                messagesSeq = it.messagesSeq + if (cachedMessages.isNotEmpty()) 1 else 0,
                messagesStartOrdinal = 0L,
                roomState = cachedRoomState,
                isMod = cachedIsMod,
                isBroadcaster = isBroadcaster,
                isLoading = cachedMessages.isEmpty(),
                modModeEnabled = false,
                emoteCompletions = emptyList(),
                showEmoteCompletions = false,
                mentionCompletions = emptyList(),
                showMentionCompletions = false,
                mentionCount = 0,
                replyingTo = null,
                pinnedMessage = null,
                pinId = null,
                pinEndsAtMs = null,
                pinnedByName = null,
                pinnedByBadges = emptyList(),
                pinLocallyHidden = false,
                livePoll = null,
                livePrediction = null,
                activePollId = null,
                activePredictionId = null,
                activePredictionOutcomes = emptyList(),
                pointsBalance = 0L,
                channelRewards = emptyList(),
                pointsIconUrl = null,
                isBanned = false,
                banReason = "",
                currentAccessToken = accessToken.ifEmpty { it.currentAccessToken },
                currentUserId = userId.ifEmpty { it.currentUserId },
                currentUserLogin = userLogin.ifEmpty { it.currentUserLogin },
                currentDisplayName = userDisplayName.ifEmpty { it.currentDisplayName },
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
                                    channelIdCache[channelLogin.lowercase()] = user.id
                                    if (user.displayName.isNotBlank()) {
                                        channelDisplayNameCache[channelLogin.lowercase()] = user.displayName
                                    }
                                    update { st ->
                                        if (st.channelLogin == channelLogin) st.copy(
                                            channelId = user.id,
                                            channelDisplayName = user.displayName
                                                .ifBlank { st.channelDisplayName }
                                        ) else st
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Napier.w("Failed to resolve channelId: ${e.message}", tag = TAG)
                        }
                    }
                }
                launch { emoteRepository.loadGlobalEmotes(); retokenizeMessages() }
                launch { loadBadgesWithToken(); retokenizeMessages() }
                launch {
                    val s2 = state.value
                    if (s2.currentUserColor.isBlank() && s2.currentAccessToken.isNotEmpty() && s2.currentUserId.isNotEmpty()) {
                        runCatching {
                            val r =
                                apiClient.getUserChatColor(s2.currentAccessToken, s2.currentUserId)
                            if (r is io.rudione.chatone.util.Result.Success) {
                                r.data?.let { color ->
                                    update { it.copy(currentUserColor = color) }
                                }
                            }
                        }
                    }
                }
                if (resolvedChannelId.isNotEmpty()) {
                    launch { loadChannelEmotesAndBadges(resolvedChannelId) }

                    launch { checkAndSetModStatus(resolvedChannelId) }
                    launch { fetchCurrentPin(resolvedChannelId) }
                    launch { fetchCurrentPoll(channelLogin, resolvedChannelId) }
                    launch { fetchCurrentPrediction(channelLogin, resolvedChannelId) }
                    launch {
                        val s2 = state.value
                        if (s2.currentAccessToken.isNotEmpty() && s2.currentUserId.isNotEmpty()) {
                            pubSubClient.connect(
                                accessToken = s2.currentAccessToken,
                                userId = s2.currentUserId,
                                channelId = resolvedChannelId,
                                isMod = s2.isMod || s2.isBroadcaster
                            )
                            if (s2.isMod || s2.isBroadcaster) {
                                eventSubClient.connect(
                                    accessToken = s2.currentAccessToken,
                                    userId = s2.currentUserId,
                                    channelId = resolvedChannelId
                                )
                            }
                        }
                    }
                }
                launch { loadRecentMessages(channelLogin) }

                launch {
                    val s2 = state.value
                    if (s2.currentAccessToken.isNotEmpty() && s2.currentUserId.isNotEmpty()
                        && s2.blockedUserIds.isEmpty()
                    ) {
                        runCatching {
                            val r = apiClient.getBlockedUsers(
                                s2.currentAccessToken, s2.currentUserId, first = 100
                            )
                            if (r is io.rudione.chatone.util.Result.Success) {
                                val ids = r.data.data.map { it.userId }.toSet()
                                update { it.copy(blockedUserIds = ids) }
                            }
                        }.onFailure {
                            Napier.w("Failed to load blocked users: ${it.message}", tag = TAG)
                        }
                    }
                }

                launch {
                    val s2 = state.value
                    if (s2.currentAccessToken.isNotEmpty() && s2.currentUserId.isNotEmpty()
                        && s2.twitchSubscriberEmotes.isEmpty()
                    ) {
                        runCatching {
                            val rawEmotes = mutableListOf<io.rudione.chatone.data.remote.dto.UserEmoteData>()
                            var template = ""
                            var cursor: String? = null
                            var pages = 0
                            do {
                                val r = apiClient.getUserEmotes(
                                    s2.currentAccessToken, s2.currentUserId, after = cursor
                                )
                                if (r !is io.rudione.chatone.util.Result.Success) break
                                if (template.isEmpty()) template = r.data.template
                                rawEmotes += r.data.data
                                cursor = r.data.pagination?.cursor?.takeIf { it.isNotEmpty() }
                                pages++
                            } while (cursor != null && pages < 20)

                            if (rawEmotes.isNotEmpty()) {
                                val ownerIds = rawEmotes
                                    .mapNotNull { it.ownerId.takeIf { id -> id.isNotBlank() } }
                                    .distinct()
                                val ownerNames = mutableMapOf<String, String>()
                                ownerIds.chunked(100).forEach { chunk ->
                                    val ur = apiClient.getUsers(s2.currentAccessToken, ids = chunk)
                                    if (ur is io.rudione.chatone.util.Result.Success)
                                        ur.data.data.forEach { ownerNames[it.id] = it.displayName }
                                }
                                val emotes = rawEmotes.map { e ->
                                    buildTwitchEmote(e, template, ownerNames[e.ownerId] ?: "")
                                }
                                update { it.copy(twitchSubscriberEmotes = emotes) }
                                Napier.d("Loaded ${emotes.size} Twitch subscriber emotes", tag = TAG)
                            }
                        }.onFailure {
                            Napier.w(
                                "Failed to load Twitch subscriber emotes: ${it.message}",
                                tag = TAG
                            )
                        }
                    }
                }

                launch {
                    val s2 = state.value
                    if (s2.currentAccessToken.isNotEmpty() && s2.channelId.isNotEmpty()
                        && s2.twitchChannelEmotes.isEmpty()
                    ) {
                        runCatching {
                            val r = apiClient.getChannelTwitchEmotes(s2.currentAccessToken, s2.channelId)
                            if (r is io.rudione.chatone.util.Result.Success) {
                                val author = s2.channelDisplayName.ifBlank { s2.channelLogin }
                                val emotes = r.data.data.map { buildTwitchEmote(it, r.data.template, author) }
                                if (emotes.isNotEmpty()) update { it.copy(twitchChannelEmotes = emotes) }
                            }
                        }.onFailure {
                            Napier.w("Failed to load Twitch channel emotes: ${it.message}", tag = TAG)
                        }
                    }
                }

                launch {
                    val s2 = state.value
                    if (s2.currentAccessToken.isNotEmpty() && s2.twitchGlobalEmotes.isEmpty()) {
                        runCatching {
                            val r = apiClient.getGlobalTwitchEmotes(s2.currentAccessToken)
                            if (r is io.rudione.chatone.util.Result.Success) {
                                val emotes = r.data.data.map { buildTwitchEmote(it, r.data.template, "Twitch") }
                                if (emotes.isNotEmpty()) update { it.copy(twitchGlobalEmotes = emotes) }
                            }
                        }.onFailure {
                            Napier.w("Failed to load Twitch global emotes: ${it.message}", tag = TAG)
                        }
                    }
                }

                streamStatePoller?.cancel()
                streamStatePoller = launch { pollStreamState(channelLogin) }
                automationController.start()
                update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Napier.e("Failed to initialize channel: ${e.message}", e, tag = TAG)
                update { it.copy(isLoading = false) }
                sendEffect(ChatEffect.ShowError("Failed to join channel: ${e.message}"))
            }
        }
    }

    private suspend fun ensureModeratedChannelsFetched(force: Boolean = false): Set<String>? {
        val s = state.value
        val token = s.currentAccessToken
        val userId = s.currentUserId
        if (token.isEmpty() || userId.isEmpty()) return null
        val cached = moderatedChannelIds
        if (!force && cached != null && moderatedChannelsUserId == userId) return cached
        val result = apiClient.getModeratedChannels(token, userId)
        return if (result is io.rudione.chatone.util.Result.Success) {
            moderatedChannelIds = result.data
            moderatedChannelsUserId = userId
            Napier.d("Fetched moderated channels: ${result.data.size}", tag = TAG)
            result.data
        } else {
            Napier.w(
                "Failed to fetch moderated channels (missing scope? re-auth may be required)",
                tag = TAG
            )
            null
        }
    }

    private suspend fun checkAndSetModStatus(channelId: String) {
        val s = state.value
        val userId = s.currentUserId
        val channelLogin = s.channelLogin
        if (userId.isEmpty() || channelId.isEmpty() || channelLogin.isEmpty()) return
        val key = channelLogin.lowercase()
        try {

            if (channelId == userId) {
                update { if (it.channelLogin == channelLogin) it.copy(isBroadcaster = true) else it }
                retokenizeMessages()

                val token = state.value.currentAccessToken
                val uid = state.value.currentUserId
                if (token.isNotEmpty() && uid.isNotEmpty()) {
                    pubSubClient.connect(accessToken = token, userId = uid, channelId = channelId, isMod = true)
                    eventSubClient.connect(accessToken = token, userId = uid, channelId = channelId)
                }
                return
            }

            val modIds = ensureModeratedChannelsFetched()
            if (modIds != null) {
                val wasMod = state.value.isMod
                val isMod = channelId in modIds
                channelModCache[key] = isMod
                writePersistedMod(key, isMod)
                update { if (it.channelLogin == channelLogin) it.copy(isMod = isMod) else it }
                if (isMod && !wasMod) {
                    Napier.d("isMod set via /moderation/channels for $channelLogin", tag = TAG)
                    retokenizeMessages()

                    val token = state.value.currentAccessToken
                    val uid = state.value.currentUserId
                    if (token.isNotEmpty() && uid.isNotEmpty()) {
                        pubSubClient.connect(
                            accessToken = token,
                            userId = uid,
                            channelId = channelId,
                            isMod = true
                        )
                        eventSubClient.connect(
                            accessToken = token,
                            userId = uid,
                            channelId = channelId
                        )
                        Napier.d("PubSub connected (deferred) for mod $channelLogin", tag = TAG)
                    }
                }
            }

        } catch (e: Exception) {
            Napier.w("Failed to check mod status: ${e.message}", tag = TAG)
        }
    }

    private fun addToHistory(state: ChatState, message: String): ChatState {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return state

        if (state.sentMessageHistory.firstOrNull() == trimmed) return state

        return state.copy(
            sentMessageHistory = listOf(trimmed) + state.sentMessageHistory.take(MAX_HISTORY - 1),
            historyIndex = -1
        )
    }

    private val historyLoadsInFlight = mutableSetOf<String>()
    private val historyUnavailableChannels = mutableSetOf<String>()

    private suspend fun loadRecentMessages(channelLogin: String) {
        val key = channelLogin.lowercase()
        if (!historyLoadsInFlight.add(key)) return
        try {
            var loadedAnything = seedHistoryFromLocalCache(channelLogin)

            if (key in historyUnavailableChannels) return

            var recent: List<io.rudione.chatone.domain.model.ChatMessage>? = null
            val retryDelays = listOf(0L, 800L, 2_000L, 5_000L, 12_000L)
            for (delayMs in retryDelays) {
                if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
                if (state.value.channelLogin != channelLogin) return
                when (val result = recentMessagesClient.getRecentMessages(channelLogin, limit = maxMessages)) {
                    is RecentMessagesResult.Success -> {
                        recent = result.messages
                    }
                    RecentMessagesResult.Unavailable -> {
                        historyUnavailableChannels.add(key)
                        return
                    }
                    RecentMessagesResult.Pending,
                    RecentMessagesResult.Failed -> Unit
                }
                if (recent != null) break
            }
            if (recent == null) {
                if (!loadedAnything && state.value.channelLogin == channelLogin) {
                    addMessage(
                        DisplayMessage.SystemMsg(
                            id = uniqueId("history_fail"),
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            channel = "#$channelLogin",
                            text = "Couldn't load chat history — new messages will still come through.",
                            type = DisplayMessage.SystemMsg.SystemType.NOTICE
                        )
                    )
                }
                return
            }
            if (recent.isNotEmpty()) {
                val currentLogin = state.value.currentUserLogin
                val currentUserId = state.value.currentUserId
                val displayMessages = recent.map { msg ->
                    val dm = chatMessageToDisplay(msg)
                    if (msg.userId.isNotEmpty() && msg.userId == currentUserId) {
                        dm
                    } else {
                        val match = checkHighlightRules(msg, currentLogin)
                        if (match != null) dm.copy(
                            isMention = true,
                            highlightColor = match.color
                        ) else dm
                    }
                }
                mergeHistoryMessages(channelLogin, displayMessages)
                recent.map { it.userId }.distinct().take(300).forEach { userId ->
                    sevenTvCosmeticsClient.requestCosmetics(userId)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e("Failed to load recent messages: ${e.message}", tag = TAG)
        } finally {
            historyLoadsInFlight.remove(key)
        }
    }

    private fun mergeHistoryMessages(channelLogin: String, displayMessages: List<DisplayMessage>) {
        if (displayMessages.isEmpty()) return
        update { state ->
            if (state.channelLogin != channelLogin) return@update state

            val prevDeletedIds = state.messages
                .filterIsInstance<DisplayMessage.PrivMsg>()
                .filter { it.isDeleted }
                .map { it.id }
                .toSet()
            val liveIds = state.messages
                .filterIsInstance<DisplayMessage.PrivMsg>()
                .map { it.id }
                .toSet()
            val history = displayMessages
                .filter { dm -> dm !is DisplayMessage.PrivMsg || dm.id !in liveIds }
                .map { dm ->
                    if (dm is DisplayMessage.PrivMsg && dm.id in prevDeletedIds)
                        dm.copy(isDeleted = true)
                    else dm
                }
            if (history.isEmpty()) return@update state
            history.forEach { dm ->
                if (dm is DisplayMessage.PrivMsg) messageIdSet.add(dm.id)
            }
            val existing = state.messages
            val mergedFull = mergeChronologically(history, existing)
            val merged =
                if (mergedFull.size > appendCap) mergedFull.takeLast(appendCap) else mergedFull
            val evicted = mergedFull.size - merged.size
            val firstExistingTs = existing.firstOrNull()?.timestamp
            val insertedBefore = if (firstExistingTs == null) history.size
            else history.count { it.timestamp <= firstExistingTs }
            state.copy(
                messages = merged,
                messagesSeq = state.messagesSeq + history.size,
                messagesStartOrdinal = state.messagesStartOrdinal - insertedBefore + evicted
            )
        }
        sendEffect(ChatEffect.HistoryMerged)
    }

    private fun mergeChronologically(
        history: List<DisplayMessage>,
        existing: List<DisplayMessage>
    ): List<DisplayMessage> {
        if (existing.isEmpty()) return history
        if (history.isEmpty()) return existing
        val out = ArrayList<DisplayMessage>(history.size + existing.size)
        var h = 0
        var e = 0
        while (h < history.size && e < existing.size) {
            if (history[h].timestamp <= existing[e].timestamp) out.add(history[h++])
            else out.add(existing[e++])
        }
        while (h < history.size) out.add(history[h++])
        while (e < existing.size) out.add(existing[e++])
        return out
    }

    private suspend fun seedHistoryFromLocalCache(channelLogin: String): Boolean {
        val channelId = state.value.channelId
        if (channelId.isEmpty()) return false
        if (state.value.messages.isNotEmpty()) return false

        val local = runCatching {
            chatRepository.getLocalHistoryForChannel(channelId, limit = maxMessages.toLong())
        }.getOrNull().orEmpty()
        if (local.isEmpty()) return false

        val cutoff = Clock.System.now().toEpochMilliseconds() - LOCAL_SEED_MAX_AGE_MS
        val fresh = local.filter { it.timestamp >= cutoff }
        if (fresh.isEmpty()) {
            Napier.d("Local cache for $channelLogin is stale, skipping seed", tag = TAG)
            return false
        }

        val displayMessages = fresh.map { chatMessageToDisplay(it) }
        mergeHistoryMessages(channelLogin, displayMessages)
        Napier.d("Seeded ${displayMessages.size} messages from local cache for $channelLogin", tag = TAG)
        return true
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
            var emotesLoaded = false
            var badgesLoaded = false

            val emotesJob = launch {
                try {
                    emoteRepository.loadChannelEmotes(channelLogin, channelId)
                    emotesLoaded = true
                } catch (e: Exception) {
                    Napier.e("Failed to load channel emotes: ${e.message}", tag = TAG)
                }
            }
            val badgesJob = launch {
                val token = state.value.currentAccessToken
                if (token.isNotEmpty()) {
                    try {
                        badgeRepository.loadChannelBadges(channelId, token)
                        badgesLoaded = true
                    } catch (e: Exception) {
                        Napier.e("Failed to load channel badges: ${e.message}", tag = TAG)
                    }
                }
            }

            emotesJob.join()
            badgesJob.join()

            if ((emotesLoaded || badgesLoaded) && state.value.channelLogin == channelLogin) {
                retokenizeMessages()
            }

            launch {
                try {
                    val emoteSetId = emoteRepository.getSevenTvEmoteSetId(channelLogin)
                    if (emoteSetId != null) sevenTvEventApi.subscribeToEmoteSet(emoteSetId)
                    sevenTvEventApi.subscribeToTwitchChannel(channelId)
                } catch (e: Exception) {
                    Napier.w("Failed to subscribe to 7TV events: ${e.message}", tag = TAG)
                }
            }

            state.value.currentUserId.takeIf { it.isNotEmpty() }?.let { selfId ->
                runCatching {
                    io.rudione.chatone.di.GlobalDi.tryGet<io.rudione.chatone.data.repository.EnrichedPersonalEmoteBackfiller>()
                        ?.request(selfId)
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
                    val personalEmotes = emoteRepository.getCachedPersonalEmotes(msg.userId)
                    val newTokens = MessageTokenizer.tokenize(
                        msg.rawMessage,
                        channelEmotes,
                        personalEmotes = personalEmotes
                    )
                    val newBadges = badgeRepository.resolveBadges(
                        msg.rawMessage.badges,
                        msg.rawMessage.channelId
                    )
                    msg.copy(tokens = newTokens, badges = newBadges)
                } else msg
            })
        }
    }

    private fun notifyMention(channelLogin: String, message: ChatMessage) {
        if (!io.rudione.chatone.util.system.isAppInForeground()) {
            io.rudione.chatone.util.system.notifySystem(
                "${message.displayName.ifEmpty { message.username }} · #$channelLogin",
                message.message.take(180)
            )
        }
    }

    private fun chatMessageToDisplay(message: ChatMessage): DisplayMessage.PrivMsg {
        val channelEmotes = emoteRepository.getResolvedEmotes(message.channelName)
        val personalEmotes = emoteRepository.getCachedPersonalEmotes(message.userId)
        val tokens =
            MessageTokenizer.tokenize(message, channelEmotes, personalEmotes = personalEmotes)
        val resolvedBadges = badgeRepository.resolveBadges(message.badges, message.channelId)
        val cosmetics = sevenTvCosmeticsClient.getCachedCosmetics(message.userId)
        val safeId = message.id.ifEmpty { uniqueId("msg") }
        return DisplayMessage.PrivMsg(
            id = safeId,
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
            isGrandMod = message.isGrandMod,
            isMention = message.isMention,
            isAction = message.isAction,
            isFirstMessage = message.isFirstMessage,
            isHighlighted = message.isHighlighted,
            customRewardId = message.customRewardId,
            rewardName = message.customRewardId
                ?.let { id -> state.value.channelRewards.firstOrNull { it.id == id }?.title }
                ?: message.rewardName,
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
                    if (sevenTvCosmeticsClient.getCachedCosmetics(message.userId) == null) {
                        launch {
                            try {
                                val cosmetics = sevenTvCosmeticsClient.getUserCosmetics(message.userId)
                                if (cosmetics != null && (cosmetics.paint != null || cosmetics.badge != null)) {
                                    update { state ->
                                        val index = state.messages.indexOfLast { it.id == msgId }
                                        if (index < 0) return@update state
                                        val target = state.messages[index] as? DisplayMessage.PrivMsg
                                            ?: return@update state
                                        if (target.sevenTvPaint == cosmetics.paint &&
                                            target.sevenTvBadge == cosmetics.badge
                                        ) return@update state

                                        val patched = state.messages.toMutableList()
                                        patched[index] = target.copy(
                                            sevenTvPaint = cosmetics.paint,
                                            sevenTvBadge = cosmetics.badge
                                        )
                                        state.copy(messages = patched)
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                    val userId = message.userId

                    if (userId.isNotEmpty()) {
                        runCatching {
                            io.rudione.chatone.di.GlobalDi.tryGet<io.rudione.chatone.data.repository.EnrichedPersonalEmoteBackfiller>()
                                ?.request(userId)
                        }
                    }
                    val displayMsg = chatMessageToDisplay(message)
                    val s = state.value
                    val isOwnMessage = message.userId == s.currentUserId

                    if (isOwnMessage && !message.id.startsWith("local_")) {
                        val incomingText = message.message.trim()
                        val incomingId = message.id

                        val resolvedEchoBadges = if (message.badges.isNotEmpty()) {

                            currentUserBadgeRaw =
                                message.badges.joinToString(",") { "${it.id}/${it.version}" }
                            badgeRepository.resolveBadges(
                                message.badges,
                                message.channelId.ifEmpty { null })
                        } else null

                        var patched = false
                        var alreadyExists = false
                        update { st ->

                            val existingIdx = st.messages.indexOfLast { dm ->
                                dm is DisplayMessage.PrivMsg && dm.id == incomingId
                            }
                            if (existingIdx != -1) {

                                alreadyExists = true
                                if (resolvedEchoBadges != null) {
                                    val existing =
                                        st.messages[existingIdx] as DisplayMessage.PrivMsg
                                    val updated = existing.copy(badges = resolvedEchoBadges)
                                    val newList = st.messages.toMutableList()
                                    newList[existingIdx] = updated
                                    return@update st.copy(messages = newList)
                                }
                                return@update st
                            }

                            val idx = st.messages.indexOfLast { dm ->
                                dm is DisplayMessage.PrivMsg &&
                                        dm.userId == message.userId &&
                                        dm.id.startsWith("local_") &&
                                        dm.rawMessage?.message?.trim() == incomingText
                            }
                            if (idx != -1) {
                                patched = true
                                val existing = st.messages[idx] as DisplayMessage.PrivMsg
                                val updated = existing.copy(
                                    id = incomingId,
                                    badges = resolvedEchoBadges ?: existing.badges
                                )
                                val newList = st.messages.toMutableList()
                                newList[idx] = updated
                                st.copy(messages = newList)
                            } else st
                        }
                        if (alreadyExists) {
                            Napier.d(
                                "IRC echo for already-known $incomingId, skipping duplicate",
                                tag = TAG
                            )
                            return@collect
                        }
                        if (patched) {
                            Napier.d(
                                "IRC echo patched → $incomingId with ${message.badges.size} badges",
                                tag = TAG
                            )
                            return@collect
                        }

                    }

                    if (!isOwnMessage && s.canModerate) {
                        applyLocalAutomod(message)
                    } else if (!isOwnMessage && !s.canModerate) {
                        val persistedMod = readPersistedMod(s.channelLogin)
                        val effectiveMod = s.isMod || persistedMod || s.isBroadcaster
                        if (effectiveMod) applyLocalAutomod(message)
                    }

                    if (!isOwnMessage && isPhraseMuted(message.message)) return@collect

                    val matchResult = if (!isOwnMessage) {
                        checkHighlightRules(message, s.currentUserLogin)
                    } else null

                    val finalMsg = if (matchResult != null) displayMsg.copy(
                        isMention = true,
                        highlightColor = matchResult.color
                    ) else displayMsg
                    automationController.onIncomingMessage(message, isOwnMessage)
                    update { state ->
                        if (!state.channelLogin.equals(
                                message.channelName,
                                ignoreCase = true
                            )
                        ) return@update state

                        if (!messageIdSet.add(finalMsg.id)) {
                            return@update state
                        }

                        val newMessages = (state.messages + finalMsg).takeLast(appendCap)
                        val evicted = (state.messages.size + 1 - newMessages.size).coerceAtLeast(0)
                        if (evicted > 0) {
                            state.messages.take(evicted).forEach { messageIdSet.remove(it.id) }
                        }
                        state.copy(
                            messages = newMessages,
                            messagesSeq = state.messagesSeq + 1,
                            messagesStartOrdinal = state.messagesStartOrdinal + evicted,
                            mentionCount = if (matchResult != null) state.mentionCount + 1 else state.mentionCount
                        )
                    }
                    sendEffect(ChatEffect.ScrollToBottom)

                    if (!isOwnMessage && message.isFirstMessage) {
                        ChatRuleEventEngine.fireFirstMessage(
                            scope = viewModelScope,
                            channelLogin = s.channelLogin,
                            username = message.displayName.ifEmpty { message.username },
                            rules = automodRepository.chatRules.value,
                            send = { text -> sendBotMessage(text) }
                        )
                    }

                    if (matchResult != null) {
                        val muted = mentionMuteRepository.isMuted(
                            userLogin = message.username,
                            channelLogin = s.channelLogin
                        )
                        if (!muted) {
                            sendEffect(
                                ChatEffect.MentionDetected(
                                    s.channelLogin,
                                    finalMsg as? DisplayMessage.PrivMsg,
                                    matchResult.playSound
                                )
                            )
                            if (matchResult.playSound) {
                                val settings = cachedSettings()
                                NotificationSoundPlayer.playMentionSound(
                                    settings.mentionSoundVolume,
                                    settings.customMentionSoundPath
                                )
                            }
                            notifyMention(s.channelLogin, message)
                        }
                    }

                    messagePersistence.enqueue(message)
                } else {
                    val s = state.value
                    val isOwnMessage = message.userId == s.currentUserId
                    if (!isOwnMessage) {
                        val matchResult = checkHighlightRules(message, s.currentUserLogin)
                        if (matchResult != null) {
                            val muted = mentionMuteRepository.isMuted(
                                userLogin = message.username,
                                channelLogin = message.channelName
                            )
                            if (!muted) {
                                val otherDisplayMsg = chatMessageToDisplay(message)
                                val otherFinalMsg = otherDisplayMsg.copy(
                                    isMention = true,
                                    highlightColor = matchResult.color
                                )
                                sendEffect(
                                    ChatEffect.MentionDetected(
                                        message.channelName,
                                        otherFinalMsg,
                                        matchResult.playSound
                                    )
                                )
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
                try {
                when (event) {
                    is IrcEvent.Notice -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            val isBanNotice = event.msgId in setOf(
                                "msg_banned",
                                "msg_channel_suspended",
                                "msg_requires_verified_phone_number"
                            )
                            if (isBanNotice) {
                                update { it.copy(isBanned = true, banReason = event.message) }
                            } else {
                                addMessage(
                                    DisplayMessage.SystemMsg(
                                        id = uniqueId("notice"),
                                        timestamp = Clock.System.now().toEpochMilliseconds(),
                                        channel = event.channel, text = event.message,
                                        type = DisplayMessage.SystemMsg.SystemType.NOTICE
                                    )
                                )
                            }
                        }
                    }

                    is IrcEvent.UserNotice -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            val isAnnouncement =
                                (event.msgId ?: "").equals("announcement", ignoreCase = true)
                            val announceColor =
                                event.tags["msg-param-color"]?.takeIf { it.isNotBlank() }
                            val rawInner = event.message?.let { chatMessageToDisplay(it) }
                            val inner = if (rawInner != null) {
                                val baseId =
                                    if (rawInner.id.isNotEmpty()) "inner_${rawInner.id}" else uniqueId(
                                        "inner"
                                    )
                                if (isAnnouncement) {
                                    rawInner.copy(
                                        id = baseId,
                                        isHighlighted = false,
                                        isMention = false,
                                        isFirstMessage = false,
                                        highlightColor = null
                                    )
                                } else rawInner.copy(id = baseId)
                            } else null
                            addMessage(
                                DisplayMessage.UserNoticeMsg(
                                    id = uniqueId("usernotice"),
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel, systemText = event.systemMsg,
                                    innerMessage = inner,
                                    noticeType = event.msgId ?: "",
                                    announceColor = announceColor
                                )
                            )
                            val msgIdLower = (event.msgId ?: "").lowercase()
                            val glowCount = when {
                                msgIdLower == "submysterygift" || msgIdLower == "subgift" ->
                                    event.tags["msg-param-mass-gift-count"]?.toIntOrNull()
                                        ?: event.tags["msg-param-sender-count"]?.toIntOrNull()
                                        ?: 1

                                msgIdLower == "sub" || msgIdLower == "resub" -> 1
                                msgIdLower == "raid" ->
                                    event.tags["msg-param-viewerCount"]?.toIntOrNull() ?: 1

                                else -> 0
                            }
                            if (glowCount > 0 && cachedSettings().chatInputEventGlow) {
                                val intensity = when {
                                    glowCount >= 1000 -> 1.0f
                                    glowCount >= 100 -> 0.85f
                                    glowCount >= 10 -> 0.6f
                                    glowCount >= 5 -> 0.4f
                                    else -> 0.25f
                                }
                                val ts = Clock.System.now().toEpochMilliseconds()
                                update {
                                    it.copy(
                                        inputGlowIntensity = intensity,
                                        inputGlowTriggerTs = ts
                                    )
                                }
                                viewModelScope.launch {
                                    kotlinx.coroutines.delay(5000L)
                                    if (state.value.inputGlowTriggerTs == ts) {
                                        update { it.copy(inputGlowIntensity = 0f) }
                                    }
                                }
                            }
                            if ((event.msgId ?: "").equals("raid", ignoreCase = true)) {
                                val raider = event.tags["msg-param-displayName"]
                                    ?: event.tags["login"] ?: ""
                                val viewers =
                                    event.tags["msg-param-viewerCount"]?.toIntOrNull() ?: 0
                                ChatRuleEventEngine.fireRaid(
                                    scope = viewModelScope,
                                    channelLogin = state.value.channelLogin,
                                    raiderLogin = raider,
                                    viewers = viewers,
                                    rules = automodRepository.chatRules.value,
                                    send = { text -> sendBotMessage(text) }
                                )
                            }
                        }
                    }

                    is IrcEvent.UserJoin -> {
                        if (cachedSettings().showViewerJoinLeave
                            && event.channel.equals(channelLogin, ignoreCase = true)
                        ) {
                            val strings = AppStrings.forLocale(cachedSettings().language)
                            addMessage(
                                DisplayMessage.SystemMsg(
                                    id = uniqueId("join"),
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel,
                                    text = strings.format(strings.chatViewerJoined, event.username),
                                    type = DisplayMessage.SystemMsg.SystemType.INFO
                                )
                            )
                        }
                    }

                    is IrcEvent.UserPart -> {
                        if (cachedSettings().showViewerJoinLeave
                            && event.channel.equals(channelLogin, ignoreCase = true)
                        ) {
                            val strings = AppStrings.forLocale(cachedSettings().language)
                            addMessage(
                                DisplayMessage.SystemMsg(
                                    id = uniqueId("part"),
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel,
                                    text = strings.format(strings.chatViewerLeft, event.username),
                                    type = DisplayMessage.SystemMsg.SystemType.INFO
                                )
                            )
                        }
                    }

                    is IrcEvent.ClearChat -> {
                        val eventKey = event.channel.removePrefix("#").lowercase()
                        val isActive = event.channel.equals(channelLogin, ignoreCase = true)
                        if (event.targetUser != null) {
                            if (isActive) {
                                val action =
                                    if (event.banDuration != null) DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT else DisplayMessage.ModerationMsg.ModerationAction.BAN
                                val targetUserId = event.tags["target-user-id"]
                                val pendingMod = consumePendingModerator(event.targetUser, targetUserId)
                                val modLogin = event.tags["moderator-login"]
                                    ?: event.tags["created-by"]
                                    ?: pendingMod?.moderatorLogin
                                val text = buildString {
                                    if (event.banDuration != null)
                                        append("${event.targetUser} was timed out for ${event.banDuration}s")
                                    else
                                        append("${event.targetUser} was banned")
                                    if (!modLogin.isNullOrBlank()) append(" by $modLogin")
                                }
                                addMessage(
                                    DisplayMessage.ModerationMsg(
                                        id = uniqueId("mod"),
                                        timestamp = Clock.System.now().toEpochMilliseconds(),
                                        channel = event.channel, text = text, action = action,
                                        targetUser = event.targetUser, duration = event.banDuration,
                                        moderatorLogin = modLogin
                                    )
                                )
                                if (!targetUserId.isNullOrBlank()) {
                                    io.rudione.chatone.di.GlobalDi.tryGet<io.rudione.chatone.data.repository.ModerationHistoryRepository>()
                                        ?.recordEvent(
                                            channelId = state.value.channelId,
                                            targetUserId = targetUserId,
                                            targetLogin = event.targetUser,
                                            action = if (event.banDuration != null)
                                                io.rudione.chatone.data.repository.ModerationHistoryRepository.ACTION_TIMEOUT
                                            else io.rudione.chatone.data.repository.ModerationHistoryRepository.ACTION_BAN,
                                            durationSeconds = event.banDuration,
                                            reason = pendingMod?.reason,
                                            moderatorLogin = modLogin
                                        )
                                }
                                update { state ->
                                    state.copy(messages = state.messages.map { dm ->
                                        if (dm is DisplayMessage.PrivMsg && dm.username.equals(
                                                event.targetUser,
                                                ignoreCase = true
                                            )
                                        ) dm.copy(isDeleted = true) else dm
                                    })
                                }
                            }
                            channelMessageCache[eventKey] =
                                (channelMessageCache[eventKey] ?: emptyList()).map { dm ->
                                    if (dm is DisplayMessage.PrivMsg && dm.username.equals(
                                            event.targetUser,
                                            ignoreCase = true
                                        )
                                    ) dm.copy(isDeleted = true) else dm
                                }
                        } else if (isActive) {
                            addMessage(
                                DisplayMessage.ModerationMsg(
                                    id = uniqueId("clear"),
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel,
                                    text = "Chat was cleared by a moderator",
                                    action = DisplayMessage.ModerationMsg.ModerationAction.CLEAR
                                )
                            )
                        }
                    }

                    is IrcEvent.ClearMsg -> {
                        val eventKey = event.channel.removePrefix("#").lowercase()
                        val isActive = event.channel.equals(channelLogin, ignoreCase = true)
                        if (isActive) {
                            update { state ->
                                state.copy(messages = state.messages.map { dm ->
                                    if (dm is DisplayMessage.PrivMsg && dm.id == event.targetMessageId) dm.copy(
                                        isDeleted = true
                                    ) else dm
                                })
                            }
                        }
                        channelMessageCache[eventKey] =
                            (channelMessageCache[eventKey] ?: emptyList()).map { dm ->
                                if (dm is DisplayMessage.PrivMsg && dm.id == event.targetMessageId) dm.copy(
                                    isDeleted = true
                                ) else dm
                            }
                        if (isActive) {
                            val targetLogin = event.login.takeIf { it.isNotBlank() }
                                ?: state.value.messages
                                    .filterIsInstance<DisplayMessage.PrivMsg>()
                                    .firstOrNull { it.id == event.targetMessageId }
                                    ?.username
                            val modLogin = consumePendingDeleteModerator(event.targetMessageId)
                            val text = buildString {
                                if (!targetLogin.isNullOrBlank()) {
                                    append("Message from $targetLogin was deleted")
                                } else {
                                    append("A message was deleted")
                                }
                                if (!modLogin.isNullOrBlank()) append(" by $modLogin")
                            }
                            addMessage(
                                DisplayMessage.ModerationMsg(
                                    id = uniqueId("del"),
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel,
                                    text = text,
                                    action = DisplayMessage.ModerationMsg.ModerationAction.DELETE,
                                    targetUser = targetLogin,
                                    moderatorLogin = modLogin
                                )
                            )
                        }
                    }

                    is IrcEvent.RoomState -> {
                        if (event.channel.equals(channelLogin, ignoreCase = true)) {
                            val roomId = event.roomId
                            if (!roomId.isNullOrEmpty() && state.value.channelId.isEmpty()) {
                                update { it.copy(channelId = roomId) }
                                channelIdCache[channelLogin.lowercase()] = roomId
                                loadChannelEmotesAndBadges(roomId)

                                viewModelScope.launch { checkAndSetModStatus(roomId) }
                            }
                            update { state ->
                                val newRoomState = RoomState(
                                    emoteOnly = event.emoteOnly ?: state.roomState.emoteOnly,
                                    followersOnly = event.followersOnly
                                        ?: state.roomState.followersOnly,
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
                            val key = channelLogin.lowercase()
                            val prevIsMod = state.value.isMod

                            val finalIsMod = event.isMod || prevIsMod
                            channelModCache[key] = finalIsMod
                            if (finalIsMod) writePersistedMod(key, true)
                            if (event.badges.isNotEmpty()) currentUserBadgeRaw = event.badges

                            if (event.badges.isNotEmpty()) {
                                val freshBadges = event.badges.split(",").mapNotNull { pair ->
                                    val parts = pair.split("/", limit = 2)
                                    if (parts.size == 2) {
                                        val badgeId = parts[0].lowercase()
                                        val badgeVersion = parts[1]

                                        val months = when (badgeId) {
                                            "subscriber", "founder", "sub-gifter" -> badgeVersion.toIntOrNull()
                                            else -> null
                                        }

                                        Badge(
                                            id = badgeId,
                                            version = badgeVersion,
                                            imageUrl = "",
                                            months = months,
                                            tooltip = "",
                                            setId = "",
                                            isGlobal = false
                                        )
                                    } else null
                                }
                                val detectedGrandMod = freshBadges.hasGrandModBadge()
                                if (detectedGrandMod != state.value.isGrandMod) {
                                    update { st ->
                                        if (st.channelLogin == channelLogin) st.copy(
                                            isGrandMod = detectedGrandMod
                                        ) else st
                                    }
                                }
                                val resolvedFreshBadges = badgeRepository.resolveBadges(
                                    freshBadges,
                                    state.value.channelId.ifEmpty { null }
                                )
                                val currentUserId = state.value.currentUserId
                                update { st ->

                                    val targetIndex = st.messages.indexOfLast { dm ->
                                        dm is DisplayMessage.PrivMsg &&
                                                dm.id.startsWith("local_") &&
                                                dm.userId == currentUserId
                                    }
                                    if (targetIndex == -1) return@update st
                                    val patched =
                                        (st.messages[targetIndex] as DisplayMessage.PrivMsg)
                                            .copy(
                                                badges = resolvedFreshBadges,
                                                isModerator = event.isMod
                                            )
                                    val newMessages = st.messages.toMutableList()
                                    newMessages[targetIndex] = patched
                                    st.copy(messages = newMessages)
                                }
                            }

                            update {
                                it.copy(
                                    isMod = finalIsMod,
                                    currentUserColor = event.color ?: it.currentUserColor,
                                    currentDisplayName = if (event.displayName.isNotEmpty()) event.displayName else it.currentDisplayName
                                )
                            }
                            if (finalIsMod && !prevIsMod) {
                                Napier.d(
                                    "isMod changed: false -> true via USERSTATE on $channelLogin",
                                    tag = TAG
                                )
                                retokenizeMessages()
                                val s2 = state.value
                                if (s2.channelId.isNotEmpty() && s2.currentAccessToken.isNotEmpty() && s2.currentUserId.isNotEmpty()) {
                                    pubSubClient.connect(
                                        accessToken = s2.currentAccessToken,
                                        userId = s2.currentUserId,
                                        channelId = s2.channelId,
                                        isMod = true
                                    )
                                    eventSubClient.connect(
                                        accessToken = s2.currentAccessToken,
                                        userId = s2.currentUserId,
                                        channelId = s2.channelId
                                    )
                                }
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

                    is IrcEvent.AutoModHeld -> {
                        if (event.channel.equals(
                                channelLogin,
                                ignoreCase = true
                            ) && state.value.canModerate
                        ) {
                            addMessage(
                                DisplayMessage.AutoModMsg(
                                    id = "automod_${event.msgId}",
                                    timestamp = Clock.System.now().toEpochMilliseconds(),
                                    channel = event.channel,
                                    msgId = event.msgId,
                                    userId = event.userId,
                                    username = event.username,
                                    displayName = event.displayName,
                                    text = event.message,
                                    color = event.color,
                                    reasonCategory = event.reasonCategory,
                                    reasonLevel = event.reasonLevel,
                                    flaggedFragments = event.flaggedFragments
                                )
                            )
                        }
                    }

                    else -> {}
                }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.e(
                        "Failed to handle IRC event ${event::class.simpleName}: ${e.message}",
                        e,
                        tag = TAG
                    )
                }
            }
        }
    }

    private fun observePubSubEvents() {
        viewModelScope.launch {
            pubSubClient.events.collect { handleModerationStreamEvent(it) }
        }
        viewModelScope.launch {
            eventSubClient.events.collect { handleModerationStreamEvent(it) }
        }
    }

    private suspend fun handleModerationStreamEvent(event: IrcEvent) {
        val s = state.value
        when (event) {
                    is IrcEvent.AutoModHeld -> {
                        val channelMatch = event.channel == s.channelId
                                || event.channel.equals(s.channelLogin, ignoreCase = true)
                                || event.channel.equals("#${s.channelLogin}", ignoreCase = true)
                        if (channelMatch) {
                            val alreadyShown = s.messages.any {
                                it is DisplayMessage.AutoModMsg && it.msgId == event.msgId
                            }
                            if (!alreadyShown) {
                                addMessage(
                                    DisplayMessage.AutoModMsg(
                                        id = "automod_${event.msgId}",
                                        timestamp = Clock.System.now().toEpochMilliseconds(),
                                        channel = "#${s.channelLogin}",
                                        msgId = event.msgId,
                                        userId = event.userId,
                                        username = event.username,
                                        displayName = event.displayName,
                                        text = event.message,
                                        color = event.color,
                                        reasonCategory = event.reasonCategory,
                                        reasonLevel = event.reasonLevel
                                    )
                                )
                            }
                        }
                    }

                    is IrcEvent.AutoModResolved -> {
                        val channelMatch = event.channel == s.channelId
                                || event.channel.equals(s.channelLogin, ignoreCase = true)
                                || event.channel.equals("#${s.channelLogin}", ignoreCase = true)
                        if (channelMatch) {
                            update { st ->
                                st.copy(messages = st.messages.filter { dm ->
                                    !(dm is DisplayMessage.AutoModMsg && dm.msgId == event.msgId)
                                })
                            }
                            val noticeText = if (event.action == "ALLOWED")
                                "${event.resolvedBy}'s message was approved"
                            else
                                "${event.resolvedBy}'s message was denied"
                            systemNotice(s.channelLogin, noticeText)
                        }
                    }

                    is IrcEvent.ModeratorAction -> {
                        val channelMatch = event.channel == s.channelId
                                || event.channel.equals(s.channelLogin, ignoreCase = true)
                                || event.channel.equals("#${s.channelLogin}", ignoreCase = true)
                        if (channelMatch) handleModeratorAction(event)
                    }

                    is IrcEvent.PointsClaimAvailable -> {
                        if (cachedSettings().autoClaimPoints && s.currentAccessToken.isNotEmpty()) {
                            viewModelScope.launch {
                                val r = twitchGqlClient.claimCommunityPoints(
                                    event.channelId, event.claimId,
                                    moderationAuthStore.resolveToken(s.currentAccessToken)
                                )
                                when (r) {
                                    is io.rudione.chatone.util.Result.Success ->
                                        Napier.d("Bonus points claimed for ${event.channelId}", tag = TAG)
                                    is io.rudione.chatone.util.Result.Error -> {
                                        Napier.w("Points claim failed: ${r.exception.message}", tag = TAG)
                                        if (!pointsClaimErrorShown) {
                                            pointsClaimErrorShown = true
                                            sendEffect(
                                                ChatEffect.ShowError(
                                                    AppStrings.forLocale(cachedSettings().language)
                                                        .actionsAutoClaimFailed
                                                )
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    is IrcEvent.PollUpdated -> applyLivePoll(event.channelId, event.poll)

                    is IrcEvent.PredictionUpdated ->
                        applyLivePrediction(event.channelId, event.prediction)

                    is IrcEvent.PinnedChatUpdated -> {
                        if (event.channel == s.channelId) {
                            when {
                                event.isUnpin -> {

                                    val currentPinId = s.pinId
                                    val stale = event.unpinId != null && currentPinId != null &&
                                            event.unpinId != currentPinId
                                    if (!stale) {
                                        if (s.pinnedMessage != null) {
                                            val who = event.unpinnedBy?.takeIf { it.isNotBlank() }
                                            systemNotice(
                                                s.channelLogin,
                                                if (who != null) "$who unpinned the message."
                                                else "Pinned message was removed."
                                            )
                                        }
                                        update {
                                            it.copy(pinnedMessage = null, pinId = null, pinEndsAtMs = null, pinnedByName = null, pinnedByBadges = emptyList())
                                        }
                                    }
                                }
                                event.pin != null -> applyPinPayload(event.pin)
                                else -> fetchCurrentPin(event.channel)
                            }
                        }
                    }

                    else -> {}
        }
    }

    private val recentModNotices = mutableMapOf<String, Long>()

    private fun addModerationNotice(
        action: DisplayMessage.ModerationMsg.ModerationAction,
        text: String,
        targetUser: String?,
        moderatorLogin: String?,
        idPrefix: String = "modact"
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val key = "${action.name}|${targetUser?.lowercase() ?: "-"}"
        val last = recentModNotices[key]
        if (last != null && now - last < MOD_NOTICE_DEDUPE_MS) return
        recentModNotices[key] = now
        if (recentModNotices.size > 128) {
            recentModNotices.entries.removeAll { now - it.value > MOD_NOTICE_DEDUPE_MS }
        }
        addMessage(
            DisplayMessage.ModerationMsg(
                id = uniqueId(idPrefix),
                timestamp = now,
                channel = "#${state.value.channelLogin}",
                text = text,
                action = action,
                targetUser = targetUser,
                moderatorLogin = moderatorLogin
            )
        )
    }

    private fun handleModeratorAction(event: IrcEvent.ModeratorAction) {
        when (event.action) {
            IrcEvent.ModeratorAction.ACTION_BAN,
            IrcEvent.ModeratorAction.ACTION_TIMEOUT,
            IrcEvent.ModeratorAction.ACTION_UNBAN,
            IrcEvent.ModeratorAction.ACTION_UNTIMEOUT -> {

                if (!event.targetUserId.isNullOrBlank()) {
                    rememberPendingModerator(event.targetUserId, event.moderator, event.reason)
                } else if (!event.target.isNullOrBlank()) {

                    val matched = state.value.messages
                        .filterIsInstance<DisplayMessage.PrivMsg>()
                        .lastOrNull { it.username.equals(event.target, ignoreCase = true) }
                        ?.userId
                    if (!matched.isNullOrBlank()) {
                        rememberPendingModerator(matched, event.moderator, event.reason)
                    }
                }

                if (event.action == IrcEvent.ModeratorAction.ACTION_UNBAN ||
                    event.action == IrcEvent.ModeratorAction.ACTION_UNTIMEOUT
                ) {
                    val target = event.target ?: "—"
                    val isUnban = event.action == IrcEvent.ModeratorAction.ACTION_UNBAN
                    addModerationNotice(
                        action = if (isUnban) DisplayMessage.ModerationMsg.ModerationAction.UNBAN
                        else DisplayMessage.ModerationMsg.ModerationAction.UNTIMEOUT,
                        text = if (isUnban) "$target was unbanned by ${event.moderator}"
                        else "$target was untimed out by ${event.moderator}",
                        targetUser = event.target,
                        moderatorLogin = event.moderator
                    )
                }
            }

            IrcEvent.ModeratorAction.ACTION_DELETE -> {
                if (!event.targetMessageId.isNullOrBlank()) {
                    rememberPendingDeleteModerator(event.targetMessageId, event.moderator)
                }
            }

            IrcEvent.ModeratorAction.ACTION_MOD,
            IrcEvent.ModeratorAction.ACTION_UNMOD,
            IrcEvent.ModeratorAction.ACTION_VIP,
            IrcEvent.ModeratorAction.ACTION_UNVIP -> {
                val target = event.target ?: "—"
                val text = when (event.action) {
                    IrcEvent.ModeratorAction.ACTION_MOD -> "$target was modded by ${event.moderator}"
                    IrcEvent.ModeratorAction.ACTION_UNMOD -> "$target was unmodded by ${event.moderator}"
                    IrcEvent.ModeratorAction.ACTION_VIP -> "$target was made VIP by ${event.moderator}"
                    else -> "$target is no longer VIP by ${event.moderator}"
                }
                addModerationNotice(
                    action = when (event.action) {
                        IrcEvent.ModeratorAction.ACTION_MOD -> DisplayMessage.ModerationMsg.ModerationAction.MOD
                        IrcEvent.ModeratorAction.ACTION_UNMOD -> DisplayMessage.ModerationMsg.ModerationAction.UNMOD
                        IrcEvent.ModeratorAction.ACTION_VIP -> DisplayMessage.ModerationMsg.ModerationAction.VIP
                        else -> DisplayMessage.ModerationMsg.ModerationAction.UNVIP
                    },
                    text = text,
                    targetUser = event.target,
                    moderatorLogin = event.moderator
                )
            }

            IrcEvent.ModeratorAction.ACTION_CLEAR -> {

            }

            "raid" -> {
                val target = event.target
                if (!target.isNullOrBlank()) {
                    setPendingRaid(target, event.targetUserId.orEmpty())
                }
            }

            "unraid" -> {
                update { it.copy(pendingRaidTarget = null, pendingRaidTargetId = null, pendingRaidStartedAt = 0L) }
            }

            "warn" -> {
                val targetUserId = event.targetUserId
                    ?: state.value.messages.filterIsInstance<DisplayMessage.PrivMsg>()
                        .lastOrNull { it.username.equals(event.target, ignoreCase = true) }?.userId
                if (!targetUserId.isNullOrBlank()) {
                    GlobalDi.tryGet<ModerationHistoryRepository>()
                        ?.recordEvent(
                            channelId = state.value.channelId,
                            targetUserId = targetUserId,
                            targetLogin = event.target.orEmpty(),
                            action = ModerationHistoryRepository.ACTION_WARN,
                            reason = event.reason,
                            moderatorLogin = event.moderator
                        )
                }
            }
        }
    }

    private fun observePersonalEmoteGrants() {
        val backfiller = runCatching {
            GlobalDi.tryGet<EnrichedPersonalEmoteBackfiller>()
        }.getOrNull() ?: return
        viewModelScope.launch {
            backfiller.granted.collect { grant ->
                sevenTvEventApi.subscribeToEmoteSet(grant.emoteSetId)
                scheduleRetokenizeForUsers(setOf(grant.twitchUserId))
            }
        }
    }

    private fun observeEmoteSetUpdates() {
        viewModelScope.launch {
            sevenTvEventApi.emoteSetUpdates.collect { event ->
                val channelLogin = state.value.channelLogin

                fun hasRealActor(actor: String): Boolean =
                    actor.isNotBlank() && !actor.equals("Unknown", ignoreCase = true)

                val noticeText: String? = when (event) {
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteAdded ->
                        if (hasRealActor(event.actorName)) "[7TV] ${event.actorName} added ${event.emoteName}" else null

                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRemoved ->
                        if (hasRealActor(event.actorName)) "[7TV] ${event.actorName} removed ${event.emoteName}" else null

                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRenamed ->
                        if (hasRealActor(event.actorName)) "[7TV] ${event.actorName} renamed ${event.oldName} → ${event.newName}" else null

                    is SevenTvEventApi.EmoteSetUpdateEvent.PersonalEmoteSetGranted,
                    is SevenTvEventApi.EmoteSetUpdateEvent.PersonalEmoteSetRevoked -> null
                }
                if (noticeText != null) {
                    addMessage(
                        DisplayMessage.SystemMsg(
                            id = uniqueId("7tv"),
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            channel = channelLogin, text = noticeText,
                            type = DisplayMessage.SystemMsg.SystemType.NOTICE
                        )
                    )
                }

                when (event) {
                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteAdded -> {
                        launch {
                            try {

                                val emote = sevenTvApi.getEmoteById(event.emoteId)
                                    ?: GenericEmote(
                                        id = event.emoteId,
                                        code = event.emoteName,
                                        url1x = "https://cdn.7tv.app/emote/${event.emoteId}/1x.webp",
                                        url2x = "https://cdn.7tv.app/emote/${event.emoteId}/2x.webp",
                                        url3x = "https://cdn.7tv.app/emote/${event.emoteId}/4x.webp",
                                        provider = io.rudione.chatone.domain.model.EmoteProvider.SEVEN_TV
                                    )
                                if (emoteRepository.isPersonalEmoteSet(event.emoteSetId)) {
                                    emoteRepository.patchPersonalSet(event.emoteSetId, emote)
                                    val affected =
                                        emoteRepository.usersForPersonalSet(event.emoteSetId)
                                    if (state.value.channelLogin == channelLogin && affected.isNotEmpty()) {
                                        scheduleRetokenizeForUsers(affected)
                                    }
                                } else {
                                    emoteRepository.patchChannelEmote(channelLogin, emote)
                                    if (state.value.channelLogin == channelLogin) scheduleRetokenize()
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }

                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRemoved -> {
                        if (emoteRepository.isPersonalEmoteSet(event.emoteSetId)) {
                            emoteRepository.removeFromPersonalSet(
                                event.emoteSetId, event.emoteId, event.emoteName
                            )
                            val affected = emoteRepository.usersForPersonalSet(event.emoteSetId)
                            if (state.value.channelLogin == channelLogin && affected.isNotEmpty()) {
                                scheduleRetokenizeForUsers(affected)
                            }
                        } else {
                            emoteRepository.removeChannelEmote(
                                channelLogin,
                                event.emoteId,
                                event.emoteName
                            )
                            if (state.value.channelLogin == channelLogin) scheduleRetokenize()
                        }
                    }

                    is SevenTvEventApi.EmoteSetUpdateEvent.EmoteRenamed -> {
                        if (emoteRepository.isPersonalEmoteSet(event.emoteSetId)) {
                            emoteRepository.renameInPersonalSet(
                                event.emoteSetId, event.emoteId, event.newName
                            )
                            val affected = emoteRepository.usersForPersonalSet(event.emoteSetId)
                            if (state.value.channelLogin == channelLogin && affected.isNotEmpty()) {
                                scheduleRetokenizeForUsers(affected)
                            }
                        } else {
                            emoteRepository.renameChannelEmote(
                                channelLogin,
                                event.emoteId,
                                event.newName
                            )
                            if (state.value.channelLogin == channelLogin) scheduleRetokenize()
                        }
                    }

                    is SevenTvEventApi.EmoteSetUpdateEvent.PersonalEmoteSetGranted -> {
                        launch {
                            try {
                                val emotes = emoteRepository.grantPersonalEmoteSet(
                                    twitchUserId = event.twitchUserId,
                                    emoteSetId = event.emoteSetId
                                )

                                sevenTvEventApi.subscribeToEmoteSet(event.emoteSetId)
                                if (!emotes.isNullOrEmpty() &&
                                    state.value.channelLogin == channelLogin
                                ) {
                                    scheduleRetokenizeForUsers(setOf(event.twitchUserId))
                                }
                            } catch (e: Exception) {
                                Napier.w(
                                    "Failed to grant personal set ${event.emoteSetId} for ${event.twitchUserId}: ${e.message}",
                                    tag = TAG
                                )
                            }
                        }
                    }

                    is SevenTvEventApi.EmoteSetUpdateEvent.PersonalEmoteSetRevoked -> {
                        emoteRepository.revokePersonalEmoteSet(event.twitchUserId, event.emoteSetId)
                        if (state.value.channelLogin == channelLogin) {
                            scheduleRetokenizeForUsers(setOf(event.twitchUserId))
                        }
                    }
                }
            }
        }
    }

    private fun retokenizeMessagesForUsers(userIds: Set<String>) {
        if (userIds.isEmpty()) return
        update { state ->
            val channelLogin = state.channelLogin
            if (channelLogin.isEmpty()) return@update state
            val channelEmotes = emoteRepository.getResolvedEmotes(channelLogin)
            state.copy(messages = state.messages.map { msg ->
                if (msg is DisplayMessage.PrivMsg &&
                    msg.userId in userIds &&
                    msg.rawMessage != null
                ) {
                    val personal = emoteRepository.getCachedPersonalEmotes(msg.userId)
                    val newTokens = MessageTokenizer.tokenize(
                        msg.rawMessage,
                        channelEmotes,
                        personalEmotes = personal
                    )
                    msg.copy(tokens = newTokens)
                } else msg
            })
        }
    }

    private val messageIdSet = LinkedHashSet<String>(512)

    private fun addMessage(msg: DisplayMessage) {
        if (!messageIdSet.add(msg.id)) return
        update { state ->
            val newList = (state.messages + msg).takeLast(appendCap)
            val evicted = (state.messages.size + 1 - newList.size).coerceAtLeast(0)
            if (evicted > 0) {
                state.messages.take(evicted).forEach { messageIdSet.remove(it.id) }
            }
            state.copy(
                messages = newList,
                messagesSeq = state.messagesSeq + 1,
                messagesStartOrdinal = state.messagesStartOrdinal + evicted
            )
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
                update {
                    it.copy(
                        mentionCompletions = recentUsers,
                        showMentionCompletions = true,
                        showEmoteCompletions = false,
                        emoteCompletions = emptyList()
                    )
                }
            } else {
                update { it.copy(showMentionCompletions = false, mentionCompletions = emptyList()) }
            }
            return
        }
        update { it.copy(showMentionCompletions = false, mentionCompletions = emptyList()) }

        val isColonQuery = lastWord.startsWith(":") && !lastWord.startsWith("::")
        val query = if (isColonQuery) lastWord.removePrefix(":") else lastWord

        if ((isColonQuery || query.length >= 2) && !query.startsWith("/")) {
            val matches = searchAllEmotes(query, limit = if (isColonQuery) 12 else 8)
            if (matches.isNotEmpty()) {
                update { it.copy(emoteCompletions = matches, showEmoteCompletions = true) }
            } else {
                update { it.copy(showEmoteCompletions = false, emoteCompletions = emptyList()) }
            }
        } else {
            update { it.copy(showEmoteCompletions = false, emoteCompletions = emptyList()) }
        }
    }

    private fun searchAllEmotes(query: String, limit: Int): List<GenericEmote> {
        val s = state.value
        val channelEmotes = emoteRepository.getResolvedEmotes(s.channelLogin)
        val seen = mutableSetOf<String>()
        val combined = mutableListOf<GenericEmote>()
        for (e in channelEmotes.allByCode.values) if (seen.add(e.code)) combined.add(e)
        for (e in s.twitchChannelEmotes + s.twitchGlobalEmotes + s.twitchSubscriberEmotes) {
            if (seen.add(e.code)) combined.add(e)
        }
        if (query.isEmpty()) return combined.take(limit)
        val prefix = mutableListOf<GenericEmote>()
        val contains = mutableListOf<GenericEmote>()
        for (e in combined) {
            when {
                e.code.startsWith(query, ignoreCase = true) -> prefix.add(e)
                e.code.contains(query, ignoreCase = true) -> contains.add(e)
            }
        }
        prefix.sortBy { it.code.length }
        contains.sortBy { it.code.length }
        return (prefix + contains).take(limit)
    }

    private fun selectMentionCompletion(username: String) {
        val current = state.value.messageInput
        val words = current.split(" ").toMutableList()
        if (words.isNotEmpty()) words[words.size - 1] = "@$username"
        val newInput = words.joinToString(" ") + " "
        update {
            it.copy(
                messageInput = newInput,
                showMentionCompletions = false,
                mentionCompletions = emptyList()
            )
        }
    }

    private fun selectEmoteCompletion(emote: GenericEmote) {
        val current = state.value.messageInput
        val words = current.split(" ").toMutableList()
        if (words.isNotEmpty()) words[words.size - 1] = emote.code
        val newInput = words.joinToString(" ") + " "
        update {
            it.copy(
                messageInput = newInput,
                showEmoteCompletions = false,
                emoteCompletions = emptyList()
            )
        }
    }

    private fun sendMessage(keepText: Boolean = false) {
        val s = state.value
        var message = s.messageInput.trim()
        val channelLogin = s.channelLogin

        if (message.isEmpty() || channelLogin.isEmpty()) return
        if (!s.isConnected) {
            sendEffect(ChatEffect.ShowError("Not connected to chat"))
            return
        }

        val now = Clock.System.now().toEpochMilliseconds()
        if (!isSendingMessage.compareAndSet(expect = false, update = true)) return
        if (now - lastMessageSentAt < 70L) {
            isSendingMessage.getAndSet(false)
            return
        }

        viewModelScope.launch {
            try {
                val commandResult = applyChatCommand(message)
                if (commandResult is ChatCommandResult.Macro) {
                    update { st -> if (!keepText) st.copy(messageInput = "") else st }
                    executeMacro(commandResult.macro)
                    lastMessageSentAt = Clock.System.now().toEpochMilliseconds()
                    return@launch
                }
                if (commandResult is ChatCommandResult.Text) {
                    message = commandResult.text
                }

                val parsed = SlashCommand.parse(message)
                if (parsed != null) {
                    update { st -> if (!keepText) st.copy(messageInput = "") else st }
                    runSlashCommand(parsed, channelLogin, s)
                    lastMessageSentAt = Clock.System.now().toEpochMilliseconds()
                    return@launch
                }

                val isPrivileged = s.isMod || s.isBroadcaster || s.isGrandMod
                val baseMessage = message
                if (!isPrivileged && baseMessage == lastSentBaseText) {
                    message = if (duplicateSuffixToggle) {
                        "$baseMessage \uDB40\uDC00"
                    } else {
                        "$baseMessage \uDB40\uDC01"
                    }
                    duplicateSuffixToggle = !duplicateSuffixToggle
                } else {
                    duplicateSuffixToggle = false
                }

                sendViaHelixOnly(channelLogin, message, keepText, s)
                lastSentBaseText = baseMessage
                lastMessageSentAt = Clock.System.now().toEpochMilliseconds()
                update { it.copy(lastMessageSentAtMs = lastMessageSentAt) }

            } catch (e: Exception) {
                Napier.e("Failed to send message: ${e.message}", e, tag = TAG)
                sendEffect(ChatEffect.ShowError("Failed to send message: ${e.message}"))
            } finally {
                isSendingMessage.getAndSet(false)
            }
        }
    }

    private suspend fun resolveUserId(login: String): String? {
        if (login.isBlank()) return null
        val s = state.value

        val cached = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
            .firstOrNull { it.username.equals(login, ignoreCase = true) }
            ?.userId
            ?.takeIf { it.isNotBlank() }
        if (cached != null) return cached
        if (s.currentAccessToken.isEmpty()) return null
        val res = apiClient.getUsers(s.currentAccessToken, logins = listOf(login.lowercase()))
        return if (res is io.rudione.chatone.util.Result.Success) {
            res.data.data.firstOrNull()?.id
        } else null
    }

    private fun systemNotice(channelLogin: String, text: String) {
        addMessage(
            DisplayMessage.SystemMsg(
                id = uniqueId("cmd"),
                timestamp = Clock.System.now().toEpochMilliseconds(),
                channel = channelLogin,
                text = text,
                type = DisplayMessage.SystemMsg.SystemType.NOTICE
            )
        )
    }

    @Suppress("LongMethod")
    private suspend fun runSlashCommand(
        cmd: SlashCommand.Parsed,
        channelLogin: String,
        s: ChatState
    ) {
        val token = s.currentAccessToken
        val broadcasterId = s.channelId
        val moderatorId = s.currentUserId

        val gqlEventToken = moderationAuthStore.resolveToken(token)
        val useGqlEvents = moderationAuthStore.hasCustomToken()

        suspend fun requireMod(): Boolean {
            if (s.canModerate) return true
            sendEffect(ChatEffect.ShowError("Mod-only command"))
            return false
        }

        when (cmd) {
            is SlashCommand.Parsed.Ban -> {
                if (!requireMod()) return
                val targetId = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found: ${cmd.targetLogin}"))
                rememberPendingModerator(targetId, s.currentUserLogin)
                val r = apiClient.banUser(
                    token,
                    broadcasterId,
                    moderatorId,
                    targetId,
                    duration = null,
                    reason = cmd.reason
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to ban ${cmd.targetLogin}"))
            }

            is SlashCommand.Parsed.Timeout -> {
                if (!requireMod()) return
                val targetId = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found: ${cmd.targetLogin}"))
                rememberPendingModerator(targetId, s.currentUserLogin)
                val r = apiClient.banUser(
                    token,
                    broadcasterId,
                    moderatorId,
                    targetId,
                    duration = cmd.seconds,
                    reason = cmd.reason
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to timeout ${cmd.targetLogin}"))
            }

            is SlashCommand.Parsed.Unban -> {
                if (!requireMod()) return
                val targetId = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found: ${cmd.targetLogin}"))
                val r = apiClient.unbanUser(token, broadcasterId, moderatorId, targetId)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to unban ${cmd.targetLogin}"))
            }

            is SlashCommand.Parsed.Clear -> {
                if (!requireMod()) return
                val r = apiClient.clearChat(token, broadcasterId, moderatorId)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to clear chat"))
            }

            is SlashCommand.Parsed.Nuke -> {
                if (!requireMod()) return
                val regex = if (cmd.isRegex)
                    runCatching { Regex(cmd.phrase, RegexOption.IGNORE_CASE) }.getOrNull() else null
                if (cmd.isRegex && regex == null)
                    return sendEffect(ChatEffect.ShowError("Invalid regex: ${cmd.phrase}"))

                val cutoff = Clock.System.now().toEpochMilliseconds() - cmd.pastSeconds * 1000L
                val phraseLower = cmd.phrase.lowercase()
                val matches = s.messages.asSequence()
                    .filterIsInstance<DisplayMessage.PrivMsg>()
                    .filter { it.timestamp >= cutoff && !it.isDeleted && !it.isBroadcaster && !it.isModerator }
                    .filter { msg ->
                        val text = msg.rawMessage?.message ?: msg.tokens.joinToString("") { t ->
                            when (t) {
                                is MessageToken.Text -> t.text
                                is MessageToken.TwitchEmoteToken -> t.name
                                is MessageToken.ThirdPartyEmoteToken -> t.emote.code
                                is MessageToken.Link -> t.displayText
                                is MessageToken.Mention -> "@${t.username}"
                                is MessageToken.Cheer -> "${t.prefix}${t.amount}"
                            }
                        }
                        if (regex != null) regex.containsMatchIn(text)
                        else text.lowercase().contains(phraseLower)
                    }
                    .toList()

                if (matches.isEmpty()) {
                    systemNotice(channelLogin, "Nuke: no messages matched \"${cmd.phrase}\" in the last ${cmd.pastSeconds}s")
                    return
                }

                when (cmd.action) {
                    SlashCommand.NukeAction.DELETE -> {
                        val toDelete = matches.take(200)
                        var ok = 0
                        toDelete.forEach { m ->
                            if (!apiClient.deleteMessage(token, broadcasterId, moderatorId, m.id).isError) ok++
                        }
                        systemNotice(channelLogin, "Nuke: deleted $ok/${matches.size} message(s) matching \"${cmd.phrase}\"")
                    }
                    SlashCommand.NukeAction.TIMEOUT -> {
                        val users = matches.map { it.userId to it.username }.distinctBy { it.first }.take(100)
                        var ok = 0
                        users.forEach { (uid, _) ->
                            rememberPendingModerator(uid, s.currentUserLogin)
                            if (!apiClient.banUser(token, broadcasterId, moderatorId, uid, duration = cmd.timeoutSeconds, reason = "Nuke: ${cmd.phrase}").isError) ok++
                        }
                        systemNotice(channelLogin, "Nuke: timed out $ok/${users.size} user(s) for ${cmd.timeoutSeconds}s")
                    }
                    SlashCommand.NukeAction.BAN -> {
                        val users = matches.map { it.userId to it.username }.distinctBy { it.first }.take(100)
                        var ok = 0
                        users.forEach { (uid, _) ->
                            rememberPendingModerator(uid, s.currentUserLogin)
                            if (!apiClient.banUser(token, broadcasterId, moderatorId, uid, duration = null, reason = "Nuke: ${cmd.phrase}").isError) ok++
                        }
                        systemNotice(channelLogin, "Nuke: banned $ok/${users.size} user(s)")
                    }
                }
            }

            is SlashCommand.Parsed.Raid -> {
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster (or a channel editor) can start a raid"))
                val targetId = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("Channel not found: ${cmd.targetLogin}"))
                val r = apiClient.startRaid(token, broadcasterId, targetId)
                if (r.isError) {
                    sendEffect(ChatEffect.ShowError("Failed to start raid"))
                } else {
                    addMessage(
                        DisplayMessage.SystemMsg(
                            id = uniqueId("raid"),
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            channel = channelLogin,
                            text = "Raid started — heading to ${cmd.targetLogin} in 90s. Use /unraid to cancel.",
                            type = DisplayMessage.SystemMsg.SystemType.NOTICE
                        )
                    )
                }
            }

            is SlashCommand.Parsed.UnRaid -> {
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster (or a channel editor) can cancel a raid"))
                val r = apiClient.cancelRaid(token, broadcasterId)
                if (r.isError) {
                    sendEffect(ChatEffect.ShowError("Failed to cancel raid"))
                } else {
                    addMessage(
                        DisplayMessage.SystemMsg(
                            id = uniqueId("raid"),
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            channel = channelLogin,
                            text = "Raid cancelled.",
                            type = DisplayMessage.SystemMsg.SystemType.NOTICE
                        )
                    )
                }
            }

            is SlashCommand.Parsed.Pin -> {
                if (!requireMod()) return

                val raw = cmd.args.trim()
                if (raw.isEmpty()) {
                    sendEffect(ChatEffect.ShowError("Usage: /pin <id|@user|text> [5m|30m|1h|indefinite]"))
                    return
                }

                val lastWord = raw.substringAfterLast(' ', "")
                val peeledDuration = if (raw.contains(' ') && lastWord.isNotEmpty())
                    SlashCommand.parsePinDuration(lastWord) else null
                val duration = peeledDuration ?: 0
                val body = if (peeledDuration != null) raw.substringBeforeLast(' ').trim() else raw
                val firstTok = body.substringBefore(' ').trim()

                when {
                    SlashCommand.isMessageId(firstTok) && body == firstTok ->
                        pinMessage(firstTok, duration)

                    firstTok.startsWith("@") -> {
                        val login = firstTok.removePrefix("@").lowercase()
                        val msg = state.value.messages.filterIsInstance<DisplayMessage.PrivMsg>()
                            .lastOrNull {
                                it.username.equals(login, true) &&
                                        SlashCommand.isMessageId(it.id) &&
                                        !it.isDeleted
                            }
                        if (msg != null) pinMessage(msg.id, duration)
                        else pinLatestModLogMessageFrom(login, duration)
                    }

                    else -> sendAndPin(body, duration)
                }
            }

            is SlashCommand.Parsed.Unpin -> {
                if (!requireMod()) return
                unpinMessageRemote()
            }

            is SlashCommand.Parsed.Announce -> {
                if (!requireMod()) return
                val r = apiClient.sendAnnouncement(
                    token,
                    broadcasterId,
                    moderatorId,
                    cmd.text,
                    cmd.color
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to send announcement"))
            }

            is SlashCommand.Parsed.Slow -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("slow_mode" to true, "slow_mode_wait_time" to cmd.seconds)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to set slow-mode"))
            }

            SlashCommand.Parsed.SlowOff -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("slow_mode" to false)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to disable slow-mode"))
            }

            is SlashCommand.Parsed.Followers -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("follower_mode" to true, "follower_mode_duration" to cmd.seconds / 60)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to set follower-mode"))
            }

            SlashCommand.Parsed.FollowersOff -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("follower_mode" to false)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to disable follower-mode"))
            }

            SlashCommand.Parsed.SubsOnly -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("subscriber_mode" to true)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to enable sub-only"))
            }

            SlashCommand.Parsed.SubsOnlyOff -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("subscriber_mode" to false)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to disable sub-only"))
            }

            SlashCommand.Parsed.EmoteOnly -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("emote_mode" to true)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to enable emote-only"))
            }

            SlashCommand.Parsed.EmoteOnlyOff -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("emote_mode" to false)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to disable emote-only"))
            }

            SlashCommand.Parsed.UniqueOn -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("unique_chat_mode" to true)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to enable unique-chat"))
            }

            SlashCommand.Parsed.UniqueOff -> {
                if (!requireMod()) return
                val r = apiClient.updateChatSettings(
                    token,
                    broadcasterId,
                    moderatorId,
                    mapOf("unique_chat_mode" to false)
                )
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to disable unique-chat"))
            }

            is SlashCommand.Parsed.Vip -> {
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Broadcaster-only command"))
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.addVip(token, broadcasterId, tid)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to add VIP"))
            }

            is SlashCommand.Parsed.UnVip -> {
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Broadcaster-only command"))
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.removeVip(token, broadcasterId, tid)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to remove VIP"))
            }

            is SlashCommand.Parsed.Mod -> {
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Broadcaster-only command"))
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.addModerator(token, broadcasterId, tid)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to add mod"))
            }

            is SlashCommand.Parsed.UnMod -> {
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Broadcaster-only command"))
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.removeModerator(token, broadcasterId, tid)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to remove mod"))
            }

            is SlashCommand.Parsed.Shoutout -> {
                if (!requireMod()) return
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.sendShoutout(token, broadcasterId, tid, moderatorId)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to send shoutout"))
            }

            is SlashCommand.Parsed.Color -> {
                val r = apiClient.updateUserChatColor(token, moderatorId, cmd.value)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to change color"))
            }

            is SlashCommand.Parsed.Warn -> {
                if (!requireMod()) return
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.warnUser(token, broadcasterId, moderatorId, tid, cmd.reason)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to warn user"))
            }

            SlashCommand.Parsed.Shield -> {
                if (!requireMod()) return
                val r = apiClient.updateShieldMode(token, broadcasterId, moderatorId, true)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to enable Shield Mode"))
            }

            SlashCommand.Parsed.ShieldOff -> {
                if (!requireMod()) return
                val r = apiClient.updateShieldMode(token, broadcasterId, moderatorId, false)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to disable Shield Mode"))
            }

            is SlashCommand.Parsed.Monitor -> {
                if (!requireMod()) return
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.addSuspiciousUser(token, broadcasterId, moderatorId, tid, restricted = false)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to monitor user"))
            }

            is SlashCommand.Parsed.Restrict -> {
                if (!requireMod()) return
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.addSuspiciousUser(token, broadcasterId, moderatorId, tid, restricted = true)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to restrict user"))
            }

            is SlashCommand.Parsed.Unmonitor -> {
                if (!requireMod()) return
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.removeSuspiciousUser(token, broadcasterId, moderatorId, tid)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to unmonitor user"))
            }

            is SlashCommand.Parsed.Unrestrict -> {
                if (!requireMod()) return
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.removeSuspiciousUser(token, broadcasterId, moderatorId, tid)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to unrestrict user"))
            }

            is SlashCommand.Parsed.Marker -> {
                if (!s.isBroadcaster) {
                    sendEffect(ChatEffect.ShowError("You must be the broadcaster to create a stream marker."))
                    return
                }
                val r = apiClient.createStreamMarker(token, broadcasterId, cmd.description)
                if (r.isError) {
                    sendEffect(ChatEffect.ShowError("Failed to create stream marker"))
                } else {
                    addMessage(
                        DisplayMessage.SystemMsg(
                            id = uniqueId("marker"),
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            channel = "#$channelLogin",
                            text = "Stream marker created.",
                            type = DisplayMessage.SystemMsg.SystemType.NOTICE
                        )
                    )
                }
            }

            is SlashCommand.Parsed.Me -> {

                sendViaHelixOnly(channelLogin, cmd.text, keepText = false, s)
            }

            is SlashCommand.Parsed.Whisper -> {
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found"))
                val r = apiClient.sendWhisper(token, moderatorId, tid, cmd.text)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to send whisper"))
            }

            is SlashCommand.Parsed.Block -> {
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found: ${cmd.targetLogin}"))
                handleBlockUser(tid, cmd.targetLogin)
            }

            is SlashCommand.Parsed.Unblock -> {
                val tid = resolveUserId(cmd.targetLogin)
                    ?: return sendEffect(ChatEffect.ShowError("User not found: ${cmd.targetLogin}"))
                handleUnblockUser(tid, cmd.targetLogin)
            }

            SlashCommand.Parsed.Help -> {
                val supported =
                    SlashCommand.ALL.joinToString("\n") { "${it.usage} — ${it.description}" }
                systemNotice(channelLogin, "Supported commands:\n$supported")
            }

            is SlashCommand.Parsed.Spam -> {
                if (spamJob?.isActive == true) {
                    return sendEffect(ChatEffect.ShowError("Spam already running. Use /spam stop first"))
                }
                val n = cmd.count.coerceIn(1, SlashCommand.MAX_SPAM_COUNT)
                val text = cmd.message
                val delay = cmd.delayMs.coerceIn(SlashCommand.MIN_SPAM_DELAY_MS, SlashCommand.MAX_SPAM_DELAY_MS)
                spamJob = viewModelScope.launch {
                    repeat(n) { i ->
                        try {
                            val payload = if (i == 0) text else text + INVISIBLE_DUP_MARKER.repeat(i)
                            sendViaHelixOnly(channelLogin, payload, keepText = true, state.value)
                        } catch (e: Exception) {
                            Napier.w("Spam iteration $i failed: ${e.message}", tag = TAG)
                        }
                        if (i < n - 1) kotlinx.coroutines.delay(delay)
                    }
                    spamJob = null
                }
            }

            is SlashCommand.Parsed.Poll -> {
                if (useGqlEvents) {
                    if (!s.canModerate) return sendEffect(ChatEffect.ShowError("Mod-only command"))
                    when (val r = twitchGqlClient.createPollGql(
                        channelId = broadcasterId,
                        title = cmd.title,
                        choices = cmd.choices,
                        durationSeconds = cmd.durationSeconds,
                        pointsPerVote = null,
                        token = gqlEventToken
                    )) {
                        is io.rudione.chatone.util.Result.Success -> {
                            systemNotice(
                                channelLogin,
                                "Poll started: \"${cmd.title}\" — ${cmd.choices.joinToString(" | ")} (${cmd.durationSeconds}s)"
                            )
                            syncPollAfterCreate(channelLogin, broadcasterId)
                        }
                        is io.rudione.chatone.util.Result.Error -> sendEffect(ChatEffect.ShowError("Failed to start poll: ${r.exception.message}"))
                        else -> {}
                    }
                    return
                }
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster can start a poll (set a first-party token in Settings to run polls as a moderator)"))
                val r = apiClient.createPoll(
                    accessToken = token,
                    broadcasterId = broadcasterId,
                    title = cmd.title,
                    choices = cmd.choices,
                    durationSeconds = cmd.durationSeconds
                )
                when (r) {
                    is io.rudione.chatone.util.Result.Success -> {
                        update { it.copy(activePollId = r.data.id, livePoll = r.data) }
                        systemNotice(
                            channelLogin,
                            "Poll started: \"${cmd.title}\" — ${cmd.choices.joinToString(" | ")} (${cmd.durationSeconds}s)"
                        )
                        startPollPolling()
                    }

                    is io.rudione.chatone.util.Result.Error -> sendEffect(ChatEffect.ShowError("Failed to start poll: ${r.exception.message}"))
                    else -> {}
                }
            }

            SlashCommand.Parsed.EndPoll -> {
                if (useGqlEvents) {
                    if (!s.canModerate) return sendEffect(ChatEffect.ShowError("Mod-only command"))
                    val pollId = s.activePollId
                        ?: twitchGqlClient.getViewablePollGql(channelLogin, gqlEventToken)?.takeIf {
                            it.status.equals("ACTIVE", ignoreCase = true)
                        }?.id
                        ?: return sendEffect(ChatEffect.ShowError("No active poll"))
                    val r = twitchGqlClient.terminatePollGql(pollId, s.currentUserId, gqlEventToken)
                    if (r.isError) sendEffect(ChatEffect.ShowError("Failed to end poll"))
                    else {
                        update { it.copy(activePollId = null, livePoll = null) }
                        pollPollingJob?.cancel()
                    }
                    return
                }
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster can end a poll"))
                val pollId = s.activePollId ?: run {
                    val active = apiClient.getActivePoll(token, broadcasterId)
                    if (active is io.rudione.chatone.util.Result.Success) active.data?.id else null
                }
                if (pollId == null) return sendEffect(ChatEffect.ShowError("No active poll"))
                val r = apiClient.endPoll(token, broadcasterId, pollId, "TERMINATED")
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to end poll"))
                else {
                    update { it.copy(activePollId = null, livePoll = null) }
                    pollPollingJob?.cancel()
                }
            }

            SlashCommand.Parsed.CancelPoll -> {
                if (useGqlEvents) {
                    if (!s.canModerate) return sendEffect(ChatEffect.ShowError("Mod-only command"))
                    val pollId = s.activePollId
                        ?: twitchGqlClient.getViewablePollGql(channelLogin, gqlEventToken)?.id
                        ?: return sendEffect(ChatEffect.ShowError("No active poll"))
                    val r = twitchGqlClient.archivePollGql(pollId, gqlEventToken)
                    if (r.isError) sendEffect(ChatEffect.ShowError("Failed to cancel poll"))
                    else {
                        update { it.copy(activePollId = null, livePoll = null) }
                        pollPollingJob?.cancel()
                    }
                    return
                }
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster can cancel a poll"))
                val pollId = s.activePollId ?: run {
                    val active = apiClient.getActivePoll(token, broadcasterId)
                    if (active is io.rudione.chatone.util.Result.Success) active.data?.id else null
                }
                if (pollId == null) return sendEffect(ChatEffect.ShowError("No active poll"))
                val r = apiClient.endPoll(token, broadcasterId, pollId, "ARCHIVED")
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to cancel poll"))
                else {
                    update { it.copy(activePollId = null, livePoll = null) }
                    pollPollingJob?.cancel()
                }
            }

            is SlashCommand.Parsed.Prediction -> {
                if (useGqlEvents) {
                    if (!s.canModerate) return sendEffect(ChatEffect.ShowError("Mod-only command"))
                    when (val r = twitchGqlClient.createPredictionGql(
                        channelId = broadcasterId,
                        title = cmd.title,
                        outcomes = cmd.outcomes,
                        windowSeconds = cmd.windowSeconds,
                        token = gqlEventToken
                    )) {
                        is io.rudione.chatone.util.Result.Success -> {
                            systemNotice(
                                channelLogin,
                                "Prediction started: \"${cmd.title}\" — ${cmd.outcomes.joinToString(" | ")} (${cmd.windowSeconds}s)"
                            )
                            syncPredictionAfterCreate(channelLogin, broadcasterId)
                        }
                        is io.rudione.chatone.util.Result.Error -> sendEffect(ChatEffect.ShowError("Failed to start prediction: ${r.exception.message}"))
                        else -> {}
                    }
                    return
                }
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster can start a prediction (set a first-party token in Settings to run predictions as a moderator)"))
                val r = apiClient.createPrediction(
                    accessToken = token,
                    broadcasterId = broadcasterId,
                    title = cmd.title,
                    outcomes = cmd.outcomes,
                    predictionWindowSeconds = cmd.windowSeconds
                )
                when (r) {
                    is io.rudione.chatone.util.Result.Success -> {
                        update {
                            it.copy(
                                activePredictionId = r.data.id,
                                activePredictionOutcomes = r.data.outcomes.map { o -> o.id to o.title },
                                livePrediction = r.data
                            )
                        }
                        systemNotice(
                            channelLogin,
                            "Prediction started: \"${cmd.title}\" — ${cmd.outcomes.joinToString(" | ")} (${cmd.windowSeconds}s)"
                        )
                        startPredictionPolling()
                    }

                    is io.rudione.chatone.util.Result.Error -> sendEffect(ChatEffect.ShowError("Failed to start prediction: ${r.exception.message}"))
                    else -> {}
                }
            }

            SlashCommand.Parsed.LockPrediction -> {
                if (useGqlEvents) {
                    if (!s.canModerate) return sendEffect(ChatEffect.ShowError("Mod-only command"))
                    val pid = s.activePredictionId
                        ?: twitchGqlClient.getActivePredictionGql(channelLogin, gqlEventToken)?.id
                        ?: return sendEffect(ChatEffect.ShowError("No active prediction"))
                    val r = twitchGqlClient.lockPredictionGql(pid, gqlEventToken)
                    if (r.isError) sendEffect(ChatEffect.ShowError("Failed to lock prediction"))
                    return
                }
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster can lock a prediction"))
                val pid = s.activePredictionId ?: run {
                    val active = apiClient.getActivePrediction(token, broadcasterId)
                    if (active is io.rudione.chatone.util.Result.Success) active.data?.id else null
                }
                if (pid == null) return sendEffect(ChatEffect.ShowError("No active prediction"))
                val r = apiClient.endPrediction(token, broadcasterId, pid, "LOCKED")
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to lock prediction"))
            }

            SlashCommand.Parsed.CancelPrediction -> {
                if (useGqlEvents) {
                    if (!s.canModerate) return sendEffect(ChatEffect.ShowError("Mod-only command"))
                    val pid = s.activePredictionId
                        ?: twitchGqlClient.getActivePredictionGql(channelLogin, gqlEventToken)?.id
                        ?: return sendEffect(ChatEffect.ShowError("No active prediction"))
                    val r = twitchGqlClient.cancelPredictionGql(pid, gqlEventToken)
                    if (r.isError) sendEffect(ChatEffect.ShowError("Failed to cancel prediction"))
                    else {
                        update {
                            it.copy(
                                activePredictionId = null,
                                activePredictionOutcomes = emptyList(),
                                livePrediction = null
                            )
                        }
                        predictionPollingJob?.cancel()
                    }
                    return
                }
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster can cancel a prediction"))
                val pid = s.activePredictionId ?: run {
                    val active = apiClient.getActivePrediction(token, broadcasterId)
                    if (active is io.rudione.chatone.util.Result.Success) active.data?.id else null
                }
                if (pid == null) return sendEffect(ChatEffect.ShowError("No active prediction"))
                val r = apiClient.endPrediction(token, broadcasterId, pid, "CANCELED")
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to cancel prediction"))
                else {
                    update {
                        it.copy(
                            activePredictionId = null,
                            activePredictionOutcomes = emptyList(),
                            livePrediction = null
                        )
                    }
                    predictionPollingJob?.cancel()
                }
            }

            is SlashCommand.Parsed.CompletePrediction -> {
                if (useGqlEvents) {
                    if (!s.canModerate) return sendEffect(ChatEffect.ShowError("Mod-only command"))
                    val info = twitchGqlClient.getActivePredictionGql(channelLogin, gqlEventToken)
                        ?: return sendEffect(ChatEffect.ShowError("No active prediction"))
                    if (info.outcomes.isEmpty()) return sendEffect(ChatEffect.ShowError("No active prediction"))
                    val ref = cmd.outcomeRef.trim()
                    val winning = ref.toIntOrNull()?.let { idx -> info.outcomes.getOrNull(idx - 1)?.id }
                        ?: info.outcomes.firstOrNull { it.title.equals(ref, ignoreCase = true) }?.id
                        ?: info.outcomes.firstOrNull { it.title.startsWith(ref, ignoreCase = true) }?.id
                    if (winning == null) return sendEffect(ChatEffect.ShowError("Outcome not found: $ref. Try a number (1..${info.outcomes.size}) or exact title."))
                    val r = twitchGqlClient.resolvePredictionGql(info.id, winning, gqlEventToken)
                    if (r.isError) sendEffect(ChatEffect.ShowError("Failed to complete prediction"))
                    else {
                        update {
                            it.copy(
                                activePredictionId = null,
                                activePredictionOutcomes = emptyList(),
                                livePrediction = null
                            )
                        }
                        predictionPollingJob?.cancel()
                    }
                    return
                }
                if (!s.isBroadcaster) return sendEffect(ChatEffect.ShowError("Only the broadcaster can complete a prediction"))
                val (pid, outcomes) = if (s.activePredictionId != null) {
                    s.activePredictionId!! to s.activePredictionOutcomes
                } else {
                    val active = apiClient.getActivePrediction(token, broadcasterId)
                    if (active is io.rudione.chatone.util.Result.Success && active.data != null) {
                        active.data.id to active.data.outcomes.map { it.id to it.title }
                    } else "" to emptyList()
                }
                if (pid.isEmpty() || outcomes.isEmpty()) return sendEffect(ChatEffect.ShowError("No active prediction"))
                val ref = cmd.outcomeRef.trim()
                val winning = ref.toIntOrNull()?.let { idx ->
                    outcomes.getOrNull(idx - 1)?.first
                } ?: outcomes.firstOrNull { (_, title) ->
                    title.equals(
                        ref,
                        ignoreCase = true
                    )
                }?.first
                if (winning == null) return sendEffect(ChatEffect.ShowError("Outcome not found: $ref. Try a number (1..${outcomes.size}) or exact title."))
                val r = apiClient.endPrediction(token, broadcasterId, pid, "RESOLVED", winning)
                if (r.isError) sendEffect(ChatEffect.ShowError("Failed to complete prediction"))
                else {
                    update {
                        it.copy(
                            activePredictionId = null,
                            activePredictionOutcomes = emptyList(),
                            livePrediction = null
                        )
                    }
                    predictionPollingJob?.cancel()
                }
            }

            SlashCommand.Parsed.SpamStop -> {
                val job = spamJob
                if (job?.isActive == true) {
                    job.cancel()
                    spamJob = null
                    systemNotice(channelLogin, "Spam cancelled.")
                } else {
                    sendEffect(ChatEffect.ShowError("No spam running"))
                }
            }

            is SlashCommand.Parsed.User -> {
                val targetLogin = cmd.targetLogin
                val targetUserId = resolveUserId(targetLogin) ?: ""
                val targetMsg = state.value.messages
                    .filterIsInstance<DisplayMessage.PrivMsg>()
                    .lastOrNull { it.username.equals(targetLogin, ignoreCase = true) }
                val displayName = targetMsg?.displayName ?: targetLogin
                val color = targetMsg?.color
                sendEffect(
                    ChatEffect.OpenUserProfile(
                        targetUserId,
                        targetLogin,
                        displayName,
                        color
                    )
                )
            }

            SlashCommand.Parsed.Points -> {
                if (!useGqlEvents) return sendEffect(ChatEffect.ShowError(GQL_TOKEN_REQUIRED_MSG))
                val info = twitchGqlClient.getChannelPointRewardsGql(channelLogin, gqlEventToken)
                    ?: return sendEffect(ChatEffect.ShowError("Failed to fetch channel points balance"))
                systemNotice(channelLogin, "Channel points balance: ${info.balance}")
            }

            is SlashCommand.Parsed.Redeem -> {
                if (!useGqlEvents) return sendEffect(ChatEffect.ShowError(GQL_TOKEN_REQUIRED_MSG))
                val info = twitchGqlClient.getChannelPointRewardsGql(channelLogin, gqlEventToken)
                    ?: return sendEffect(ChatEffect.ShowError("Failed to fetch channel point rewards"))
                val query = cmd.rewardQuery.trim()
                val reward = info.rewards.firstOrNull { it.title.equals(query, ignoreCase = true) }
                    ?: info.rewards.firstOrNull { it.title.contains(query, ignoreCase = true) }
                    ?: return sendEffect(ChatEffect.ShowError("Reward not found: $query"))
                if (!reward.isEnabled || !reward.isInStock) {
                    return sendEffect(ChatEffect.ShowError("Reward unavailable: ${reward.title}"))
                }
                if (reward.isUserInputRequired && cmd.textInput.isBlank()) {
                    return sendEffect(ChatEffect.ShowError("This reward needs text input: /redeem ${reward.title} | your text"))
                }
                val r = twitchGqlClient.redeemCustomRewardGql(
                    info.channelId, reward, cmd.textInput, gqlEventToken
                )
                when (r) {
                    is io.rudione.chatone.util.Result.Success ->
                        systemNotice(channelLogin, "Redeemed \"${reward.title}\" — balance: ${r.data.balance}")
                    is io.rudione.chatone.util.Result.Error ->
                        sendEffect(ChatEffect.ShowError("Failed to redeem: ${r.exception.message}"))
                    else -> {}
                }
            }

            is SlashCommand.Parsed.Unknown -> {
                sendEffect(ChatEffect.ShowError("Unknown command: /${cmd.name}. Type /help for a list."))
            }

            is SlashCommand.Parsed.BadUsage -> {
                when (cmd.name) {
                    "poll" -> openPollCreation()
                    "prediction" -> openPredictionCreation()
                    else -> sendEffect(ChatEffect.ShowError("Usage: ${cmd.usage}"))
                }
            }
        }
    }

    private suspend fun sendViaIrc(channelLogin: String, message: String, s: ChatState) {
        try {
            sendMessageUseCase(channelLogin, message)
            Napier.d("Sent IRC command to #$channelLogin: $message", tag = TAG)
        } catch (e: Exception) {
            Napier.e("IRC send failed: ${e.message}", e, tag = TAG)
            sendEffect(ChatEffect.ShowError("Failed to send command"))
        }
    }

    private suspend fun sendViaHelixOnly(
        channelLogin: String,
        message: String,
        keepText: Boolean,
        s: ChatState
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val localId = uniqueId("local")

        val ownBadges: List<Badge> = if (currentUserBadgeRaw.isNotEmpty()) {

            val parsed = currentUserBadgeRaw.split(",").mapNotNull { pair ->
                val parts = pair.split("/", limit = 2)
                if (parts.size == 2) {
                    val months = when (parts[0].lowercase()) {
                        "subscriber", "founder", "sub-gifter" -> parts[1].toIntOrNull()
                        else -> null
                    }
                    Badge(id = parts[0], version = parts[1], imageUrl = "", months = months)
                } else null
            }
            badgeRepository.resolveBadges(parsed, s.channelId.ifEmpty { null })
        } else {

            buildList {
                if (s.isBroadcaster) add(Badge("broadcaster", "1", ""))
                if (s.isGrandMod) add(Badge("sub-gifter", "5", ""))
                if (s.isMod && !s.isBroadcaster) add(Badge("moderator", "1", ""))
            }
        }

        val parent = s.replyingTo
        val rawMsg = ChatMessage(
            id = localId,
            channelId = s.channelId,
            channelName = channelLogin,
            userId = s.currentUserId,
            username = s.currentUserLogin,
            displayName = s.currentDisplayName.ifEmpty { s.currentUserLogin },
            message = message,
            timestamp = now,
            color = s.currentUserColor.ifEmpty { "#9146FF" },
            badges = ownBadges,
            isModerator = s.isMod,
            replyParentMsgId = parent?.id,
            replyParentUserLogin = parent?.username,
            replyParentDisplayName = parent?.displayName,
            replyParentMsgBody = parent?.tokens?.joinToString("") { t ->
                when (t) {
                    is MessageToken.Text -> t.text
                    is MessageToken.TwitchEmoteToken -> t.name
                    is MessageToken.ThirdPartyEmoteToken -> t.emote.code
                    is MessageToken.Link -> t.displayText
                    is MessageToken.Mention -> "@${t.username}"
                    is MessageToken.Cheer -> "${t.prefix}${t.amount}"
                }
            }
        )
        val displayMsg = chatMessageToDisplay(rawMsg)
        val newHistory = (listOf(message) + s.sentMessageHistory).take(MAX_HISTORY)

        update { state ->
            val newMessages = (state.messages + displayMsg).takeLast(appendCap)
            val evicted = (state.messages.size + 1 - newMessages.size).coerceAtLeast(0)
            state.copy(
                messageInput = if (keepText) state.messageInput else "",
                showEmoteCompletions = false,
                emoteCompletions = emptyList(),
                messages = newMessages,
                messagesSeq = state.messagesSeq + 1,
                messagesStartOrdinal = state.messagesStartOrdinal + evicted,
                replyingTo = if (keepText) state.replyingTo else null,
                sentMessageHistory = newHistory,
                historyIndex = -1
            )
        }
        sendEffect(ChatEffect.ScrollToBottom)

        if (s.currentAccessToken.isNotEmpty() && s.channelId.isNotEmpty()) {
            val replyId = s.replyingTo?.id?.takeIf { it.isNotEmpty() && !it.startsWith("local_") }
            val helixResult = apiClient.sendChatMessage(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                senderId = s.currentUserId,
                message = message,
                replyParentMessageId = replyId
            )

            when (helixResult) {
                is io.rudione.chatone.util.Result.Success -> {
                    val realId = helixResult.data.messageId
                    if (realId.isNotEmpty()) {

                        update { st ->
                            val idx = st.messages.indexOfLast { dm ->
                                dm is DisplayMessage.PrivMsg && dm.id == localId
                            }
                            if (idx != -1) {
                                val patched =
                                    (st.messages[idx] as DisplayMessage.PrivMsg).copy(id = realId)
                                val newMessages = st.messages.toMutableList()
                                newMessages[idx] = patched
                                st.copy(messages = newMessages)
                            } else st
                        }
                        Napier.d("Message sent via Helix, patched $localId → $realId", tag = TAG)
                    }
                }

                is io.rudione.chatone.util.Result.Error -> {
                    Napier.w(
                        "Helix send failed: ${helixResult.exception.message}, message still shown locally",
                        tag = TAG
                    )

                }

                is io.rudione.chatone.util.Result.Loading -> {}
            }
        } else {

            Napier.w("Cannot send via Helix: missing token or channelId", tag = TAG)
            sendEffect(ChatEffect.ShowError("Authentication required to send messages"))
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

    private fun toggleModMode() {
        update { it.copy(modModeEnabled = !it.modModeEnabled) }
    }

    private var streamStatePoller: kotlinx.coroutines.Job? = null
    private val previousStreamLive = mutableMapOf<String, Boolean>()

    private suspend fun sendBotMessage(text: String) {
        val s = state.value
        if (text.isBlank() || s.channelLogin.isEmpty()) return
        try {
            if (s.currentAccessToken.isNotEmpty() && s.channelId.isNotEmpty() && s.currentUserId.isNotEmpty()) {
                apiClient.sendChatMessage(
                    accessToken = s.currentAccessToken,
                    broadcasterId = s.channelId,
                    senderId = s.currentUserId,
                    message = text
                )
            } else {
                sendMessageUseCase(s.channelLogin, text)
            }
        } catch (e: Exception) {
            Napier.w("sendBotMessage failed: ${e.message}", tag = TAG)
        }
    }

    private suspend fun pollStreamState(channelLogin: String) {
        try {

            var seeded = false
            while (true) {
                val s = state.value
                if (s.channelLogin != channelLogin) return
                val token = s.currentAccessToken
                if (token.isNotEmpty()) {
                    val result = apiClient.getStreams(token, userLogins = listOf(channelLogin))
                    if (result is io.rudione.chatone.util.Result.Success) {
                        val isLive = result.data.data.isNotEmpty()
                        val stream = result.data.data.firstOrNull()
                        if (state.value.channelLogin == channelLogin) {
                            update { it.copy(liveStream = stream) }
                        }
                        stream?.userName?.takeIf { it.isNotBlank() }?.let { dn ->
                            if (state.value.channelLogin == channelLogin && state.value.channelDisplayName != dn) {
                                update { it.copy(channelDisplayName = dn) }
                            }
                        }
                        val key = channelLogin.lowercase()
                        val prev = previousStreamLive[key]
                        previousStreamLive[key] = isLive
                        if (seeded && prev != null && prev != isLive) {
                            val rules = automodRepository.chatRules.value
                            if (isLive) {
                                ChatRuleEventEngine.fireStreamOnline(
                                    scope = viewModelScope,
                                    channelLogin = channelLogin,
                                    rules = rules,
                                    send = { text -> sendBotMessage(text) }
                                )
                            } else {
                                ChatRuleEventEngine.fireStreamOffline(
                                    scope = viewModelScope,
                                    channelLogin = channelLogin,
                                    rules = rules,
                                    send = { text -> sendBotMessage(text) }
                                )
                            }
                        }
                        seeded = true
                    }
                }
                kotlinx.coroutines.delay(120_000L)
            }
        } catch (_: kotlinx.coroutines.CancellationException) {

        } catch (e: Exception) {
            Napier.w("Stream poller error: ${e.message}", tag = TAG)
        }
    }

    private data class PendingModEntry(val moderatorLogin: String, val reason: String?, val timestamp: Long)

    private val pendingModerationActors = mutableMapOf<String, PendingModEntry>()
    private val pendingDeleteActors = mutableMapOf<String, Pair<String, Long>>()
    private val MOD_ACTOR_TTL_MS = 15_000L

    private fun rememberPendingModerator(targetUserId: String, moderatorLogin: String, reason: String? = null) {
        if (targetUserId.isBlank() || moderatorLogin.isBlank()) return
        pendingModerationActors[targetUserId] =
            PendingModEntry(moderatorLogin, reason, Clock.System.now().toEpochMilliseconds())
    }

    private fun rememberPendingDeleteModerator(messageId: String, moderatorLogin: String) {
        if (messageId.isBlank() || moderatorLogin.isBlank()) return
        pendingDeleteActors[messageId] = moderatorLogin to Clock.System.now().toEpochMilliseconds()
    }

    private fun consumePendingDeleteModerator(messageId: String): String? {
        if (messageId.isBlank()) return null
        val now = Clock.System.now().toEpochMilliseconds()
        val stale = pendingDeleteActors.filter { now - it.value.second > MOD_ACTOR_TTL_MS }.keys
        stale.forEach { pendingDeleteActors.remove(it) }
        return pendingDeleteActors.remove(messageId)?.first
    }

    private fun consumePendingModerator(targetLogin: String?, targetUserId: String?): PendingModEntry? {
        val now = Clock.System.now().toEpochMilliseconds()

        val stale = pendingModerationActors.filter { now - it.value.timestamp > MOD_ACTOR_TTL_MS }.keys
        stale.forEach { pendingModerationActors.remove(it) }
        if (!targetUserId.isNullOrBlank()) {
            pendingModerationActors.remove(targetUserId)?.let { return it }
        }

        if (!targetLogin.isNullOrBlank()) {
            val matchedUserId = state.value.messages
                .filterIsInstance<DisplayMessage.PrivMsg>()
                .lastOrNull { it.username.equals(targetLogin, ignoreCase = true) }
                ?.userId
            if (!matchedUserId.isNullOrBlank()) {
                pendingModerationActors.remove(matchedUserId)?.let { return it }
            }
        }
        return null
    }

    private fun timeoutUser(userId: String, duration: Int) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        val reason = cachedSettings().savedTimeoutReason.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            val result = apiClient.banUser(
                s.currentAccessToken,
                s.channelId,
                s.currentUserId,
                userId,
                duration,
                reason
            )
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to timeout user"))
            else {
                rememberPendingModerator(userId, s.currentUserLogin)
                denyPendingAutoModFor(userId)
            }
        }
    }

    private fun banUser(userId: String, overrideReason: String? = null) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        val reason = overrideReason?.takeIf { it.isNotBlank() }
            ?: cachedSettings().savedBanReason.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            val result =
                apiClient.banUser(
                    s.currentAccessToken,
                    s.channelId,
                    s.currentUserId,
                    userId,
                    null,
                    reason
                )
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to ban user"))
            else {
                rememberPendingModerator(userId, s.currentUserLogin)
                denyPendingAutoModFor(userId)
            }
        }
    }

    private fun denyPendingAutoModFor(targetUserId: String) {
        if (targetUserId.isEmpty()) return
        val pendingIds = state.value.messages
            .filterIsInstance<DisplayMessage.AutoModMsg>()
            .filter {
                it.userId == targetUserId &&
                        it.status == DisplayMessage.AutoModMsg.AutoModStatus.PENDING
            }
            .map { it.msgId }
            .distinct()
        pendingIds.forEach { handleAutoMod(it, "DENY") }
    }

    private fun deleteMessage(messageId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        val actorLogin = s.currentUserLogin
        if (actorLogin.isNotBlank()) rememberPendingDeleteModerator(messageId, actorLogin)

        val cacheKey = s.channelLogin.lowercase()
        update { st ->
            st.copy(messages = st.messages.map { dm ->
                if (dm is DisplayMessage.PrivMsg && dm.id == messageId) dm.copy(isDeleted = true) else dm
            })
        }

        channelMessageCache[cacheKey] = (channelMessageCache[cacheKey] ?: emptyList()).map { dm ->
            if (dm is DisplayMessage.PrivMsg && dm.id == messageId) dm.copy(isDeleted = true) else dm
        }

        viewModelScope.launch {
            val result = apiClient.deleteMessage(
                s.currentAccessToken,
                s.channelId,
                s.currentUserId,
                messageId
            )
            if (result.isError) {

                update { st ->
                    st.copy(messages = st.messages.map { dm ->
                        if (dm is DisplayMessage.PrivMsg && dm.id == messageId) dm.copy(isDeleted = false) else dm
                    })
                }
                channelMessageCache[cacheKey] =
                    (channelMessageCache[cacheKey] ?: emptyList()).map { dm ->
                        if (dm is DisplayMessage.PrivMsg && dm.id == messageId) dm.copy(isDeleted = false) else dm
                    }
                sendEffect(ChatEffect.ShowError("Failed to delete message"))
            }
        }
    }

    private fun unbanUser(userId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result =
                apiClient.unbanUser(s.currentAccessToken, s.channelId, s.currentUserId, userId)
            if (result.isError) {
                sendEffect(ChatEffect.ShowError("Failed to unban user"))
            } else {
                val targetLogin = s.messages
                    .filterIsInstance<DisplayMessage.PrivMsg>()
                    .lastOrNull { it.userId == userId }
                    ?.username
                    ?: userId
                val showActor = s.canModerate
                val actor = s.currentUserLogin
                val text = buildString {
                    append("$targetLogin was unbanned")
                    if (showActor && actor.isNotBlank()) append(" by $actor")
                }
                addModerationNotice(
                    action = DisplayMessage.ModerationMsg.ModerationAction.UNBAN,
                    text = text,
                    targetUser = targetLogin,
                    moderatorLogin = if (showActor) actor.takeIf { it.isNotBlank() } else null,
                    idPrefix = "unban"
                )
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
        update { it.copy(messageInput = "/w $username ") }
    }

    private fun insertMention(displayName: String) {
        update { st ->
            val current = st.messageInput
            val tag = "@$displayName "
            val newInput = when {
                current.isEmpty() -> tag
                current.endsWith(" ") -> "$current$tag"
                else -> "$current $tag"
            }

            st.copy(
                messageInput = newInput,
                showMentionCompletions = false,
                mentionCompletions = emptyList()
            )
        }
    }

    private fun updateChatSettings(settings: Map<String, Any>) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.updateChatSettings(
                s.currentAccessToken,
                s.channelId,
                s.currentUserId,
                settings
            )
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to update chat settings"))
        }
    }

    private fun syntheticPrivMsg(
        messageId: String,
        channel: String,
        userId: String,
        username: String,
        displayName: String,
        color: String?,
        text: String,
        timestampMs: Long
    ): DisplayMessage.PrivMsg = DisplayMessage.PrivMsg(
        id = messageId,
        timestamp = timestampMs,
        channel = channel,
        userId = userId,
        username = username,
        displayName = displayName.ifBlank { username },
        tokens = listOf(io.rudione.chatone.util.chat.MessageToken.Text(text)),
        color = color,
        badges = emptyList(),
        isModerator = false,
        isSubscriber = false,
        isVip = false,
        isBroadcaster = false,
        isMention = false,
        isAction = false
    )

    private fun applyPinPayload(pin: IrcEvent.PinnedChatPayload) {
        val s = state.value
        val local = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
            .firstOrNull { it.id == pin.messageId && pin.messageId.isNotBlank() }
        val displayMsg = local ?: syntheticPrivMsg(
            messageId = pin.messageId.ifBlank { pin.pinId },
            channel = s.channelLogin,
            userId = pin.senderId,
            username = pin.senderLogin,
            displayName = pin.senderName,
            color = pin.senderColor,
            text = pin.text,
            timestampMs = Clock.System.now().toEpochMilliseconds()
        )
        val pinnerBadges = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
            .lastOrNull {
                (pin.pinnerUserId.isNotBlank() && it.userId == pin.pinnerUserId) ||
                        (pin.pinnerLogin.isNotBlank() && it.username.equals(pin.pinnerLogin, ignoreCase = true))
            }?.badges ?: emptyList()
        update {
            it.copy(
                pinnedMessage = displayMsg,
                pinId = pin.pinId.ifBlank { null },
                pinEndsAtMs = pin.endsAtEpochMs,
                pinnedByName = pin.pinnerName.ifBlank { null },
                pinnedByBadges = pinnerBadges,
                pinLocallyHidden = false
            )
        }
    }

    private fun fetchCurrentPin(channelId: String) {
        viewModelScope.launch {

            if (!moderationAuthStore.hasCustomToken()) return@launch
            val token = moderationAuthStore.resolveToken(state.value.currentAccessToken)
            if (token.isBlank()) return@launch
            val pin = twitchGqlClient.getPinnedChat(channelId, token)
            if (pin == null) {

                return@launch
            }
            val s = state.value
            val local = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
                .firstOrNull { it.id == pin.messageId }
            val displayMsg = local ?: syntheticPrivMsg(
                messageId = pin.messageId.ifBlank { pin.pinId },
                channel = s.channelLogin,
                userId = pin.authorId,
                username = pin.authorLogin,
                displayName = pin.authorName,
                color = pin.authorColor,
                text = pin.text,
                timestampMs = pin.pinnedAtEpochMs ?: Clock.System.now().toEpochMilliseconds()
            )
            update {
                if (it.channelId == channelId)
                    it.copy(pinnedMessage = displayMsg, pinId = pin.pinId, pinEndsAtMs = pin.endsAtEpochMs)
                else it
            }
        }
    }

    private fun pinMessage(messageId: String, durationSeconds: Int = 0) {
        val s = state.value
        val msg = s.messages.filterIsInstance<DisplayMessage.PrivMsg>()
            .firstOrNull { it.id == messageId }

        if (!SlashCommand.isMessageId(messageId)) {
            sendEffect(ChatEffect.ShowError(pinFailedText("message is no longer pinnable")))
            return
        }
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) {
            sendEffect(ChatEffect.ShowError(pinFailedText("not connected to the channel")))
            return
        }

        val previousPinned = s.pinnedMessage
        val previousEndsAt = s.pinEndsAtMs
        val optimisticEndsAt = if (durationSeconds > 0)
            Clock.System.now().toEpochMilliseconds() + durationSeconds * 1000L else null
        if (msg != null) update { it.copy(pinnedMessage = msg, pinEndsAtMs = optimisticEndsAt) }

        viewModelScope.launch {
            val gqlToken = moderationAuthStore.resolveToken(s.currentAccessToken)
            when (val r = twitchGqlClient.pinMessage(s.channelId, messageId, durationSeconds, gqlToken)) {
                is io.rudione.chatone.util.Result.Success -> update { it.copy(pinId = r.data) }
                is io.rudione.chatone.util.Result.Error -> {
                    update { it.copy(pinnedMessage = previousPinned, pinEndsAtMs = previousEndsAt) }
                    sendEffect(ChatEffect.ShowError(pinFailedText(r.exception.message ?: "unknown error")))
                }
                else -> {}
            }
        }
    }

    private fun pinFailedText(reason: String): String = "Pin failed: $reason"

    private fun pinLatestModLogMessageFrom(login: String, durationSeconds: Int) {
        val s = state.value
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) {
            sendEffect(ChatEffect.ShowError("No recent message from @$login to pin."))
            return
        }
        viewModelScope.launch {
            val userResult = apiClient.getUsers(s.currentAccessToken, logins = listOf(login))
            val user = (userResult as? io.rudione.chatone.util.Result.Success)?.data?.data?.firstOrNull()
            if (user == null) {
                sendEffect(ChatEffect.ShowError("No recent message from @$login to pin."))
                return@launch
            }
            val modLogMsg = twitchGqlClient.getLatestModLogMessageBySender(
                s.channelId, user.id, moderationAuthStore.resolveToken(s.currentAccessToken)
            )
            if (modLogMsg == null) {
                sendEffect(ChatEffect.ShowError("No recent message from @$login to pin."))
                return@launch
            }
            val synthetic = syntheticPrivMsg(
                messageId = modLogMsg.id,
                channel = s.channelLogin,
                userId = user.id,
                username = user.login,
                displayName = user.displayName,
                color = null,
                text = modLogMsg.text,
                timestampMs = modLogMsg.sentAtEpochMs ?: Clock.System.now().toEpochMilliseconds()
            )
            update { it.copy(pinnedMessage = synthetic) }
            pinMessage(modLogMsg.id, durationSeconds)
        }
    }

    private fun sendAndPin(text: String, durationSeconds: Int) {
        val s = state.value
        val sentAt = Clock.System.now().toEpochMilliseconds()
        sendRawMessage(text)
        viewModelScope.launch {
            val deadline = sentAt + 4000
            while (Clock.System.now().toEpochMilliseconds() < deadline) {
                kotlinx.coroutines.delay(150)
                val mine = state.value.messages.filterIsInstance<DisplayMessage.PrivMsg>()
                    .lastOrNull {
                        it.userId == s.currentUserId &&
                            SlashCommand.isMessageId(it.id) &&
                            it.timestamp >= sentAt - 500
                    }
                if (mine != null) {
                    pinMessage(mine.id, durationSeconds)
                    return@launch
                }
            }
            sendEffect(ChatEffect.ShowError("Could not find the sent message to pin. Try again."))
        }
    }

    private fun unpinMessageRemote() {
        val s = state.value
        update { it.copy(pinnedMessage = null, pinEndsAtMs = null, pinnedByName = null, pinnedByBadges = emptyList()) }
        val pinId = s.pinId
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) {
            update { it.copy(pinId = null) }
            return
        }
        viewModelScope.launch {
            val gqlToken = moderationAuthStore.resolveToken(s.currentAccessToken)
            val id = pinId ?: twitchGqlClient.getCurrentPinId(s.channelId, gqlToken)
            if (id != null) twitchGqlClient.unpinMessage(id, gqlToken)
            update { it.copy(pinId = null) }
        }
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
            val result = apiClient.sendAnnouncement(
                s.currentAccessToken,
                s.channelId,
                s.currentUserId,
                message,
                color
            )
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to send announcement"))
        }
    }

    private fun setPendingRaid(targetLogin: String, targetId: String) {
        update {
            it.copy(
                pendingRaidTarget = targetLogin,
                pendingRaidTargetId = targetId,
                pendingRaidStartedAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    private fun startRaid(targetLogin: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val usersResult = apiClient.getUsers(s.currentAccessToken, logins = listOf(targetLogin))
            if (usersResult !is io.rudione.chatone.util.Result.Success) {
                sendEffect(ChatEffect.ShowError("Failed to resolve user for raid"))
                return@launch
            }
            val targetId = usersResult.data.data.firstOrNull()?.id
            if (targetId == null) {
                sendEffect(ChatEffect.ShowError("User not found: $targetLogin"))
                return@launch
            }
            if (s.isBroadcaster) {
                val result = apiClient.startRaid(s.currentAccessToken, s.channelId, targetId)
                if (result.isError) sendEffect(ChatEffect.ShowError("Failed to start raid"))
                else setPendingRaid(targetLogin, targetId)
            } else if (s.isMod) {
                when (val r = twitchGqlClient.createRaid(
                    s.channelId, targetId, moderationAuthStore.resolveToken(s.currentAccessToken)
                )) {
                    is io.rudione.chatone.util.Result.Success -> setPendingRaid(targetLogin, targetId)
                    is io.rudione.chatone.util.Result.Error ->
                        sendEffect(ChatEffect.ShowError("Failed to start raid: ${r.exception.message}"))
                    else -> {}
                }
            } else {
                sendEffect(ChatEffect.ShowError("You must be the broadcaster or a moderator to start a raid."))
            }
        }
    }

    private fun cancelRaid() {
        val s = state.value

        update {
            it.copy(
                pendingRaidTarget = null,
                pendingRaidTargetId = null,
                pendingRaidStartedAt = 0L
            )
        }
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            if (s.isBroadcaster) {
                apiClient.cancelRaid(s.currentAccessToken, s.channelId)
            } else if (s.isMod) {
                twitchGqlClient.cancelRaidGql(
                    s.channelId, moderationAuthStore.resolveToken(s.currentAccessToken)
                )
            }
        }
    }

    private fun rememberPollHistory(title: String, choices: List<String>, durationSeconds: Int) {
        val entry = PollHistoryEntry(title, choices, durationSeconds)
        update {
            it.copy(recentPolls = (listOf(entry) + it.recentPolls.filterNot { p -> p.title == title }).take(5))
        }
    }

    private fun rememberPredictionHistory(title: String, outcomes: List<String>, windowSeconds: Int) {
        val entry = PredictionHistoryEntry(title, outcomes, windowSeconds)
        update {
            it.copy(recentPredictions = (listOf(entry) + it.recentPredictions.filterNot { p -> p.title == title }).take(5))
        }
    }

    private fun submitPollFromForm(title: String, choices: List<String>, durationSeconds: Int) {
        val s = state.value
        update { it.copy(showPollCreation = false) }
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) return
        viewModelScope.launch {
            val useGqlEvents = moderationAuthStore.hasCustomToken()
            if (useGqlEvents) {
                if (!s.canModerate) return@launch sendEffect(ChatEffect.ShowError("Mod-only command"))
                val gqlEventToken = moderationAuthStore.resolveToken(s.currentAccessToken)
                when (val r = twitchGqlClient.createPollGql(
                    channelId = s.channelId,
                    title = title,
                    choices = choices,
                    durationSeconds = durationSeconds,
                    pointsPerVote = null,
                    token = gqlEventToken
                )) {
                    is io.rudione.chatone.util.Result.Success -> {
                        rememberPollHistory(title, choices, durationSeconds)
                        systemNotice(
                            s.channelLogin,
                            "Poll started: \"$title\" — ${choices.joinToString(" | ")} (${durationSeconds}s)"
                        )
                        syncPollAfterCreate(s.channelLogin, s.channelId)
                    }
                    is io.rudione.chatone.util.Result.Error ->
                        sendEffect(ChatEffect.ShowError("Failed to start poll: ${r.exception.message}"))
                    else -> {}
                }
                return@launch
            }
            if (!s.isBroadcaster) return@launch sendEffect(ChatEffect.ShowError("Only the broadcaster can start a poll (set a first-party token in Settings to run polls as a moderator)"))
            when (val r = apiClient.createPoll(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                title = title,
                choices = choices,
                durationSeconds = durationSeconds
            )) {
                is io.rudione.chatone.util.Result.Success -> {
                    update { it.copy(activePollId = r.data.id, livePoll = r.data) }
                    rememberPollHistory(title, choices, durationSeconds)
                    systemNotice(
                        s.channelLogin,
                        "Poll started: \"$title\" — ${choices.joinToString(" | ")} (${durationSeconds}s)"
                    )
                    startPollPolling()
                }
                is io.rudione.chatone.util.Result.Error ->
                    sendEffect(ChatEffect.ShowError("Failed to start poll: ${r.exception.message}"))
                else -> {}
            }
        }
    }

    private fun submitPredictionFromForm(title: String, outcomes: List<String>, windowSeconds: Int) {
        val s = state.value
        update { it.copy(showPredictionCreation = false) }
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) return
        viewModelScope.launch {
            val useGqlEvents = moderationAuthStore.hasCustomToken()
            if (useGqlEvents) {
                if (!s.canModerate) return@launch sendEffect(ChatEffect.ShowError("Mod-only command"))
                val gqlEventToken = moderationAuthStore.resolveToken(s.currentAccessToken)
                when (val r = twitchGqlClient.createPredictionGql(
                    channelId = s.channelId,
                    title = title,
                    outcomes = outcomes,
                    windowSeconds = windowSeconds,
                    token = gqlEventToken
                )) {
                    is io.rudione.chatone.util.Result.Success -> {
                        rememberPredictionHistory(title, outcomes, windowSeconds)
                        systemNotice(
                            s.channelLogin,
                            "Prediction started: \"$title\" — ${outcomes.joinToString(" | ")} (${windowSeconds}s)"
                        )
                        syncPredictionAfterCreate(s.channelLogin, s.channelId)
                    }
                    is io.rudione.chatone.util.Result.Error ->
                        sendEffect(ChatEffect.ShowError("Failed to start prediction: ${r.exception.message}"))
                    else -> {}
                }
                return@launch
            }
            if (!s.isBroadcaster) return@launch sendEffect(ChatEffect.ShowError("Only the broadcaster can start a prediction (set a first-party token in Settings to run predictions as a moderator)"))
            val r = apiClient.createPrediction(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                title = title,
                outcomes = outcomes,
                predictionWindowSeconds = windowSeconds
            )
            when (r) {
                is io.rudione.chatone.util.Result.Success -> {
                    update {
                        it.copy(
                            activePredictionId = r.data.id,
                            activePredictionOutcomes = r.data.outcomes.map { o -> o.id to o.title },
                            livePrediction = r.data
                        )
                    }
                    rememberPredictionHistory(title, outcomes, windowSeconds)
                    systemNotice(
                        s.channelLogin,
                        "Prediction started: \"$title\" — ${outcomes.joinToString(" | ")} (${windowSeconds}s)"
                    )
                    startPredictionPolling()
                }
                is io.rudione.chatone.util.Result.Error ->
                    sendEffect(ChatEffect.ShowError("Failed to start prediction: ${r.exception.message}"))
                else -> {}
            }
        }
    }

    private fun openPointsBitsPanel() {
        val s = state.value
        update { it.copy(showPointsBitsPanel = true, pointsBitsLoading = true, pointsBitsError = null) }
        if (!moderationAuthStore.hasCustomToken()) {
            update {
                it.copy(
                    pointsBitsLoading = false,
                    pointsBitsError = GQL_TOKEN_REQUIRED_MSG
                )
            }
            return
        }
        viewModelScope.launch {
            val gqlToken = moderationAuthStore.resolveToken(s.currentAccessToken)
            val info = twitchGqlClient.getChannelPointRewardsGql(s.channelLogin, gqlToken)
            if (info == null) {
                update {
                    it.copy(
                        pointsBitsLoading = false,
                        pointsBitsError = "Failed to load channel points"
                    )
                }
            } else {
                update {
                    it.copy(
                        pointsBitsLoading = false,
                        pointsBalance = info.balance,
                        channelRewards = info.rewards,
                        pointsIconUrl = info.currencyIconUrl
                    )
                }
            }
        }
    }

    private fun redeemReward(reward: io.rudione.chatone.data.remote.GqlChannelPointReward, textInput: String) {
        val s = state.value
        if (!moderationAuthStore.hasCustomToken()) {
            return sendEffect(ChatEffect.ShowError(GQL_TOKEN_REQUIRED_MSG))
        }
        viewModelScope.launch {
            val gqlToken = moderationAuthStore.resolveToken(s.currentAccessToken)
            when (val r = twitchGqlClient.redeemCustomRewardGql(s.channelId, reward, textInput, gqlToken)) {
                is io.rudione.chatone.util.Result.Success -> {
                    update { it.copy(pointsBalance = r.data.balance) }
                    systemNotice(s.channelLogin, "Redeemed \"${reward.title}\"")
                }
                is io.rudione.chatone.util.Result.Error ->
                    sendEffect(ChatEffect.ShowError("Failed to redeem: ${r.exception.message}"))
                else -> {}
            }
        }
    }

    private fun raidNow() {
        val s = state.value
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) return
        if (!s.isBroadcaster && !s.isMod) {
            sendEffect(ChatEffect.ShowError("You must be the broadcaster or a moderator to send the raid now."))
            return
        }
        viewModelScope.launch {
            when (val r = twitchGqlClient.goRaidNow(
                s.channelId, moderationAuthStore.resolveToken(s.currentAccessToken)
            )) {
                is io.rudione.chatone.util.Result.Success ->
                    update { it.copy(pendingRaidTarget = null, pendingRaidTargetId = null, pendingRaidStartedAt = 0L) }
                is io.rudione.chatone.util.Result.Error ->
                    sendEffect(ChatEffect.ShowError("Failed to send raid now: ${r.exception.message}"))
                else -> {}
            }
        }
    }

    private fun sendShoutout(targetUserId: String) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return
        viewModelScope.launch {
            val result = apiClient.sendShoutout(
                s.currentAccessToken,
                s.channelId,
                targetUserId,
                s.currentUserId
            )
            if (result.isError) sendEffect(ChatEffect.ShowError("Failed to send shoutout"))
        }
    }

    fun triggerCommandHotkey(command: io.rudione.chatone.domain.model.ChatCommand) {
        val settings = cachedSettings()
        when (command.kind) {
            io.rudione.chatone.domain.model.ChatCommandKind.MACRO -> {
                val macro = command.macroId?.let { id ->
                    settings.macros.firstOrNull { it.id == id }
                } ?: return
                executeMacro(macro)
            }

            io.rudione.chatone.domain.model.ChatCommandKind.TEXT -> {
                if (command.replacement.isBlank()) return
                if (command.sendImmediately) {
                    update { it.copy(messageInput = command.replacement) }
                    sendMessage(keepText = false)
                } else {
                    update { it.copy(messageInput = command.replacement, historyIndex = -1) }
                    sendEffect(ChatEffect.FocusChatInput)
                }
            }
        }
    }

    private sealed class ChatCommandResult {
        data class Text(val text: String) : ChatCommandResult()
        data class Macro(val macro: io.rudione.chatone.domain.model.Macro) : ChatCommandResult()
        object None : ChatCommandResult()
    }

    private fun applyChatCommand(message: String): ChatCommandResult {
        val settings = cachedSettings()
        val commands = settings.chatCommands.filter { it.enabled && it.trigger.isNotBlank() }
        if (commands.isEmpty()) return ChatCommandResult.None
        val trimmed = message.trimStart()
        val leadingWs = message.length - trimmed.length
        val firstWordEnd = trimmed.indexOfFirst { it.isWhitespace() }.let {
            if (it == -1) trimmed.length else it
        }
        val firstWord = trimmed.substring(0, firstWordEnd)
        if (firstWord.isEmpty()) return ChatCommandResult.None

        if (firstWord.startsWith("/")) {
            val rest = trimmed.substring(firstWordEnd)
            val argTrimmed = rest.trimStart()
            if (argTrimmed.isEmpty()) return ChatCommandResult.None
            val gap = rest.length - argTrimmed.length
            val argEnd = argTrimmed.indexOfFirst { it.isWhitespace() }.let {
                if (it == -1) argTrimmed.length else it
            }
            val argWord = argTrimmed.substring(0, argEnd)
            val tail = argTrimmed.substring(argEnd)
            val match = commands.firstOrNull { it.trigger == argWord }
                ?: commands.firstOrNull { it.trigger.equals(argWord, ignoreCase = true) }
                ?: return ChatCommandResult.None
            if (match.kind != io.rudione.chatone.domain.model.ChatCommandKind.TEXT) return ChatCommandResult.None
            if (match.replacement.isBlank()) return ChatCommandResult.None
            if (match.wholeMessageOnly && tail.isNotBlank()) return ChatCommandResult.None
            val prefix = message.substring(0, leadingWs) + firstWord + rest.substring(0, gap)
            return ChatCommandResult.Text(prefix + match.replacement + tail)
        }

        val rest = trimmed.substring(firstWordEnd)
        val command = commands.firstOrNull { it.trigger == firstWord }
            ?: commands.firstOrNull { it.trigger.equals(firstWord, ignoreCase = true) }
            ?: return ChatCommandResult.None

        if (command.wholeMessageOnly && rest.isNotBlank()) return ChatCommandResult.None

        return when (command.kind) {
            io.rudione.chatone.domain.model.ChatCommandKind.MACRO -> {
                val macro = command.macroId?.let { id ->
                    settings.macros.firstOrNull { it.id == id }
                } ?: return ChatCommandResult.None
                ChatCommandResult.Macro(macro)
            }

            io.rudione.chatone.domain.model.ChatCommandKind.TEXT -> {
                if (command.replacement.isBlank()) return ChatCommandResult.None
                val prefix = message.substring(0, leadingWs)
                ChatCommandResult.Text(prefix + command.replacement + rest)
            }
        }
    }

    private fun executeMacro(macro: Macro) {
        val s = state.value
        if (s.channelLogin.isEmpty()) return
        viewModelScope.launch {
            macro.steps.forEach { step ->
                when (step) {
                    is MacroStep.SendMessage -> {
                        val times = step.repeatCount.coerceAtLeast(1)
                        repeat(times) { iteration ->
                            try {
                                sendMessageUseCase(s.channelLogin, step.text)
                                val now = Clock.System.now().toEpochMilliseconds() + iteration
                                val rawMsg = ChatMessage(
                                    id = "local_macro_${now}_$iteration",
                                    channelId = state.value.channelId,
                                    channelName = state.value.channelLogin,
                                    userId = state.value.currentUserId,
                                    username = state.value.currentUserLogin,
                                    displayName = state.value.currentDisplayName.ifEmpty { state.value.currentUserLogin },
                                    message = step.text,
                                    timestamp = now,
                                    color = state.value.currentUserColor.ifEmpty { "#9146FF" },
                                    isModerator = state.value.isMod
                                )
                                val displayMsg = chatMessageToDisplay(rawMsg)
                                update { st ->
                                    val newMessages =
                                        (st.messages + displayMsg).takeLast(appendCap)
                                    val evicted =
                                        (st.messages.size + 1 - newMessages.size).coerceAtLeast(0)
                                    st.copy(
                                        messages = newMessages,
                                        messagesSeq = st.messagesSeq + 1,
                                        messagesStartOrdinal = st.messagesStartOrdinal + evicted
                                    )
                                }
                                sendEffect(ChatEffect.ScrollToBottom)
                                if (iteration < times - 1) delay(300L)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    is MacroStep.InsertText ->
                        update { it.copy(messageInput = step.text) }

                    is MacroStep.Delay ->
                        repeat(step.repeatCount.coerceAtLeast(1)) { delay(step.seconds * 1000L) }

                    is MacroStep.SubMode ->
                        updateChatSettings(mapOf("subscriber_mode" to step.enable))

                    is MacroStep.EmoteMode ->
                        updateChatSettings(mapOf("emote_mode" to step.enable))

                    is MacroStep.SlowMode ->
                        if (step.enable) updateChatSettings(
                            mapOf(
                                "slow_mode" to true,
                                "slow_mode_wait_time" to step.seconds
                            )
                        )
                        else updateChatSettings(mapOf("slow_mode" to false))

                    is MacroStep.FollowerMode ->
                        if (step.enable) updateChatSettings(
                            mapOf(
                                "follower_mode" to true,
                                "follower_mode_duration" to step.minutes
                            )
                        )
                        else updateChatSettings(mapOf("follower_mode" to false))

                    is MacroStep.R9KMode ->
                        updateChatSettings(mapOf("unique_chat_mode" to step.enable))

                    is MacroStep.StartRaid ->
                        startRaid(step.targetLogin)

                    is MacroStep.PinMessage ->
                        try {
                            sendMessageUseCase(s.channelLogin, "/pin ${step.message}")
                        } catch (_: Exception) {
                        }

                    is MacroStep.ClearChat ->
                        clearChat()
                }
            }
        }
    }

    private fun applyLocalAutomod(message: ChatMessage) {
        val s = state.value
        if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) return

        val target = AutomodTarget(
            userId = message.userId,
            username = message.username,
            isMod = message.isModerator,
            isSubscriber = message.isSubscriber,
            isVip = message.isVip,
            isBroadcaster = message.isBroadcaster
        )

        val wordRules = automodRepository.rulesForChannel(s.channelLogin)
        val wordVerdict = if (wordRules.isNotEmpty()) {
            AutomodEngine.evaluate(
                text = message.message,
                target = target,
                currentChannelLogin = s.channelLogin,
                rules = wordRules
            )
        } else null

        if (wordVerdict != null) {
            val noticeText = buildString {
                append("Automod [${wordVerdict.rule.scopeLabel}] ")
                append(
                    when (wordVerdict.action) {
                        AutomodAction.DELETE -> "deleted"
                        AutomodAction.TIMEOUT -> "timed out (${wordVerdict.timeoutSeconds}s)"
                        AutomodAction.BAN -> "banned"
                    }
                )
                append(" @${message.displayName.ifEmpty { message.username }}: matched \"${wordVerdict.matchedPattern}\"")
            }
            when (wordVerdict.action) {
                AutomodAction.DELETE -> if (message.id.isNotEmpty()) deleteMessage(message.id)
                AutomodAction.TIMEOUT -> if (message.userId.isNotEmpty()) timeoutUser(
                    message.userId,
                    wordVerdict.timeoutSeconds
                )

                AutomodAction.BAN -> if (message.userId.isNotEmpty()) banUser(message.userId)
            }
            addMessage(
                DisplayMessage.SystemMsg(
                    id = uniqueId("automod"),
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    channel = s.channelLogin,
                    text = noticeText,
                    type = DisplayMessage.SystemMsg.SystemType.NOTICE
                )
            )
            return
        }

        val chatRules = automodRepository.chatRulesForChannel(s.channelLogin)
        if (chatRules.isEmpty()) return

        val tokens = state.value.messages
            .filterIsInstance<DisplayMessage.PrivMsg>()
            .firstOrNull { it.userId == message.userId && it.rawMessage?.id == message.id }
            ?.tokens ?: emptyList()

        val chatVerdict = ChatRuleEngine.evaluate(
            text = message.message,
            tokens = tokens,
            target = target,
            currentChannelLogin = s.channelLogin,
            rules = chatRules
        ) ?: return

        val chatNoticeText = buildString {
            append("ChatRule [${chatVerdict.rule.scopeLabel}] ")
            append(
                when (chatVerdict.action) {
                    ChatRuleAction.DELETE -> "deleted"
                    ChatRuleAction.TIMEOUT -> "timed out (${chatVerdict.timeoutSeconds}s)"
                    ChatRuleAction.BAN -> "banned"
                    ChatRuleAction.SEND_MESSAGE -> "responded"
                }
            )
            append(" @${message.displayName.ifEmpty { message.username }}: ${chatVerdict.reason}")
        }
        when (chatVerdict.action) {
            ChatRuleAction.DELETE -> if (message.id.isNotEmpty()) deleteMessage(message.id)
            ChatRuleAction.TIMEOUT -> if (message.userId.isNotEmpty()) timeoutUser(
                message.userId,
                chatVerdict.timeoutSeconds
            )

            ChatRuleAction.BAN -> if (message.userId.isNotEmpty()) banUser(message.userId)
            ChatRuleAction.SEND_MESSAGE -> Unit
        }
        addMessage(
            DisplayMessage.SystemMsg(
                id = uniqueId("chatrule"),
                timestamp = Clock.System.now().toEpochMilliseconds(),
                channel = s.channelLogin,
                text = chatNoticeText,
                type = DisplayMessage.SystemMsg.SystemType.NOTICE
            )
        )
    }
}
