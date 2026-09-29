package io.rudione.chatone.presentation.stream

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
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
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
actual fun PictureInPictureAutoEnterEffect(
    controller: PictureInPictureController,
    enabled: Boolean,
    aspectRatio: Float
) {
    val androidController = controller as? AndroidPictureInPictureController ?: return
    val shouldAutoEnter = androidController.isSupported && enabled
    LaunchedEffect(androidController, shouldAutoEnter, aspectRatio) {
        androidController.updateParams(autoEnter = shouldAutoEnter, aspectRatio = aspectRatio)
    }
    DisposableEffect(androidController) {
        onDispose { androidController.updateParams(autoEnter = false, aspectRatio = aspectRatio) }
    }
    DisposableEffect(androidController, shouldAutoEnter) {
        val hint = Runnable {
            if (shouldAutoEnter && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) androidController.enter()
        }
        androidController.activity?.addOnUserLeaveHintListener(hint)
        onDispose { androidController.activity?.removeOnUserLeaveHintListener(hint) }
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
    private var appliedRatio: Rational? = null
    private var appliedAutoEnter: Boolean? = null

    override fun enter() {
        val host = activity ?: return
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            host.enterPictureInPictureMode(params(autoEnter = false))
            appliedRatio = ratio
            appliedAutoEnter = false
        }
    }

    fun updateParams(autoEnter: Boolean, aspectRatio: Float) {
        ratio = stableRatio(aspectRatio, ratio)
        val host = activity ?: return
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (ratio == appliedRatio && autoEnter == appliedAutoEnter) return
        runCatching {
            host.setPictureInPictureParams(params(autoEnter))
            appliedRatio = ratio
            appliedAutoEnter = autoEnter
        }
    }

    private fun params(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(ratio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
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

private object NoOpOrientationController : StreamOrientationController {
    override fun enterLandscape() = Unit
    override fun exitLandscape() = Unit
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
