package io.rudione.chatone.presentation.chat.events

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_vm_failed_to_cancel_prediction
import chatone.composeapp.generated.resources.chat_vm_failed_to_complete_prediction
import chatone.composeapp.generated.resources.chat_vm_failed_to_lock_prediction
import chatone.composeapp.generated.resources.chat_vm_failed_to_place_prediction
import chatone.composeapp.generated.resources.chat_vm_failed_to_start_prediction
import chatone.composeapp.generated.resources.chat_vm_gql_token_required
import chatone.composeapp.generated.resources.chat_vm_mod_only_command
import chatone.composeapp.generated.resources.chat_vm_only_the_broadcaster_can_start_a_alt
import chatone.composeapp.generated.resources.chat_vm_prediction_started_s
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.data.remote.dto.PredictionData
import io.rudione.chatone.data.repository.ModerationAuthStore
import io.rudione.chatone.presentation.chat.ChatState
import io.rudione.chatone.presentation.chat.PredictionHistoryEntry
import io.rudione.chatone.presentation.text.UiText
import io.rudione.chatone.presentation.text.uiText
import io.rudione.chatone.util.Result
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PredictionController(
    private val scope: CoroutineScope,
    private val helix: TwitchApiClient,
    private val gql: TwitchGqlClient,
    private val auth: ModerationAuthStore,
    private val getState: () -> ChatState,
    private val update: ((ChatState) -> ChatState) -> Unit,
    private val onError: (UiText) -> Unit,
    private val onNotice: (channelLogin: String, text: UiText) -> Unit
) {
    private var predictionPollingJob: Job? = null

    fun stopPredictionPolling() {
        predictionPollingJob?.cancel()
        predictionPollingJob = null
    }

    fun startPredictionPolling() {
        predictionPollingJob?.cancel()
        predictionPollingJob = scope.launch {
            while (currentCoroutineContext()[Job]?.isActive == true) {
                val s = getState()
                if (s.currentAccessToken.isEmpty() || s.channelId.isEmpty()) {
                    delay(5000L.milliseconds)
                    continue
                }
                val requestedChannelId = s.channelId
                val r = helix.getActivePrediction(s.currentAccessToken, requestedChannelId)
                if (getState().channelId != requestedChannelId) break
                if (r is Result.Success) {
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
                delay(4000L)
            }
        }
    }

    fun fetchCurrentPrediction(channelLogin: String, channelId: String) {
        scope.launch {
            if (!auth.hasCustomToken()) return@launch
            val token = auth.resolveToken(getState().currentAccessToken)
            if (token.isBlank()) return@launch
            val prediction = gql.getActivePredictionGql(channelLogin, token) ?: return@launch
            if (prediction.status != "ACTIVE" && prediction.status != "LOCKED") return@launch
            update {
                if (it.channelId == channelId) it.copy(
                    livePrediction = prediction,
                    activePredictionId = prediction.id,
                    activePredictionOutcomes = prediction.outcomes.map { o -> o.id to o.title }
                ) else it
            }
            startPredictionPollingGql(channelLogin, channelId)
        }
    }

    fun syncPredictionAfterCreate(channelLogin: String, channelId: String) {
        scope.launch {
            repeat(6) { attempt ->
                if (attempt > 0) delay(600L)
                if (getState().channelId != channelId) return@launch
                val token = auth.resolveToken(getState().currentAccessToken)
                if (token.isBlank()) return@launch
                val prediction = gql.getActivePredictionGql(channelLogin, token)
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

    private fun startPredictionPollingGql(channelLogin: String, channelId: String) {
        predictionPollingJob?.cancel()
        predictionPollingJob = scope.launch {
            while (currentCoroutineContext()[Job]?.isActive == true) {
                delay(4000L)
                if (!auth.hasCustomToken()) break
                val token = auth.resolveToken(getState().currentAccessToken)
                if (token.isBlank()) continue
                val prediction = gql.getActivePredictionGql(channelLogin, token)
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

    fun placePrediction(eventId: String, outcomeId: String, points: Int) {
        val s = getState()
        if (!auth.hasCustomToken()) {
            onError(uiText(Res.string.chat_vm_gql_token_required)); return
        }
        if (points <= 0) return
        val prediction = s.livePrediction
        if (prediction == null || prediction.id != eventId || prediction.selfOutcomeId != null) return
        val token = auth.resolveToken(s.currentAccessToken)
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
        scope.launch {
            val r = gql.makePredictionGql(eventId, outcomeId, points, token)
            if (r.isError) {
                onError(uiText(Res.string.chat_vm_failed_to_place_prediction, (r as Result.Error).exception.message))
            }
        }
    }

    fun applyLivePrediction(
        channelId: String,
        prediction: PredictionData?
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

    fun openPredictionCreation() {
        update { it.copy(showPredictionCreation = true) }
        val s = getState()
        if (s.channelLogin.isNotEmpty() && s.channelId.isNotEmpty()) {
            fetchCurrentPrediction(s.channelLogin, s.channelId)
        }
    }

    fun resolvePrediction(eventId: String, outcomeId: String) = changePredictionStatus(eventId, "RESOLVED", outcomeId)

    fun lockPrediction(eventId: String) = changePredictionStatus(eventId, "LOCKED")

    fun changePredictionStatus(eventId: String, status: String, outcomeId: String? = null) {
        val snapshot = getState()
        if (!snapshot.canModerate) {
            onError(uiText(Res.string.chat_vm_mod_only_command))
            return
        }
        val prediction = snapshot.livePrediction ?: return
        if (snapshot.predictionActionPending || prediction.id != eventId || prediction.status !in setOf("ACTIVE", "LOCKED")) return
        val useGql = auth.hasCustomToken()
        if (!useGql && !snapshot.isBroadcaster) {
            onError(uiText(Res.string.chat_vm_gql_token_required))
            return
        }
        val errorText = when (status) {
            "LOCKED" -> Res.string.chat_vm_failed_to_lock_prediction
            "CANCELED" -> Res.string.chat_vm_failed_to_cancel_prediction
            else -> Res.string.chat_vm_failed_to_complete_prediction
        }
        update { it.copy(predictionActionPending = true) }
        scope.launch {
            try {
                val result = if (useGql) {
                    val token = auth.resolveToken(snapshot.currentAccessToken)
                    when (status) {
                        "LOCKED" -> gql.lockPredictionGql(eventId, token)
                        "CANCELED" -> gql.cancelPredictionGql(eventId, token)
                        else -> gql.resolvePredictionGql(eventId, outcomeId ?: return@launch, token)
                    }
                } else helix.endPrediction(snapshot.currentAccessToken, snapshot.channelId, eventId, status, outcomeId)
                if (result.isError) {
                    onError(uiText(errorText))
                    return@launch
                }
                if (status != "LOCKED" && getState().channelId == snapshot.channelId && getState().livePrediction?.id == eventId) {
                    predictionPollingJob?.cancel()
                }
                update {
                    val current = it.livePrediction
                    if (it.channelId != snapshot.channelId || current?.id != eventId) it
                    else if (status == "LOCKED") it.copy(livePrediction = current.copy(status = status))
                    else it.copy(livePrediction = null, activePredictionId = null, activePredictionOutcomes = emptyList(), showPredictionCreation = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(uiText(errorText))
            } finally {
                update { if (it.channelId == snapshot.channelId) it.copy(predictionActionPending = false) else it }
            }
        }
    }

    private fun rememberPredictionHistory(title: String, outcomes: List<String>, windowSeconds: Int) {
        val entry = PredictionHistoryEntry(title, outcomes, windowSeconds)
        update {
            it.copy(recentPredictions = (listOf(entry) + it.recentPredictions.filterNot { p -> p.title == title }).take(5))
        }
    }

    fun submitPredictionFromForm(title: String, outcomes: List<String>, windowSeconds: Int) {
        val s = getState()
        update { it.copy(showPredictionCreation = false) }
        if (s.currentAccessToken.isBlank() || s.channelId.isBlank()) return
        scope.launch {
            val useGqlEvents = auth.hasCustomToken()
            if (useGqlEvents) {
                if (!s.canModerate) return@launch onError(uiText(Res.string.chat_vm_mod_only_command))
                val gqlEventToken = auth.resolveToken(s.currentAccessToken)
                when (val r = gql.createPredictionGql(
                    channelId = s.channelId,
                    title = title,
                    outcomes = outcomes,
                    windowSeconds = windowSeconds,
                    token = gqlEventToken
                )) {
                    is Result.Success -> {
                        rememberPredictionHistory(title, outcomes, windowSeconds)
                        onNotice(
                            s.channelLogin,
                            uiText(Res.string.chat_vm_prediction_started_s, title, outcomes.joinToString(" | "), windowSeconds)
                        )
                        syncPredictionAfterCreate(s.channelLogin, s.channelId)
                    }
                    is Result.Error ->
                        onError(uiText(Res.string.chat_vm_failed_to_start_prediction, r.exception.message))
                    else -> {}
                }
                return@launch
            }
            if (!s.isBroadcaster) return@launch onError(uiText(Res.string.chat_vm_only_the_broadcaster_can_start_a_alt))
            val r = helix.createPrediction(
                accessToken = s.currentAccessToken,
                broadcasterId = s.channelId,
                title = title,
                outcomes = outcomes,
                predictionWindowSeconds = windowSeconds
            )
            when (r) {
                is Result.Success -> {
                    update {
                        it.copy(
                            activePredictionId = r.data.id,
                            activePredictionOutcomes = r.data.outcomes.map { o -> o.id to o.title },
                            livePrediction = r.data
                        )
                    }
                    rememberPredictionHistory(title, outcomes, windowSeconds)
                    onNotice(
                        s.channelLogin,
                        uiText(Res.string.chat_vm_prediction_started_s, title, outcomes.joinToString(" | "), windowSeconds)
                    )
                    startPredictionPolling()
                }
                is Result.Error ->
                    onError(uiText(Res.string.chat_vm_failed_to_start_prediction, r.exception.message))
                else -> {}
            }
        }
    }
}
