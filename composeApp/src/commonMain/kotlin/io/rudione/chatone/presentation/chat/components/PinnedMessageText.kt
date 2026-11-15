package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.chat.AnimatedEmoteImage
import io.rudione.chatone.presentation.chat.CheerToken
import io.rudione.chatone.presentation.chat.computeEmoteDisplaySize
import io.rudione.chatone.presentation.chat.rememberChatNickname
import io.rudione.chatone.presentation.chat.rememberSevenTvPaintBrushProvider
import io.rudione.chatone.presentation.chat.rendering.appendInlineGlyph
import io.rudione.chatone.presentation.chat.rendering.inlineTextSemantics
import io.rudione.chatone.presentation.chat.sevenTvPaintShadow
import io.rudione.chatone.util.chat.MessageToken

@Composable
internal fun PinnedMessageText(
    message: DisplayMessage.PrivMsg,
    expanded: Boolean,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
    val nickname = rememberChatNickname(message.userId, message.username, message.displayName, message.color, message.sevenTvPaint)
    val badges = collectBadgeVisuals(
        message.username, message.userId, message.badges, message.sevenTvBadge,
        sharedChat = message.rawMessage?.sourceRoomId != null
    )
    val inline = mutableMapOf<String, InlineTextContent>()
    val nicknameStyle = style.copy(fontWeight = FontWeight.Bold)
    val paint = nickname.paint
    if (paint != null) {
        val measurer = rememberTextMeasurer()
        val layout = remember(nickname.name, nicknameStyle, measurer) {
            measurer.measure(AnnotatedString(nickname.name), nicknameStyle, softWrap = false, maxLines = 1)
        }
        val density = LocalDensity.current
        val width = with(density) { layout.size.width.toSp() }
        val baseline = with(density) { layout.firstBaseline.toSp() }
        inline["author"] = InlineTextContent(Placeholder(width, baseline, PlaceholderVerticalAlign.AboveBaseline)) {
            val brush = rememberSevenTvPaintBrushProvider(paint, nickname.color)
            val fallback = remember(nickname.color) { SolidColor(nickname.color) }
            val shadow = remember(paint, density) { sevenTvPaintShadow(paint, density.density) }
            Spacer(Modifier.fillMaxSize().drawBehind {
                drawText(layout, brush = brush() ?: fallback, shadow = shadow)
            })
        }
    }
    val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val annotated = buildAnnotatedString {
        badges.forEachIndexed { index, badge ->
            val key = "badge_$index"
            appendInlineGlyph(key, badge.tooltip)
            append(" ")
            inline[key] = InlineTextContent(Placeholder(13.sp, 13.sp, PlaceholderVerticalAlign.TextCenter)) {
                BadgeIcon(badge, size = 13.dp)
            }
        }
        if (paint != null) appendInlineGlyph("author", nickname.name)
        else withStyle(SpanStyle(color = nickname.color, fontWeight = FontWeight.Bold)) { append(nickname.name) }
        append(if (message.isAction) " " else ": ")
        if (message.isAction) pushStyle(SpanStyle(color = nickname.color, fontStyle = FontStyle.Italic))
        if (message.isDeleted) pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        message.tokens.forEachIndexed { index, token ->
            val key = "token_$index"
            when (token) {
                is MessageToken.Text -> append(token.text)
                is MessageToken.Link -> {
                    if (expanded) withLink(LinkAnnotation.Clickable(token.url, TextLinkStyles(style = linkStyle)) { onOpenLink(token.url) }) {
                        append(token.displayText)
                    } else withStyle(linkStyle) { append(token.displayText) }
                }
                is MessageToken.TwitchEmoteToken -> {
                    appendInlineGlyph(key, token.name)
                    inline[key] = InlineTextContent(Placeholder(18.sp, 18.sp, PlaceholderVerticalAlign.TextCenter)) {
                        AnimatedEmoteImage(token.url, token.name, Modifier.fillMaxSize())
                    }
                }
                is MessageToken.ThirdPartyEmoteToken -> {
                    appendInlineGlyph(key, token.emote.code)
                    val (width, height) = computeEmoteDisplaySize(token.emote.width, token.emote.height, 18.sp)
                    inline[key] = InlineTextContent(Placeholder(width, height, PlaceholderVerticalAlign.TextCenter)) {
                        PinnedEmote(token.emote, token.overlays)
                    }
                }
                is MessageToken.Mention -> withStyle(SpanStyle(color = nickname.color, fontWeight = FontWeight.Medium)) { append(token.username) }
                is MessageToken.Cheer -> {
                    appendInlineGlyph(key, "${token.prefix}${token.amount}")
                    inline[key] = InlineTextContent(Placeholder((20 + token.amount.toString().length * 7).sp, 18.sp, PlaceholderVerticalAlign.TextCenter)) {
                        CheerToken(amount = token.amount, fontSizeSp = 12f)
                    }
                }
                is MessageToken.GifToken -> append("[${token.title}]")
            }
        }
        if (message.isDeleted) pop()
        if (message.isAction) pop()
    }
    BasicText(
        text = annotated,
        style = style,
        inlineContent = inline,
        maxLines = if (expanded) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.inlineTextSemantics(annotated)
    )
}

@Composable
private fun PinnedEmote(emote: GenericEmote, overlays: List<GenericEmote>) {
    Box(Modifier.fillMaxSize()) {
        AnimatedEmoteImage(emote.url2x.ifBlank { emote.url1x }, emote.code, Modifier.fillMaxSize())
        overlays.forEach { overlay ->
            AnimatedEmoteImage(overlay.url2x.ifBlank { overlay.url1x }, overlay.code, Modifier.fillMaxSize())
        }
    }
}
