package io.rudione.chatone.presentation.chat.mentions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.rudione.chatone.domain.mention.MentionReadMark
import io.rudione.chatone.domain.mention.MentionReadStore
import io.rudione.chatone.domain.mention.MentionRef
import io.rudione.chatone.domain.model.DisplayMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.time.Clock

const val MENTION_LINGER_MS = 10_000L

@Immutable
internal data class LingeringMention(val mention: MentionRef, val untilMs: Long)

@Stable
class MentionJumpState internal constructor(
    private val channelLogin: String,
    private val store: MentionReadStore,
    private val clock: () -> Long,
    private val lingerMs: Long = MENTION_LINGER_MS
) {
    private var known by mutableStateOf<List<MentionRef>>(emptyList())
    private var mark by mutableStateOf<MentionReadMark?>(null)
    private var lingering by mutableStateOf<List<LingeringMention>>(emptyList())

    val pending: List<MentionRef> by derivedStateOf { mark?.unread(known).orEmpty() }

    val pendingIds: Set<String> by derivedStateOf { pending.mapTo(HashSet()) { it.messageId } }

    val count: Int get() = pending.size + lingering.size

    val lingerEndsAtMs: Long?
        get() = if (pending.isEmpty()) lingering.maxOfOrNull { it.untilMs } else null

    internal val nextExpiryMs: Long? get() = lingering.minOfOrNull { it.untilMs }

    fun sync(mentions: List<MentionRef>, newestTimestampMs: Long?) {
        known = mentions
        mark = store.mark(channelLogin)
            ?: mark
            ?: newestTimestampMs?.let { MentionReadMark.startingAt(it).also { start -> store.save(channelLogin, start) } }
        if (lingering.isNotEmpty()) {
            val ids = mentions.mapTo(HashSet()) { it.messageId }
            if (lingering.any { it.mention.messageId !in ids }) {
                lingering = lingering.filter { it.mention.messageId in ids }
            }
        }
    }

    fun markSeen(ids: Set<String>, live: Boolean) {
        val fresh = pending.filter { it.messageId in ids }
        if (fresh.isEmpty()) return
        commit(fresh.map { it.messageId })
        if (live) {
            val until = clock() + lingerMs
            lingering = lingering + fresh.map { LingeringMention(it, until) }
        }
    }

    fun consume(messageId: String) {
        if (messageId in pendingIds) commit(listOf(messageId))
        if (lingering.any { it.mention.messageId == messageId }) {
            lingering = lingering.filterNot { it.mention.messageId == messageId }
        }
    }

    fun consumeAll(): List<MentionRef> {
        val all = (pending + lingering.map { it.mention })
            .distinctBy { it.messageId }
            .sortedBy { it.timestampMs }
        commit(pending.map { it.messageId })
        lingering = emptyList()
        return all
    }

    fun authors(): List<String> = (pending + lingering.map { it.mention })
        .sortedBy { it.timestampMs }
        .map { it.insertionName }
        .distinctBy { it.lowercase() }

    fun nearest(indexOf: (String) -> Int, anchorIndex: Int): MentionRef? {
        val candidates = pending.ifEmpty { lingering.map { it.mention } }
        return candidates
            .mapNotNull { ref -> indexOf(ref.messageId).takeIf { it >= 0 }?.let { ref to it } }
            .minWithOrNull(compareBy({ abs(it.second - anchorIndex) }, { it.second }))
            ?.first
    }

    internal fun expire() {
        val now = clock()
        if (lingering.any { it.untilMs <= now }) lingering = lingering.filter { it.untilMs > now }
    }

    private fun commit(ids: List<String>) {
        val current = store.mark(channelLogin) ?: mark ?: return
        val next = current.withRead(ids, known)
        if (next == current) return
        mark = next
        store.save(channelLogin, next)
    }
}

fun mentionRefsOf(
    messages: List<DisplayMessage>,
    isMuted: (login: String) -> Boolean
): List<MentionRef> = messages.mapNotNull { message ->
    val msg = message as? DisplayMessage.PrivMsg ?: return@mapNotNull null
    if (!msg.isMention || msg.isDeleted || isMuted(msg.username)) return@mapNotNull null
    MentionRef(
        messageId = msg.id,
        timestampMs = msg.timestamp,
        authorLogin = msg.username,
        authorDisplayName = msg.displayName
    )
}

@Composable
fun rememberMentionJumpState(
    channelLogin: String,
    mentions: List<MentionRef>?,
    newestTimestampMs: Long?
): MentionJumpState {
    val store = koinInject<MentionReadStore>()
    val state = remember(channelLogin, store) {
        MentionJumpState(channelLogin, store, clock = { Clock.System.now().toEpochMilliseconds() })
    }
    LaunchedEffect(state, mentions, newestTimestampMs) {
        if (mentions != null) state.sync(mentions, newestTimestampMs)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.nextExpiryMs }.collectLatest { expiresAt ->
            if (expiresAt == null) return@collectLatest
            while (true) {
                val wait = expiresAt - Clock.System.now().toEpochMilliseconds()
                if (wait <= 0L) break
                delay(wait)
            }
            state.expire()
        }
    }
    return state
}
