package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.ui.text.buildAnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InlineCopyTextTest {

    private val message = buildAnnotatedString {
        append("02:09 ")
        appendInlineGlyph("badges", "")
        appendInlineGlyph("nick", "FANTOM___1337")
        append(": Тамагочи ")
        appendInlineGlyph("emote_0", "KEKW")
        append(" френдзона")
    }

    @Test
    fun everyInlineItemTakesExactlyOneCharacter() {
        assertEquals("02:09 \uFFFC\uFFFC: Тамагочи \uFFFC френдзона", message.text)
    }

    @Test
    fun copyRestoresNicknamesAndEmoteCodes() {
        assertEquals("02:09 FANTOM___1337: Тамагочи KEKW френдзона", message.copyableText())
    }

    @Test
    fun aSelectionAfterTheNicknameKeepsItsOwnText() {
        val start = message.text.indexOf("Тамагочи")
        val selected = message.subSequence(start, message.length)
        assertEquals("Тамагочи KEKW френдзона", selected.copyableText())
    }

    @Test
    fun restoresOnlyWhenTheClipboardHoldsTheCurrentSelection() {
        val selected = listOf(message.subSequence(message.text.indexOf("Тамагочи"), message.length))
        assertEquals("Тамагочи KEKW френдзона", restoredSelection(selected.single().text, selected))
        assertNull(restoredSelection("something else", selected))
        assertNull(restoredSelection(null, selected))
    }

    @Test
    fun plainSelectionsAreLeftUntouched() {
        val plain = listOf(buildAnnotatedString { append("просто текст") })
        assertNull(restoredSelection("просто текст", plain))
    }
}
