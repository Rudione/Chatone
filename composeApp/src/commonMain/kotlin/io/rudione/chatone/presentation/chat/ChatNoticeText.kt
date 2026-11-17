package io.rudione.chatone.presentation.chat

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.presentation.chat.rendering.appendInlineGlyph
import io.rudione.chatone.presentation.chat.rendering.inlineTextSemantics

private val PlainLinkStyles = TextLinkStyles()

internal fun noticeAuthorRange(text: String, user: ChatNoticeUser): IntRange? =
    listOf(user.displayName, user.login).filter(String::isNotBlank).mapNotNull { name ->
        var start = text.indexOf(name, ignoreCase = true)
        while (start >= 0) {
            val end = start + name.length
            val before = text.getOrNull(start - 1)
            val after = text.getOrNull(end)
            if ((before == null || !before.isLetterOrDigit() && before != '_') &&
                (after == null || !after.isLetterOrDigit() && after != '_')) return@mapNotNull start until end
            start = text.indexOf(name, start + 1, ignoreCase = true)
        }
        null
    }.minByOrNull { it.first }

@Composable
internal fun ChatNoticeText(text: String, author: ChatNoticeUser?, color: Color, onOpenUser: (ChatNoticeUser) -> Unit) {
    val range = author?.let { noticeAuthorRange(text, it) }
    if (author == null || range == null) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        return
    }
    val nickname = rememberChatNickname(author.userId, author.login, author.displayName, author.color)
    val inline = mutableMapOf<String, InlineTextContent>()
    nickname.paint?.let { paint ->
        registerPaintedNick(
            inline, "author", nickname.name, paint, MaterialTheme.typography.bodySmall.fontSize.value,
            false, nickname.color, onClick = { onOpenUser(author) }, onRightClick = {}
        )
    }
    val annotated = buildAnnotatedString {
        append(text.substring(0, range.first))
        withLink(LinkAnnotation.Clickable("notice_author", PlainLinkStyles) { onOpenUser(author) }) {
            if (nickname.paint != null) appendInlineGlyph("author", nickname.name)
            else withStyle(SpanStyle(color = nickname.color, fontWeight = FontWeight.Bold)) { append(nickname.name) }
        }
        append(text.substring(range.last + 1))
    }
    Text(
        annotated,
        inlineContent = inline,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.inlineTextSemantics(annotated)
    )
}
