package io.rudione.chatone.presentation.chat.emote

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt

@Stable
private class SharedDrawablePainter(private val drawable: Drawable) : Painter(), RememberObserver {

    private var frameTick by mutableIntStateOf(0)
    private val onFrame: () -> Unit = { frameTick++ }

    override val intrinsicSize: Size
        get() {
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            return if (width > 0 && height > 0) Size(width.toFloat(), height.toFloat()) else Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        frameTick
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }

    override fun onRemembered() = SharedEmoteDrawables.attach(drawable, onFrame)

    override fun onForgotten() = SharedEmoteDrawables.detach(drawable, onFrame)

    override fun onAbandoned() = Unit
}

@Composable
internal fun rememberSharedEmotePainter(url: String, maxDimension: Int = 0): Painter? {
    if (url.isBlank()) return null
    val context = LocalContext.current.applicationContext
    val key = SharedEmoteDrawables.keyOf(url, maxDimension)
    var drawable by remember(key) { mutableStateOf(SharedEmoteDrawables.peek(key)) }
    if (drawable == null) {
        LaunchedEffect(key) {
            drawable = SharedEmoteDrawables.load(context, url, maxDimension)
        }
    }
    val loaded = drawable ?: return null
    return remember(loaded) { SharedDrawablePainter(loaded) }
}

@Composable
internal fun SharedEmoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxDimension: Int = 0
) {
    val painter = rememberSharedEmotePainter(url, maxDimension)
    val described = if (contentDescription == null) modifier else modifier.semantics {
        this.contentDescription = contentDescription
        role = Role.Image
    }
    Box(if (painter == null) described else described.paint(painter, contentScale = ContentScale.Fit))
}
