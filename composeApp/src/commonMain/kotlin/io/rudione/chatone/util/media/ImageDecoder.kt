package io.rudione.chatone.util.media

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

fun ImageBitmap.scaledTo(targetWidth: Int, targetHeight: Int): ImageBitmap {
    val width = targetWidth.coerceAtLeast(1)
    val height = targetHeight.coerceAtLeast(1)
    if (width == this.width && height == this.height) return this

    val target = ImageBitmap(width, height)
    Canvas(target).drawImageRect(
        image = this,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(this.width, this.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = Paint().apply { filterQuality = FilterQuality.Medium }
    )
    return target
}
