package io.rudione.chatone.presentation.chat.events

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_vm_failed_to_start_poll
import chatone.composeapp.generated.resources.chat_vm_failed_to_vote
import chatone.composeapp.generated.resources.chat_vm_gql_token_required
import chatone.composeapp.generated.resources.chat_vm_mod_only_command
import chatone.composeapp.generated.resources.chat_vm_only_the_broadcaster_can_start_a
import chatone.composeapp.generated.resources.chat_vm_poll_started_s
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.data.remote.dto.PollData
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.presentation.chat.ChatState
import io.rudione.chatone.presentation.chat.PollHistoryEntry
import io.rudione.chatone.presentation.text.UiText
import io.rudione.chatone.presentation.text.uiText
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PollController(
    private val scope: CoroutineScope,
    private val helix: TwitchApiClient,
    private val gql: TwitchGqlClient,
    private val auth: ModerationAuthStore,
    private val getState: () -> ChatState,
    private val update: ((ChatState) -> ChatState) -> Unit,
    private val onError: (UiText) -> Unit,
    private val onNotice: (channelLogin: String, text: UiText) -> Unit
) {
    private var pollPollingJob: Job? = null

    fun stopPollPolling() {
        pollPollingJob?.cancel()
        pollPollingJob = null
    }

    fun startPollPolling() {
        pollPollingJob?.cancel()
        pollPollingJob = scope.launch {
            while (currentCoroutineContext()[Job]?.isActive == true) {
                val s = getState()
                if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) {
                    delay(5000L)
                    continue
                }
                val requestedChannelId = s.channelId
                val r = helix.getActivePoll(s.currentAccessToken, requestedChannelId)
                if (getState().channelId != requestedChannelId) break
                if (r is Result.Success) {
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

    fun fetchCurrentPoll(channelLogin: String, channelId: String) {
        scope.launch {
            if (!auth.hasCustomToken()) return@launch
            val token = auth.resolveToken(getState().currentAccessToken)
            if (token.isBlank()) return@launch
            val poll = gql.getViewablePollGql(channelLogin, token) ?: return@launch
            if (!poll.status.equals("ACTIVE", ignoreCase = true)) return@launch
            update { if (it.channelId == channelId) it.copy(livePoll = poll, activePollId = poll.id) else it }
            startPollPollingGql(channelLogin, channelId)
        }
    }

    fun syncPollAfterCreate(channelLogin: String, channelId: String) {
        scope.launch {
            repeat(6) { attempt ->
                if (attempt > 0) delay(600L)
                if (getState().channelId != channelId) return@launch
                val token = auth.resolveToken(getState().currentAccessToken)
                if (token.isBlank()) return@launch
                val poll = gql.getViewablePollGql(channelLogin, token)
                    ?.takeIf { it.status.equals("ACTIVE", ignoreCase = true) } ?: return@repeat
                update { if (it.channelId == channelId) it.copy(livePoll = poll, activePollId = poll.id) else it }
                startPollPollingGql(channelLogin, channelId)
                return@launch
            }
        }
    }

    private fun startPollPollingGql(channelLogin: String, channelId: String) {
        pollPollingJob?.cancel()
        pollPollingJob = scope.launch {
            while (currentCoroutineContext()[Job]?.isActive == true) {
                delay(4000L)
                if (!auth.hasCustomToken()) break
                val token = auth.resolveToken(getState().currentAccessToken)
                if (token.isBlank()) continue
                val poll = gql.getViewablePollGql(channelLogin, token)
                update { if (it.channelId == channelId) it.copy(livePoll = poll, activePollId = poll?.id) else it }
                if (poll == null || !poll.status.equals("ACTIVE", ignoreCase = true)) break
            }
        }
    }

    fun votePoll(pollId: String, choiceId: String) {
        val s = getState()
        if (!auth.hasCustomToken()) {
            onError(uiText(Res.string.chat_vm_gql_token_required)); return
        }
        if (s.currentUserId.isBlank()) return
        val poll = s.livePoll
        if (poll == null || poll.id != pollId || poll.selfVoteChoiceId != null) return
        val token = auth.resolveToken(s.currentAccessToken)
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
        scope.launch {
            val r = gql.voteInPollGql(
                pollId = pollId,
                choiceId = choiceId,
                userId = s.currentUserId,
                extraVotes = 0,
                pointsPerVote = null,
                token = token
            )
            if (r.isError) {
                onError(uiText(Res.string.chat_vm_failed_to_vote, (r as Result.Error).exception.message))
            }
        }
    }

    fun applyLivePoll(channelId: String, poll: PollData?) {
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

    fun openPollCreation() {
        update { it.copy(showPollCreation = true) }
        val s = getState()
        if (s.channelLogin.isNotEmpty() && s.channelId.isNotEmpty()) {
            fetchCurrentPoll(s.channelLogin, s.channelId)
        }
    }

    private fun rememberPollHistory(title: String, choices: List<String>, durationSeconds: Int) {
        val entry = PollHistoryEntry(title, choices, durationSeconds)
        update {
            it.copy(recentPolls = (listOf(entry) + it.recentPolls.filterNot { p -> p.title == title }).take(5))
        }
    }

    fun submitPollFromForm(title: String, choices: List<String>, durationSeconds: Int) {
        val s = getState()
        update { it.copy(showPollCreation = false) }
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) return
        scope.launch {
            val useGqlEvents = auth.hasCustomToken()
            if (useGqlEvents) {
                if (!s.canModerate) return@launch onError(uiText(Res.string.chat_vm_mod_only_command))
                val gqlEventToken = auth.resolveToken(s.currentAccessToken)
                when (val r = gql.createPollGql(
                    channelId = s.channelId,
                    title = title,
                    choices = choices,
                    durationSeconds = durationSeconds,
                    pointsPerVote = null,
                    token = gqlEventToken
                )) {
                    is Result.Success -> {
                        rememberPollHistory(title, choices, durationSeconds)
                        onNotice(
                            s.channelLogin,
                            uiText(Res.string.chat_vm_poll_started_s, title, choices.joinToString(" | "), durationSeconds)
                        )
                        syncPollAfterCreate(s.channelLogin, s.channelId)
                    }
                    is Result.Error ->
                        onError(uiText(Res.string.chat_vm_failed_to_start_poll, r.exception.message))
                    else -> {}
                }
                return@launch
            }
            if (!s.isBroadcaster) return@launch onError(uiText(Res.string.chat_vm_only_the_broadcaster_can_start_a))
            when (val r = helix.createPoll(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                title = title,
                choices = choices,
                durationSeconds = durationSeconds
            )) {
                is Result.Success -> {
                    update { it.copy(activePollId = r.data.id, livePoll = r.data) }
                    rememberPollHistory(title, choices, durationSeconds)
                    onNotice(
                        s.channelLogin,
                        uiText(Res.string.chat_vm_poll_started_s, title, choices.joinToString(" | "), durationSeconds)
                    )
                    startPollPolling()
                }
                is Result.Error ->
                    onError(uiText(Res.string.chat_vm_failed_to_start_poll, r.exception.message))
                else -> {}
            }
        }
    }
}
