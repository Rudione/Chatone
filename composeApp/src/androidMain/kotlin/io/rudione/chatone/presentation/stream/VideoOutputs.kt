package io.rudione.chatone.presentation.stream

import android.graphics.Rect
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class DetachedVideoLayer {
    private val control: SurfaceControl = SurfaceControl.Builder()
        .setName(NAME)
        .setBufferSize(BASE_WIDTH, BASE_HEIGHT)
        .build()

    val surface: Surface = Surface(control)

    private var released = false
    private var window: AttachedSurfaceControl? = null

    fun showIn(host: SurfaceView): Boolean {
        val parent = host.surfaceControl
        val frame = host.holder.surfaceFrame
        if (released || parent == null || !parent.isValid || frame.isEmpty) return false
        window = host.rootSurfaceControl
        commit { reparent(control, parent).scaleTo(frame).setVisibility(control, true) }
        host.invalidate()
        return true
    }

    fun fitTo(host: SurfaceView) {
        val frame = host.holder.surfaceFrame
        if (released || frame.isEmpty) return
        window = host.rootSurfaceControl
        commit { scaleTo(frame) }
        host.invalidate()
    }

    fun hide() {
        if (released) return
        commit { reparent(control, null).setVisibility(control, false) }
        window = null
    }

    private fun SurfaceControl.Transaction.scaleTo(frame: Rect): SurfaceControl.Transaction =
        setScale(control, frame.width() / BASE_WIDTH.toFloat(), frame.height() / BASE_HEIGHT.toFloat())

    private inline fun commit(change: SurfaceControl.Transaction.() -> Unit) {
        val transaction = SurfaceControl.Transaction().apply(change)
        if (window?.applyTransactionOnDraw(transaction) == true) return
        transaction.use { it.apply() }
    }

    fun release() {
        if (released) return
        window = null
        hide()
        released = true
        surface.release()
        control.release()
    }

    companion object {
        private const val NAME = "ChatoneVideoLayer"
        private const val BASE_WIDTH = 1920
        private const val BASE_HEIGHT = 1080
    }
}

@UnstableApi
internal class VideoOutputs(
    private val player: ExoPlayer,
    private val layer: DetachedVideoLayer?
) {
    private val views = ArrayList<View>(2)
    private val watchers = HashMap<SurfaceView, SurfaceHolder.Callback>()
    private var layerBound = false
    private var layerHost: SurfaceView? = null

    val surfaceName: String
        get() = when {
            views.lastOrNull() is TextureView -> TEXTURE_VIEW
            layer != null -> SURFACE_CONTROL
            else -> SURFACE_VIEW
        }

    init {
        bindLayer()
    }

    fun attach(view: View) {
        views.remove(view)
        views.add(view)
        if (layer != null && view is SurfaceView) watch(view)
        present(view)
    }

    fun detach(view: View) {
        val wasCurrent = views.lastOrNull() === view
        views.remove(view)
        if (view is SurfaceView) unwatch(view)
        if (!wasCurrent) return
        withdraw(view)
        views.lastOrNull()?.let(::present)
    }

    fun frameSource(view: SurfaceView): Surface? = when {
        layer == null -> view.holder.surface
        layerHost === view -> layer.surface
        else -> null
    }

    fun release() {
        watchers.forEach { (view, watcher) -> view.holder.removeCallback(watcher) }
        watchers.clear()
        views.clear()
        layerHost = null
        layer?.release()
    }

    private fun present(view: View) {
        when (view) {
            is TextureView -> {
                hideLayer()
                layerBound = false
                player.setVideoTextureView(view)
            }

            is SurfaceView -> if (layer == null) {
                player.setVideoSurfaceView(view)
            } else {
                bindLayer()
                showLayerIn(view)
            }
        }
    }

    private fun withdraw(view: View) {
        when (view) {
            is TextureView -> if (layer == null) player.clearVideoTextureView(view) else bindLayer()
            is SurfaceView -> if (layer == null) player.clearVideoSurfaceView(view) else layerHost = null
        }
    }

    private fun bindLayer() {
        val target = layer ?: return
        if (layerBound) return
        layerBound = true
        player.setVideoSurface(target.surface)
    }

    private fun showLayerIn(view: SurfaceView) {
        val target = layer ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || view.holder.surface?.isValid != true) return
        if (target.showIn(view)) layerHost = view
    }

    private fun hideLayer() {
        if (layerHost == null) return
        layerHost = null
        layer?.hide()
    }

    private fun watch(view: SurfaceView) {
        if (view in watchers) return
        val watcher = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = showIfCurrent(view)

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                if (layerHost === view) layer?.fitTo(view)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
        }
        watchers[view] = watcher
        view.holder.addCallback(watcher)
    }

    private fun unwatch(view: SurfaceView) {
        watchers.remove(view)?.let(view.holder::removeCallback)
    }

    private fun showIfCurrent(view: SurfaceView) {
        if (layerBound && views.lastOrNull() === view) showLayerIn(view)
    }

    private companion object {
        const val SURFACE_VIEW = "SurfaceView"
        const val TEXTURE_VIEW = "TextureView"
        const val SURFACE_CONTROL = "SurfaceControl"
    }
}
