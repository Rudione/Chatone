package io.rudione.chatone.presentation.stream.ambient

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmbientColorsTest {

    private val width = 64
    private val height = 36

    private fun frame(pixel: (x: Int, y: Int) -> Int) = IntArray(width * height) { pixel(it % width, it / width) }

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun analyze(argb: IntArray) = FloatArray(AmbientColors.SIZE).also { AmbientColors.analyze(argb, width, height, it) }

    private fun FloatArray.color(edge: AmbientEdge, stop: Int): Triple<Float, Float, Float> {
        val offset = AmbientColors.index(edge, stop)
        return Triple(this[offset], this[offset + 1], this[offset + 2])
    }

    @Test
    fun eachSideFollowsItsOwnEdgeOfTheFrame() {
        val palette = analyze(frame { x, _ -> if (x < width / 2) rgb(20, 40, 200) else rgb(30, 190, 40) })
        repeat(AmbientColors.STOPS) { stop ->
            val (lr, lg, lb) = palette.color(AmbientEdge.LEFT, stop)
            val (rr, rg, rb) = palette.color(AmbientEdge.RIGHT, stop)
            assertTrue(lb > lg && lb > lr)
            assertTrue(rg > rb && rg > rr)
        }
    }

    @Test
    fun topAndBottomStopsFollowTheFrameHorizontally() {
        val palette = analyze(frame { x, y -> if (y < height / 2) rgb(240, 240, 240) else if (x < width / 2) rgb(200, 0, 0) else rgb(0, 0, 200) })
        val (tr, tg, tb) = palette.color(AmbientEdge.TOP, 0)
        assertTrue(tr > 0.7f && tg > 0.7f && tb > 0.7f)
        val (fr, _, fb) = palette.color(AmbientEdge.BOTTOM, 0)
        val (lr, _, lb) = palette.color(AmbientEdge.BOTTOM, AmbientColors.STOPS - 1)
        assertTrue(fr > fb)
        assertTrue(lb > lr)
    }

    @Test
    fun pixelsCloserToTheEdgeWeighMore() {
        val palette = analyze(frame { x, _ -> if (x == 0) rgb(255, 0, 0) else rgb(0, 0, 255) })
        val (r, _, b) = palette.color(AmbientEdge.LEFT, 0)
        val uniformShare = 1f / (width / 8)
        assertTrue(r / (r + b) > uniformShare)
    }

    @Test
    fun barsAreDimmedAndCapped() {
        val palette = analyze(frame { _, _ -> rgb(255, 255, 255) })
        val color = AmbientColors.barColor(palette, AmbientColors.index(AmbientEdge.LEFT, 0), alpha = 1f)
        assertTrue(color.red < 0.35f && color.green < 0.35f && color.blue < 0.35f)
        assertEquals(1f, color.alpha)
    }

    @Test
    fun blackEdgesStayBlack() {
        val palette = analyze(frame { _, _ -> rgb(0, 0, 0) })
        val color = AmbientColors.barColor(palette, AmbientColors.index(AmbientEdge.RIGHT, 2), alpha = 0.5f)
        assertEquals(0f, color.red)
        assertEquals(0f, color.green)
        assertEquals(0f, color.blue)
    }

    @Test
    fun glowIsBrighterThanBarsOnDarkBackgrounds() {
        val palette = analyze(frame { _, _ -> rgb(90, 140, 60) })
        val offset = AmbientColors.index(AmbientEdge.BOTTOM, 1)
        val bar = AmbientColors.barColor(palette, offset, 1f)
        val glow = AmbientColors.glowColor(palette, offset, 1f, darkBackground = true)
        assertTrue(glow.green > bar.green)
    }

    @Test
    fun lightBackgroundsGetAPastelGlow() {
        val palette = analyze(frame { _, _ -> rgb(20, 60, 200) })
        val glow = AmbientColors.glowColor(palette, AmbientColors.index(AmbientEdge.BOTTOM, 0), 1f, darkBackground = false)
        assertTrue(glow.red > 0.6f && glow.blue > glow.red)
    }

    @Test
    fun malformedInputLeavesThePaletteUntouched() {
        val out = FloatArray(AmbientColors.SIZE) { 0.5f }
        AmbientColors.analyze(IntArray(10), width, height, out)
        assertTrue(out.all { it == 0.5f })
    }
}
