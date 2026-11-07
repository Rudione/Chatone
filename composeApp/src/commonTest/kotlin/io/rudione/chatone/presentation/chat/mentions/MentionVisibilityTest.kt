package io.rudione.chatone.presentation.chat.mentions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MentionVisibilityTest {

    private val rows = listOf(
        RowSpan("top", -30, 10),
        RowSpan("half", 380, 420),
        RowSpan("full", 100, 140),
        RowSpan("covered", 450, 490),
        RowSpan(42, 200, 240)
    )

    @Test
    fun rowsShownAtLeastHalfwayCount() {
        val shown = shownKeys(rows, viewportTop = 0, viewportBottom = 400, wanted = setOf("top", "half", "full", "covered"))
        assertEquals(setOf("half", "full"), shown)
    }

    @Test
    fun onlyWantedStringKeysAreReported() {
        val shown = shownKeys(rows, viewportTop = 0, viewportBottom = 500, wanted = setOf("full"))
        assertEquals(setOf("full"), shown)
    }

    @Test
    fun emptyViewportShowsNothing() {
        assertTrue(shownKeys(rows, viewportTop = 0, viewportBottom = 0, wanted = setOf("full")).isEmpty())
    }
}
