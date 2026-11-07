package io.rudione.chatone.presentation.ai

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiMarkdownTest {

    @Test
    fun boldMarkersAreRemovedAndStyled() {
        val out = AiMarkdown.render("**Главные темы:** игра")
        assertEquals("Главные темы: игра", out.text)
        val bold = out.spanStyles.single { it.item.fontWeight == FontWeight.SemiBold }
        assertEquals("Главные темы:", out.text.substring(bold.start, bold.end))
    }

    @Test
    fun bulletsBecomeDotsWithNestedIndent() {
        val out = AiMarkdown.render("*   **Игра:** GTA\n    * миссии")
        assertEquals("• Игра: GTA\n        • миссии", out.text)
    }

    @Test
    fun headingsDropHashes() {
        val out = AiMarkdown.render("## Настроение\nW")
        assertEquals("Настроение\nW", out.text)
        assertTrue(out.spanStyles.any { it.item.fontWeight == FontWeight.SemiBold && it.start == 0 && it.end == 10 })
    }

    @Test
    fun underscoresInsideNicknamesStayIntact() {
        val out = AiMarkdown.render("brisk_tide и saltmarsh_ivy пишут чаще всех")
        assertEquals("brisk_tide и saltmarsh_ivy пишут чаще всех", out.text)
        assertTrue(out.spanStyles.isEmpty())
    }

    @Test
    fun singleStarIsItalicAndLoneStarIsKept() {
        val out = AiMarkdown.render("*тихо* и 5 * 3")
        assertEquals("тихо и 5 * 3", out.text)
        assertTrue(out.spanStyles.any { it.item.fontStyle == FontStyle.Italic && it.start == 0 && it.end == 4 })
    }

    @Test
    fun codeSpansLoseBackticks() {
        assertEquals("напишите !schedule", AiMarkdown.render("напишите `!schedule`").text)
    }

    @Test
    fun blankRunsCollapseAndRulesDisappear() {
        assertEquals("a\n\nb", AiMarkdown.render("a\n\n\n---\n\nb").text)
    }

    @Test
    fun numberedListsKeepNumbers() {
        assertEquals("1. первое\n2. второе", AiMarkdown.render("1) первое\n2. второе").text)
    }
}
