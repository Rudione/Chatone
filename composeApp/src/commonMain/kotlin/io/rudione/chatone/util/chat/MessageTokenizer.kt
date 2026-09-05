package io.rudione.chatone.util.chat

import androidx.compose.runtime.Immutable
import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.GenericEmote

@Immutable
sealed class MessageToken {
    data class Text(val text: String) : MessageToken()
    data class TwitchEmoteToken(
        val id: String,
        val name: String,
        val url: String
    ) : MessageToken()
    data class ThirdPartyEmoteToken(
        val emote: GenericEmote,
        val overlays: List<GenericEmote> = emptyList()
    ) : MessageToken()
    data class Link(val url: String, val displayText: String) : MessageToken()
    data class Mention(val username: String) : MessageToken()
    data class Cheer(val prefix: String, val amount: Int) : MessageToken()
    data class GifToken(
        val id: String,
        val url: String,
        val title: String
    ) : MessageToken()
}

fun MessageToken.plainText(): String = when (this) {
    is MessageToken.Text -> text
    is MessageToken.TwitchEmoteToken -> name
    is MessageToken.ThirdPartyEmoteToken -> emote.code
    is MessageToken.Link -> displayText
    is MessageToken.Mention -> username
    is MessageToken.Cheer -> "$prefix$amount"
    is MessageToken.GifToken -> title
}

object MessageTokenizer {
    private val URL_REGEX = Regex(
        """https?://[^\s<>"{}|\\^`\[\]]+""",
        RegexOption.IGNORE_CASE
    )

    private val BARE_LINK_REGEX = Regex(
        """(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+(?:me|gg|tv|com|org|net|io|co|ru|de|fr|uk|us|info|dev|app|xyz|pro|live|stream|chat|link|ly|be)/[^\s<>"{}|\\^`\[\]]+""",
        RegexOption.IGNORE_CASE
    )

    private val CHEER_REGEX = Regex("""^([A-Za-z]+)([1-9]\d{0,6})$""")

    private val OVERLAY_EMOTES = setOf(
        "SoSnowy", "IceCold", "SantaHat", "TopHat",
        "ReinDeer", "CandyCane", "cvMask", "cvHazmat"
    )

    fun tokenize(
        message: ChatMessage,
        channelEmotes: ChannelEmotes,
        currentUsername: String? = null,
        personalEmotes: List<GenericEmote> = emptyList()
    ): List<MessageToken> {
        val text = message.message
        if (text.isEmpty()) return emptyList()

        val anchoredRanges = mutableMapOf<IntRange, MessageToken>()
        message.emotes.forEach { emote ->
            emote.positions.forEach { pos ->
                anchoredRanges[pos.start..pos.end] = MessageToken.TwitchEmoteToken(
                    id = emote.id,
                    name = emote.name,
                    url = emote.imageUrl
                )
            }
        }
        message.gifs.forEach { gif ->
            gif.positions.forEach { pos ->
                anchoredRanges[pos.start..pos.end] = MessageToken.GifToken(
                    id = gif.id,
                    url = gif.url,
                    title = gif.title
                )
            }
        }

        val hasBits = message.bits > 0
        val tokens = mutableListOf<MessageToken>()
        var i = 0

        while (i < text.length) {

            val anchored = anchoredRanges.entries.find { it.key.first == i }
            if (anchored != null) {
                tokens.add(anchored.value)
                i = anchored.key.last + 1
                continue
            }

            val nextAnchorStart = anchoredRanges.keys
                .filter { it.first > i }
                .minByOrNull { it.first }?.first ?: text.length

            val segment = text.substring(i, nextAnchorStart)
            tokens.addAll(
                tokenizeSegment(segment, channelEmotes, currentUsername, personalEmotes, hasBits)
            )
            i = nextAnchorStart
        }

        return adjustOverlayEmotes(tokens)
    }

    private fun tokenizeSegment(
        segment: String,
        channelEmotes: ChannelEmotes,
        currentUsername: String?,
        personalEmotes: List<GenericEmote>,
        hasBits: Boolean
    ): List<MessageToken> {
        if (segment.isEmpty()) return emptyList()

        val tokens = mutableListOf<MessageToken>()
        val words = segment.split(" ")
        val emoteMap = if (personalEmotes.isNotEmpty()) {
            buildMap {
                putAll(channelEmotes.allByCode)
                personalEmotes.forEach { put(it.code, it) }
            }
        } else channelEmotes.allByCode

        for ((index, word) in words.withIndex()) {
            if (word.isEmpty()) {
                if (index < words.lastIndex) tokens.add(MessageToken.Text(" "))
                continue
            }

            val cheer = if (hasBits && emoteMap[word] == null) {
                CHEER_REGEX.matchEntire(word)
            } else null

            val emote = emoteMap[word]
            when {
                cheer != null -> {
                    tokens.add(
                        MessageToken.Cheer(
                            prefix = cheer.groupValues[1],
                            amount = cheer.groupValues[2].toIntOrNull() ?: 0
                        )
                    )
                }
                emote != null -> {
                    tokens.add(MessageToken.ThirdPartyEmoteToken(emote))
                }
                URL_REGEX.matches(word) -> {
                    tokens.add(MessageToken.Link(word, word))
                }
                BARE_LINK_REGEX.matches(word) -> {
                    tokens.add(MessageToken.Link("https://$word", word))
                }
                word.startsWith("@") && word.length >= 4 -> {
                    val core = word.removePrefix("@")
                    val endIdx = core.indexOfFirst { c ->
                        !(c.isLetterOrDigit() || c == '_')
                    }.let { if (it < 0) core.length else it }
                    if (endIdx >= 3) {
                        val login = core.substring(0, endIdx)
                        tokens.add(MessageToken.Mention("@$login"))
                        if (endIdx < core.length) {
                            tokens.add(MessageToken.Text(core.substring(endIdx)))
                        }
                    } else {
                        tokens.add(MessageToken.Text(word))
                    }
                }
                else -> {
                    tokens.add(MessageToken.Text(word))
                }
            }

            if (index < words.lastIndex) {
                tokens.add(MessageToken.Text(" "))
            }
        }

        return tokens
    }

    private fun adjustOverlayEmotes(tokens: List<MessageToken>): List<MessageToken> {
        val result = mutableListOf<MessageToken>()

        for (token in tokens) {
            if (token is MessageToken.ThirdPartyEmoteToken &&
                (token.emote.isZeroWidth || token.emote.code in OVERLAY_EMOTES)
            ) {

                val lastEmoteIndex = result.indexOfLast {
                    it is MessageToken.ThirdPartyEmoteToken || it is MessageToken.TwitchEmoteToken
                }
                if (lastEmoteIndex >= 0 && result[lastEmoteIndex] is MessageToken.ThirdPartyEmoteToken) {
                    val base = result[lastEmoteIndex] as MessageToken.ThirdPartyEmoteToken
                    result[lastEmoteIndex] = base.copy(overlays = base.overlays + token.emote)

                    if (result.lastIndex > lastEmoteIndex && result.last() is MessageToken.Text &&
                        (result.last() as MessageToken.Text).text == " "
                    ) {
                        result.removeAt(result.lastIndex)
                    }
                    continue
                }
            }
            result.add(token)
        }

        return result
    }
}
