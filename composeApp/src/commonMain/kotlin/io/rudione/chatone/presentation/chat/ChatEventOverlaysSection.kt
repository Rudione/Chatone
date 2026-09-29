package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.rudione.chatone.presentation.chat.components.ChannelPointsBitsSheet
import io.rudione.chatone.presentation.chat.components.PinnedMessageBar
import io.rudione.chatone.presentation.chat.components.RaidBanner
import io.rudione.chatone.presentation.components.DockHost
import io.rudione.chatone.presentation.components.insetBottom
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import kotlinx.datetime.Instant

@Composable
internal fun BoxScope.ChatEventOverlaysSection(
    state: ChatState,
    viewModel: ChatViewModel,
    showPinnedMessage: Boolean,
    scrollbarWidthDp: Int,
    dockHost: DockHost?,
    bottomInsetPx: () -> Int
) {
    var predictionResolveDetached by remember { mutableStateOf(false) }
    val strings = LocalStrings.current
    fun isoToMs(iso: String?): Long? =
        iso?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
    val pollHidden = state.livePoll?.id in state.hiddenEventIds
    val predictionHidden = state.livePrediction?.id in state.hiddenEventIds

    val bannerItems = buildList {
        state.pinnedMessage?.takeIf { showPinnedMessage && !state.pinLocallyHidden }?.let { pinned ->
            add(
                io.rudione.chatone.presentation.chat.components.EventBannerItem(
                    key = "pin",
                    label = strings.eventLabelPinned,
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
                        onHide = { viewModel.sendEvent(ChatEvent.OnHideEventBanner("pin")) })
                }
            )
        }
        state.livePoll?.takeIf { !pollHidden }?.let { poll ->
            val ends = isoToMs(poll.startedAt)?.plus(poll.duration * 1000L)
            add(
                io.rudione.chatone.presentation.chat.components.EventBannerItem(
                    key = "poll",
                    label = strings.eventLabelPoll,
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
                    label = strings.eventLabelPrediction,
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
                    label = strings.eventLabelRaid,
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
    val bannerEndInset = (scrollbarWidthDp + 10).dp
    if (bannerItems.isNotEmpty()) {
        io.rudione.chatone.presentation.chat.components.UnifiedEventBanner(
            items = bannerItems,
            horizontalInset = bannerEndInset,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(5f)
                .padding(top = 6.dp)
        )
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
                .insetBottom(bottomInsetPx)
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
