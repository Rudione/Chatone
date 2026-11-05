package io.rudione.chatone.presentation.settings.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchEngineTest {

    @Test
    fun normalizationFoldsCaseYoAndPunctuation() {
        assertEquals("зачеркивание и текст", SearchText.normalize("  Зачёркивание, и — ТЕКСТ! "))
    }

    @Test
    fun stemmerCutsCommonEndings() {
        assertEquals(SearchText.stem("боковая"), SearchText.stem("боковую"))
        assertEquals("notification", SearchText.stem("notifications"))
    }

    @Test
    fun distanceCountsTranspositionsAsOneEdit() {
        assertEquals(1, SearchText.distance("тмеа", "тема", 2))
        assertEquals(3, SearchText.distance("abc", "xyzabc", 2))
    }

    @Test
    fun highlightCoversWholeWordsForStemsAndLayouts() {
        assertEquals(listOf(8..17), SearchText.highlightRanges("Масштаб интерфейса", "интерфейс"))
        assertEquals(listOf(0..3), SearchText.highlightRanges("Тема", "ntvf"))
        assertEquals(2, SearchText.highlightRanges("Лимит истории сообщений", "сообщений лимит").size)
        assertTrue(SearchText.highlightRanges("Тема", "x").isEmpty())
    }

    @Test
    fun transliterationRoundTrips() {
        assertEquals("fon", SearchText.transliterate("фон"))
        assertEquals("чат", SearchText.transliterate("chat"))
    }
}
