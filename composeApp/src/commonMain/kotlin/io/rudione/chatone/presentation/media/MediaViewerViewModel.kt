package io.rudione.chatone.presentation.media

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
import io.rudione.chatone.data.repository.StreamPlayerPreferencesRepository
import io.rudione.chatone.domain.media.ClipOutcome
import io.rudione.chatone.domain.media.ClipStudio
import io.rudione.chatone.domain.media.MediaCatalog
import io.rudione.chatone.domain.media.MediaDetails
import io.rudione.chatone.domain.media.MediaKind
import io.rudione.chatone.domain.media.MediaModeration
import io.rudione.chatone.domain.media.MediaPlayback
import io.rudione.chatone.domain.media.MediaPlaybackFactory
import io.rudione.chatone.domain.media.ReplayChannel
import io.rudione.chatone.domain.media.ReplayChatRenderer
import io.rudione.chatone.domain.media.ReplayLine
import io.rudione.chatone.domain.media.ReplayTimeline
import io.rudione.chatone.domain.media.revealTimesMs
import io.rudione.chatone.domain.model.DisplayMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ViewerLoad { LOADING, READY, MISSING, FAILED }

@Immutable
data class ReplayEntry(val revealAtMs: Long, val message: DisplayMessage.PrivMsg)

@Immutable
data class MediaViewerState(
    val load: ViewerLoad = ViewerLoad.LOADING,
    val details: MediaDetails? = null,
    val sourceIndex: Int = 0,
    val muted: Boolean = false,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val looping: Boolean = true,
    val replayMessages: List<ReplayEntry> = emptyList(),
    val replayPrimed: Boolean = false,
    val replayHasClock: Boolean = false,
    val ambientLight: Boolean = true,
    val canModerate: Boolean = false,
    val busy: Boolean = false,
    val deleted: Boolean = false
) : UiState

sealed interface MediaViewerEvent : UiEvent {
    data object Retry : MediaViewerEvent
    data class SelectSource(val index: Int) : MediaViewerEvent
    data object ToggleMute : MediaViewerEvent
    data class SetVolume(val volume: Float) : MediaViewerEvent
    data class SetSpeed(val speed: Float) : MediaViewerEvent
    data object ToggleLooping : MediaViewerEvent
    data class Progress(val positionSeconds: Int) : MediaViewerEvent
    data object DeleteClip : MediaViewerEvent
    data object BanCurator : MediaViewerEvent
}

sealed interface MediaViewerEffect : UIEffect {
    data object Deleted : MediaViewerEffect
    data object Banned : MediaViewerEffect
    data object ActionFailed : MediaViewerEffect
}

