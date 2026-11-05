package io.rudione.chatone.presentation.media

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import io.rudione.chatone.base.BaseViewModel
import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.base.UiEvent
import io.rudione.chatone.base.UiState
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
import io.rudione.chatone.domain.model.DisplayMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class ViewerLoad { LOADING, READY, MISSING, FAILED }

@Immutable
data class ReplayEntry(val offsetSeconds: Int, val message: DisplayMessage.PrivMsg)

@Immutable
data class MediaViewerState(
    val load: ViewerLoad = ViewerLoad.LOADING,
    val details: MediaDetails? = null,
    val sourceIndex: Int = 0,
    val muted: Boolean = false,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val looping: Boolean = true,
    val replay: List<ReplayLine> = emptyList(),
    val replayMessages: List<ReplayEntry> = emptyList(),
    val replayHasClock: Boolean = false,
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
    factory: MediaPlaybackFactory
) : BaseViewModel<MediaViewerState, MediaViewerEvent, MediaViewerEffect>(MediaViewerState(looping = kind == MediaKind.CLIP)) {

    val playback: MediaPlayback? = factory.create()
    private var replayJob: Job? = null
    private var renderJob: Job? = null
    private var replayChannel: ReplayChannel? = null
    private var replayLoadedUntil = -1

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

            is MediaViewerEvent.Progress -> ensureReplay(event.positionSeconds)
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
            ensureReplay(startSeconds)
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

    private fun ensureReplay(positionSeconds: Int) {
        val details = state.value.details ?: return
        val videoId = details.replayVideoId ?: return
        val absolute = details.replayOffsetSeconds + positionSeconds
        if (replayJob?.isActive == true) return
        if (replayLoadedUntil >= 0 && absolute + REPLAY_LOOKAHEAD < replayLoadedUntil) return
        val from =
            if (replayLoadedUntil < 0) (absolute - REPLAY_BACKFILL).coerceAtLeast(0) else replayLoadedUntil
        replayJob = viewModelScope.launch {
            val lines = catalog.replay(videoId, from).orEmpty()
            replayLoadedUntil = (lines.maxOfOrNull { it.offsetSeconds } ?: (from + REPLAY_STEP)) + 1
            update { s ->
                s.copy(replay = (s.replay + lines).distinctBy { it.id }.takeLast(MAX_REPLAY))
            }
            renderReplay()
            if (replayChat.loadCosmetics(lines.map { it.userId })) renderReplay()
        }
    }

    private fun prepareReplayChat(details: MediaDetails) {
        if (details.replayVideoId == null || details.broadcaster.id.isBlank()) return
        val startedAt = details.createdAtMs?.takeIf { details.kind == MediaKind.VIDEO } ?: 0L
        val channel = ReplayChannel(details.broadcaster.id, details.broadcaster.login.lowercase(), startedAt)
        replayChannel = channel
        viewModelScope.launch {
            replayChat.prepare(channel)
            renderReplay()
        }
    }

    private fun renderReplay() {
        val channel = replayChannel ?: return
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val lines = state.value.replay
            val messages = replayChat.render(channel, lines)
            val entries = lines.zip(messages) { line, message -> ReplayEntry(line.offsetSeconds, message) }
            update { it.copy(replayMessages = entries) }
        }
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
        const val REPLAY_LOOKAHEAD = 20
        const val REPLAY_BACKFILL = 30
        const val REPLAY_STEP = 60
        const val MAX_REPLAY = 400
        const val BAN_REASON = "Clip"
    }
}
