package io.rudione.chatone.presentation.chat.timeline

import io.rudione.chatone.data.repository.EmoteRepository
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.util.chat.MessageToken
import io.rudione.chatone.util.chat.MessageTokenizer
import kotlinx.datetime.TimeZone
import kotlin.concurrent.Volatile

internal class PastMessageRenderer(private val emotes: EmoteRepository) {

    @Volatile
    private var rendered = RenderedPast(null, null, emptyMap())

    fun rows(entries: List<TimelineEntry>, zone: TimeZone, author: DisplayMessage.PrivMsg): List<TimelineRow> {
        val emoteSet = emotes.getResolvedEmotes(author.channel)
        val look = AuthorLook.of(author)
        val previous = rendered
        val known = if (previous.emoteSet === emoteSet && previous.look == look) previous.messages else emptyMap()
        val fresh = HashMap<String, DisplayMessage.PrivMsg>(known.size + 64)
        val rows = ProfileTimeline.rows(entries, zone) { past ->
            val cached = known[past.id]?.takeIf { it.isDeleted == past.deleted && it.rawMessage?.message == past.text }
            (cached ?: render(author, past)).also { fresh[past.id] = it }
        }
        rendered = RenderedPast(emoteSet, look, fresh)
        return rows
    }

    private fun render(author: DisplayMessage.PrivMsg, past: TimelineEntry.Past): DisplayMessage.PrivMsg {
        val raw = ChatMessage(
            id = past.id,
            channelId = author.rawMessage?.channelId.orEmpty(),
            channelName = author.channel,
            userId = author.userId,
            username = author.username,
            displayName = author.displayName,
            message = past.text,
            timestamp = past.timestamp,
            color = author.color
        )
        val tokens = runCatching {
            MessageTokenizer.tokenize(
                raw,
                emotes.getResolvedEmotes(author.channel),
                personalEmotes = emotes.getCachedPersonalEmotes(author.userId)
            )
        }.getOrElse { listOf(MessageToken.Text(past.text)) }
        return author.copy(
            id = past.id,
            timestamp = past.timestamp,
            tokens = tokens,
            isDeleted = past.deleted,
            isMention = false,
            isAction = false,
            isFirstMessage = false,
            isHighlighted = false,
            customRewardId = null,
            rewardName = null,
            rawMessage = raw,
            highlightColor = null
        )
    }

    private data class AuthorLook(val author: DisplayMessage.PrivMsg, val channelId: String?) {
        companion object {
            fun of(author: DisplayMessage.PrivMsg) = AuthorLook(
                author.copy(
                    id = "",
                    timestamp = 0L,
                    tokens = emptyList(),
                    isMention = false,
                    isAction = false,
                    isFirstMessage = false,
                    isHighlighted = false,
                    customRewardId = null,
                    rewardName = null,
                    isDeleted = false,
                    rawMessage = null,
                    highlightColor = null
                ),
                author.rawMessage?.channelId
            )
        }
    }

    private class RenderedPast(
        val emoteSet: Any?,
        val look: AuthorLook?,
        val messages: Map<String, DisplayMessage.PrivMsg>
    )
}
