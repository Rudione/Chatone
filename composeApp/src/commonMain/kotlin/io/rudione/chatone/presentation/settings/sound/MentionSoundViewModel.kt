package io.rudione.chatone.presentation.settings.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.rudione.chatone.data.sound.MentionSoundRepository
import io.rudione.chatone.data.sound.SoundImportResult
import io.rudione.chatone.domain.sound.CustomSound
import io.rudione.chatone.domain.sound.MentionSoundConfig
import io.rudione.chatone.domain.sound.MentionSoundService
import io.rudione.chatone.domain.sound.PcmAudio
import io.rudione.chatone.domain.sound.SoundEdit
import io.rudione.chatone.domain.sound.SoundInfo
import io.rudione.chatone.domain.sound.SoundProcessing
import io.rudione.chatone.util.link.TwitchLinks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SoundNotice { Added, Saved, Deleted, Unsupported, TooLarge, Failed, BadChannel }

data class SoundPlayback(val id: String, val token: Long, val durationMs: Long)

data class SoundEditorState(
    val id: String,
    val name: String,
    val sourceDurationMs: Long = 0L,
    val peaks: List<Float> = emptyList(),
    val edit: SoundEdit = SoundEdit(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val trimHint: Boolean = false
)

data class MentionSoundUiState(
    val config: MentionSoundConfig = MentionSoundConfig(),
    val library: List<CustomSound> = emptyList(),
    val builtInInfo: Map<String, SoundInfo> = emptyMap(),
    val importing: Int = 0,
    val notice: SoundNotice? = null,
    val noticeToken: Long = 0L,
    val playing: SoundPlayback? = null,
    val editor: SoundEditorState? = null
) {
    fun info(id: String): SoundInfo? =
        builtInInfo[id] ?: library.firstOrNull { it.id == id }?.let { SoundInfo(it.durationMs, it.peaks) }
}

sealed interface MentionSoundEvent {
    data class Select(val id: String, val volume: Float) : MentionSoundEvent
    data class Preview(val id: String, val volume: Float) : MentionSoundEvent
    data object StopPreview : MentionSoundEvent
    data class Import(val paths: List<String>) : MentionSoundEvent
    data class OpenEditor(val id: String) : MentionSoundEvent
    data class EditorChanged(val edit: SoundEdit) : MentionSoundEvent
    data class EditorRenamed(val name: String) : MentionSoundEvent
    data class EditorPreview(val volume: Float) : MentionSoundEvent
    data object EditorAutoTrim : MentionSoundEvent
    data object EditorReset : MentionSoundEvent
    data object EditorSave : MentionSoundEvent
    data object EditorDelete : MentionSoundEvent
    data object CloseEditor : MentionSoundEvent
    data class SetCooldown(val ms: Long) : MentionSoundEvent
    data class SetMediaStream(val enabled: Boolean) : MentionSoundEvent
    data class AddChannel(val input: String) : MentionSoundEvent
    data class SetChannelSound(val login: String, val id: String) : MentionSoundEvent
    data class RemoveChannel(val login: String) : MentionSoundEvent
    data object DismissNotice : MentionSoundEvent
}

class MentionSoundViewModel(
    private val repository: MentionSoundRepository,
    private val service: MentionSoundService,
    private val now: () -> Long,
    private val compute: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _state = MutableStateFlow(MentionSoundUiState())
    val state: StateFlow<MentionSoundUiState> = _state.asStateFlow()
    private var editorSource: PcmAudio? = null
    private var playbackJob: Job? = null
    private var noticeJob: Job? = null

    init {
        viewModelScope.launch {
            combine(repository.config, repository.library, repository.builtInInfo) { config, library, info ->
                Triple(config, library, info)
            }.collect { (config, library, info) ->
                _state.update { it.copy(config = config, library = library, builtInInfo = info) }
            }
        }
    }

    fun onEvent(event: MentionSoundEvent) {
        when (event) {
            is MentionSoundEvent.Select -> {
                repository.select(event.id)
                preview(event.id, event.volume)
            }
            is MentionSoundEvent.Preview -> preview(event.id, event.volume)
            MentionSoundEvent.StopPreview -> stopPreview()
            is MentionSoundEvent.Import -> import(event.paths)
            is MentionSoundEvent.OpenEditor -> openEditor(event.id, trimHint = false)
            is MentionSoundEvent.EditorChanged -> updateEditor { editor ->
                editor.copy(edit = SoundProcessing.clampEdit(event.edit, editor.sourceDurationMs))
            }
            is MentionSoundEvent.EditorRenamed -> updateEditor { it.copy(name = event.name.take(MAX_NAME)) }
            is MentionSoundEvent.EditorPreview -> previewEditor(event.volume)
            MentionSoundEvent.EditorAutoTrim -> editorSource?.let { source ->
                updateEditor { it.copy(edit = SoundProcessing.defaultEdit(source).copy(gain = it.edit.gain, normalize = it.edit.normalize)) }
            }
            MentionSoundEvent.EditorReset -> updateEditor { editor ->
                editor.copy(edit = SoundProcessing.clampEdit(SoundEdit(), editor.sourceDurationMs))
            }
            MentionSoundEvent.EditorSave -> saveEditor()
            MentionSoundEvent.EditorDelete -> deleteEditor()
            MentionSoundEvent.CloseEditor -> closeEditor()
            is MentionSoundEvent.SetCooldown -> repository.setCooldown(event.ms)
            is MentionSoundEvent.SetMediaStream -> repository.setMediaStream(event.enabled)
            is MentionSoundEvent.AddChannel -> addChannel(event.input)
            is MentionSoundEvent.SetChannelSound -> repository.setChannelSound(event.login, event.id)
            is MentionSoundEvent.RemoveChannel -> repository.setChannelSound(event.login, null)
            MentionSoundEvent.DismissNotice -> _state.update { it.copy(notice = null) }
        }
    }

    private fun preview(id: String, volume: Float) {
        service.preview(id, volume)
        markPlaying(id, _state.value.info(id)?.durationMs ?: DEFAULT_PLAY_MS)
    }

    private fun stopPreview() {
        service.stop()
        playbackJob?.cancel()
        _state.update { it.copy(playing = null) }
    }

    private fun markPlaying(id: String, durationMs: Long) {
        playbackJob?.cancel()
        val token = now()
        _state.update { it.copy(playing = SoundPlayback(id, token, durationMs.coerceAtLeast(MIN_PLAY_MS))) }
        playbackJob = viewModelScope.launch {
            delay(durationMs.coerceAtLeast(MIN_PLAY_MS) + PLAY_TAIL_MS)
            _state.update { if (it.playing?.token == token) it.copy(playing = null) else it }
        }
    }

    private fun import(paths: List<String>) {
        val accepted = paths.filter { it.isNotBlank() }.take(MAX_IMPORT)
        if (accepted.isEmpty()) return
        _state.update { it.copy(importing = it.importing + accepted.size) }
        viewModelScope.launch {
            var lastAdded: SoundImportResult.Added? = null
            var failure: SoundNotice? = null
            accepted.forEach { path ->
                when (val result = repository.import(path)) {
                    is SoundImportResult.Added -> lastAdded = result
                    SoundImportResult.Unsupported -> failure = SoundNotice.Unsupported
                    SoundImportResult.TooLarge -> failure = SoundNotice.TooLarge
                    SoundImportResult.Failed -> failure = SoundNotice.Failed
                }
                _state.update { it.copy(importing = (it.importing - 1).coerceAtLeast(0)) }
            }
            val added = lastAdded
            notice(if (added != null) SoundNotice.Added else failure ?: SoundNotice.Failed)
            if (added != null) {
                if (added.needsTrim) {
                    openEditor(added.sound.id, trimHint = true, source = added.source)
                } else {
                    markPlaying(added.sound.id, added.sound.durationMs)
                    service.preview(added.sound.id)
                }
            }
        }
    }

    private fun openEditor(id: String, trimHint: Boolean, source: PcmAudio? = null) {
        val sound = _state.value.library.firstOrNull { it.id == id } ?: return
        stopPreview()
        _state.update { it.copy(editor = SoundEditorState(id = id, name = sound.name, edit = sound.edit, trimHint = trimHint)) }
        viewModelScope.launch {
            val pcm = source ?: repository.loadSource(id)
            if (_state.value.editor?.id != id) return@launch
            if (pcm == null) {
                updateEditor { it.copy(loading = false, failed = true) }
                return@launch
            }
            editorSource = pcm
            val peaks = withContext(compute) { SoundProcessing.peaks(pcm, EDITOR_BUCKETS) }
            updateEditor {
                it.copy(
                    loading = false,
                    sourceDurationMs = pcm.durationMs,
                    peaks = peaks,
                    edit = SoundProcessing.clampEdit(sound.edit, pcm.durationMs)
                )
            }
        }
    }

    private fun previewEditor(volume: Float) {
        val source = editorSource ?: return
        val editor = _state.value.editor ?: return
        viewModelScope.launch {
            val path = withContext(compute) { repository.preview(source, editor.edit) } ?: return@launch
            service.previewFile(path, volume)
            markPlaying(PREVIEW_ID, editor.edit.endMs - editor.edit.startMs)
        }
    }

    private fun saveEditor() {
        val source = editorSource ?: return
        val editor = _state.value.editor ?: return
        if (editor.saving) return
        updateEditor { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = repository.saveEdit(editor.id, editor.name, editor.edit, source)
            if (saved == null) {
                updateEditor { it.copy(saving = false) }
                notice(SoundNotice.Failed)
                return@launch
            }
            closeEditor()
            notice(SoundNotice.Saved)
            repository.select(saved.id)
            service.preview(saved.id)
            markPlaying(saved.id, saved.durationMs)
        }
    }

    private fun deleteEditor() {
        val editor = _state.value.editor ?: return
        closeEditor()
        viewModelScope.launch {
            repository.delete(editor.id)
            notice(SoundNotice.Deleted)
        }
    }

    private fun closeEditor() {
        stopPreview()
        editorSource = null
        _state.update { it.copy(editor = null) }
    }

    private fun addChannel(input: String) {
        val login = TwitchLinks.channelLogin(input)
        if (login == null) {
            notice(SoundNotice.BadChannel)
            return
        }
        val config = _state.value.config
        if (login !in config.channelSounds) repository.setChannelSound(login, config.selectedId)
    }

    private fun notice(notice: SoundNotice) {
        noticeJob?.cancel()
        val token = now()
        _state.update { it.copy(notice = notice, noticeToken = token) }
        noticeJob = viewModelScope.launch {
            delay(NOTICE_MS)
            _state.update { if (it.noticeToken == token) it.copy(notice = null) else it }
        }
    }

    private inline fun updateEditor(crossinline transform: (SoundEditorState) -> SoundEditorState) {
        _state.update { state -> state.editor?.let { state.copy(editor = transform(it)) } ?: state }
    }

    override fun onCleared() {
        service.stop()
    }

    companion object {
        const val PREVIEW_ID = "preview"
        const val EDITOR_BUCKETS = 220
        val cooldownOptions = listOf(0L, 3_000L, 10_000L, 30_000L)
        private const val MAX_NAME = 40
        private const val MAX_IMPORT = 8
        private const val DEFAULT_PLAY_MS = 900L
        private const val MIN_PLAY_MS = 250L
        private const val PLAY_TAIL_MS = 120L
        private const val NOTICE_MS = 4_000L
    }
}
