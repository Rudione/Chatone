package io.rudione.chatone.domain.points

object WatchMinutePlan {
    const val MINUTE_MS = 60_000L
    val streakRefreshMinutes: Set<Int> = setOf(1, 5, 10, 20, 40)

    fun shouldRefreshStreak(minutesWatched: Int): Boolean = minutesWatched in streakRefreshMinutes
}
