package io.rudione.chatone.util.emote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class EmotePlayer internal constructor(val frames: AnimatedFrames) {

    private val frameEnds = LongArray(frames.durations.size).also { ends ->
        var total = 0L
        frames.durations.forEachIndexed { index, duration ->
            total += duration.coerceAtLeast(1)
            ends[index] = total
        }
    }
    private val loopMs = frameEnds.lastOrNull()?.coerceAtLeast(1L) ?: 1L

    internal val isAnimated: Boolean = frames.frames.size > 1 && frameEnds.size > 1

    var frameIndex by mutableIntStateOf(frameAt(animationNowMs()))
        private set

    internal var holders = 0

    internal fun advance(nowMs: Long) {
        val index = frameAt(nowMs)
        if (index != frameIndex) {
            frameIndex = index
            frames.lastAccess = System.currentTimeMillis()
        }
    }

    internal fun frameAt(nowMs: Long): Int {
        if (!isAnimated) return 0
        val position = Math.floorMod(nowMs, loopMs)
        var low = 0
        var high = frameEnds.lastIndex
        while (low < high) {
            val mid = (low + high) ushr 1
            if (frameEnds[mid] <= position) low = mid + 1 else high = mid
        }
        return low.coerceAtMost(frames.frames.lastIndex)
    }

    internal fun nextChangeAt(nowMs: Long): Long {
        if (!isAnimated) return Long.MAX_VALUE
        val position = Math.floorMod(nowMs, loopMs)
        return nowMs + (frameEnds[frameAt(nowMs)] - position).coerceAtLeast(1L)
    }
}

object SharedEmoteAnimator {

    private const val FRAME_QUANTUM_MS = 40L
    private const val IDLE_RECHECK_MS = 1_000L

    private val lock = Any()
    private val shared = HashMap<AnimatedFrames, EmotePlayer>()
    private val active = LinkedHashSet<EmotePlayer>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    fun playerFor(frames: AnimatedFrames): EmotePlayer = synchronized(lock) {
        shared.getOrPut(frames) { EmotePlayer(frames) }
    }

    fun retain(player: EmotePlayer) {
        player.frames.lastAccess = System.currentTimeMillis()
        synchronized(lock) {
            if (player.holders++ > 0) return
            shared.putIfAbsent(player.frames, player)
            if (!player.isAnimated) return
            player.advance(animationNowMs())
            active.add(player)
            if (ticker?.isActive != true) ticker = scope.launch { runTicker() }
        }
        wake.trySend(Unit)
    }

    fun release(player: EmotePlayer) = synchronized(lock) {
        if (--player.holders > 0) return@synchronized
        player.holders = 0
        active.remove(player)
        shared.remove(player.frames, player)
        shared.values.removeAll { it.holders == 0 }
    }

    private suspend fun runTicker() {
        while (true) {
            val now = animationNowMs()
            val players = synchronized(lock) {
                if (active.isEmpty()) {
                    ticker = null
                    return
                }
                active.toList()
            }
            var next = Long.MAX_VALUE
            players.forEach { player ->
                player.advance(now)
                next = minOf(next, player.nextChangeAt(now))
            }
            if (next == Long.MAX_VALUE) next = now + IDLE_RECHECK_MS
            val wakeAt = ((next + FRAME_QUANTUM_MS - 1) / FRAME_QUANTUM_MS) * FRAME_QUANTUM_MS
            withTimeoutOrNull((wakeAt - animationNowMs()).coerceAtLeast(1L)) { wake.receive() }
        }
    }
}

internal fun animationNowMs(): Long = System.nanoTime() / 1_000_000L
