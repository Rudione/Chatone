package io.rudione.chatone.domain.points

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchStreakTest {

    private val rewards = listOf(
        WatchStreakReward(2, 300),
        WatchStreakReward(3, 350),
        WatchStreakReward(4, 400),
        WatchStreakReward(5, 450)
    )

    @Test
    fun freshStreakPointsAtTheFirstReward() {
        val streak = WatchStreak(current = 0, rewards = rewards)
        assertEquals(WatchStreakReward(2, 300), streak.nextReward)
        assertEquals(2, streak.streamsUntilNextReward)
        assertEquals(0f, streak.progressToNextReward)
    }

    @Test
    fun progressCountsFromThePreviousReward() {
        val streak = WatchStreak(current = 3, rewards = rewards)
        assertEquals(WatchStreakReward(4, 400), streak.nextReward)
        assertEquals(1, streak.streamsUntilNextReward)
        assertEquals(0f, streak.progressToNextReward)
        assertEquals(0.5f, WatchStreak(current = 1, rewards = rewards).progressToNextReward)
    }

    @Test
    fun beyondTheLastRewardThereIsNothingNext() {
        val streak = WatchStreak(current = 9, rewards = rewards.shuffled())
        assertNull(streak.nextReward)
        assertNull(streak.streamsUntilNextReward)
        assertEquals(1f, streak.progressToNextReward)
    }

    @Test
    fun streakIsRefreshedOnlyAtPlannedMinutes() {
        assertTrue(WatchMinutePlan.shouldRefreshStreak(5))
        assertFalse(WatchMinutePlan.shouldRefreshStreak(6))
    }
}
