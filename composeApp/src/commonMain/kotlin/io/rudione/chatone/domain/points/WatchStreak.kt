package io.rudione.chatone.domain.points

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

@Immutable
data class WatchStreakReward(val streakLength: Int, val points: Int)

@Immutable
data class WatchStreak(
    val current: Int,
    val rewards: List<WatchStreakReward>,
    val countedThisStream: Boolean = false
) {

    val nextReward: WatchStreakReward?
        get() = rewards.filter { it.streakLength > current }.minByOrNull { it.streakLength }

    val streamsUntilNextReward: Int?
        get() = nextReward?.let { it.streakLength - current }

    val progressToNextReward: Float
        get() {
            val next = nextReward ?: return 1f
            val previous = rewards.filter { it.streakLength <= current }.maxOfOrNull { it.streakLength } ?: 0
            val span = (next.streakLength - previous).coerceAtLeast(1)
            return ((current - previous).toFloat() / span).coerceIn(0f, 1f)
        }
}

object WatchStreakCounting {
    fun countedThisStream(achievedAt: String?, streamStartedAt: String?): Boolean {
        val achieved = achievedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        val started = streamStartedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return achieved >= started
    }
}

interface WatchStreakSource {
    val streaks: StateFlow<Map<String, WatchStreak>>
    suspend fun refresh(channelLogin: String)
}

interface WatchActivitySink {
    fun onPlayback(channelLogin: String?, playing: Boolean)

    object None : WatchActivitySink {
        override fun onPlayback(channelLogin: String?, playing: Boolean) = Unit
    }
}
