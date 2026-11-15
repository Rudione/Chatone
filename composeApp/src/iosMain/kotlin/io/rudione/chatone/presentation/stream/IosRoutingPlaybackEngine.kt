package io.rudione.chatone.presentation.stream

import io.rudione.chatone.domain.stream.*
import platform.AVFAudio.currentRoute
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionPortAirPlay
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal object IosAirPlayRouting {
    var owner: IosRoutingPlaybackEngine? = null
    fun prepare() { owner?.beginRouting() }
    fun finishPicking() { owner?.finishRouting() }
}

internal class IosRoutingPlaybackEngine(
    private val localFactory: () -> StreamPlaybackEngine,
    private val externalFactory: () -> StreamPlaybackEngine
) : StreamPlaybackEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val activeEngine = MutableStateFlow(localFactory())
    private val _status = MutableStateFlow(PlaybackStatus())
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()
    private var external = false
    private var picking = false
    private var routeCheck: Job? = null
    private val routeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
        AVAudioSessionRouteChangeNotification, null, NSOperationQueue.mainQueue
    ) { if (!picking) checkRoute() }
    private var released = false
    private var manifest: StreamManifest? = null
    private var selection: StreamQualitySelection = StreamQualitySelection.Auto
    private var lowLatency = true
    private var playing = true
    private var muted = false
    private var media: StreamMediaInfo? = null

    init {
        IosAirPlayRouting.owner = this
        checkRoute()
        scope.launch { activeEngine.collectLatest { engine -> engine.status.collect { _status.value = it } } }
    }

    fun beginRouting() {
        picking = true
        routeCheck?.cancel()
        routeExternally(true)
    }

    fun finishRouting() { picking = false; checkRoute() }

    private fun checkRoute() {
        routeCheck?.cancel()
        routeCheck = scope.launch {
            delay(1_000)
            if (picking || released) return@launch
            val active = AVAudioSession.sharedInstance().currentRoute.outputs
                .filterIsInstance<AVAudioSessionPortDescription>().any { it.portType == AVAudioSessionPortAirPlay }
            routeExternally(active)
        }
    }

    private fun routeExternally(external: Boolean) {
        if (released || this.external == external) return
        this.external = external
        val old = activeEngine.value
        playing = old.status.value.phase != StreamPhase.PAUSED
        muted = old.status.value.isMuted
        old.release()
        val engine = if (external) externalFactory() else localFactory()
        activeEngine.value = engine
        manifest?.let { engine.load(it, selection, if (external) false else lowLatency) }
        engine.setMuted(muted)
        if (!playing) engine.setPlaying(false)
        media?.let(engine::setMediaInfo)
    }

    override fun load(manifest: StreamManifest, selection: StreamQualitySelection, lowLatency: Boolean) {
        this.manifest = manifest
        this.selection = selection
        this.lowLatency = lowLatency
        playing = true
        activeEngine.value.load(manifest, selection, if (external) false else lowLatency)
    }
    override fun selectQuality(selection: StreamQualitySelection) { this.selection = selection; activeEngine.value.selectQuality(selection) }
    override fun setPlaying(playing: Boolean) { this.playing = playing; activeEngine.value.setPlaying(playing) }
    override fun setMuted(muted: Boolean) { this.muted = muted; activeEngine.value.setMuted(muted) }
    override fun seekToLive() = activeEngine.value.seekToLive()
    override fun setMediaInfo(info: StreamMediaInfo) { media = info; activeEngine.value.setMediaInfo(info) }
    override fun metrics(): PlaybackMetrics? = activeEngine.value.metrics()
    override fun release() {
        if (released) return
        released = true
        if (IosAirPlayRouting.owner === this) IosAirPlayRouting.owner = null
        NSNotificationCenter.defaultCenter.removeObserver(routeObserver)
        scope.cancel()
        activeEngine.value.release()
    }
}
