package io.rudione.chatone.presentation.liveactivity

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveEventActivitiesTest {

    private val started = mutableListOf<LiveChatEvent?>()

    private fun event(id: String, phase: String = "ACTIVE") = LiveChatEvent(
        id = id,
        kind = "prediction",
        channel = "streamer",
        title = "Win?",
        phase = phase,
        deadlineMs = 1_000L,
        options = listOf("Yes", "No"),
        totals = listOf(1, 2),
        updatedAtMs = 0L
    )

    @BeforeTest
    fun setUp() {
        LiveEventActivities.available.value = true
        LiveEventActivities.onFollow = { started += it }
    }

    @AfterTest
    fun tearDown() {
        LiveEventActivities.clearSelection()
        LiveEventActivities.onFollow = null
        LiveEventActivities.available.value = false
    }

    @Test
    fun startsOnItsOwnWhenNothingIsShown() {
        LiveEventActivities.autoFollow(event("a"))
        assertEquals("a", LiveEventActivities.followed.value?.id)
        assertEquals(listOf("a"), started.map { it?.id })
    }

    @Test
    fun doesNotReplaceAnEventThatIsStillRunning() {
        LiveEventActivities.autoFollow(event("a"))
        LiveEventActivities.autoFollow(event("b"))
        assertEquals("a", LiveEventActivities.followed.value?.id)
        LiveEventActivities.observed(event("a", phase = "RESOLVED"))
        LiveEventActivities.autoFollow(event("b"))
        assertEquals("b", LiveEventActivities.followed.value?.id)
    }

    @Test
    fun staysAwayFromAnEventTheUserSwipedOff() {
        val swiped = event("swiped")
        LiveEventActivities.autoFollow(swiped)
        LiveEventActivities.dismissedByUser(swiped.key)
        LiveEventActivities.clearSelection()
        LiveEventActivities.autoFollow(swiped)
        assertNull(LiveEventActivities.followed.value)
    }

    @Test
    fun doesNotRetryAnEventThatFailedToStart() {
        LiveEventActivities.autoFollow(event("broken"))
        LiveEventActivities.failed()
        LiveEventActivities.autoFollow(event("broken"))
        assertNull(LiveEventActivities.followed.value)
        assertEquals(listOf("broken"), started.map { it?.id })
        LiveEventActivities.autoFollow(event("next"))
        assertEquals("next", LiveEventActivities.followed.value?.id)
    }

    @Test
    fun doesNothingWhereLiveActivitiesAreUnavailable() {
        LiveEventActivities.available.value = false
        LiveEventActivities.autoFollow(event("c"))
        assertNull(LiveEventActivities.followed.value)
    }
}
