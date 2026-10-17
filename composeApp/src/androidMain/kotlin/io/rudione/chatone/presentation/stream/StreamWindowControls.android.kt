package io.rudione.chatone.presentation.stream

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.view.OrientationEventListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.shared.R
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
actual fun StreamImmersiveEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (enabled && controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (enabled) controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
actual fun StreamVisibilityEffect(onVisibilityChanged: (Boolean) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onVisibilityChanged)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> callback(true)
                Lifecycle.Event.ON_STOP -> callback(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
actual fun StreamBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun rememberStreamOrientationController(active: Boolean): StreamOrientationController {
    val activity = LocalContext.current.findActivity()
    val controller = remember(activity) { activity?.let(::AndroidStreamOrientationController) }
    DisposableEffect(controller, active) {
        onDispose { if (active) controller?.release() }
    }
    return controller ?: NoOpOrientationController
}

@Composable
actual fun rememberPictureInPictureController(): PictureInPictureController {
    val activity = LocalContext.current.findActivity() as? ComponentActivity
    val controller = remember(activity) { AndroidPictureInPictureController(activity) }
    DisposableEffect(activity) {
        val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
            controller.isActive = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(listener)
        controller.isActive = activity?.isInPictureInPictureMode == true
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(listener)
            controller.isActive = false
        }
    }
    return controller
}

@Composable
actual fun rememberAppBackgroundController(): AppBackgroundController {
    val activity = LocalContext.current.findActivity()
    return remember(activity) { AppBackgroundController { activity?.moveTaskToBack(true) } }
}

@Composable
actual fun PictureInPictureDismissEffect(controller: PictureInPictureController, onDismissed: () -> Unit) {
    val activity = (controller as? AndroidPictureInPictureController)?.activity ?: return
    val dismissed by rememberUpdatedState(onDismissed)
    DisposableEffect(activity) {
        val tracker = PictureInPictureExitTracker { dismissed() }
        val modeListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            tracker.onModeChanged(
                inPictureInPicture = info.isInPictureInPictureMode,
                started = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            )
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> tracker.onResumed()
                Lifecycle.Event.ON_STOP -> tracker.onStopped()
                else -> Unit
            }
        }
        activity.addOnPictureInPictureModeChangedListener(modeListener)
        activity.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            activity.removeOnPictureInPictureModeChangedListener(modeListener)
            activity.lifecycle.removeObserver(lifecycleObserver)
        }
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
    val androidController = controller as? AndroidPictureInPictureController ?: return
    val shouldAutoEnter = androidController.isSupported && enabled
    val strings = LocalStrings.current.player
    val buttons = PictureInPictureButtons(
        playing = playback.playing,
        muted = playback.muted,
        playbackLabel = if (playback.playing) strings.pause else strings.play,
        soundLabel = if (playback.muted) strings.unmute else strings.mute
    )
    LaunchedEffect(androidController, shouldAutoEnter, aspectRatio, buttons) {
        androidController.updateParams(autoEnter = shouldAutoEnter, aspectRatio = aspectRatio, buttons = buttons)
    }
    DisposableEffect(androidController) {
        onDispose { androidController.updateParams(autoEnter = false, aspectRatio = aspectRatio, buttons = null) }
    }
    PictureInPictureButtonReceiver(androidController.activity, onTogglePlayback, onToggleMute)
    DisposableEffect(androidController, shouldAutoEnter) {
        val hint = Runnable {
            if (shouldAutoEnter && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) androidController.enter()
        }
        androidController.activity?.addOnUserLeaveHintListener(hint)
        onDispose { androidController.activity?.removeOnUserLeaveHintListener(hint) }
    }
}

private data class PictureInPictureButtons(
    val playing: Boolean,
    val muted: Boolean,
    val playbackLabel: String,
    val soundLabel: String
)

@Composable
private fun PictureInPictureButtonReceiver(
    activity: ComponentActivity?,
    onTogglePlayback: () -> Unit,
    onToggleMute: () -> Unit
) {
    val togglePlayback by rememberUpdatedState(onTogglePlayback)
    val toggleMute by rememberUpdatedState(onToggleMute)
    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getIntExtra(EXTRA_PIP_CONTROL, -1)) {
                    PIP_CONTROL_PLAYBACK -> togglePlayback()
                    PIP_CONTROL_SOUND -> toggleMute()
                }
            }
        }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { activity.unregisterReceiver(receiver) } }
    }
}

