package io.rudione.chatone.presentation.ai

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object AiMarkdown {
    private val heading = Regex("^\\s{0,3}#{1,6}\\s+(.*?)\\s*#*\\s*$")
    private val bullet = Regex("^(\\s*)[*+\\-]\\s+(.*)$")
    private val numbered = Regex("^(\\s*)(\\d{1,3})[.)]\\s+(.*)$")
    private val rule = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")

    fun render(text: String, codeBackground: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
        var blank = 0
        var first = true
        text.replace("\r\n", "\n").lines().forEach { raw ->
            val line = raw.trimEnd()
            if (line.isBlank() || rule.matches(line)) {
                blank++
                return@forEach
            }
            if (!first) append(if (blank > 0) "\n\n" else "\n")
            first = false
            blank = 0
            val h = heading.matchEntire(line)
            val b = bullet.matchEntire(line)
            val n = numbered.matchEntire(line)
            when {
                h != null -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { inline(h.groupValues[1], codeBackground) }
                b != null -> {
                    append(indent(b.groupValues[1]))
                    append("• ")
                    inline(b.groupValues[2], codeBackground)
                }
                n != null -> {
                    append(indent(n.groupValues[1]))
                    append(n.groupValues[2])
                    append(". ")
                    inline(n.groupValues[3], codeBackground)
                }
                else -> inline(line, codeBackground)
            }
        }
    }

    private fun indent(spaces: String): String = "    ".repeat((spaces.replace("\t", "    ").length / 2).coerceAtMost(3))

    private fun AnnotatedString.Builder.inline(source: String, codeBackground: Color) {
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c == '`' -> {
                    val end = source.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                            append(source.substring(i + 1, end))
                        }
                        i = end + 1
                        continue
                    }
                }
                source.startsWith("**", i) || source.startsWith("__", i) -> {
                    val marker = source.substring(i, i + 2)
                    val end = source.indexOf(marker, i + 2)
                    if (end > i + 2 && !source[i + 2].isWhitespace() && !source[end - 1].isWhitespace() && (marker == "**" || boundary(source, i, end + 2))) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { inline(source.substring(i + 2, end), codeBackground) }
                        i = end + 2
                        continue
                    }
                }
                c == '*' -> {
                    val end = source.indexOf('*', i + 1)
                    if (end > i + 1 && !source[i + 1].isWhitespace() && !source[end - 1].isWhitespace()) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { inline(source.substring(i + 1, end), codeBackground) }
                        i = end + 1
                        continue
                    }
                }
            }
            append(c)
            i++
        }
    }

    private fun boundary(source: String, start: Int, end: Int): Boolean {
        val before = source.getOrNull(start - 1)
        val after = source.getOrNull(end)
        return (before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())
    }
}
