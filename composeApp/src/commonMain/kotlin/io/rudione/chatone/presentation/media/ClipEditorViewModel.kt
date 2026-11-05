package io.rudione.chatone.presentation.media

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.media.ClipDraft
import io.rudione.chatone.domain.media.ClipFailure
import io.rudione.chatone.domain.media.ClipOrigin
import io.rudione.chatone.domain.media.ClipOutcome
import io.rudione.chatone.domain.media.ClipStudio
import io.rudione.chatone.domain.media.ClipTrim
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.domain.media.MediaPlaybackFactory
import io.rudione.chatone.domain.media.PlaybackPhase
import io.rudione.chatone.domain.profile.ChannelProfileSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

enum class ClipEditorPhase { PREPARING, EDITING, PUBLISHING, DONE, FAILED }

@Immutable
data class ClipEditorState(
    val login: String,
    val channelName: String = "",
    val phase: ClipEditorPhase = ClipEditorPhase.PREPARING,
    val failure: ClipFailure? = null,
    val draft: ClipDraft? = null,
    val totalSeconds: Float = FALLBACK_WINDOW,
    val start: Float = FALLBACK_WINDOW - ClipTrim.DEFAULT_SECONDS,
    val end: Float = FALLBACK_WINDOW,
    val rangeEdited: Boolean = false,
    val trimming: Boolean = false,
    val title: String = "",
    val slug: String? = null,
    val publishedSlug: String? = null
) : UiState {
    val length: Float get() = end - start
    val range: ClosedFloatingPointRange<Float> get() = start..end
    val canPublish: Boolean get() = phase == ClipEditorPhase.EDITING && title.isNotBlank() && slug != null

    companion object {
        const val FALLBACK_WINDOW = 60f
    }
}

sealed interface ClipEditorEvent : UiEvent {
    data object Retry : ClipEditorEvent
    data object BeginTrim : ClipEditorEvent
    data class SetRange(val start: Float, val end: Float, val previewAt: Float) : ClipEditorEvent
    data object EndTrim : ClipEditorEvent
    data class Seek(val seconds: Float) : ClipEditorEvent
    data class Preset(val seconds: Float) : ClipEditorEvent
    data class EditTitle(val value: String) : ClipEditorEvent
    data object Publish : ClipEditorEvent
}

sealed interface ClipEditorEffect : UIEffect

