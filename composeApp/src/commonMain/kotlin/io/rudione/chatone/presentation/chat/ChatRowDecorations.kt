package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.chat.rendering.ChatterColorIndex
import io.rudione.chatone.presentation.chat.rendering.ChatterColors
import io.rudione.chatone.util.automod.relativeSimilarity
import io.rudione.chatone.util.chat.plainText
import kotlin.time.Clock

private const val REPEAT_SIMILARITY_THRESHOLD = 0.85f
private const val REPEAT_SCAN_LIMIT = 300

@Immutable
internal class ChatRowDecorations(
    val zebraParityById: Map<String, Boolean>,
    val userColorByLogin: ChatterColors,
    val repeatCountByMsgId: Map<String, Int>,
    val dayBreakById: Map<String, Long>
)

@Composable
internal fun rememberChatRowDecorations(
    state: ChatState,
    dedupedMessages: List<DisplayMessage>,
    channelLogin: String,
    alternateRows: Boolean,
    showRepeatCounter: Boolean,
    repeatWindowSeconds: Int
): ChatRowDecorations {
    val repeatWindowMs = repeatWindowSeconds.coerceAtLeast(1) * 1000L
    val zebraParityById = remember(state.messagesSeq, state.messagesStartOrdinal, alternateRows) {
        if (!alternateRows) {
            emptyMap()
        } else {
            buildMap {
                var ordinal = state.messagesStartOrdinal
                state.messages.forEach { message ->
                    if (message is DisplayMessage.NotableChatterMsg) return@forEach
                    put(message.id, ordinal and 1L == 1L)
                    ordinal++
                }
            }
        }
    }
    val chatterColorIndex = remember(channelLogin) { ChatterColorIndex() }
    val userColorByLogin: ChatterColors = remember(chatterColorIndex, state.messagesSeq) {
        chatterColorIndex.apply { absorb(state.messages) }
    }
    val repeatCountByMsgId = remember(dedupedMessages, showRepeatCounter, repeatWindowMs) {
        if (showRepeatCounter) repeatCounts(dedupedMessages, repeatWindowMs) else emptyMap()
    }
    val dayIndex = remember(channelLogin) { ChatDayIndex() }
    val dayBreakById = remember(dayIndex, dedupedMessages) {
        dayIndex.breaks(dedupedMessages, Clock.System.now().toEpochMilliseconds())
    }
    return remember(zebraParityById, userColorByLogin, repeatCountByMsgId, dayBreakById) {
        ChatRowDecorations(zebraParityById, userColorByLogin, repeatCountByMsgId, dayBreakById)
    }
}

private fun repeatCounts(messages: List<DisplayMessage>, windowMs: Long): Map<String, Int> = buildMap {
    val scanned = if (messages.size > REPEAT_SCAN_LIMIT) {
        messages.subList(messages.size - REPEAT_SCAN_LIMIT, messages.size)
    } else {
        messages
    }
    val recent = ArrayDeque<Pair<String, Long>>()
    for (message in scanned) {
        if (message !is DisplayMessage.PrivMsg) continue
        val timestamp = message.timestamp
        val normalized = message.tokens.joinToString("") { it.plainText() }.trim().lowercase()
        if (normalized.isEmpty()) continue
        while (recent.isNotEmpty() && timestamp - recent.first().second > windowMs) {
            recent.removeFirst()
        }
        val count = recent.count {
            it.first == normalized ||
                relativeSimilarity(it.first, normalized, atLeast = REPEAT_SIMILARITY_THRESHOLD) >= REPEAT_SIMILARITY_THRESHOLD
        } + 1
        if (count > 1) put(message.id, count)
        recent.addLast(normalized to timestamp)
    }
}
