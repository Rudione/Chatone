package io.rudione.chatone.util

import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.GenericEmote

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

    private val OVERLAY_EMOTES = setOf(
        "SoSnowy", "IceCold", "SantaHat", "TopHat",
        "ReinDeer", "CandyCane", "cvMask", "cvHazmat"
    )

    fun tokenize(
        message: ChatMessage,
        channelEmotes: ChannelEmotes,
        currentUsername: String? = null
    ): List<MessageToken> {
        val text = message.message
        if (text.isEmpty()) return emptyList()


        val twitchEmoteRanges = mutableMapOf<IntRange, MessageToken.TwitchEmoteToken>()
        message.emotes.forEach { emote ->
            emote.positions.forEach { pos ->
                val range = pos.start..pos.end
                twitchEmoteRanges[range] = MessageToken.TwitchEmoteToken(
                    id = emote.id,
                    name = emote.name,
                    url = emote.imageUrl
                )
            }
        }


        val tokens = mutableListOf<MessageToken>()
        var i = 0

        while (i < text.length) {

            val twitchEntry = twitchEmoteRanges.entries.find { it.key.first == i }
            if (twitchEntry != null) {
                tokens.add(twitchEntry.value)
                i = twitchEntry.key.last + 1
                continue
            }


            val nextTwitchStart = twitchEmoteRanges.keys
                .filter { it.first > i }
                .minByOrNull { it.first }?.first ?: text.length

            val segment = text.substring(i, nextTwitchStart)
            tokens.addAll(tokenizeSegment(segment, channelEmotes, currentUsername))
            i = nextTwitchStart
        }

        return adjustOverlayEmotes(tokens)
    }

    private fun tokenizeSegment(
        segment: String,
        channelEmotes: ChannelEmotes,
        currentUsername: String?
    ): List<MessageToken> {
        if (segment.isEmpty()) return emptyList()

        val tokens = mutableListOf<MessageToken>()
        val words = segment.split(" ")
        val emoteMap = channelEmotes.allByCode

        for ((index, word) in words.withIndex()) {
            if (word.isEmpty()) {
                if (index < words.lastIndex) tokens.add(MessageToken.Text(" "))
                continue
            }

            val emote = emoteMap[word]
            when {
                emote != null -> {
                    tokens.add(MessageToken.ThirdPartyEmoteToken(emote))
                }
                URL_REGEX.matches(word) -> {
                    tokens.add(MessageToken.Link(word, word))
                }
                BARE_LINK_REGEX.matches(word) -> {
                    tokens.add(MessageToken.Link("https://$word", word))
                }
                currentUsername != null && word.equals("@$currentUsername", ignoreCase = true) -> {
                    tokens.add(MessageToken.Mention(word))
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