private class AndroidPictureInPictureController(
    val activity: ComponentActivity?
) : PictureInPictureController {

    override val isSupported: Boolean =
        activity != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    override var isActive: Boolean by mutableStateOf(false)

    private var ratio: Rational = DEFAULT_RATIONAL
    private var buttons: PictureInPictureButtons? = null
    private var appliedRatio: Rational? = null
    private var appliedAutoEnter: Boolean? = null
    private var appliedButtons: PictureInPictureButtons? = null

    override fun enter(): Boolean {
        val host = activity ?: return false
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val autoEnter = appliedAutoEnter == true
        return runCatching {
            host.enterPictureInPictureMode(params(autoEnter)).also {
                appliedRatio = ratio
                appliedAutoEnter = autoEnter
                appliedButtons = buttons
            }
        }.getOrDefault(false)
    }

    fun updateParams(autoEnter: Boolean, aspectRatio: Float, buttons: PictureInPictureButtons?) {
        ratio = stableRatio(aspectRatio, ratio)
        this.buttons = buttons
        val host = activity ?: return
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (ratio == appliedRatio && autoEnter == appliedAutoEnter && buttons == appliedButtons) return
        runCatching {
            host.setPictureInPictureParams(params(autoEnter))
            appliedRatio = ratio
            appliedAutoEnter = autoEnter
            appliedButtons = buttons
        }
    }

    private fun params(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(ratio)
        val host = activity
        val current = buttons
        builder.setActions(if (host != null && current != null) remoteActions(host, current) else emptyList())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun remoteActions(host: Context, current: PictureInPictureButtons): List<RemoteAction> = listOf(
        remoteAction(
            host,
            if (current.playing) R.drawable.ic_pip_pause else R.drawable.ic_pip_play,
            current.playbackLabel,
            PIP_CONTROL_PLAYBACK
        ),
        remoteAction(
            host,
            if (current.muted) R.drawable.ic_pip_volume_off else R.drawable.ic_pip_volume_on,
            current.soundLabel,
            PIP_CONTROL_SOUND
        )
    )

    private fun remoteAction(host: Context, icon: Int, label: String, control: Int): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL)
            .setPackage(host.packageName)
            .putExtra(EXTRA_PIP_CONTROL, control)
        val pendingIntent = PendingIntent.getBroadcast(
            host,
            control,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return RemoteAction(Icon.createWithResource(host, icon), label, label, pendingIntent)
    }

    private fun stableRatio(requested: Float, current: Rational): Rational {
        if (!requested.isFinite() || requested <= 0f) return current
        val clamped = requested.coerceIn(MIN_PIP_RATIO, MAX_PIP_RATIO)
        if (abs(clamped / current.toFloat() - 1f) <= RATIO_TOLERANCE) return current
        if (abs(clamped / DEFAULT_RATIONAL.toFloat() - 1f) <= RATIO_TOLERANCE) return DEFAULT_RATIONAL
        return Rational((clamped * RATIO_SCALE).roundToInt(), RATIO_SCALE)
    }

    private companion object {
        val DEFAULT_RATIONAL = Rational(16, 9)
        const val MIN_PIP_RATIO = 0.42f
        const val MAX_PIP_RATIO = 2.39f
        const val RATIO_TOLERANCE = 0.02f
        const val RATIO_SCALE = 1000
    }
}

private class AndroidStreamOrientationController(
    private val activity: Activity
) : StreamOrientationController {

    private val originalOrientation = activity.requestedOrientation
    private var targetLandscape: Boolean? = null
    private var reachedTarget = false

    private val listener = object : OrientationEventListener(activity) {
        override fun onOrientationChanged(orientation: Int) {
            onDeviceRotated(orientation)
        }
    }

    override fun enterLandscape() = lock(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, landscape = true)

    override fun exitLandscape() = lock(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, landscape = false)

    fun release() {
        listener.disable()
        targetLandscape = null
        reachedTarget = false
        activity.requestedOrientation = originalOrientation
    }

    private fun lock(orientation: Int, landscape: Boolean) {
        activity.requestedOrientation = orientation
        targetLandscape = landscape
        reachedTarget = false
        if (isAutoRotateEnabled() && listener.canDetectOrientation()) listener.enable() else listener.disable()
    }

    private fun onDeviceRotated(degrees: Int) {
        val target = targetLandscape ?: return
        if (degrees == OrientationEventListener.ORIENTATION_UNKNOWN) return
        val landscapeNow = degrees in 60..120 || degrees in 240..300
        val portraitNow = degrees <= 30 || degrees >= 330
        val atTarget = if (target) landscapeNow else portraitNow
        val atOpposite = if (target) portraitNow else landscapeNow
        when {
            atTarget -> reachedTarget = true
            reachedTarget && atOpposite -> release()
        }
    }

    private fun isAutoRotateEnabled(): Boolean =
        runCatching {
            Settings.System.getInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        }.getOrDefault(false)
}

private const val ACTION_PIP_CONTROL = "io.rudione.chatone.action.PIP_CONTROL"
private const val EXTRA_PIP_CONTROL = "control"
private const val PIP_CONTROL_PLAYBACK = 1
private const val PIP_CONTROL_SOUND = 2

private object NoOpOrientationController : StreamOrientationController {
    override fun enterLandscape() = Unit
    override fun exitLandscape() = Unit
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
