package io.rudione.chatone.presentation.components.motion

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath

internal class GlyphLayer(
    val path: Path,
    val stroke: Stroke?,
    val filled: Boolean
)

internal class GlyphLayers(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val layers: List<GlyphLayer>
) {
    val size: Int get() = layers.size
    operator fun get(index: Int): GlyphLayer? = layers.getOrNull(index)
}

internal object GlyphCache {
    private val cache = HashMap<ImageVector, GlyphLayers>()

    fun layers(vector: ImageVector): GlyphLayers = cache.getOrPut(vector) { parse(vector) }

    private fun parse(vector: ImageVector): GlyphLayers {
        val out = ArrayList<GlyphLayer>()
        collect(vector.root, out)
        return GlyphLayers(vector.viewportWidth, vector.viewportHeight, out)
    }

    private fun collect(group: VectorGroup, out: MutableList<GlyphLayer>) {
        for (node in group) {
            when (node) {
                is VectorGroup -> collect(node, out)
                is VectorPath -> out += layerOf(node)
            }
        }
    }

    private fun layerOf(node: VectorPath): GlyphLayer {
        val path = PathParser().addPathNodes(node.pathData).toPath()
        path.fillType = node.pathFillType
        val fill = node.fill
        val filled = fill != null && !(fill is SolidColor && fill.value.alpha == 0f)
        val strokeBrush = node.stroke
        val stroked = strokeBrush != null && !(strokeBrush is SolidColor && strokeBrush.value.alpha == 0f) &&
            node.strokeLineWidth > 0f
        val stroke = if (stroked) {
            Stroke(
                width = node.strokeLineWidth,
                cap = node.strokeLineCap,
                join = node.strokeLineJoin,
                miter = node.strokeLineMiter
            )
        } else null
        return GlyphLayer(path, stroke, filled)
    }
}
