package io.rudione.chatone.presentation.chat.components

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentedMessageBarTest {

    private val spans = listOf(
        SegmentSpan(Rect(400f, 0f, 434f, 34f), round = true),
        SegmentSpan(Rect(0f, 0f, 34f, 34f), round = true),
        SegmentSpan(Rect(34f, 0f, 400f, 34f), round = false)
    )

    @Test
    fun mergedSegmentsFormOnePillWithSquareSeams() {
        val rects = segmentRects(spans, height = 34f, split = 0f, fieldCornerPx = 16f)
        assertEquals(listOf(0f, 34f, 400f), rects.map { it.left })
        assertEquals(rects[0].right, rects[1].left)
        assertEquals(rects[1].right, rects[2].left)
        assertEquals(17f, rects[0].topLeftCornerRadius.x)
        assertEquals(0f, rects[0].topRightCornerRadius.x)
        assertEquals(0f, rects[1].topLeftCornerRadius.x)
        assertEquals(0f, rects[1].bottomRightCornerRadius.x)
        assertEquals(17f, rects[2].bottomRightCornerRadius.x)
    }

    @Test
    fun splitSegmentsAreRoundOnEverySide() {
        val rects = segmentRects(spans, height = 34f, split = 1f, fieldCornerPx = 16f)
        assertEquals(17f, rects[0].topRightCornerRadius.x)
        assertEquals(16f, rects[1].topLeftCornerRadius.x)
        assertEquals(16f, rects[1].bottomRightCornerRadius.x)
        assertEquals(17f, rects[2].topLeftCornerRadius.x)
    }

    @Test
    fun sideButtonsStretchToTheFieldHeightOnlyWhenMerged() {
        val tall = listOf(
            SegmentSpan(Rect(0f, 46f, 34f, 80f), round = true),
            SegmentSpan(Rect(34f, 0f, 300f, 80f), round = false)
        )
        val merged = segmentRects(tall, height = 80f, split = 0f, fieldCornerPx = 16f)
        assertEquals(0f, merged[0].top)
        val split = segmentRects(tall, height = 80f, split = 1f, fieldCornerPx = 16f)
        assertEquals(46f, split[0].top)
        assertEquals(17f, split[0].topLeftCornerRadius.x)
    }
}
