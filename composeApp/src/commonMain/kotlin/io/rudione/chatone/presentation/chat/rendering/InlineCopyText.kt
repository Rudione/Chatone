package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString

internal const val INLINE_GLYPH = "\uFFFC"

private const val INLINE_COPY_TAG = "io.rudione.chatone.inlineCopy"

internal fun AnnotatedString.Builder.appendInlineGlyph(id: String, copyText: String) {
    pushStringAnnotation(INLINE_COPY_TAG, copyText)
    appendInlineContent(id, INLINE_GLYPH)
    pop()
}

internal fun AnnotatedString.copyableText(): String {
    val inline = getStringAnnotations(INLINE_COPY_TAG, 0, length)
    if (inline.isEmpty()) return text
    return buildString(length) {
        var cursor = 0
        inline.sortedBy { it.start }.forEach { range ->
            if (range.start < cursor) return@forEach
            append(text, cursor, range.start)
            append(range.item)
            cursor = range.end
        }
        append(text, cursor, text.length)
    }
}

internal fun Modifier.inlineTextSemantics(text: AnnotatedString): Modifier =
    semantics { contentDescription = text.copyableText() }
