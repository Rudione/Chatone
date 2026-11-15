package io.rudione.chatone.presentation.stream

import platform.UIKit.setNeedsUpdateOfSupportedInterfaceOrientations
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.stream.lowlatency.LowLatencyStreamPlaybackEngine
import io.rudione.chatone.presentation.stream.lowlatency.LowLatencyVideoHost
import io.rudione.chatone.presentation.system.IosSystemBars
import io.rudione.chatone.presentation.system.NativeVideoPresenceEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.UIKit.UIInterfaceOrientationMask

@Composable
actual fun PlatformVideoView(
    engine: StreamPlaybackEngine?,
    keepScreenOn: Boolean,
    modifier: Modifier,
    surfaceKind: VideoSurfaceKind,
    frameSink: VideoFrameSink?
) {
    if (engine is IosRoutingPlaybackEngine) {
        val current by engine.activeEngine.collectAsState()
        PlatformVideoView(current, keepScreenOn, modifier, surfaceKind, frameSink)
        return
    }
    KeepScreenOnEffect(keepScreenOn)
    NativeVideoPresenceEffect()
    when (engine) {
        is LowLatencyStreamPlaybackEngine -> {
            if (frameSink != null) {
                FrameSamplingEffect(engine, frameSink, engine::sampleFrame) {
                    engine.frameSamplingIntervalMs(frameSink.sampleIntervalMs)
                }
            }
            UIKitView(
                factory = { LowLatencyVideoHost() },
                modifier = modifier,
                update = { it.attach(engine.surfaces) },
                onRelease = { it.dispose() },
                properties = PassiveVideoSurface
            )
        }

        else -> {
            val avEngine = engine as? IosStreamPlaybackEngine
            if (avEngine != null && frameSink != null) {
                FrameSamplingEffect(avEngine, frameSink, avEngine::sampleFrame) { frameSink.sampleIntervalMs }
            }
            UIKitView(
                factory = { PlayerLayerView(pictureInPicture = surfaceKind == VideoSurfaceKind.Direct) },
                modifier = modifier,
                update = { it.attach(avEngine?.player) },
                onRelease = { it.dispose() },
                properties = PassiveVideoSurface
            )
        }
    }
}

internal val PassiveVideoSurface = UIKitInteropProperties(interactionMode = null)

@Composable
private fun FrameSamplingEffect(
    engine: Any,
    frameSink: VideoFrameSink,
    sample: (Int, Int) -> IntArray?,
    nextDelayMs: () -> Long
) {
    val sampler = rememberUpdatedState(sample)
    val pacing = rememberUpdatedState(nextDelayMs)
    LaunchedEffect(engine, frameSink) {
        while (isActive) {
            sampler.value(frameSink.sampleWidth, frameSink.sampleHeight)?.let(frameSink::accept)
            delay(pacing.value())
        }
    }
}

@Composable
internal fun KeepScreenOnEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        if (enabled) ScreenAwake.acquire()
        onDispose { if (enabled) ScreenAwake.release() }
    }
}

private object ScreenAwake {
    private var holders = 0

    fun acquire() {
        if (holders++ == 0) UIApplication.sharedApplication.idleTimerDisabled = true
    }

    fun release() {
        if (holders == 0) return
        if (--holders == 0) UIApplication.sharedApplication.idleTimerDisabled = false
    }
}

@Composable
actual fun StreamImmersiveEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        if (enabled) IosSystemBars.acquireImmersive()
        onDispose { if (enabled) IosSystemBars.releaseImmersive() }
    }
}

@Composable
actual fun StreamVisibilityEffect(onVisibilityChanged: (Boolean) -> Unit) {
    val latest = rememberUpdatedState(onVisibilityChanged)
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        var pendingHide: Job? = null
        val center = NSNotificationCenter.defaultCenter
        val active = center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) { _ ->
            pendingHide?.cancel()
            pendingHide = null
            latest.value(true)
        }
        val background = center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            pendingHide?.cancel()
            pendingHide = if (IosPictureInPicture.isActive || IosPictureInPicture.startsAutomatically) {
                scope.launch {
                    delay(PICTURE_IN_PICTURE_SETTLE_MS)
                    if (!IosPictureInPicture.isActive) latest.value(false)
                }
            } else {
                latest.value(false)
                null
            }
        }
        onDispose {
            pendingHide?.cancel()
            center.removeObserver(active)
            center.removeObserver(background)
        }
    }
}

@Composable
actual fun StreamBackHandler(enabled: Boolean, onBack: () -> Unit) = SystemBackHandler(enabled, onBack)

@Composable
actual fun rememberStreamOrientationController(active: Boolean): StreamOrientationController =
    remember { SceneOrientationController }

@Composable
actual fun rememberPictureInPictureController(): PictureInPictureController = SystemPictureInPictureController

@Composable
actual fun rememberAppBackgroundController(): AppBackgroundController = NoOpAppBackgroundController

private val NoOpAppBackgroundController = AppBackgroundController {}

private const val PICTURE_IN_PICTURE_SETTLE_MS = 1_500L

@Composable
actual fun PictureInPictureDismissEffect(controller: PictureInPictureController, onDismissed: () -> Unit) {
    val latest = rememberUpdatedState(onDismissed)
    DisposableEffect(controller) {
        val listener = { latest.value() }
        IosPictureInPicture.onDismissed = listener
        onDispose { if (IosPictureInPicture.onDismissed === listener) IosPictureInPicture.onDismissed = null }
    }
}

@Composable
actual fun PictureInPictureAutoEnterEffect(
    controller: PictureInPictureController,
    enabled: Boolean,
    aspectRatio: Float,
    playback: PictureInPicturePlayback,
    onTogglePlayback: () -> Unit,
    onToggleMute: () -> Unit
) {
    val active = enabled && playback.playing
    DisposableEffect(active) {
        IosPictureInPicture.setAutoEnter(active)
        onDispose { IosPictureInPicture.setAutoEnter(false) }
    }
}

private object SceneOrientationController : StreamOrientationController {
    override fun enterLandscape() = request(UIInterfaceOrientationMaskLandscape)

    override fun exitLandscape() = request(UIInterfaceOrientationMaskPortrait)

    private fun request(mask: UIInterfaceOrientationMask) {
        val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene ?: return
        scene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        scene.requestGeometryUpdateWithPreferences(UIWindowSceneGeometryPreferencesIOS(interfaceOrientations = mask), errorHandler = null)
    }
}

private object SystemPictureInPictureController : PictureInPictureController {
    override val isSupported: Boolean get() = IosPictureInPicture.isSupported
    override val isActive: Boolean = false
    override fun enter(): Boolean = IosPictureInPicture.start()
}
