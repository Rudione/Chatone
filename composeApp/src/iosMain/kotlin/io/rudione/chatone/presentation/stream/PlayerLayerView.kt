package io.rudione.chatone.presentation.stream

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObjectProtocol

@OptIn(ExperimentalForeignApi::class)
internal class PlayerLayerView(private val pictureInPicture: Boolean = false) : UIView(frame = CGRectZero.readValue()) {

    val playerLayer: AVPlayerLayer = AVPlayerLayer()
    private var attached: AVPlayer? = null
    private val observers = ArrayList<NSObjectProtocol>(2)
    private var rendered: AVPlayer? = null

    init {
        backgroundColor = UIColor.blackColor
        playerLayer.videoGravity = AVLayerVideoGravityResizeAspect
        layer.addSublayer(playerLayer)
        val center = NSNotificationCenter.defaultCenter
        observers += center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            if (!IosPictureInPicture.keepsLayer(playerLayer)) playerLayer.player = null
        }
        observers += center.addObserverForName(UIApplicationWillEnterForegroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            playerLayer.player = rendered
        }
    }

    fun attach(player: AVPlayer?) {
        if (attached === player) return
        attached?.let { PlayerLayerOwnership.release(it, this) }
        attached = player
        player?.let { PlayerLayerOwnership.claim(it, this) }
    }

    internal fun render(player: AVPlayer?, refresh: Boolean = false) {
        if (rendered === player && !refresh) return
        if (refresh) playerLayer.player = null
        rendered = player
        playerLayer.player = player
        if (pictureInPicture && player != null) IosPictureInPicture.bind(playerLayer)
    }

    fun dispose() {
        if (pictureInPicture) IosPictureInPicture.unbind(playerLayer)
        attach(null)
        observers.forEach(NSNotificationCenter.defaultCenter::removeObserver)
        observers.clear()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}

internal object PlayerLayerOwnership {
    private val owners = HashMap<AVPlayer, ArrayList<PlayerLayerView>>()

    fun claim(player: AVPlayer, view: PlayerLayerView) {
        val views = owners.getOrPut(player) { ArrayList(2) }
        views.remove(view)
        views.add(view)
        views.forEach { it.render(if (it === view) player else null) }
    }

    fun release(player: AVPlayer, view: PlayerLayerView) {
        val views = owners[player] ?: return
        val wasCurrent = views.lastOrNull() === view
        views.remove(view)
        view.render(null)
        if (views.isEmpty()) owners.remove(player) else if (wasCurrent) views.last().render(player, refresh = true)
    }
}
