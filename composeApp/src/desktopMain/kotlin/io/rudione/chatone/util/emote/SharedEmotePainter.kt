package io.rudione.chatone.util.emote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

private class EmotePlayerHandle(val player: EmotePlayer) : RememberObserver {
    var lastIndex = player.frameIndex

    fun frameIndex(paused: Boolean): Int {
        if (!paused) lastIndex = player.frameIndex
        return lastIndex
    }

    override fun onRemembered() = SharedEmoteAnimator.retain(player)

    override fun onForgotten() = SharedEmoteAnimator.release(player)

    override fun onAbandoned() = Unit
}

@Stable
private class SharedEmotePainter(
    private val handle: EmotePlayerHandle,
    private val paused: State<Boolean>
) : Painter(), RememberObserver by handle {

    private val frames = handle.player.frames.frames
    private var alpha = 1f
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size =
        frames.first().let { Size(it.width.toFloat(), it.height.toFloat()) }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val image = frames[handle.frameIndex(paused.value).coerceIn(0, frames.lastIndex)]
        drawImage(
            image = image,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            alpha = alpha,
            colorFilter = colorFilter,
            filterQuality = DrawScope.DefaultFilterQuality
        )
    }
}

@Composable
internal fun rememberSharedEmotePainter(frames: AnimatedFrames, paused: Boolean): Painter {
    val pausedState = rememberUpdatedState(paused)
    return remember(frames) {
        SharedEmotePainter(EmotePlayerHandle(SharedEmoteAnimator.playerFor(frames)), pausedState)
    }
}

@Composable
internal fun rememberSharedEmoteFrame(frames: AnimatedFrames, paused: Boolean): State<ImageBitmap?> {
    val pausedState = rememberUpdatedState(paused)
    val handle = remember(frames) { EmotePlayerHandle(SharedEmoteAnimator.playerFor(frames)) }
    return remember(handle) {
        derivedStateOf { frames.frames.getOrNull(handle.frameIndex(pausedState.value)) }
    }
}
