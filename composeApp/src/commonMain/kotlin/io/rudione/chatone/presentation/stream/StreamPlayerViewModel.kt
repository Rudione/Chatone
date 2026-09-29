package io.rudione.chatone.presentation.stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.repository.StreamPlayerPreferencesRepository
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.PlaybackStatus
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifest
import io.rudione.chatone.domain.stream.StreamManifestResult
import io.rudione.chatone.domain.stream.StreamManifestSource
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamQualityResolver
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamStats
import io.rudione.chatone.domain.stream.StreamVideoScale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Clock

class StreamPlayerViewModel(
    private val manifestSource: StreamManifestSource,
    private val preferencesRepository: StreamPlayerPreferencesRepository,
    private val engineFactory: StreamPlaybackEngineFactory,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ViewModel() {

    private val _state = MutableStateFlow(
        StreamPlayerUiState(
            isSupported = engineFactory.isSupported,
            preferences = preferencesRepository.load()
        )
    )
    val state: StateFlow<StreamPlayerUiState> = _state.asStateFlow()

    private val _stats = MutableStateFlow<StreamStats?>(null)
    val stats: StateFlow<StreamStats?> = _stats.asStateFlow()

    private val _latencySeconds = MutableStateFlow<Float?>(null)
    val latencySeconds: StateFlow<Float?> = _latencySeconds.asStateFlow()

    private val _playbackEngine = MutableStateFlow<StreamPlaybackEngine?>(null)
    val playbackEngine: StateFlow<StreamPlaybackEngine?> = _playbackEngine.asStateFlow()

    private var engine: StreamPlaybackEngine? = null
    private var manifest: StreamManifest? = null
    private var manifestJob: Job? = null
    private var statusJob: Job? = null
    private var metricsJob: Job? = null
    private var retryJob: Job? = null
    private var probeJob: Job? = null
    private var probeLogin: String? = null
    private var followTarget: String? = null
    private var dismissedLogin: String? = null
    private var retryAttempt = 0
    private var resumeWhenVisible = false
    private var appVisible = true
    private var viewportWidth = 0
    private var viewportHeight = 0

    fun onEvent(event: StreamPlayerEvent) {
        when (event) {
            is StreamPlayerEvent.Open -> openRequested(event.channelLogin, event.quality)
            StreamPlayerEvent.Close -> closeRequested()
            StreamPlayerEvent.Dismiss -> dismiss()
            is StreamPlayerEvent.FollowChannel -> follow(event.channelLogin)
            StreamPlayerEvent.TogglePlayback -> togglePlayback()
            StreamPlayerEvent.ToggleMute -> setMuted(!_state.value.isMuted)
            is StreamPlayerEvent.SelectQuality -> selectQuality(event.selection)
            is StreamPlayerEvent.SetLowLatency -> setLowLatency(event.enabled)
            is StreamPlayerEvent.SetVideoScale -> updatePreferences { it.copy(videoScale = event.scale) }
            StreamPlayerEvent.ToggleVideoScale -> updatePreferences {
                it.copy(videoScale = if (it.videoScale == StreamVideoScale.FIT) StreamVideoScale.FILL else StreamVideoScale.FIT)
            }
            is StreamPlayerEvent.SetLandscapeChatMode -> updatePreferences { it.copy(landscapeChatMode = event.mode) }
            StreamPlayerEvent.CycleLandscapeChatMode -> updatePreferences {
                it.copy(landscapeChatMode = LandscapeChatMode.entries[(it.landscapeChatMode.ordinal + 1) % LandscapeChatMode.entries.size])
            }
            is StreamPlayerEvent.SetSideChatFraction -> updatePreferences { it.copy(sideChatFraction = event.fraction) }
            is StreamPlayerEvent.SetOverlayBounds -> updatePreferences { it.copy(overlayBounds = event.bounds.sanitized()) }
            is StreamPlayerEvent.SetOverlayOpacity -> updatePreferences { it.copy(overlayOpacity = event.opacity) }
            is StreamPlayerEvent.SetOverlayMessageOpacity -> updatePreferences { it.copy(overlayMessageOpacity = event.opacity) }
            is StreamPlayerEvent.SetOverlayLocked -> updatePreferences { it.copy(overlayLocked = event.locked) }
            is StreamPlayerEvent.SetAutoPictureInPicture -> updatePreferences { it.copy(autoPictureInPicture = event.enabled) }
            is StreamPlayerEvent.SetStatsVisible -> setStatsVisible(event.visible)
            is StreamPlayerEvent.SetSettingsVisible -> _state.update { it.copy(settingsVisible = event.visible && it.isOpen) }
            StreamPlayerEvent.Retry -> _state.value.channelLogin?.let { open(it, force = true) }
            StreamPlayerEvent.Reload -> reload()
            StreamPlayerEvent.DismissChannelNotice -> _state.update { it.copy(channelNoticeDismissed = true) }
            StreamPlayerEvent.StreamWentLive -> onFollowedChannelLive()
            is StreamPlayerEvent.ViewportChanged -> {
                viewportWidth = event.width.coerceAtLeast(0)
                viewportHeight = event.height.coerceAtLeast(0)
            }
            is StreamPlayerEvent.AppVisibilityChanged -> onVisibilityChanged(event.visible)
        }
    }

    private fun open(channelLogin: String, force: Boolean) {
        if (!engineFactory.isSupported) return
        val login = channelLogin.trim().lowercase()
        if (login.isEmpty() || login.startsWith("/")) return
        val current = _state.value
        if (!force && current.channelLogin == login) return
        if (current.channelLogin == login && current.isPlaybackActive && force && manifest != null) return
        cancelSession()
        if (current.channelLogin != login) engine?.setPlaying(false)
        _state.update {
            it.copy(
                channelLogin = login,
                phase = StreamPhase.LOADING,
                errorKind = null,
                variants = if (it.channelLogin == login) it.variants else emptyList(),
                activeVariant = null,
                chatChannelLogin = null,
                channelNoticeDismissed = false
            )
        }
        loadManifest(login)
    }

    private fun openRequested(channelLogin: String, quality: String?) {
        if (!engineFactory.isSupported) return
        dismissedLogin = null
        updatePreferences { it.copy(autoOpen = true, preferredQuality = quality ?: it.preferredQuality) }
        val currentManifest = manifest
        if (quality != null && currentManifest != null && _state.value.channelLogin == normalizedLogin(channelLogin)) {
            selectQuality(StreamQualityResolver.selectionFor(quality, currentManifest.variants))
        }
        open(channelLogin, force = true)
    }

    private fun reload() {
        val current = _state.value
        val login = current.channelLogin ?: return
        val playbackEngine = engine
        retryAttempt = 0
        if (playbackEngine == null || manifest == null || current.needsAttention) {
            open(login, force = true)
            return
        }
        playbackEngine.seekToLive()
        if (appVisible) playbackEngine.setPlaying(true)
        retryJob?.cancel()
        manifestJob?.cancel()
        manifestJob = viewModelScope.launch {
            val result = manifestSource.fetchManifest(login, engineFactory.supportedCodecs)
            if (_state.value.channelLogin != login) return@launch
            when (result) {
                is StreamManifestResult.Ready -> startPlayback(result.manifest)
                StreamManifestResult.Offline -> showTerminal(StreamPhase.OFFLINE, null)
                is StreamManifestResult.Failed -> Napier.w("Stream reload kept current session: ${result.kind}", tag = TAG)
            }
        }
    }

    private fun closeRequested() {
        updatePreferences { it.copy(autoOpen = false) }
        close()
    }

    private fun dismiss() {
        dismissedLogin = _state.value.channelLogin ?: followTarget
        close()
    }

    private fun follow(channelLogin: String?) {
        val current = _state.value
        val login = normalizedLogin(channelLogin)
        if (login != dismissedLogin) dismissedLogin = null
        followTarget = login
        cancelProbe()
        if (!current.isOpen) {
            login?.let(::autoOpen)
            return
        }
        if (login == null) {
            close()
            return
        }
        if (login == current.channelLogin) {
            _state.update { it.copy(chatChannelLogin = null, channelNoticeDismissed = false) }
            return
        }
        val streamIsUsable = manifest != null && !current.needsAttention
        if (!streamIsUsable) {
            retryAttempt = 0
            _state.update { it.copy(chatChannelLogin = null, channelNoticeDismissed = false) }
            open(login, force = false)
            return
        }
        probe(login) { result ->
            if (!_state.value.isOpen) return@probe
            if (result is StreamManifestResult.Ready) {
                beginSession(login, result.manifest)
            } else {
                _state.update { it.copy(chatChannelLogin = login, channelNoticeDismissed = false) }
            }
        }
    }

    private fun autoOpen(login: String) {
        if (!engineFactory.isSupported || !_state.value.preferences.autoOpen || login == dismissedLogin) return
        if (probeJob?.isActive == true && probeLogin == login) return
        probe(login) { result ->
            val current = _state.value
            if (current.isOpen || !current.preferences.autoOpen || login == dismissedLogin) return@probe
            if (result is StreamManifestResult.Ready) beginSession(login, result.manifest)
        }
    }

    private fun onFollowedChannelLive() {
        val current = _state.value
        val target = followTarget
        when {
            !current.isOpen -> target?.let(::autoOpen)
            target != null && current.channelLogin != target -> follow(target)
            current.phase == StreamPhase.OFFLINE -> current.channelLogin?.let { open(it, force = true) }
        }
    }

    private fun probe(login: String, onResult: (StreamManifestResult) -> Unit) {
        cancelProbe()
        probeLogin = login
        probeJob = viewModelScope.launch {
            val result = manifestSource.fetchManifest(login, engineFactory.supportedCodecs)
            if (followTarget != login) return@launch
            onResult(result)
        }
    }

    private fun cancelProbe() {
        probeJob?.cancel()
        probeJob = null
        probeLogin = null
    }

    private fun beginSession(login: String, newManifest: StreamManifest) {
        cancelSession()
        retryAttempt = 0
        _state.update {
            it.copy(
                channelLogin = login,
                phase = StreamPhase.LOADING,
                errorKind = null,
                variants = emptyList(),
                activeVariant = null,
                chatChannelLogin = null,
                channelNoticeDismissed = false
            )
        }
        startPlayback(newManifest)
    }

    private fun normalizedLogin(channelLogin: String?): String? =
        channelLogin?.trim()?.lowercase()?.takeUnless { it.isEmpty() || it.startsWith("/") }

    private fun close() {
        cancelSession()
        releaseEngine()
        retryAttempt = 0
        resumeWhenVisible = false
        _latencySeconds.value = null
        _stats.value = null
        _state.update {
            StreamPlayerUiState(isSupported = it.isSupported, preferences = it.preferences, isMuted = it.isMuted)
        }
    }

    private fun loadManifest(login: String) {
        manifestJob = viewModelScope.launch {
            val result = manifestSource.fetchManifest(login, engineFactory.supportedCodecs)
            if (_state.value.channelLogin != login) return@launch
            when (result) {
                is StreamManifestResult.Ready -> startPlayback(result.manifest)
                StreamManifestResult.Offline -> showTerminal(StreamPhase.OFFLINE, null)
                is StreamManifestResult.Failed -> onFailure(result.kind)
            }
        }
    }

    private fun startPlayback(newManifest: StreamManifest) {
        manifest = newManifest
        val preferences = _state.value.preferences
        val selection = StreamQualityResolver.selectionFor(preferences.preferredQuality, newManifest.variants)
        val playbackEngine = engine ?: engineFactory.create().also { created ->
            engine = created
            _playbackEngine.value = created
            observe(created)
        }
        playbackEngine.setMuted(_state.value.isMuted)
        playbackEngine.load(newManifest, selection, preferences.lowLatency)
        if (!appVisible) {
            resumeWhenVisible = true
            playbackEngine.setPlaying(false)
        }
        _state.update {
            it.copy(
                variants = StreamQualityResolver.orderedForMenu(newManifest.variants),
                selection = selection,
                errorKind = null,
                streamStartedAtMs = newManifest.serving.streamStartedAtMs?.minus(newManifest.clockOffsetMs)
            )
        }
        startMetrics()
    }

    private fun observe(playbackEngine: StreamPlaybackEngine) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            playbackEngine.status.collect(::onStatus)
        }
    }

    private fun onStatus(status: PlaybackStatus) {
        val current = _state.value
        val currentManifest = manifest ?: return
        if (!current.isOpen || currentManifest.channelLogin != current.channelLogin) return
        when (status.phase) {
            StreamPhase.IDLE -> return
            StreamPhase.ERROR -> {
                onFailure(status.errorKind ?: StreamErrorKind.UNKNOWN)
                return
            }
            StreamPhase.ENDED -> {
                _state.update { it.copy(phase = StreamPhase.LOADING) }
                reloadAfter(0L)
                return
            }
            StreamPhase.PLAYING -> retryAttempt = 0
            else -> Unit
        }
        val variants = manifest?.variants.orEmpty()
        val active = if (current.selection == StreamQualitySelection.AudioOnly) {
            variants.firstOrNull { it.isAudioOnly }
        } else {
            StreamQualityResolver.activeVariant(status.activeBandwidth, status.activeHeight, variants)
        }
        val aspect = if (status.videoWidth > 0 && status.videoHeight > 0) {
            (status.videoWidth.toFloat() / status.videoHeight).coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
        } else {
            current.videoAspectRatio
        }
        _state.update {
            it.copy(
                phase = status.phase,
                errorKind = null,
                isMuted = status.isMuted,
                activeVariant = active ?: it.activeVariant,
                videoAspectRatio = aspect
            )
        }
    }

    private fun onFailure(kind: StreamErrorKind) {
        val login = _state.value.channelLogin ?: return
        val retryable = kind == StreamErrorKind.NETWORK || kind == StreamErrorKind.UNKNOWN
        if (retryable && retryAttempt < MAX_RETRIES) {
            val backoff = min(BASE_RETRY_DELAY_MS shl retryAttempt, MAX_RETRY_DELAY_MS)
            retryAttempt++
            Napier.w("Stream playback failed ($kind), retry $retryAttempt for $login in ${backoff}ms", tag = TAG)
            _state.update { it.copy(phase = StreamPhase.LOADING, errorKind = null) }
            reloadAfter(backoff)
            return
        }
        showTerminal(StreamPhase.ERROR, kind)
    }

    private fun reloadAfter(delayMs: Long) {
        val login = _state.value.channelLogin ?: return
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            if (_state.value.channelLogin == login) {
                manifestJob?.cancel()
                loadManifest(login)
            }
        }
    }

    private fun showTerminal(phase: StreamPhase, kind: StreamErrorKind?) {
        releaseEngine()
        _latencySeconds.value = null
        _stats.value = null
        _state.update {
            it.copy(phase = phase, errorKind = kind, activeVariant = null, settingsVisible = it.settingsVisible && phase != StreamPhase.OFFLINE)
        }
    }

    private fun togglePlayback() {
        val current = _state.value
        val playbackEngine = engine
        when {
            current.needsAttention -> current.channelLogin?.let { open(it, force = true) }
            playbackEngine == null -> Unit
            current.phase == StreamPhase.PAUSED -> {
                playbackEngine.seekToLive()
                playbackEngine.setPlaying(true)
            }
            else -> playbackEngine.setPlaying(false)
        }
    }

    private fun setMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
        engine?.setMuted(muted)
    }

    private fun selectQuality(selection: StreamQualitySelection) {
        val current = _state.value
        if (current.selection == selection && manifest != null) return
        updatePreferences { it.copy(preferredQuality = StreamQualityResolver.preferenceOf(selection)) }
        _state.update { it.copy(selection = selection) }
        if (manifest != null) engine?.selectQuality(selection)
    }

    private fun setLowLatency(enabled: Boolean) {
        if (_state.value.preferences.lowLatency == enabled) return
        updatePreferences { it.copy(lowLatency = enabled) }
        val currentManifest = manifest ?: return
        engine?.load(currentManifest, _state.value.selection, enabled)
    }

    private fun setStatsVisible(visible: Boolean) {
        _state.update { it.copy(statsVisible = visible && it.isOpen) }
        if (!visible) _stats.value = null else publishMetrics()
    }

    private fun onVisibilityChanged(visible: Boolean) {
        if (appVisible == visible) return
        appVisible = visible
        val playbackEngine = engine ?: return
        if (!visible) {
            resumeWhenVisible = _state.value.isPlaybackActive
            if (resumeWhenVisible) playbackEngine.setPlaying(false)
        } else if (resumeWhenVisible) {
            resumeWhenVisible = false
            playbackEngine.seekToLive()
            playbackEngine.setPlaying(true)
        }
    }

    private inline fun updatePreferences(transform: (StreamPlayerPreferences) -> StreamPlayerPreferences) {
        val updated = transform(_state.value.preferences)
        if (updated == _state.value.preferences) return
        _state.update { it.copy(preferences = updated) }
        preferencesRepository.save(updated)
    }

    private fun startMetrics() {
        if (metricsJob?.isActive == true) return
        metricsJob = viewModelScope.launch {
            while (isActive) {
                publishMetrics()
                delay(METRICS_INTERVAL_MS)
            }
        }
    }

    private fun publishMetrics() {
        val playbackEngine = engine
        val currentManifest = manifest
        val current = _state.value
        val metrics = if (playbackEngine != null && currentManifest != null) playbackEngine.metrics() else null
        if (metrics == null || currentManifest == null) {
            _latencySeconds.value = null
            if (current.statsVisible) _stats.value = null
            return
        }
        val latency = metrics.playbackWallClockMs
            ?.let { (nowMs() + currentManifest.clockOffsetMs - it) / 1000f }
            ?.takeIf { it.isFinite() && it in 0f..MAX_REASONABLE_LATENCY_SECONDS }
            ?.let { (it * 100f).roundToInt() / 100f }
        _latencySeconds.value = latency.takeIf { current.phase == StreamPhase.PLAYING }
        if (!current.statsVisible) return
        _stats.value = StreamStats(
            downloadResolution = resolution(metrics.downloadWidth, metrics.downloadHeight),
            renderResolution = resolution(metrics.renderWidth, metrics.renderHeight),
            viewportResolution = resolution(viewportWidth, viewportHeight),
            downloadBitrateKbps = metrics.downloadBitrateKbps,
            bandwidthEstimateKbps = metrics.bandwidthEstimateKbps,
            framesPerSecond = metrics.framesPerSecond,
            droppedFrames = metrics.droppedFrames,
            bufferSeconds = metrics.bufferMs / 1000f,
            latencySeconds = latency,
            codecs = metrics.codecs,
            protocol = PROTOCOL_HLS,
            lowLatency = metrics.lowLatencyActive,
            renderSurface = metrics.renderSurface,
            backendVersion = metrics.backendVersion,
            playSessionId = currentManifest.playSessionId,
            servingId = currentManifest.serving.servingId
        )
    }

    private fun resolution(width: Int, height: Int): String =
        if (width > 0 && height > 0) "${width}x$height" else NOT_AVAILABLE

    private fun cancelSession() {
        manifestJob?.cancel()
        retryJob?.cancel()
        cancelProbe()
        manifestJob = null
        retryJob = null
    }

    private fun releaseEngine() {
        metricsJob?.cancel()
        statusJob?.cancel()
        metricsJob = null
        statusJob = null
        _playbackEngine.value = null
        engine?.release()
        engine = null
        manifest = null
    }

    override fun onCleared() {
        cancelSession()
        releaseEngine()
        super.onCleared()
    }

    private companion object {
        const val TAG = "StreamPlayer"
        const val METRICS_INTERVAL_MS = 1_000L
        const val BASE_RETRY_DELAY_MS = 1_500L
        const val MAX_RETRY_DELAY_MS = 20_000L
        const val MAX_RETRIES = 5
        const val MAX_REASONABLE_LATENCY_SECONDS = 120f
        const val MIN_ASPECT_RATIO = 0.5f
        const val MAX_ASPECT_RATIO = 3f
        const val PROTOCOL_HLS = "HLS"
        const val NOT_AVAILABLE = "—"
    }
}