class MediaViewerViewModel(
    private val kind: MediaKind,
    private val id: String,
    private val startSeconds: Int,
    private val catalog: MediaCatalog,
    private val studio: ClipStudio,
    private val moderation: MediaModeration,
    private val replayChat: ReplayChatRenderer,
    factory: MediaPlaybackFactory,
    playerPreferences: StreamPlayerPreferencesRepository
) : BaseViewModel<MediaViewerState, MediaViewerEvent, MediaViewerEffect>(
    MediaViewerState(looping = kind == MediaKind.CLIP, ambientLight = playerPreferences.load().ambientLight)
) {

    val playback: MediaPlayback? = factory.create()
    private var replayJob: Job? = null
    private var replayChannel: ReplayChannel? = null
    private var timeline = ReplayTimeline()
    private val replayPlayhead = MutableStateFlow(-1)
    private val rendered = HashMap<String, DisplayMessage.PrivMsg>()
    private val renderLock = Mutex()

    init {
        subscribeToEvents()
        playback?.setLooping(kind == MediaKind.CLIP)
        load()
    }

    override suspend fun onEvent(event: MediaViewerEvent) {
        when (event) {
            MediaViewerEvent.Retry -> load()
            is MediaViewerEvent.SelectSource -> selectSource(event.index)
            MediaViewerEvent.ToggleMute -> {
                val muted = !state.value.muted
                playback?.setVolume(if (muted) 0f else state.value.volume)
                update { it.copy(muted = muted) }
            }

            is MediaViewerEvent.SetVolume -> {
                val volume = event.volume.coerceIn(0f, 1f)
                playback?.setVolume(volume)
                update { it.copy(volume = volume, muted = volume == 0f) }
            }

            is MediaViewerEvent.SetSpeed -> {
                playback?.setSpeed(event.speed)
                update { it.copy(speed = event.speed) }
            }

            MediaViewerEvent.ToggleLooping -> {
                val looping = !state.value.looping
                playback?.setLooping(looping)
                update { it.copy(looping = looping) }
            }

            is MediaViewerEvent.Progress -> movePlayhead(event.positionSeconds)
            MediaViewerEvent.DeleteClip -> deleteClip()
            MediaViewerEvent.BanCurator -> banCurator()
        }
    }

    private fun load() {
        update { it.copy(load = ViewerLoad.LOADING) }
        viewModelScope.launch {
            val details =
                runCatching { if (kind == MediaKind.CLIP) catalog.clip(id) else catalog.video(id) }.getOrNull()
            if (details == null || details.sources.isEmpty()) {
                update { it.copy(load = if (details == null) ViewerLoad.FAILED else ViewerLoad.MISSING) }
                return@launch
            }
            val index =
                details.sources.indexOfFirst { it.label == PREFERRED_QUALITY }.takeIf { it >= 0 }
                    ?: 0
            update {
                it.copy(
                    load = ViewerLoad.READY,
                    details = details,
                    sourceIndex = index,
                    replayHasClock = details.kind == MediaKind.VIDEO && details.createdAtMs != null
                )
            }
            prepareReplayChat(details)
            playback?.load(details.sources[index], startSeconds * 1000L)
            playback?.setPlaying(true)
            startReplay(details)
            movePlayhead(startSeconds)
            val canModerate =
                details.kind == MediaKind.CLIP && moderation.canModerate(details.broadcaster.id)
            update { it.copy(canModerate = canModerate) }
        }
    }

    private fun selectSource(index: Int) {
        val details = state.value.details ?: return
        val source = details.sources.getOrNull(index) ?: return
        val position = playback?.positionMs() ?: 0L
        playback?.load(source, position)
        update { it.copy(sourceIndex = index) }
    }

    private fun movePlayhead(positionSeconds: Int) {
        val details = state.value.details ?: return
        if (details.replayVideoId == null) return
        replayPlayhead.value = details.replayOffsetSeconds + positionSeconds.coerceAtLeast(0)
    }

    private fun startReplay(details: MediaDetails) {
        val videoId = details.replayVideoId ?: return
        replayJob?.cancel()
        timeline = ReplayTimeline()
        rendered.clear()
        replayJob = viewModelScope.launch {
            replayPlayhead.collect { now -> if (now >= 0) pumpReplay(videoId) }
        }
    }

    private suspend fun pumpReplay(videoId: String) {
        repeat(MAX_PAGES_PER_STEP) {
            val before = timeline.lines
            val from = timeline.nextFetch(replayPlayhead.value)
            if (timeline.lines !== before || timeline.primed != state.value.replayPrimed) publishReplay()
            if (from == null) return
            val page = fetchReplay(videoId, from) ?: return
            val fresh = timeline.accept(from, page)
            if (fresh.isNotEmpty()) {
                render(fresh)
                viewModelScope.launch { if (replayChat.loadCosmetics(fresh.map { it.userId })) rerender(fresh) }
            }
            publishReplay()
        }
    }

    private suspend fun fetchReplay(videoId: String, from: Int): List<ReplayLine>? = try {
        catalog.replay(videoId, from)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun prepareReplayChat(details: MediaDetails) {
        if (details.replayVideoId == null || details.broadcaster.id.isBlank()) return
        val startedAt = details.createdAtMs?.takeIf { details.kind == MediaKind.VIDEO } ?: 0L
        val channel = ReplayChannel(details.broadcaster.id, details.broadcaster.login.lowercase(), startedAt)
        replayChannel = channel
        viewModelScope.launch {
            replayChat.prepare(channel)
            rerender(timeline.lines)
        }
    }

    private suspend fun rerender(lines: List<ReplayLine>) {
        render(lines)
        publishReplay()
    }

    private suspend fun render(lines: List<ReplayLine>) {
        val channel = replayChannel ?: return
        if (lines.isEmpty()) return
        renderLock.withLock {
            val messages = replayChat.render(channel, lines)
            lines.forEachIndexed { i, line -> messages.getOrNull(i)?.let { rendered[line.id] = it } }
        }
    }

    private fun publishReplay() {
        val lines = timeline.lines
        val ids = lines.mapTo(HashSet()) { it.id }
        rendered.keys.retainAll(ids)
        val reveals = lines.revealTimesMs()
        val entries = lines.indices.mapNotNull { i -> rendered[lines[i].id]?.let { ReplayEntry(reveals[i], it) } }
        update { it.copy(replayMessages = entries, replayPrimed = timeline.primed) }
    }

    private fun deleteClip() {
        val details = state.value.details ?: return
        if (state.value.busy || details.kind != MediaKind.CLIP) return
        update { it.copy(busy = true) }
        viewModelScope.launch {
            val ok = studio.delete(details.id) is ClipOutcome.Done
            update { it.copy(busy = false, deleted = ok || it.deleted) }
            sendEffect(if (ok) MediaViewerEffect.Deleted else MediaViewerEffect.ActionFailed)
        }
    }

    private fun banCurator() {
        val details = state.value.details ?: return
        val curator = details.curator ?: return
        if (state.value.busy || curator.id.isBlank()) return
        update { it.copy(busy = true) }
        viewModelScope.launch {
            val ok = moderation.ban(details.broadcaster.id, curator.id, BAN_REASON)
            update { it.copy(busy = false) }
            sendEffect(if (ok) MediaViewerEffect.Banned else MediaViewerEffect.ActionFailed)
        }
    }

    override fun onCleared() {
        playback?.release()
        super.onCleared()
    }

    private companion object {
        const val PREFERRED_QUALITY = "720p"
        const val MAX_PAGES_PER_STEP = 12
        const val BAN_REASON = "Clip"
    }
}
