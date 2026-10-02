package io.rudione.chatone.presentation.browse

import io.rudione.chatone.domain.browse.BrowseStream
import io.rudione.chatone.presentation.theme.i18n.BrowseStringsEn
import io.rudione.chatone.presentation.theme.i18n.BrowseStringsRu
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowseFormattingTest {

    @Test
    fun compactNumbersFollowTheLocale() {
        assertEquals("950", BrowseFormatting.compact(950, BrowseStringsRu))
        assertEquals("1,2 тыс.", BrowseFormatting.compact(1_234, BrowseStringsRu))
        assertEquals("126 тыс.", BrowseFormatting.compact(126_193, BrowseStringsRu))
        assertEquals("1,5 млн", BrowseFormatting.compact(1_512_000, BrowseStringsRu))
        assertEquals("1.2K", BrowseFormatting.compact(1_234, BrowseStringsEn))
        assertEquals("2K", BrowseFormatting.compact(2_000, BrowseStringsEn))
    }

    @Test
    fun russianViewerCountsArePluralised() {
        assertEquals("1 зритель", BrowseFormatting.viewers(1, BrowseStringsRu))
        assertEquals("3 зрителя", BrowseFormatting.viewers(3, BrowseStringsRu))
        assertEquals("11 зрителей", BrowseFormatting.viewers(11, BrowseStringsRu))
        assertEquals("21 зритель", BrowseFormatting.viewers(21, BrowseStringsRu))
        assertEquals("126 тыс. зрителей", BrowseFormatting.viewers(126_193, BrowseStringsRu))
        assertEquals("1 viewer", BrowseFormatting.viewers(1, BrowseStringsEn))
        assertEquals("2.5K viewers", BrowseFormatting.viewers(2_535, BrowseStringsEn))
    }

    @Test
    fun uptimeIsHoursAndMinutes() {
        assertEquals("2h 05 min", BrowseFormatting.uptime(startedAtMs = 0L, nowMs = 125 * 60_000L))
        assertEquals("0 min", BrowseFormatting.uptime(startedAtMs = 1_000L, nowMs = 1_000L))
        assertNull(BrowseFormatting.uptime(startedAtMs = null, nowMs = 1_000L))
        assertNull(BrowseFormatting.uptime(startedAtMs = 5_000_000L, nowMs = 1_000L))
    }

    @Test
    fun aiPromptCarriesTheStreamContext() {
        val stream = BrowseStream(
            id = "1", login = "streamer", displayName = "Streamer", avatarUrl = "", title = "Ranked\ngrind",
            viewers = 2_535, startedAtMs = 0L, previewUrl = "", language = "en", tags = listOf("English", "FPS")
        )

        val prompt = BrowseFormatting.aiPrompt(stream, "Valorant", BrowseStringsEn, nowMs = 90 * 60_000L)

        assertTrue("Streamer" in prompt)
        assertTrue("\"Ranked grind\"" in prompt)
        assertTrue("Category: Valorant" in prompt)
        assertTrue("Tags: English, FPS" in prompt)
        assertTrue("Language: EN" in prompt)
        assertTrue("Viewers: 2.5K viewers" in prompt)
        assertTrue("Live for: 1h 30 min" in prompt)
    }

    @Test
    fun aiPromptMarksMissingDetails() {
        val stream = BrowseStream(
            id = "1", login = "quiet", displayName = "", avatarUrl = "", title = " ",
            viewers = 0, startedAtMs = null, previewUrl = "", language = "", tags = emptyList()
        )

        val prompt = BrowseFormatting.aiPrompt(stream, "Just Chatting", BrowseStringsRu, nowMs = 0L)

        assertTrue("quiet" in prompt)
        assertTrue("Название: «—»" in prompt)
        assertTrue("Теги: —" in prompt)
        assertTrue("В эфире: —" in prompt)
    }
}
