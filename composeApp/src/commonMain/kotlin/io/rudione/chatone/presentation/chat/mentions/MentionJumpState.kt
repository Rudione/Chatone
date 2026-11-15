package io.rudione.chatone.presentation.chat.mentions

import androidx.compose.runtime.Composable
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

const val MENTION_BUTTON_TIMEOUT_MS = 10_000L

@Stable
class MentionJumpState internal constructor(
    private val channelLogin: String,
    private val store: MentionReadStore,
    private val clock: () -> Long,
    private val timeoutMs: Long = MENTION_BUTTON_TIMEOUT_MS
) {
    private var known by mutableStateOf<List<MentionRef>>(emptyList())
    private var mark by mutableStateOf<MentionReadMark?>(null)
    private var lingering by mutableStateOf<List<MentionRef>>(emptyList())
    private var onScreen by mutableStateOf<Set<String>>(emptySet())
    private var attentive by mutableStateOf(true)
    private var deadlineMs by mutableStateOf<Long?>(null)

    val pending: List<MentionRef> by derivedStateOf { mark?.unread(known).orEmpty() }

    val pendingIds: Set<String> by derivedStateOf { pending.mapTo(HashSet()) { it.messageId } }

    private val targets: List<MentionRef> by derivedStateOf {
        (pending + lingering).distinctBy { it.messageId }
    }

    val targetIds: Set<String> by derivedStateOf { targets.mapTo(HashSet()) { it.messageId } }

    internal val pendingOnScreen: Set<String> by derivedStateOf { pendingIds intersect onScreen }

    private val offscreen: List<MentionRef> by derivedStateOf { targets.filterNot { it.messageId in onScreen } }

    val count: Int get() = offscreen.size

    val dismissAtMs: Long? get() = deadlineMs.takeIf { targets.isNotEmpty() }

    fun sync(mentions: List<MentionRef>, newestTimestampMs: Long?) {
        val before = targetIds
        known = mentions
        mark = store.mark(channelLogin)
            ?: mark
            ?: newestTimestampMs?.let { MentionReadMark.startingAt(it).also { start -> store.save(channelLogin, start) } }
        if (lingering.isNotEmpty()) {
            val ids = mentions.mapTo(HashSet()) { it.messageId }
            if (lingering.any { it.messageId !in ids }) {
                lingering = lingering.filter { it.messageId in ids }
            }
        }
        if (targetIds.any { it !in before }) restartTimer()
    }

    fun markSeen(ids: Set<String>, live: Boolean) {
        val fresh = pending.filter { it.messageId in ids }
        if (fresh.isEmpty()) return
        commit(fresh.map { it.messageId })
        if (live) lingering = lingering + fresh
    }

    fun consume(messageId: String) {
        if (messageId in pendingIds) commit(listOf(messageId))
        if (lingering.any { it.messageId == messageId }) {
            lingering = lingering.filterNot { it.messageId == messageId }
        }
    }

    fun consumeAll(): List<MentionRef> {
        val all = targets.sortedBy { it.timestampMs }
        commit(pending.map { it.messageId })
        lingering = emptyList()
        deadlineMs = null
        return all
    }

    fun authors(): List<String> = targets
        .sortedBy { it.timestampMs }
        .map { it.insertionName }
        .distinctBy { it.lowercase() }

    fun jump(indexOf: (String) -> Int, anchorIndex: Int): MentionRef? {
        val waiting = offscreen.filter { it.messageId in pendingIds }
        val target = waiting.ifEmpty { offscreen }
            .mapNotNull { ref -> indexOf(ref.messageId).takeIf { it >= 0 }?.let { ref to it } }
            .minWithOrNull(compareBy({ abs(it.second - anchorIndex) }, { it.second }))
            ?.first
            ?: return null
        consume(target.messageId)
        restartTimer()
        return target
    }

    internal fun updateOnScreen(ids: Set<String>) {
        if (ids != onScreen) onScreen = ids
    }

    internal fun updateAttention(isAttentive: Boolean) {
        if (attentive == isAttentive) return
        attentive = isAttentive
        if (isAttentive) restartTimer() else deadlineMs = null
    }

    internal fun expire() {
        val at = dismissAtMs ?: return
        if (clock() < at) return
        commit(pending.map { it.messageId })
        lingering = emptyList()
        deadlineMs = null
    }

    private fun restartTimer() {
        deadlineMs = if (attentive && targets.isNotEmpty()) clock() + timeoutMs else null
    }

    private fun commit(ids: List<String>) {
        if (ids.isEmpty()) return
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
        snapshotFlow { state.dismissAtMs }.collectLatest { dismissAt ->
            if (dismissAt == null) return@collectLatest
            while (true) {
                val wait = dismissAt - Clock.System.now().toEpochMilliseconds()
                if (wait <= 0L) break
                delay(wait)
            }
            state.expire()
        }
    }
    return state
}