class ClipEditorViewModel(
    private val login: String,
    private val origin: ClipOrigin,
    private val profiles: ChannelProfileSource,
    private val studio: ClipStudio,
    factory: MediaPlaybackFactory,
    private val clock: Clock = Clock.System
) : BaseViewModel<ClipEditorState, ClipEditorEvent, ClipEditorEffect>(ClipEditorState(login = login)) {

    val playback: MediaPlayback? = factory.create()
    private val scrubTarget = MutableStateFlow<Float?>(null)
    private var prepareJob: Job? = null
    private var previewJob: Job? = null
    private var loopJob: Job? = null

    init {
        subscribeToEvents()
        followScrubbing()
        prepare()
    }

    override suspend fun onEvent(event: ClipEditorEvent) {
        when (event) {
            ClipEditorEvent.Retry -> prepare()
            ClipEditorEvent.BeginTrim -> beginTrim()
            is ClipEditorEvent.SetRange -> setRange(event.start, event.end, event.previewAt)
            ClipEditorEvent.EndTrim -> endTrim()
            is ClipEditorEvent.Seek -> seekWithinRange(event.seconds)
            is ClipEditorEvent.Preset -> applyPreset(event.seconds)
            is ClipEditorEvent.EditTitle -> update {
                it.copy(
                    title = event.value.take(MAX_TITLE),
                    failure = null
                )
            }

            ClipEditorEvent.Publish -> publish()
        }
    }

    private fun prepare() {
        previewJob?.cancel()
        loopJob?.cancel()
        prepareJob?.cancel()
        update { ClipEditorState(login = login, title = it.title) }
        prepareJob = viewModelScope.launch {
            val draftSlug = when (val created = createOriginDraft()) {
                is ClipOutcome.Done -> created.value
                is ClipOutcome.Failed -> return@launch fail(created.reason)
            }
            val draft = awaitDraft(draftSlug)
            val total = draft?.durationSeconds?.takeIf { it >= ClipTrim.MIN_SECONDS }
                ?: ClipEditorState.FALLBACK_WINDOW
            val range = ClipTrim.defaultRange(total)
            update {
                it.copy(
                    phase = ClipEditorPhase.EDITING,
                    draft = draft,
                    slug = draftSlug,
                    totalSeconds = total,
                    start = range.start,
                    end = range.endInclusive
                )
            }
            if (draft != null) startPreview(draft)
        }
    }

    private suspend fun createOriginDraft(): ClipOutcome<String> {
        val profile = profiles.profile(login)
        profile?.let { found -> update { it.copy(channelName = found.displayName) } }
        return when (origin) {
            is ClipOrigin.Live -> {
                val live = profile?.live
                val startedAt = live?.startedAtMs
                if (live == null || startedAt == null) return ClipOutcome.Failed(ClipFailure.NOT_LIVE)
                val offset = ((clock.now().toEpochMilliseconds() - startedAt) / 1000f -
                        (origin.delaySeconds ?: DEFAULT_DELAY)).coerceAtLeast(0f)
                withTransientRetries { studio.createDraft(profile.id, live.broadcastId, offset) }
            }

            is ClipOrigin.Video -> withTransientRetries {
                studio.createVideoDraft(origin.broadcasterId, origin.videoId, origin.offsetSeconds)
            }
        }
    }

    private suspend fun withTransientRetries(create: suspend () -> ClipOutcome<String>): ClipOutcome<String> {
        var outcome = create()
        repeat(TRANSIENT_RETRIES) {
            val failed = outcome as? ClipOutcome.Failed ?: return outcome
            if (failed.reason !in TRANSIENT_FAILURES) return outcome
            delay(TRANSIENT_RETRY_MS)
            outcome = create()
        }
        return outcome
    }

    private fun startPreview(draft: ClipDraft) {
        val player = playback ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            var current = draft
            repeat(PREVIEW_ATTEMPTS) { attempt ->
                val candidates = current.previewCandidates
                if (candidates.isNotEmpty()) {
                    val source =
                        candidates[(attempt / ATTEMPTS_PER_SOURCE).coerceAtMost(candidates.lastIndex)]
                    player.load(source, secondsToMs(state.value.start))
                    player.setPlaying(true)
                    val settled =
                        player.phase.first { it == PlaybackPhase.READY || it == PlaybackPhase.ERROR }
                    if (settled == PlaybackPhase.READY) {
                        matchWindowToMedia(player)
                        loopInsideRange(player)
                        return@launch
                    }
                }
                delay(PREVIEW_RETRY_MS)
                current = studio.draft(current.slug) ?: current
            }
        }
    }

    private fun matchWindowToMedia(player: MediaPlayback) {
        val media = player.durationMs() / MS_PER_SECOND
        val current = state.value
        if (!ClipTrim.needsWindowUpdate(current.totalSeconds, media)) return
        val range =
            if (current.rangeEdited) ClipTrim.fit(current.range, media) else ClipTrim.defaultRange(
                media
            )
        update { it.copy(totalSeconds = media, start = range.start, end = range.endInclusive) }
        player.seekTo(secondsToMs(range.start))
    }

    private fun loopInsideRange(player: MediaPlayback) {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (isActive) {
                delay(LOOP_TICK_MS)
                val current = state.value
                if (current.phase != ClipEditorPhase.EDITING || current.trimming) continue
                val phase = player.phase.value
                if (phase != PlaybackPhase.READY && phase != PlaybackPhase.ENDED) continue
                val position = player.positionMs() / MS_PER_SECOND
                if (phase == PlaybackPhase.ENDED || ClipTrim.leavesRange(position, current.range)) {
                    player.seekTo(secondsToMs(current.start))
                }
            }
        }
    }

    private fun followScrubbing() {
        viewModelScope.launch {
            scrubTarget.filterNotNull().collectLatest { seconds ->
                playback?.scrubTo(secondsToMs(seconds))
                delay(SCRUB_INTERVAL_MS)
            }
        }
    }

    private fun beginTrim() {
        playback?.setPlaying(false)
        update { it.copy(trimming = true, rangeEdited = true) }
    }

    private fun setRange(start: Float, end: Float, previewAt: Float) {
        update { it.copy(start = start, end = end) }
        scrubTarget.value = previewAt
    }

    private fun endTrim() {
        scrubTarget.value = null
        update { it.copy(trimming = false) }
        playback?.seekTo(secondsToMs(state.value.start))
        playback?.setPlaying(true)
    }

    private fun seekWithinRange(seconds: Float) {
        playback?.seekTo(secondsToMs(ClipTrim.playhead(seconds, state.value.range)))
    }

    private fun applyPreset(seconds: Float) {
        val current = state.value
        val range = ClipTrim.withLength(current.range, seconds, current.totalSeconds)
        update { it.copy(start = range.start, end = range.endInclusive, rangeEdited = true) }
        playback?.seekTo(secondsToMs(range.start))
    }

    private suspend fun awaitDraft(slug: String): ClipDraft? {
        var latest: ClipDraft? = null
        repeat(DRAFT_ATTEMPTS) {
            latest = studio.draft(slug) ?: latest
            if (latest?.ready == true) return latest
            delay(DRAFT_POLL_MS)
        }
        return latest
    }

    private fun publish() {
        val current = state.value
        val draftSlug = current.slug ?: return
        if (!current.canPublish) return
        playback?.setPlaying(false)
        update { it.copy(phase = ClipEditorPhase.PUBLISHING, failure = null) }
        viewModelScope.launch {
            when (val result = publishWithRetry(draftSlug, current)) {
                is ClipOutcome.Done -> update {
                    it.copy(
                        phase = ClipEditorPhase.DONE,
                        publishedSlug = result.value
                    )
                }

                is ClipOutcome.Failed -> {
                    update { it.copy(phase = ClipEditorPhase.EDITING, failure = result.reason) }
                    playback?.setPlaying(true)
                }
            }
        }
    }

    private suspend fun publishWithRetry(
        slug: String,
        current: ClipEditorState
    ): ClipOutcome<String> {
        var outcome = studio.publish(slug, current.start, current.length, current.title)
        repeat(TRANSIENT_RETRIES) {
            val failed = outcome as? ClipOutcome.Failed ?: return outcome
            if (failed.reason !in TRANSIENT_FAILURES) return outcome
            delay(TRANSIENT_RETRY_MS)
            outcome = studio.publish(slug, current.start, current.length, current.title)
        }
        return outcome
    }

    private fun fail(reason: ClipFailure) {
        update { it.copy(phase = ClipEditorPhase.FAILED, failure = reason) }
    }

    private fun secondsToMs(seconds: Float): Long = (seconds * MS_PER_SECOND).toLong()

    override fun onCleared() {
        playback?.release()
        super.onCleared()
    }

    private companion object {
        const val DEFAULT_DELAY = 6f
        const val DRAFT_ATTEMPTS = 24
        const val DRAFT_POLL_MS = 1_500L
        const val PREVIEW_ATTEMPTS = 16
        const val ATTEMPTS_PER_SOURCE = 3
        const val PREVIEW_RETRY_MS = 1_500L
        const val LOOP_TICK_MS = 100L
        const val SCRUB_INTERVAL_MS = 140L
        const val TRANSIENT_RETRIES = 2
        const val TRANSIENT_RETRY_MS = 1_500L
        const val MAX_TITLE = 100
        const val MS_PER_SECOND = 1000f
        val TRANSIENT_FAILURES = setOf(ClipFailure.TWITCH_ERROR, ClipFailure.FAILED)
    }
}
