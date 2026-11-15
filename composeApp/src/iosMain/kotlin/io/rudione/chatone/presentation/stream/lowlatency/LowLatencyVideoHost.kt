package io.rudione.chatone.presentation.stream.lowlatency

import io.rudione.chatone.presentation.stream.IosPictureInPicture
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth

@OptIn(ExperimentalForeignApi::class)
internal class LowLatencyVideoHost : UIView(frame = CGRectZero.readValue()) {

    private var stack: VideoSurfaceStack? = null

    init {
        backgroundColor = UIColor.blackColor
        clipsToBounds = true
    }

    fun attach(target: VideoSurfaceStack?) {
        if (stack === target) return
        stack?.release(this)
        stack = target
        target?.claim(this)
    }

    fun dispose() = attach(null)

    fun mount(video: UIView) {
        if (video.superview === this) return
        video.setFrame(bounds)
        video.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        addSubview(video)
    }
}

internal class VideoSurfaceStack(private val player: LowLatencyPlayer) {
    private val hosts = ArrayList<LowLatencyVideoHost>(2)

    fun claim(host: LowLatencyVideoHost) {
        hosts.remove(host)
        hosts.add(host)
        host.mount(player.view)
        bindPictureInPicture()
    }

    fun release(host: LowLatencyVideoHost) {
        val wasVisible = hosts.lastOrNull() === host
        hosts.remove(host)
        if (wasVisible) hosts.lastOrNull()?.mount(player.view) ?: player.view.removeFromSuperview()
        bindPictureInPicture()
    }

    fun dispose() {
        hosts.clear()
        player.view.removeFromSuperview()
        IosPictureInPicture.unbind(player)
    }

    private fun bindPictureInPicture() {
        if (hosts.isEmpty()) {
            IosPictureInPicture.unbind(player)
        } else {
            IosPictureInPicture.bind(player) { player.pictureInPictureController() }
        }
    }
}
