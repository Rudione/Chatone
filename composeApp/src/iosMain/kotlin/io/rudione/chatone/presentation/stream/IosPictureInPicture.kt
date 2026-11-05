package io.rudione.chatone.presentation.stream

import platform.AVKit.AVPictureInPictureController
import platform.AVKit.AVPictureInPictureControllerDelegateProtocol
import platform.AVFoundation.AVPlayerLayer
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationState
import platform.darwin.NSObject

internal object IosPictureInPicture {
    private var controller: AVPictureInPictureController? = null
    private var layer: AVPlayerLayer? = null
    private var autoEnter = false
    private val delegate = DismissTracker()

    var onDismissed: (() -> Unit)?
        get() = delegate.onDismissed
        set(value) {
            delegate.onDismissed = value
        }

    val isSupported: Boolean by lazy { AVPictureInPictureController.isPictureInPictureSupported() }

    fun bind(target: AVPlayerLayer) {
        if (!isSupported || layer === target) return
        layer = target
        controller = AVPictureInPictureController(playerLayer = target).apply {
            canStartPictureInPictureAutomaticallyFromInline = autoEnter
            delegate = this@IosPictureInPicture.delegate
        }
    }

    fun unbind(target: AVPlayerLayer) {
        if (layer !== target) return
        controller?.stopPictureInPicture()
        controller = null
        layer = null
    }

    fun start(): Boolean {
        val current = controller ?: return false
        if (!current.isPictureInPicturePossible()) return false
        current.startPictureInPicture()
        return true
    }

    fun setAutoEnter(enabled: Boolean) {
        autoEnter = enabled
        controller?.canStartPictureInPictureAutomaticallyFromInline = enabled
    }

    fun keepsLayer(target: AVPlayerLayer): Boolean =
        layer === target && (autoEnter || controller?.isPictureInPictureActive() == true)
}

private class DismissTracker : NSObject(), AVPictureInPictureControllerDelegateProtocol {
    var onDismissed: (() -> Unit)? = null
    private var restoring = false

    override fun pictureInPictureControllerWillStartPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        restoring = false
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        restoreUserInterfaceForPictureInPictureStopWithCompletionHandler: (Boolean) -> Unit
    ) {
        restoring = true
        restoreUserInterfaceForPictureInPictureStopWithCompletionHandler(true)
    }

    override fun pictureInPictureControllerDidStopPictureInPicture(pictureInPictureController: AVPictureInPictureController) {
        val closedFromOutside = !restoring &&
            UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateBackground
        restoring = false
        if (closedFromOutside) onDismissed?.invoke()
    }
}
