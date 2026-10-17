package io.rudione.chatone.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class AnimatedBorderTest {

    private val tint = Color(0xFF7C4DFF)

    @Test
    fun matchesTheStockBorderForCommonShapes() {
        val shapes: List<Shape> = listOf(RectangleShape, RoundedCornerShape(10.dp), CircleShape, RoundedCornerShape(50))
        for (shape in shapes) {
            for (width in listOf(1.dp, 1.5.dp, 2.dp)) {
                assertSimilar(shape, width, maxMismatchRatio = 0.0)
            }
        }
    }

    @Test
    fun staysCloseToTheStockBorderForGenericShapes() {
        assertSimilar(CutCornerShape(8.dp), 2.dp, maxMismatchRatio = 0.02)
        assertSimilar(RoundedCornerShape(topStart = 12.dp, bottomEnd = 4.dp), 1.dp, maxMismatchRatio = 0.02)
    }

    private fun assertSimilar(shape: Shape, width: Dp, maxMismatchRatio: Double) {
        val expected = render(Modifier.border(width, tint, shape))
        val actual = render(Modifier.animatedBorder(width, shape) { tint })
        assertTrue(expected.count { it != WHITE } > 0, "$shape $width: stock border drew nothing")
        val mismatched = expected.indices.count { channelDistance(expected[it], actual[it]) > CHANNEL_TOLERANCE }
        val ratio = mismatched.toDouble() / expected.size
        assertTrue(ratio <= maxMismatchRatio, "$shape $width: $mismatched pixels differ")
    }

    private fun render(border: Modifier): IntArray {
        val scene = ImageComposeScene(SIZE_PX, SIZE_PX, Density(2f)) {
            Box(Modifier.size(60.dp).background(Color.White).then(border))
        }
        try {
            val image = scene.render()
            return pixels(image)
        } finally {
            scene.close()
        }
    }

    private fun pixels(image: Image): IntArray {
        val bitmap = Bitmap.makeFromImage(image)
        return IntArray(SIZE_PX * SIZE_PX) { index -> bitmap.getColor(index % SIZE_PX, index / SIZE_PX) }
    }

    private fun channelDistance(a: Int, b: Int): Int =
        (0 until 4).maxOf { shift -> abs(((a ushr (shift * 8)) and 0xFF) - ((b ushr (shift * 8)) and 0xFF)) }

    private companion object {
        const val SIZE_PX = 120
        const val CHANNEL_TOLERANCE = 24
        const val WHITE = -1
    }
}
